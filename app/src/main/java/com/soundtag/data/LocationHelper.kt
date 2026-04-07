package com.soundtag.data

import android.annotation.SuppressLint
import android.app.Application
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

class LocationHelper(application: Application) {

    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(application)

    @SuppressLint("MissingPermission")
    suspend fun getLocation(): LocationFix? = try {
        withTimeoutOrNull(5000L) {
            val location = fusedClient.lastLocation.await()
                ?: fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).await()

            location?.let {
                LocationFix(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracy
                )
            }
        }
    } catch (_: SecurityException) {
        null
    } catch (_: Exception) {
        null
    }
}
