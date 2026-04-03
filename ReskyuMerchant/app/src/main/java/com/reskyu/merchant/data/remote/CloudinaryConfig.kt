package com.reskyu.merchant.data.remote

/**
 * Cloudinary configuration constants.
 * Replace the placeholder values with your actual Cloudinary credentials.
 *
 * ⚠️ Do NOT store the API Secret here — use it only on a secure backend.
 * Upload from the app should use unsigned presets.
 */
object CloudinaryConfig {
    const val CLOUD_NAME = "your_cloud_name"            // Replace with your cloud name
    const val UPLOAD_PRESET = "your_unsigned_preset"    // Replace with your unsigned preset
    const val BASE_UPLOAD_URL = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
}
