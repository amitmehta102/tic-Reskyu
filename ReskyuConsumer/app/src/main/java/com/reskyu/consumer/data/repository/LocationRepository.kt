package com.reskyu.consumer.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationRepository(context: Context) {

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    companion object {
        const val DEFAULT_LAT = 23.2599
        const val DEFAULT_LNG = 77.4126
    }

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
                val last = fusedClient.lastLocation.await()
                if (last != null) Pair(last.latitude, last.longitude)
                else Pair(DEFAULT_LAT, DEFAULT_LNG)
            }
        } catch (e: Exception) {
            Pair(DEFAULT_LAT, DEFAULT_LNG)
        }
    }
}
