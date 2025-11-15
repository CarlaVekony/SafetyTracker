package com.example.safetytracker.sensors

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext

data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)

class GPSManager(private val context: Context) {
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null

    fun getLocationData(): Flow<LocationReading> = callbackFlow {
        // Permissions
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            close()
            return@callbackFlow
        }

        // Providers
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            close()
            return@callbackFlow
        }

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val reading = LocationReading(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time
                )
                trySend(reading)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var registered = false

            // 🔥 FIX: requestLocationUpdates MUST be on Main Thread
            withContext(Dispatchers.Main) {
                for (provider in providers) {
                    if (locationManager.isProviderEnabled(provider)) {
                        locationManager.requestLocationUpdates(
                            provider,
                            1000L,
                            1f,
                            locationListener!!
                        )
                        registered = true
                        break
                    }
                }
            }

            if (!registered) {
                close()
                return@callbackFlow
            }

            // Last known location (safe anywhere)
            for (provider in providers) {
                val lastLocation = locationManager.getLastKnownLocation(provider)
                if (lastLocation != null) {
                    val reading = LocationReading(
                        latitude = lastLocation.latitude,
                        longitude = lastLocation.longitude,
                        accuracy = lastLocation.accuracy,
                        timestamp = lastLocation.time
                    )
                    trySend(reading)
                    break
                }
            }
        } catch (e: SecurityException) {
            close()
        }

        awaitClose {
            locationListener?.let {
                locationManager.removeUpdates(it)
                locationListener = null
            }
        }
    }

    fun getCurrentLocation(): LocationReading? {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    return LocationReading(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time
                    )
                }
            }
        } catch (e: SecurityException) {}

        return null
    }

    fun stop() {
        locationListener?.let {
            locationManager.removeUpdates(it)
            locationListener = null
        }
    }
}
