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
import net.canvoki.carburoid.R

class LocationService(
    private val activity: Activity,
    private val notify: (String) -> Unit,
    private val updateUi: () -> Unit,
) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    private var currentLocation: Location? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    init {
        refreshLocation()
    }

    fun refreshLocation() {
        if (hasPermission()) {
            requestLastLocation()
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
            requestLastLocation()
        } else {
            handlePermissionDenied()
        }
    }

    private fun setFallback() {
        val madrid = Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        currentLocation = madrid
        CurrentDistancePolicy.setMethod(DistanceFromAddress(madrid))
    }

    private fun requestLastLocation() {
        if (!hasPermission()) {
            setFallback()
            updateUi()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                handleLocationSuccess(location)
            }
            .addOnFailureListener { exception ->
                handleLocationError(exception)
            }
    }

    private fun handleLocationSuccess(location: Location?) {
        if (location != null) {
            currentLocation = location
            CurrentDistancePolicy.setMethod(DistanceFromCurrentPosition(location))
        } else {
            setFallback()
            notify(activity.getString(R.string.location_not_available))
        }
        updateUi()
    }

    private fun handlePermissionDenied() {
        setFallback()
        notify(activity.getString(R.string.location_forbidden))
        updateUi()
    }

    private fun handleLocationError(exception: Exception) {
        setFallback()
        log("Obtaining location: ${exception.message}")
        notify(activity.getString(R.string.location_error))
        updateUi()
    }

    fun getCurrentLocationDescription(): String {
        return currentLocation?.let { location ->
            "(${location.latitude}, ${location.longitude})"
        } ?: "Location not available"
    }
}
