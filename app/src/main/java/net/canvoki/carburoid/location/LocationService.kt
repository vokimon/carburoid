package net.canvoki.carburoid.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.canvoki.carburoid.R
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.log
import net.canvoki.carburoid.timeit
import java.util.Locale

class LocationService(
    private val activity: Activity,
    private val notify: (String) -> Unit,
    private val suggestAction: (String, String, () -> Unit) -> Unit,
    private val updateUi: () -> Unit,
) : CoroutineScope by MainScope() {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(activity)

    private val prefs = activity.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    private var currentLocation: Location? = null

    private var description: String? = null

    private var geocodingJob: Job? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    private fun tr(stringId: Int): String {
        return activity.getString(stringId)
    }

    fun refreshLocation() {
        if (!hasPermission()) {
            requestPermission()
            return
        }
        if (!isLocationEnabled()) {
            handleLocationDisabled()
            return
        }
        requestDeviceLocation()
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
        // result is asynchronously retrieved by processPermission
        // called from onRequestPermissionsResult in the main activity
    }

    fun processPermission(requestCode: Int, results: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            requestDeviceLocation()
        } else {
            handlePermissionDenied()
        }
    }

    private fun setFallback() {
        val location: Location = getSavedLocation() ?: getLastResortLocation()
        setLocation(location)
    }

    private fun getLastResortLocation(): Location {
        val madrid = Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        return madrid
    }

    private fun setLocation(location: Location) {
        currentLocation = location
        CurrentDistancePolicy.setMethod(DistanceFromAddress(location))
        updateDescription()
        updateUi()
    }

    private fun requestDeviceLocation() {
        if (!hasPermission()) {
            setFallback()
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                handleDeviceLocationSuccess(location)
            }
            .addOnFailureListener { exception ->
                handleDeviceLocationError(exception)
            }
    }

    private fun handleDeviceLocationSuccess(location: Location?) {
        if (location == null) {
            setFallback()
            notify(tr(R.string.location_not_available))
            return
        }
        saveLastRealLocation(location)
        setLocation(location)
    }

    private fun handlePermissionDenied() {
        setFallback()
        suggestAction(
            tr(R.string.location_forbidden),
            tr(R.string.location_permisions_concede),
        ) {
            openSystemPermissionsSettings()
        }
    }

    private fun handleDeviceLocationError(exception: Exception) {
        setFallback()
        log("Obtaining location: ${exception.message}")
        notify(tr(R.string.location_error))
    }

    private fun handleLocationDisabled() {
        setFallback()
        suggestAction(
            tr(R.string.location_deactivated),
            tr(R.string.location_activate),
        ) {
            openLocationSettings()
        }
    }

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
                timeit("GEOCODING $location") {
                    val geocoder = Geocoder(activity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    if (addresses == null || addresses.isEmpty()) {
                        null
                    } else {
                        val address = addresses[0]
                        listOfNotNull(
                            address.thoroughfare,
                            address.subThoroughfare,
                            address.locality,
                            address.adminArea,
                            address.countryName,
                        ).filter { it?.isNotBlank() == true }.joinToString(", ")
                    }
                }
            } catch (e: Exception) {
                log("Error getting address: ${e.message}")
                null
            }
        }
    }

    private fun saveLastRealLocation(location: Location) {
        prefs.edit {
            putLong("last_lat", java.lang.Double.doubleToRawLongBits(location.latitude))
            putLong("last_lng", java.lang.Double.doubleToRawLongBits(location.longitude))
        }
    }

    private fun getSavedLocation(): Location? {
        val latBits = prefs.getLong("last_lat", Long.MIN_VALUE)
        val lngBits = prefs.getLong("last_lng", Long.MIN_VALUE)

        if (latBits == Long.MIN_VALUE || lngBits == Long.MIN_VALUE) {
            return null
        }

        val location = Location("")
        location.latitude = java.lang.Double.longBitsToDouble(latBits)
        location.longitude = java.lang.Double.longBitsToDouble(lngBits)
        return location
    }

    fun isLocationEnabled(): Boolean {
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )
    }

    private fun openSystemPermissionsSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        activity.startActivity(intent)
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(intent)
        } else {
            // Si no es pot obrir la configuració de localització, obre la configuració general
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            activity.startActivity(settingsIntent)
        }
    }
}
