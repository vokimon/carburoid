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
import net.canvoki.carburoid.timeit
import net.canvoki.carburoid.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.withContext
import android.location.Geocoder
import java.util.Locale

class LocationService(
    private val activity: Activity,
    private val notify: (String) -> Unit,
    private val updateUi: () -> Unit,
) : CoroutineScope by MainScope() {
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
        updateDescription()
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
            updateDescription()
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



    private var description: String? = null
    private var geocodingJob: Job? = null  // Per cancel·lar tasques

    private fun updateDescription() {
        geocodingJob?.cancel()
        description = null
        val location: Location = currentLocation ?: return
        description = "(${location.latitude}, ${location.longitude})" // Provisional
        geocodingJob = launch {
            description = geocodeLocation(location) ?: description
            updateUi() // TODO: Should update just the location field, location is the same
        }
    }

    fun getCurrentLocationDescription(): String {
        return description ?: "Location not available"
    }

    private suspend fun geocodeLocation(location: Location): String? {
        return withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            try {
                timeit("GEOCODING ${location}") {
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses == null || addresses.isEmpty()){
                        null
                    } else {
                        val address = addresses[0]
                        listOfNotNull(
                            address.thoroughfare,
                            address.subThoroughfare,
                            address.locality,
                            address.adminArea,
                            address.countryName
                        ).filter { it?.isNotBlank() == true }.joinToString(", ")
                    }
                }
            } catch (e: Exception) {
                log("Error getting address: ${e.message}")
                null
            }
        }
    }
}
