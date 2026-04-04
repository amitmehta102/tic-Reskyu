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
 * Uploads images to Cloudinary using a 2-step signed-upload flow:
 *
 *   Step 1 — GET https://cloudnary-api.onrender.com/api/get-signature
 *             Returns { timestamp, signature, folder, apiKey, cloudName }
 *
 *   Step 2 — POST https://api.cloudinary.com/v1_1/{cloudName}/image/upload
 *             Multipart form with: file, api_key, timestamp, signature, folder
 *             Returns { secure_url, ... }
 *
 * This avoids storing Cloudinary secrets in the Android app entirely.
 * All secrets stay on the Render server.
 */
class CloudinaryUploadService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)   // Render cold-start can take ~30s
        .writeTimeout(90, TimeUnit.SECONDS)      // Large image upload to Cloudinary
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private val signatureEndpoint = "https://cloudnary-api.onrender.com/api/get-signature"
    private val cloudinaryApiBase = "https://api.cloudinary.com/v1_1"

    companion object {
        private const val TAG            = "CloudinaryUpload"
        private const val MAX_RETRIES    = 2
        private const val RETRY_DELAY_MS = 3000L
        private const val UPLOAD_FOLDER  = "reskyu/listings"
    }

    /**
     * Uploads the image at [uri] to Cloudinary using signed credentials from the proxy.
     *
     * @param context  Used to open the content-URI input stream.
     * @param uri      The local Android content URI selected from gallery.
     * @return         The public Cloudinary `secure_url` for storing in Firestore.
     * @throws IOException on any network or parse error after all retries.
     */
    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {

        // ── Read image bytes once (before retry loop) ────────────────────────
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open image — check storage permission")
        val imageBytes = inputStream.use { it.readBytes() }

        if (imageBytes.isEmpty()) throw IOException("Selected image appears to be empty")

        val mimeType  = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png"  -> "png"
            "image/webp" -> "webp"
            else         -> "jpg"
        }

        Log.d(TAG, "Starting signed upload: ${imageBytes.size / 1024} KB, type=$mimeType")

        var lastError: Exception = IOException("Upload failed")

        repeat(MAX_RETRIES + 1) { attempt ->
            if (attempt > 0) {
                Log.w(TAG, "Retry $attempt — last error: ${lastError.message}")
                delay(RETRY_DELAY_MS)
            }

            try {
                // ── Step 1: Get signed credentials from Render ────────────────
                Log.d(TAG, "Step 1: fetching upload signature…")
                val sigRequest = Request.Builder()
                    .url("$signatureEndpoint?folder=$UPLOAD_FOLDER")
                    .get()
                    .build()

                val sigJson = client.newCall(sigRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            when (response.code) {
                                404  -> "Signature endpoint not found — check Render deployment"
                                500  -> "Render server error (${response.code})"
                                else -> "Could not get upload signature: HTTP ${response.code}"
                            }
                        )
                    }
                    response.body?.string()
                        ?: throw IOException("Empty response from signature endpoint")
                }

                val sig       = JSONObject(sigJson)
                val timestamp = sig.getString("timestamp")
                val signature = sig.getString("signature")
                val apiKey    = sig.getString("apiKey")
                val cloudName = sig.getString("cloudName")
                val folder    = sig.optString("folder", UPLOAD_FOLDER)

                Log.d(TAG, "Step 1 OK — cloudName=$cloudName, folder=$folder")

                // ── Step 2: Upload directly to Cloudinary API ─────────────────
                Log.d(TAG, "Step 2: uploading to Cloudinary…")
                val uploadUrl = "$cloudinaryApiBase/$cloudName/image/upload"

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        name     = "file",
                        filename = "listing_image.$extension",
                        body     = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
                    )
                    .addFormDataPart("api_key",   apiKey)
                    .addFormDataPart("timestamp", timestamp)
                    .addFormDataPart("signature", signature)
                    .addFormDataPart("folder",    folder)
                    .build()

                val uploadRequest = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .build()

                val uploadJson = client.newCall(uploadRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException(
                            when (response.code) {
                                400  -> "Invalid signature or parameters (HTTP 400)"
                                401  -> "Cloudinary auth failed — check API credentials"
                                413  -> "Image too large — please choose a smaller photo"
                                else -> "Cloudinary upload failed: HTTP ${response.code}"
                            }
                        )
                    }
                    response.body?.string()
                        ?: throw IOException("Empty response from Cloudinary")
                }

                val secureUrl = JSONObject(uploadJson).optString("secure_url", "")
                if (secureUrl.isBlank()) throw IOException("Cloudinary returned no secure_url")

                Log.d(TAG, "Upload success ✅ $secureUrl")
                return@withContext secureUrl     // ← exit retry loop on success

            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Upload attempt ${attempt + 1} failed: ${e.message}")
            }
        }

        throw IOException("Upload failed after ${MAX_RETRIES + 1} attempts: ${lastError.message}")
    }
}
