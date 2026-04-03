package com.reskyu.consumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * HomeViewModel
 *
 * Fetches and exposes nearby food listings to [HomeScreen].
 * In production, triggers a location permission request and then
 * uses the device's lat/lng to query [ListingRepository.getNearbyListings].
 *
 * TODO: Inject a LocationProvider and observe location updates reactively.
 */
class HomeViewModel : ViewModel() {

    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // TODO: Get actual device location before calling fetchListings
        fetchListings(lat = 23.2599, lng = 77.4126) // Placeholder: Bhopal coordinates
    }

    /**
     * Fetches nearby OPEN listings using GeoHash proximity search.
     *
     * @param lat  Device latitude
     * @param lng  Device longitude
     */
    fun fetchListings(lat: Double, lng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _listings.value = listingRepository.getNearbyListings(lat, lng)
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load listings"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        // TODO: Use actual device location here
        fetchListings(lat = 23.2599, lng = 77.4126)
    }
}
