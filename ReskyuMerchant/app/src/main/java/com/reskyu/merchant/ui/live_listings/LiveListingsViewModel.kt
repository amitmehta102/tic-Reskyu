package com.reskyu.merchant.ui.live_listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LiveListingsViewModel : ViewModel() {

    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadListings(merchantId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _listings.value = listingRepository.getActiveListings(merchantId)
            } catch (e: Exception) {
                // TODO: error state
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelListing(listingId: String, merchantId: String) {
        viewModelScope.launch {
            try {
                listingRepository.cancelListing(listingId)
                loadListings(merchantId)
            } catch (e: Exception) {
                // TODO: error state
            }
        }
    }
}
