package com.reskyu.consumer.data.remote

import com.reskyu.consumer.BuildConfig

object CloudinaryConfig {

    val CLOUD_NAME: String get() = BuildConfig.CLOUDINARY_CLOUD_NAME

    val BASE_DELIVERY_URL: String get() = "https://res.cloudinary.com/$CLOUD_NAME"

    fun buildUrl(imageUrl: String, width: Int? = null): String {
        if (imageUrl.startsWith("https://res.cloudinary.com/")) return imageUrl
        val transform = if (width != null) "w_$width,c_fill,q_auto,f_auto/" else "q_auto,f_auto/"
        return "$BASE_DELIVERY_URL/image/upload/$transform$imageUrl"
    }
}
