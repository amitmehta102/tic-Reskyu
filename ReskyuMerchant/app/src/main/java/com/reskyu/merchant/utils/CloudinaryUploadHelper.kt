package com.reskyu.merchant.utils

import android.content.Context
import android.net.Uri
import com.reskyu.merchant.data.remote.CloudinaryConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Helper for uploading images to Cloudinary using unsigned uploads.
 *
 * Usage:
 *   val url = CloudinaryUploadHelper.upload(context, imageUri)
 *
 * ⚠️ Requires OkHttp dependency in build.gradle.kts.
 * ⚠️ Replace [CloudinaryConfig.CLOUD_NAME] and [CloudinaryConfig.UPLOAD_PRESET] with real values.
 */
object CloudinaryUploadHelper {

    private val client = OkHttpClient()

    /**
     * Uploads the image at [uri] to Cloudinary and returns the secure image URL.
     *
     * @param context  Android Context (needed to resolve the content URI).
     * @param uri      Content URI from camera/gallery picker.
     * @return         The Cloudinary secure_url for the uploaded image.
     * @throws         Exception on network or parse failure.
     */
    suspend fun upload(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        // Copy URI content to a temp file
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI: $uri")
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { inputStream.copyTo(it) }

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                tempFile.name,
                tempFile.asRequestBody("image/*".toMediaTypeOrNull())
            )
            .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
            .build()

        val request = Request.Builder()
            .url(CloudinaryConfig.BASE_UPLOAD_URL)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()
            ?: throw Exception("Empty response from Cloudinary")

        tempFile.delete()

        val json = JSONObject(responseBody)
        json.getString("secure_url")
    }
}
