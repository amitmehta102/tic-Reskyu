package com.reskyu.merchant.ui.live_listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.repository.ListingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LiveListingsViewModel : ViewModel() {

    private val listingRepository = ListingRepository()

    private val _listings  = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /**
     * Subscribes to real-time listing updates via Firestore snapshot listener.
     * The listener stays active as long as this ViewModel is alive.
     * Calling with a new merchantId replaces any previous subscription.
     */
    fun loadListings(merchantId: String) {
        viewModelScope.launch {
            listingRepository.observeActiveListings(merchantId)
                .onStart  { _isLoading.value = true }
                .catch    { e -> _error.value = e.message; _isLoading.value = false }
                .collect  { listings ->
                    _listings.value  = listings
                    _isLoading.value = false
                    _error.value     = null
                }
        }
    }

    /**
     * Cancels a listing by setting its Firestore status to CANCELLED.
     * The snapshot listener will automatically remove it from [listings].
     * No manual reload is needed.
     */
    fun cancelListing(listingId: String) {
        viewModelScope.launch {
            try {
                listingRepository.cancelListing(listingId)
                // Firestore snapshot listener fires instantly — no reload needed
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
