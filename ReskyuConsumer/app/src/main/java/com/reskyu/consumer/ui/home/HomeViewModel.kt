package com.reskyu.consumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.reskyu.consumer.data.model.DietaryTag
import com.reskyu.consumer.data.model.Listing
import com.reskyu.consumer.data.model.ListingStatus
import com.reskyu.consumer.data.repository.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * HomeViewModel
 *
 * Fetches nearby food listings from Firestore.
 * Falls back to dev sample listings when Firebase isn't configured.
 *
 * TODO: Wire real device location via FusedLocationProviderClient.
 */
class HomeViewModel : ViewModel() {

    private val listingRepository = ListingRepository()

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** Active dietary filter — null means "show all" */
    private val _selectedFilter = MutableStateFlow<DietaryTag?>(null)
    val selectedFilter: StateFlow<DietaryTag?> = _selectedFilter.asStateFlow()

    init {
        // TODO: Replace with real device location
        fetchListings(lat = 23.2599, lng = 77.4126)
    }

    fun fetchListings(lat: Double, lng: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val result = listingRepository.getNearbyListings(lat, lng)
                _listings.value = result.ifEmpty { devSampleListings() }
            } catch (e: Exception) {
                // Firebase not configured or no internet — show dev samples
                _listings.value = devSampleListings()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setFilter(tag: DietaryTag?) {
        _selectedFilter.value = tag
    }

    fun refresh() = fetchListings(lat = 23.2599, lng = 77.4126)

    /** Realistic sample listings for dev mode testing */
    private fun devSampleListings(): List<Listing> {
        val now = System.currentTimeMillis() / 1000
        // Coordinates scattered around Bhopal city center (23.2599, 77.4126)
        return listOf(
            Listing(
                id = "dev_listing_1",
                merchantId = "merchant_1",
                businessName = "The Bread Basket",
                heroItem = "Assorted Pastry Box (4 pcs)",
                dietaryTag = DietaryTag.VEG.name,
                mealsLeft = 3,
                originalPrice = 280.0,
                discountedPrice = 99.0,
                imageUrl = "",
                geoHash = "test",
                lat = 23.2612, lng = 77.4098,
                expiresAt = Timestamp(now + TimeUnit.HOURS.toSeconds(2), 0),
                status = ListingStatus.OPEN.name
            ),
            Listing(
                id = "dev_listing_2",
                merchantId = "merchant_2",
                businessName = "Green Leaf Café",
                heroItem = "Veg Thali Combo (Full meal)",
                dietaryTag = DietaryTag.VEGAN.name,
                mealsLeft = 1,
                originalPrice = 200.0,
                discountedPrice = 79.0,
                imageUrl = "",
                geoHash = "test",
                lat = 23.2585, lng = 77.4150,
                expiresAt = Timestamp(now + TimeUnit.MINUTES.toSeconds(45), 0),
                status = ListingStatus.OPEN.name
            ),
            Listing(
                id = "dev_listing_3",
                merchantId = "merchant_3",
                businessName = "Spice Garden",
                heroItem = "Chicken Biryani + Raita",
                dietaryTag = DietaryTag.NON_VEG.name,
                mealsLeft = 5,
                originalPrice = 380.0,
                discountedPrice = 149.0,
                imageUrl = "",
                geoHash = "test",
                lat = 23.2630, lng = 77.4180,
                expiresAt = Timestamp(now + TimeUnit.HOURS.toSeconds(3), 0),
                status = ListingStatus.OPEN.name
            ),
            Listing(
                id = "dev_listing_4",
                merchantId = "merchant_4",
                businessName = "Jain Sweets & Farsan",
                heroItem = "Farsan Sampler Platter",
                dietaryTag = DietaryTag.JAIN.name,
                mealsLeft = 2,
                originalPrice = 150.0,
                discountedPrice = 59.0,
                imageUrl = "",
                geoHash = "test",
                lat = 23.2570, lng = 77.4110,
                expiresAt = Timestamp(now + TimeUnit.HOURS.toSeconds(1), 0),
                status = ListingStatus.OPEN.name
            ),
            Listing(
                id = "dev_listing_5",
                merchantId = "merchant_5",
                businessName = "Italiano Express",
                heroItem = "Mixed Pizza Slices Box (6 pcs)",
                dietaryTag = DietaryTag.VEG.name,
                mealsLeft = 4,
                originalPrice = 320.0,
                discountedPrice = 129.0,
                imageUrl = "",
                geoHash = "test",
                lat = 23.2650, lng = 77.4070,
                expiresAt = Timestamp(now + TimeUnit.HOURS.toSeconds(4), 0),
                status = ListingStatus.OPEN.name
            )
        )
    }
}
