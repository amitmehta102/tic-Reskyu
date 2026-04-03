package com.reskyu.merchant.data.model

/**
 * Sealed class tracking the image upload state to Cloudinary.
 */
sealed class UploadState {
    object Idle : UploadState()
    object Uploading : UploadState()
    data class Success(val imageUrl: String) : UploadState()
    data class Error(val message: String) : UploadState()
}
