package net.canvoki.carburoid.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.canvoki.carburoid.R
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.log
import net.canvoki.carburoid.timeit
import net.canvoki.carburoid.ui.usermessage.UserMessage
import java.util.Locale

class LocationService(
    private val context: Context,
) : CoroutineScope by MainScope() {
    private val _locationChanged = MutableSharedFlow<Location>(replay = 0)
    val locationChanged = _locationChanged.asSharedFlow()

    private val _descriptionUpdated = MutableSharedFlow<String>(replay = 0)
    val descriptionUpdated = _descriptionUpdated.asSharedFlow()

    private val provider = LocationProvider(context)

    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    private var currentLocation: Location? = null

    private var fixedLocation: Location? = null

    private var description: String? = null

    private var geocodingJob: Job? = null

    private fun tr(stringId: Int): String = context.getString(stringId)

    fun setFixedLocation(location: Location) {
        fixedLocation = location
        saveLastRealLocation(location)
        setLocation(location)
    }

    private fun refreshDeviceLocation() {
        if (!isLocationDeviceEnabled()) {
            handleLocationDisabled()
            return
        }
        requestDeviceLocation()
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

    fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
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
        val madrid =
            Location("").apply {
                latitude = 40.4168
                longitude = -3.7038
            }
        return madrid
    }

    private fun setLocation(location: Location) {
        currentLocation = location
        CurrentDistancePolicy.setMethod(DistanceFromAddress(location))
        updateDescription()
        CoroutineScope(Dispatchers.Main).launch {
            _locationChanged.emit(location)
        }
    }

    private fun requestDeviceLocation() {
        if (!hasPermission()) {
            setFallback()
            return
        }
        provider.getLastKnownLocation(
            onSuccess = { location -> handleDeviceLocationSuccess(location) },
            onError = { e -> handleDeviceLocationError(e) },
        )
    }

    private fun handleDeviceLocationSuccess(location: Location?) {
        if (location == null) {
            setFallback()
            UserMessage.Info(tr(R.string.location_not_available)).post()
            return
        }
        saveLastRealLocation(location)
        setLocation(location)
    }

    private fun handlePermissionDenied() {
        setFallback()
        UserMessage
            .Suggestion(
                tr(R.string.location_forbidden),
                tr(R.string.location_permisions_concede),
            ) {
                openSystemPermissionsSettings()
            }.post()
    }

    private fun handleDeviceLocationError(exception: Exception) {
        setFallback()
        log("Obtaining location: ${exception.message}")
        UserMessage.Info(tr(R.string.location_error)).post()
    }

    private fun handleLocationDisabled() {
        setFallback()
        UserMessage
            .Suggestion(
                tr(R.string.location_deactivated),
                tr(R.string.location_activate),
            ) {
                openLocationSettings()
            }.post()
    }

    private fun updateDescription() {
        geocodingJob?.cancel()
        val location = currentLocation
        if (location == null) {
            description = null
            CoroutineScope(Dispatchers.Main).launch {
                _descriptionUpdated.emit(description ?: "")
            }
            return
        }
        // Provisional description
        description = "(${ "%.3f".format(location.latitude) }, ${ "%.3f".format(location.longitude) })"
        CoroutineScope(Dispatchers.Main).launch {
            _descriptionUpdated.emit(description ?: "")
        }
        geocodingJob =
            launch {
                description = geocodeLocation(location) ?: description
                _descriptionUpdated.emit(description ?: "")
            }
    }

    fun getCurrentLocationDescription(): String = description ?: "Location not available"

    private suspend fun geocodeLocation(location: Location): String? {
        return withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }

            try {
                timeit("GEOCODING $location") {
                    val geocoder = Geocoder(context, Locale.getDefault())

                    @Suppress("DEPRECATION")
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
                        ).filter { it.isNotBlank() == true }.joinToString(", ")
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

    fun getSavedLocation(): Location? {
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

    fun isLocationDeviceEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return (
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        )
    }

    private fun openSystemPermissionsSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
        context.startActivity(intent)
    }

    private fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Si no es pot obrir la configuració de localització, obre la configuració general
            val settingsIntent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(settingsIntent)
        }
    }

    fun getCurrentLocation(): Location? = fixedLocation ?: currentLocation

    @Composable
    fun rememberLocationController(): () -> Unit {
        val permissionLauncher =
            rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { isGranted ->
                onPermissionResult(isGranted)
            }
        val refresh = {
            val hasPermission = hasPermission()
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                refreshDeviceLocation()
            }
        }

        LaunchedEffect(Unit) {
            if (getCurrentLocation() == null) {
                refresh()
            }
        }
        return remember(this) { refresh }
    }
}
