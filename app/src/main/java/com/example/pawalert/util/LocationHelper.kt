package com.example.pawalert.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

object LocationHelper {

    /**
     * Calculates distance in meters between two lat/lng coordinates.
     */
    fun calculateDistanceMeters(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLng, endLat, endLng, results)
        return results[0]
    }

    /**
     * Formats distance in meters to a human-readable string (e.g., "350 m away", "1.4 km away").
     */
    fun formatDistance(meters: Float?): String {
        if (meters == null) return "Distance unknown"
        return if (meters < 1000) {
            "${meters.toInt()} m away"
        } else {
            String.format(Locale.getDefault(), "%.1f km away", meters / 1000f)
        }
    }

    /**
     * Fetches the current device location with high accuracy using FusedLocationProviderClient.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? = withContext(Dispatchers.IO) {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()

            location ?: fusedClient.lastLocation.await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Reverse geocodes latitude/longitude into a readable street/neighborhood address.
     */
    @Suppress("DEPRECATION")
    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val address = addresses.firstOrNull()
                            val formatted = formatAddress(address, latitude, longitude)
                            continuation.resume(formatted)
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(formatCoordinatesFallback(latitude, longitude))
                        }
                    })
                }
            } else {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                formatAddress(address, latitude, longitude)
            }
        } catch (e: Exception) {
            formatCoordinatesFallback(latitude, longitude)
        }
    }

    private fun formatAddress(address: Address?, lat: Double, lng: Double): String {
        if (address == null) return formatCoordinatesFallback(lat, lng)
        val parts = mutableListOf<String>()
        address.subLocality?.let { parts.add(it) }
        address.thoroughfare?.let { parts.add(it) }
        address.locality?.let { parts.add(it) }
        address.subAdminArea?.let { if (!parts.contains(it)) parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            address.getAddressLine(0) ?: formatCoordinatesFallback(lat, lng)
        }
    }

    private fun formatCoordinatesFallback(lat: Double, lng: Double): String {
        return String.format(Locale.getDefault(), "Lat: %.4f, Lng: %.4f", lat, lng)
    }

    /**
     * Creates a temporary file in the cache directory and returns a content URI for camera captures.
     */
    fun createTempImageUri(context: Context): Uri {
        val tempFile = File.createTempFile(
            "pawalert_photo_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        ).apply {
            createNewFile()
            deleteOnExit()
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    /**
     * Formats timestamp into relative "time ago" string.
     */
    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
