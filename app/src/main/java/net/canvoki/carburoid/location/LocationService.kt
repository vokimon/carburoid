package net.canvoki.carburoid.location

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.distances.DistanceFromCurrentPosition
import net.canvoki.carburoid.log


class LocationService(
    private val activity: Activity,
    private val notify: (String) -> Unit,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    fun setFallback() {
        val madrid = android.location.Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        CurrentDistancePolicy.setMethod(DistanceFromAddress(madrid))
    }
    fun lastLocation(block: ()->Unit) {

        if (!hasPermission()) {
            setFallback() // ✅ Set fallback if permission not granted
            block()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    CurrentDistancePolicy.setMethod(DistanceFromCurrentPosition(location))
                } else {
                    setFallback()
                    notify(LocationHelper.getNotAvailableMessage(activity))
                }
                block()
            }
            .addOnFailureListener { e->
                setFallback()
                log("Obtaining location: {e.message}")
                notify(LocationHelper.getErrorMessage(activity))
                block()
            }
    }
}
