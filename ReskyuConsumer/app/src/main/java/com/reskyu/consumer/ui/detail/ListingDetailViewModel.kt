package com.reskyu.consumer.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ListingDetailViewModel
 *
 * Loads a single listing document from Firestore by ID and exposes it
 * to [ListingDetailScreen].
 *
 * TODO: Add a real-time Firestore listener (snapshotFlow) so mealsLeft
 *       updates live while the user is viewing the detail screen.
 */
class ListingDetailViewModel : ViewModel() {

    private val listingRepository = ListingRepository()

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Fetches the listing with the given [listingId] from Firestore.
     * Called once from [ListingDetailScreen] via LaunchedEffect.
     *
     * @param listingId  Firestore document ID
     */
    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _listing.value = listingRepository.getListingById(listingId)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load listing"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
