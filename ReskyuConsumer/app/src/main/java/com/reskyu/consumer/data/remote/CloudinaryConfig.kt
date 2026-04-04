package com.reskyu.consumer.data.remote

import com.reskyu.consumer.BuildConfig

/**
 * CloudinaryConfig
 *
 * Cloudinary is used for listing image hosting. The consumer app only READS images —
 * merchants upload them via the merchant app. imageUrl is stored directly in
 * Firestore listing documents and loaded by Coil.
 *
 * Cloud name is read from BuildConfig (sourced from local.properties).
 */
object CloudinaryConfig {

    val CLOUD_NAME: String get() = BuildConfig.CLOUDINARY_CLOUD_NAME

    val BASE_DELIVERY_URL: String get() = "https://res.cloudinary.com/$CLOUD_NAME"

    /**
     * Builds an optimised Cloudinary delivery URL.
     * If the imageUrl is already a full https://res.cloudinary.com/... URL,
     * it is returned as-is (Firestore stores full URLs from the merchant app).
     *
     * @param imageUrl  Full Cloudinary URL or a legacy publicId string
     * @param width     Optional pixel width for responsive delivery
     */
    fun buildUrl(imageUrl: String, width: Int? = null): String {
        if (imageUrl.startsWith("https://res.cloudinary.com/")) return imageUrl
        val transform = if (width != null) "w_$width,c_fill,q_auto,f_auto/" else "q_auto,f_auto/"
        return "$BASE_DELIVERY_URL/image/upload/$transform$imageUrl"
    }
}
