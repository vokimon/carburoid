package net.canvoki.carburoid.location

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.distances.DistanceFromCurrentPosition
import net.canvoki.carburoid.log

class LocationService(
    private val activity: Activity,
    private val notify: (String) -> Unit,
    private val updateUi: () -> Unit,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    init {
        if (hasPermission()) {
            getLastLocation()
        } else {
            requestPermission()
        }
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE,
        )
    }

    fun processPermission(requestCode: Int, results: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            getLastLocation()
        } else {
            setFallback()
            notify(LocationHelper.getForbiddenMessage(activity))
            updateUi()
        }
    }

    private fun setFallback() {
        val madrid = android.location.Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        CurrentDistancePolicy.setMethod(DistanceFromAddress(madrid))
    }

    fun getLastLocation() {
        if (!hasPermission()) {
            setFallback() // ✅ Set fallback if permission not granted
            updateUi()
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
                updateUi()
            }
            .addOnFailureListener { e ->
                setFallback()
                log("Obtaining location: {e.message}")
                notify(LocationHelper.getErrorMessage(activity))
                updateUi()
            }
    }
}
