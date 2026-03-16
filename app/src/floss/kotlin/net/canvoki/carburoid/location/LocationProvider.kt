package net.canvoki.carburoid.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import net.canvoki.carburoid.location.GeoPoint

class LocationProvider(
    private val context: Context,
) {
    fun getLastKnownLocation(
        onSuccess: (GeoPoint?) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        try {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            // Linter is unable to see that we are checking in hasPermission

            @SuppressLint("MissingPermission")
            val gps = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

            @SuppressLint("MissingPermission")
            val net = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            onSuccess((gps ?: net)?.let { GeoPoint.fromAndroidLocation(it) })
        } catch (e: Exception) {
            onError(e)
        }
    }
}
