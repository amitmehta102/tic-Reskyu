package com.reskyu.merchant.ui.post_listing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.ListingForm
import com.reskyu.merchant.data.model.PublishState
import com.reskyu.merchant.data.model.UploadState
import com.reskyu.merchant.data.repository.ListingRepository
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PostListingViewModel : ViewModel() {

    private val listingRepository = ListingRepository()
    private val authRepository = MerchantAuthRepository()

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
     * Records the selected image URI in the form.
     * Real Cloudinary upload will replace this once credentials are configured.
     */
    fun uploadImage(localUri: String) {
        _form.value = _form.value.copy(imageUri = localUri, imageUrl = localUri)
        _uploadState.value = UploadState.Success(localUri)
    }

    /**
     * Publishes the listing to Firestore.
     */
    fun publishListing(merchantId: String, businessName: String, geoHash: String) {
        _publishState.value = PublishState.Publishing
        viewModelScope.launch {
            try {
                val listingId = listingRepository.postListing(
                    form = _form.value,
                    merchantId = merchantId,
                    businessName = businessName,
                    geoHash = geoHash
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
