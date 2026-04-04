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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.abs

// ── Location state ────────────────────────────────────────────────────────────
sealed interface LocationState {
    data object Idle        : LocationState
    data object Fetching    : LocationState
    data class  Captured(val lat: Double, val lng: Double, val display: String) : LocationState
    data class  Error(val msg: String) : LocationState
}

class OnboardingViewModel : ViewModel() {

    private val authRepository     = MerchantAuthRepository()
    private val merchantRepository = MerchantRepository()
    private val httpClient         = OkHttpClient()

    private val _draft = MutableStateFlow(MerchantDraft())
    val draft: StateFlow<MerchantDraft> = _draft

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    private val _locationState = MutableStateFlow<LocationState>(LocationState.Idle)
    val locationState: StateFlow<LocationState> = _locationState

    // Map center — defaults to center of India (zoom 5 on first load)
    private val _mapCenter = MutableStateFlow(Pair(20.5937, 78.9629))
    val mapCenter: StateFlow<Pair<Double, Double>> = _mapCenter

    fun onMapCenterChanged(lat: Double, lng: Double) {
        _mapCenter.value = Pair(lat, lng)
    }

    fun updateBusinessName(name: String) {
        _draft.value = _draft.value.copy(businessName = name)
    }

    fun updateClosingTime(time: String) {
        _draft.value = _draft.value.copy(closingTime = time)
    }

    fun updateLocation(lat: Double, lng: Double, geoHash: String) {
        _draft.value = _draft.value.copy(lat = lat, lng = lng, geoHash = geoHash)
    }

    // ── GPS fetch ─────────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
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
                    // Snap the map center to GPS
                    _mapCenter.value = Pair(lat, lng)
                    val geoHash = encodeGeohash(lat, lng, precision = 6)
                    updateLocation(lat, lng, geoHash)
                    reverseGeocode(lat, lng)          // fetches address and sets Captured
                } else {
                    _locationState.value = LocationState.Error("Could not get location. Is GPS enabled?")
                }
            } catch (e: Exception) {
                _locationState.value = LocationState.Error(e.localizedMessage ?: "Location error")
            }
        }
    }

    // ── Nominatim reverse-geocode ─────────────────────────────────────────────

    /**
     * Calls OpenStreetMap's Nominatim API to convert (lat, lng) → human-readable address.
     * Falls back to raw coordinates if the network request fails.
     * Rate limit: Nominatim allows 1 req/s — only called on user action, so this is fine.
     */
    fun reverseGeocode(lat: Double, lng: Double) {
        _locationState.value = LocationState.Fetching
        val geoHash = encodeGeohash(lat, lng, precision = 6)
        updateLocation(lat, lng, geoHash)

        viewModelScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    val url = "https://nominatim.openstreetmap.org/reverse" +
                            "?lat=$lat&lon=$lng&format=json&addressdetails=1"
                    val request = Request.Builder()
                        .url(url)
                        .header("User-Agent", "ReskyuMerchantApp/1.0")
                        .build()
                    val response = httpClient.newCall(request).execute()
                    val body     = response.body?.string() ?: ""
                    parseNominatimAddress(body, lat, lng)
                }
                _locationState.value = LocationState.Captured(lat, lng, address)
            } catch (e: Exception) {
                // Network failure — show raw coordinates
                _locationState.value = LocationState.Captured(lat, lng, formatLatLng(lat, lng))
            }
        }
    }

    fun resetLocationState() {
        _locationState.value = LocationState.Idle
    }

    // ── Onboarding save ───────────────────────────────────────────────────────

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

    // ── Nominatim response parser ─────────────────────────────────────────────

    private fun parseNominatimAddress(json: String, lat: Double, lng: Double): String {
        return try {
            val obj  = JSONObject(json)
            val addr = obj.optJSONObject("address")
            if (addr != null) {
                listOfNotNull(
                    addr.optString("road").takeIf       { it.isNotBlank() },
                    addr.optString("suburb").takeIf     { it.isNotBlank() },
                    addr.optString("city").takeIf       { it.isNotBlank() }
                        ?: addr.optString("town").takeIf  { it.isNotBlank() }
                        ?: addr.optString("village").takeIf { it.isNotBlank() },
                    addr.optString("state").takeIf      { it.isNotBlank() }
                ).joinToString(", ").ifBlank { formatLatLng(lat, lng) }
            } else {
                // Use the flat display_name but trim it to first 3 comma-parts
                obj.optString("display_name")
                    .split(",")
                    .take(3)
                    .joinToString(", ")
                    .ifBlank { formatLatLng(lat, lng) }
            }
        } catch (e: JSONException) {
            formatLatLng(lat, lng)
        }
    }

    // ── Geohash encoder ───────────────────────────────────────────────────────

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

    private fun formatLatLng(lat: Double, lng: Double): String {
        val latDir = if (lat >= 0) "N" else "S"
        val lngDir = if (lng >= 0) "E" else "W"
        return "%.4f°%s, %.4f°%s".format(abs(lat), latDir, abs(lng), lngDir)
    }
}
