package net.canvoki.carburoid.location

import android.annotation.SuppressLint
import android.app.Activity
import android.location.Location
import com.google.android.gms.location.LocationServices

class LocationProvider(private val activity: Activity) {
    fun getLastKnownLocation(onSuccess: (Location?) -> Unit, onError: (Exception) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(activity)
        try {
            // Linter is unable to see that we are checking in hasPermission
            @SuppressLint("MissingPermission")
            fused.lastLocation
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onError)
        } catch (e: Exception) {
            onError(e)
        }
    }
}
