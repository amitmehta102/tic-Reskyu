package com.reskyu.consumer.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/**
 * LocationRepository
 *
 * Wraps FusedLocationProviderClient to provide the device's current coordinates.
 * Falls back to Bhopal (23.2599, 77.4126) if location is unavailable or permission denied.
 *
 * Usage:
 *   val (lat, lng) = locationRepository.getCurrentLocation()
 *
 * ⚠️ Ensure ACCESS_FINE_LOCATION permission is granted before calling getCurrentLocation().
 *    The HomeScreen handles the permission request via rememberPermissionState.
 */
class LocationRepository(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // Default fallback — Bhopal city centre
    companion object {
        const val DEFAULT_LAT = 23.2599
        const val DEFAULT_LNG = 77.4126
    }

    /**
     * Returns the device's current lat/lng pair.
     * On permission denial or any error, returns fallback Bhopal coordinates.
     *
     * Uses getCurrentLocation() with PRIORITY_HIGH_ACCURACY for a fresh GPS fix.
     * If that fails, tries getLastKnownLocation() as a quick fallback.
     */
    @SuppressLint("MissingPermission")   // caller (HomeScreen) requests permission first
    suspend fun getCurrentLocation(): Pair<Double, Double> {
        return try {
            val cts = CancellationTokenSource()
            val location = fusedClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .await()

            if (location != null) {
                Pair(location.latitude, location.longitude)
            } else {
                // Fresh fix failed — try last known
                val last = fusedClient.lastLocation.await()
                if (last != null) Pair(last.latitude, last.longitude)
                else Pair(DEFAULT_LAT, DEFAULT_LNG)
            }
        } catch (e: Exception) {
            Pair(DEFAULT_LAT, DEFAULT_LNG)
        }
    }
}
