package net.canvoki.carburoid.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import net.canvoki.carburoid.location.GeoPoint

class LocationProvider(
    private val activity: Context,
) {
    fun getLastKnownLocation(
        onSuccess: (GeoPoint?) -> Unit,
        onError: (Exception) -> Unit,
    ) {
        val fused = LocationServices.getFusedLocationProviderClient(activity)
        try {
            // Linter is unable to see that we are checking in hasPermission
            @SuppressLint("MissingPermission")
            fused.lastLocation
                .addOnSuccessListener { location ->
                    onSuccess(GeoPoint.fromAndroidLocation(location))
                }.addOnFailureListener(onError)
        } catch (e: Exception) {
            onError(e)
        }
    }
}
