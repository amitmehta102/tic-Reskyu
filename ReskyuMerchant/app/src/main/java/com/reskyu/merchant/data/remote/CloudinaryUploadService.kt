package com.reskyu.merchant.data.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Uploads images to Cloudinary via the custom proxy API:
 *   POST https://cloudnary-api.onrender.com/upload
 *
 * The server handles Cloudinary credentials — we just multipart-POST the file bytes.
 *
 * Expected JSON response:
 *   { "secure_url": "https://res.cloudinary.com/.../image.jpg", ... }
 *
 * Retry policy: up to [MAX_RETRIES] attempts with [RETRY_DELAY_MS] between each.
 * Render free tier cold-starts can cause the first request to fail.
 */
class CloudinaryUploadService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)   // Render cold-start can take ~30s
        .writeTimeout(90, TimeUnit.SECONDS)      // Large images need more write time
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val uploadEndpoint = "https://cloudnary-api.onrender.com/upload"

    companion object {
        private const val TAG           = "CloudinaryUpload"
        private const val MAX_RETRIES   = 2
        private const val RETRY_DELAY_MS = 3000L
    }

    /**
     * Uploads the image at [uri] to Cloudinary via the proxy.
     * Retries up to [MAX_RETRIES] times on failure (handles Render cold-starts).
     *
     * @param context  Used to open the content-URI input stream.
     * @param uri      The local Android content URI selected from gallery.
     * @return         The public Cloudinary `secure_url` for use in the listing.
     * @throws IOException on network / parse error after all retries exhausted.
     */
    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {

        // ── Read image bytes once (before retry loop) ────────────────────────
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open image: check storage permission")
        val imageBytes = inputStream.use { it.readBytes() }

        if (imageBytes.isEmpty()) throw IOException("Selected image is empty")

        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png"  -> "png"
            "image/webp" -> "webp"
            else         -> "jpg"
        }

        Log.d(TAG, "Uploading ${imageBytes.size / 1024}KB $mimeType image…")

        // ── Retry loop ───────────────────────────────────────────────────────
        var lastError: Exception = IOException("Upload failed")

        repeat(MAX_RETRIES + 1) { attempt ->
            if (attempt > 0) {
                Log.w(TAG, "Retry $attempt after failure: ${lastError.message}")
                delay(RETRY_DELAY_MS)
            }
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name     = "file",
                        filename = "listing_image.$extension",
                        body     = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url(uploadEndpoint)
                    .post(requestBody)
                    .build()

                val responseBody = client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val msg = when (response.code) {
                            404  -> "Upload server not found (404) — check Render deployment"
                            413  -> "Image too large — try a smaller photo"
                            500  -> "Server error — try again in a moment"
                            else -> "HTTP ${response.code}: ${response.message}"
                        }
                        throw IOException(msg)
                    }
                    response.body?.string()
                        ?: throw IOException("Empty response from upload server")
                }

                // ── Parse secure_url ─────────────────────────────────────────
                val secureUrl = try {
                    JSONObject(responseBody).getString("secure_url")
                } catch (e: Exception) {
                    throw IOException("Unexpected server response: $responseBody")
                }

                Log.d(TAG, "Upload success: $secureUrl")
                return@withContext secureUrl      // ← success, exit retry loop

            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Upload attempt ${attempt + 1} failed: ${e.message}")
            }
        }

        // All retries exhausted
        throw IOException("Upload failed after ${MAX_RETRIES + 1} attempts: ${lastError.message}")
    }
}
