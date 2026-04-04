package com.reskyu.merchant.data.remote

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
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
 */
class CloudinaryUploadService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)   // image upload can be slow
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val uploadEndpoint = "https://cloudnary-api.onrender.com/upload"

    /**
     * Uploads the image at [uri] to Cloudinary via the proxy.
     *
     * @param context  Used to open the content-URI input stream.
     * @param uri      The local Android content URI selected from gallery.
     * @return         The public Cloudinary `secure_url` for use in the listing.
     * @throws IOException on any network or parse error.
     */
    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // ── Read image bytes from ContentResolver ────────────────────────────
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Could not open image URI: $uri")
        val imageBytes = inputStream.use { it.readBytes() }

        // Infer MIME type — default to JPEG
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = when (mimeType) {
            "image/png"  -> "png"
            "image/webp" -> "webp"
            else         -> "jpg"
        }

        // ── Build multipart body ─────────────────────────────────────────────
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name     = "file",
                filename = "listing_image.$extension",
                body     = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            )
            .build()

        // ── POST to proxy ────────────────────────────────────────────────────
        val request = Request.Builder()
            .url(uploadEndpoint)
            .post(requestBody)
            .build()

        val responseBody = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Cloudinary upload failed: HTTP ${response.code} — ${response.message}")
            }
            response.body?.string()
                ?: throw IOException("Empty response from Cloudinary proxy")
        }

        // ── Parse secure_url ─────────────────────────────────────────────────
        return@withContext try {
            JSONObject(responseBody).getString("secure_url")
        } catch (e: Exception) {
            throw IOException("Could not parse Cloudinary response: $responseBody")
        }
    }
}
