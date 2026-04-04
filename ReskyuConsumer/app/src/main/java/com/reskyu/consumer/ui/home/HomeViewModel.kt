package com.reskyu.consumer.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.repository.ListingRepository
import com.reskyu.consumer.data.repository.LocationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val listingRepository  = ListingRepository()
    private val locationRepository = LocationRepository(application)

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedFilter = MutableStateFlow<DietaryTag?>(null)
    val selectedFilter: StateFlow<DietaryTag?> = _selectedFilter.asStateFlow()

    private val _userLat = MutableStateFlow(LocationRepository.DEFAULT_LAT)
    private val _userLng = MutableStateFlow(LocationRepository.DEFAULT_LNG)
    val userLat: StateFlow<Double> = _userLat.asStateFlow()
    val userLng: StateFlow<Double> = _userLng.asStateFlow()

    private val _merchantRatings = MutableStateFlow<Map<String, Double>>(emptyMap())
    val merchantRatings: StateFlow<Map<String, Double>> = _merchantRatings.asStateFlow()

    private val db = FirebaseFirestore.getInstance()

    init { startListingStream() }

    fun onLocationPermissionResult(granted: Boolean) {
        startListingStream()
    }

    private fun startListingStream() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val (lat, lng) = locationRepository.getCurrentLocation()
            _userLat.value = lat
            _userLng.value = lng

            val radiusKm = try {
                val uid = com.reskyu.consumer.data.repository.AuthRepository().requireUid()
                com.reskyu.consumer.data.repository.UserRepository()
                    .getUserProfile(uid)
                    ?.discoveryRadiusKm
                    ?.toDouble()
                    ?: 2.0
            } catch (_: Exception) { 2.0 }

            listingRepository
                .observeNearbyListings(lat, lng, radiusKm)
                .catch { _isLoading.value = false }
                .collect { liveListings ->
                    _isLoading.value = false
                    _listings.value = liveListings
                    val newIds = liveListings
                        .map { it.merchantId }
                        .filter { it.isNotBlank() && it !in _merchantRatings.value.keys }
                        .distinct()
                        .take(10)   // Firestore whereIn max = 10
                    if (newIds.isNotEmpty()) launch { fetchMerchantRatings(newIds) }
                }
        }
    }

    fun setFilter(tag: DietaryTag?) { _selectedFilter.value = tag }

    fun refresh() = startListingStream()

    private suspend fun fetchMerchantRatings(merchantIds: List<String>) {
        try {
            val docs = db.collection("merchants")
                .whereIn("uid", merchantIds)
                .get().await()
            val newEntries = docs.documents.associate { doc ->
                val uid   = doc.getString("uid") ?: doc.id
                val sum   = doc.getDouble("ratingSum") ?: doc.getLong("ratingSum")?.toDouble() ?: 0.0
                val count = doc.getLong("ratingCount")?.toInt() ?: 0
                uid to if (count > 0) sum / count else 0.0
            }
            _merchantRatings.value = _merchantRatings.value + newEntries
        } catch (_: Exception) {  }
    }
}
