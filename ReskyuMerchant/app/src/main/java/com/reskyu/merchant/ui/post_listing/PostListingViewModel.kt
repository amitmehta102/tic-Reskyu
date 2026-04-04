package com.reskyu.merchant.ui.post_listing

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.PublishState
import com.reskyu.merchant.data.model.UploadState
import com.reskyu.merchant.data.remote.CloudinaryUploadService
import com.reskyu.merchant.data.repository.ListingRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Post Listing screen.
 *
 * Uses [AndroidViewModel] so the Application context is available for
 * [CloudinaryUploadService] — it needs ContentResolver to read the image bytes
 * from the gallery URI before uploading.
 */
class PostListingViewModel(app: Application) : AndroidViewModel(app) {

    private val listingRepository   = ListingRepository()
    private val merchantRepository  = MerchantRepository()
    private val cloudinaryService   = CloudinaryUploadService()

    private val _form = MutableStateFlow(ListingForm())
    val form: StateFlow<ListingForm> = _form

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState

    private val _publishState = MutableStateFlow<PublishState>(PublishState.Idle)
    val publishState: StateFlow<PublishState> = _publishState

    fun updateForm(update: ListingForm.() -> ListingForm) {
        _form.value = _form.value.update()
    }

    /**
     * Uploads the image selected from gallery to Cloudinary via the proxy API.
     * On success, writes the returned `secure_url` into [ListingForm.imageUrl]
     * so it gets stored in Firestore and served to consumers.
     */
    fun uploadImage(localUri: String) {
        _uploadState.value = UploadState.Uploading
        // Keep a local URI preview immediately so the UI can show the thumbnail
        _form.value = _form.value.copy(imageUri = localUri)

        viewModelScope.launch {
            try {
                val uri        = Uri.parse(localUri)
                val context    = getApplication<Application>()
                val secureUrl  = cloudinaryService.upload(context, uri)

                _form.value        = _form.value.copy(imageUrl = secureUrl)
                _uploadState.value = UploadState.Success(secureUrl)
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.localizedMessage ?: "Image upload failed"
                )
            }
        }
    }

    /**
     * Publishes the listing to Firestore /listings.
     * Loads the merchant's profile to denormalize [businessName] and [geoHash].
     */
    fun publishListing(merchantId: String) {
        if (merchantId.isBlank()) {
            _publishState.value = PublishState.Error("You must be signed in to post a listing")
            return
        }
        _publishState.value = PublishState.Publishing
        viewModelScope.launch {
            try {
                val merchant  = merchantRepository.getMerchant(merchantId)
                val listingId = listingRepository.postListing(
                    form         = _form.value,
                    merchantId   = merchantId,
                    businessName = merchant?.businessName ?: "",
                    geoHash      = merchant?.geoHash      ?: ""
                )
                _publishState.value = PublishState.Live(listingId)
            } catch (e: Exception) {
                _publishState.value = PublishState.Error(e.localizedMessage ?: "Publish failed")
            }
        }
    }

    fun resetPublishState() {
        _publishState.value = PublishState.Idle
    }
}

