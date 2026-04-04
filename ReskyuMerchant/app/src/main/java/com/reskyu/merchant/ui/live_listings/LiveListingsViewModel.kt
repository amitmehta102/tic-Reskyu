package com.reskyu.merchant.ui.live_listings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.merchant.data.model.Listing
import com.reskyu.merchant.data.repository.ListingRepository
import kotlinx.coroutines.delay
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

    private var currentMerchantId = ""

    /**
     * Subscribes to real-time listing updates via Firestore snapshot listener.
     *
     * On first load also calls [expireOverdueListings] immediately so any listings
     * that expired while the app was closed are cleaned up at once.
     *
     * Also starts a 60-second periodic ticker so listings that expire while the
     * merchant is watching the screen are removed in near-real-time.
     */
    fun loadListings(merchantId: String) {
        if (merchantId.isBlank()) return
        currentMerchantId = merchantId

        // ── Real-time Firestore listener ──────────────────────────────────────
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

        // ── Eager expire on open (catches offline-created stale listings) ─────
        viewModelScope.launch {
            runCatching { listingRepository.expireOverdueListings(merchantId) }
        }

        // ── Periodic expiry ticker — runs every 60 s while screen is alive ────
        viewModelScope.launch {
            while (true) {
                delay(60_000L)   // check every minute
                runCatching { listingRepository.expireOverdueListings(currentMerchantId) }
            }
        }
    }

    /**
     * Cancels a listing by setting its Firestore status to CANCELLED.
     * The snapshot listener will automatically remove it from [listings].
     */
    fun cancelListing(listingId: String) {
        viewModelScope.launch {
            try {
                listingRepository.cancelListing(listingId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

