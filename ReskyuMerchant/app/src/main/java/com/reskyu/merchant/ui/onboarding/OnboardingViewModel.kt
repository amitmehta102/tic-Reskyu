package com.reskyu.merchant.ui.onboarding

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.reskyu.merchant.data.model.MerchantDraft
import com.reskyu.merchant.data.model.SaveState
import com.reskyu.merchant.data.repository.MerchantAuthRepository
import com.reskyu.merchant.data.repository.MerchantRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.abs

// ── Location state ────────────────────────────────────────────────────────────
sealed interface LocationState {
    data object Idle        : LocationState
    data object Fetching    : LocationState
    data class  Captured(val display: String) : LocationState
    data class  Error(val msg: String)        : LocationState
}

class OnboardingViewModel : ViewModel() {

    private val authRepository     = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()

    private val _draft = MutableStateFlow(MerchantDraft())
    val draft: StateFlow<MerchantDraft> = _draft

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState

    fun updateBusinessName(name: String) {
        _draft.value = _draft.value.copy(businessName = name)
    }

    fun updateClosingTime(time: String) {
        _draft.value = _draft.value.copy(closingTime = time)
    }

    fun updateLocation(lat: Double, lng: Double, geoHash: String) {
        _draft.value = _draft.value.copy(lat = lat, lng = lng, geoHash = geoHash)
    }

    /**
     * Fetches the device's current GPS coordinates using [FusedLocationProviderClient].
     * Requires ACCESS_FINE_LOCATION permission to have been granted before calling.
     */
    @SuppressLint("MissingPermission")   // permission is checked in the composable before calling
    fun fetchLocation(context: Context) {
        _locationState.value = LocationState.Fetching
        viewModelScope.launch {
            try {
                val client   = LocationServices.getFusedLocationProviderClient(context)
                val location = client.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY, null
                ).await()

                if (location != null) {
                    val lat     = location.latitude
                    val lng     = location.longitude
                    val geoHash = encodeGeohash(lat, lng, precision = 6)
                    updateLocation(lat, lng, geoHash)
                    _locationState.value = LocationState.Captured(
                        formatLatLng(lat, lng)
                    )
                } else {
                    // getCurrentLocation can return null on emulators with no mock location
                    _locationState.value = LocationState.Error("Could not get location. Is GPS enabled?")
                }
            } catch (e: Exception) {
                _locationState.value = LocationState.Error(e.localizedMessage ?: "Location error")
            }
        }
    }

    fun resetLocationState() {
        _locationState.value = LocationState.Idle
    }

    /**
     * Finalises onboarding by committing the [MerchantDraft] to Firestore.
     */
    fun completeOnboarding() {
        val uid = authRepository.getCurrentUid() ?: return
        _saveState.value = SaveState.Saving

        viewModelScope.launch {
            try {
                val finalDraft = _draft.value.copy(uid = uid)
                merchantRepository.completeMerchantOnboarding(finalDraft)
                _saveState.value = SaveState.Saved
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.localizedMessage ?: "Onboarding failed")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }

    // ── Geohash encoder ───────────────────────────────────────────────────────

    /**
     * Encodes a (lat, lng) pair into a geohash string at the given [precision].
     * Uses the standard base32 alphabet — no external library needed.
     */
    private fun encodeGeohash(lat: Double, lng: Double, precision: Int): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var minLat = -90.0;  var maxLat = 90.0
        var minLng = -180.0; var maxLng = 180.0
        val result = StringBuilder()
        var bit = 0; var idx = 0; var isLng = true

        while (result.length < precision) {
            if (isLng) {
                val mid = (minLng + maxLng) / 2
                if (lng >= mid) { idx = idx shl 1 or 1; minLng = mid } else { idx = idx shl 1; maxLng = mid }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat >= mid) { idx = idx shl 1 or 1; minLat = mid } else { idx = idx shl 1; maxLat = mid }
            }
            isLng = !isLng
            if (++bit == 5) { result.append(base32[idx]); bit = 0; idx = 0 }
        }
        return result.toString()
    }

    /** Formats lat/lng as a human-readable string for the UI display. */
    private fun formatLatLng(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return "%.4f°%s, %.4f°%s".format(abs(lat), latDir, abs(lng), lngDir)
    }
}
