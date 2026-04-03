package com.reskyu.consumer.data.remote

/**
 * CloudinaryConfig
 *
 * Holds the configuration values for Cloudinary image hosting.
 * Used when displaying listing images fetched from Firestore (`imageUrl` field).
 *
 * Note: The consumer app only READS images — merchants upload them.
 * All Cloudinary URLs are stored directly in the Firestore `listings` documents.
 *
 * TODO: Replace with your actual Cloudinary cloud name.
 */
object CloudinaryConfig {

    // Your Cloudinary cloud name (find it in your Cloudinary dashboard)
    const val CLOUD_NAME = "your_cloud_name"

    // Base URL pattern for Cloudinary image delivery
    // Usage: "$BASE_DELIVERY_URL/image/upload/$imagePublicId"
    const val BASE_DELIVERY_URL = "https://res.cloudinary.com/$CLOUD_NAME"

    /**
     * Builds a Cloudinary URL with optional transformations.
     * Example: getImageUrl("sample", width = 400) → optimized 400px wide URL
     *
     * @param publicId  The public ID of the image stored in Cloudinary
     * @param width     Optional width for responsive image delivery
     */
    fun getImageUrl(publicId: String, width: Int? = null): String {
        val transform = if (width != null) "w_$width,c_fill,q_auto,f_auto/" else "q_auto,f_auto/"
        return "$BASE_DELIVERY_URL/image/upload/$transform$publicId"
    }
}
