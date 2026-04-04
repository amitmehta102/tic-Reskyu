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

/**
 * HomeViewModel
 *
 * Fetches the device's GPS location, then subscribes to a real-time Firestore
 * GeoHash listing stream for nearby food drops within 2km.
 *
 * Flow:
 *  1. Request GPS location (real or Bhopal fallback).
 *  2. Collect ListingRepository.observeNearbyListings(lat, lng) as a real-time Flow.
 *  3. Show empty state if Firestore returns no OPEN listings in range.
 *
 * Dev samples are intentionally removed — the app now shows only live Firestore data.
 * If the merchant has posted an OPEN listing in a nearby GeoHash cell, it will appear.
 */
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

    /** Current device location — drives both map center and GeoHash query */
    private val _userLat = MutableStateFlow(LocationRepository.DEFAULT_LAT)
    private val _userLng = MutableStateFlow(LocationRepository.DEFAULT_LNG)
    val userLat: StateFlow<Double> = _userLat.asStateFlow()
    val userLng: StateFlow<Double> = _userLng.asStateFlow()

    /** Merchant ratings cache: merchantId → avg rating (loaded lazily per listing batch) */
    private val _merchantRatings = MutableStateFlow<Map<String, Double>>(emptyMap())
    val merchantRatings: StateFlow<Map<String, Double>> = _merchantRatings.asStateFlow()

    private val db = FirebaseFirestore.getInstance()

    init { startListingStream() }

    /**
     * Called from HomeScreen after the location permission dialog result.
     * Restarts the stream with the real device coordinates.
     */
    fun onLocationPermissionResult(granted: Boolean) {
        startListingStream()
    }

    private fun startListingStream() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            // 1. Get real GPS (falls back to Bhopal automatically)
            val (lat, lng) = locationRepository.getCurrentLocation()
            _userLat.value = lat
            _userLng.value = lng

            // 2. Read user's saved discovery radius (default 2km if unavailable)
            val radiusKm = try {
                val uid = com.reskyu.consumer.data.repository.AuthRepository().requireUid()
                com.reskyu.consumer.data.repository.UserRepository()
                    .getUserProfile(uid)
                    ?.discoveryRadiusKm
                    ?.toDouble()
                    ?: 2.0
            } catch (_: Exception) { 2.0 }

            // 3. Subscribe to real-time Firestore stream (OPEN listings in nearby GeoHash cells)
            listingRepository
                .observeNearbyListings(lat, lng, radiusKm)
                .catch { _isLoading.value = false }
                .collect { liveListings ->
                    _isLoading.value = false
                    _listings.value = liveListings
                    // Fetch ratings for any merchantIds we haven't seen yet
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

    /** Re-fetches location and restarts the stream on pull-to-refresh. */
    fun refresh() = startListingStream()

    // ── Merchant rating lookup ───────────────────────────────────────────────

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
        } catch (_: Exception) { /* silently ignore — ratings are best-effort */ }
    }
}
