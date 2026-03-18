package net.canvoki.carburoid.location

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
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
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.shared.log
import net.canvoki.shared.timeit
import net.canvoki.shared.usermessage.UserMessage
import java.util.Locale

fun Double.notNaNOrNull(): Double? = this.takeUnless { this.isNaN() }

fun positionFromIntent(
    intent: Intent,
    prefix: String,
): GeoPoint? {
    val lat = intent.getDoubleExtra(prefix + "_lat", Double.NaN).notNaNOrNull() ?: return null
    val lon = intent.getDoubleExtra(prefix + "_lon", Double.NaN).notNaNOrNull() ?: return null
    return GeoPoint(
        latitude = lat,
        longitude = lon,
    )
}

fun positionToIntent(
    intent: Intent,
    prefix: String,
    location: GeoPoint?,
) {
    if (location == null) return
    intent.apply {
        putExtra(prefix + "_lat", location.latitude)
        putExtra(prefix + "_lon", location.longitude)
    }
}

fun positionFromBundle(
    bundle: Bundle,
    prefix: String,
): GeoPoint? {
    val lat = bundle.getDouble(prefix + "_lat", Double.NaN).notNaNOrNull() ?: return null
    val lon = bundle.getDouble(prefix + "_lon", Double.NaN).notNaNOrNull() ?: return null
    return GeoPoint(
        latitude = lat,
        longitude = lon,
    )
}

fun positionToBundle(
    bundle: Bundle,
    prefix: String,
    location: GeoPoint?,
) {
    if (location == null) return
    bundle.apply {
        putDouble(prefix + "_lat", location.latitude)
        putDouble(prefix + "_lon", location.longitude)
    }
}

class LocationService(
    private val context: Context,
) : CoroutineScope by MainScope() {
    companion object {
        const val PREF_CURRENT_LOCATION = "current_location"
        const val PREF_TARGET_LOCATION = "target_location"
        const val EXTRA_CURRENT_PREFIX = "current"
        const val EXTRA_TARGET_PREFIX = "target"
    }

    private val _locationChanged = MutableSharedFlow<GeoPoint>(replay = 0)
    val locationChanged = _locationChanged.asSharedFlow()

    private val _descriptionUpdated = MutableSharedFlow<String>(replay = 0)
    val descriptionUpdated = _descriptionUpdated.asSharedFlow()

    private val provider = LocationProvider(context)

    private val prefs = context.getSharedPreferences("location_prefs", Context.MODE_PRIVATE)

    private var currentLocation: GeoPoint? = null

    private var fixedLocation: GeoPoint? = null

    private var targetLocation: GeoPoint? = null

    private var description: String? = null

    private var geocodingJob: Job? = null

    private fun tr(stringId: Int): String = context.getString(stringId)

    fun setFixedLocation(
        location: GeoPoint,
        target: GeoPoint?,
    ) {
        saveLocation(PREF_CURRENT_LOCATION, location)
        saveLocation(PREF_TARGET_LOCATION, target)
        setLocation(location, target)
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

    private fun onPermissionResult(isGranted: Boolean) {
        if (isGranted) {
            requestDeviceLocation()
        } else {
            handlePermissionDenied()
        }
    }

    private fun setFallback() {
        val (current, target) = loadLastLocation() ?: getLastResortLocation()
        setLocation(current, target)
    }

    private fun getLastResortLocation(): Pair<GeoPoint, GeoPoint?> {
        val madrid =
            GeoPoint(
                latitude = 40.4168,
                longitude = -3.7038,
            )
        return madrid to null
    }

    private fun setLocation(
        location: GeoPoint,
        target: GeoPoint?,
    ) {
        currentLocation = location
        targetLocation = target
        CurrentDistancePolicy.setMethod(DistanceFromAddress(location, targetLocation))
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

    private fun handleDeviceLocationSuccess(location: GeoPoint?) {
        if (location == null) {
            setFallback()
            UserMessage.Info(tr(R.string.location_not_available)).post()
            return
        }
        log("HANDLE_DEVICE_LOCATION_SUCCESS $location")
        setFixedLocation(location, targetLocation)
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
        description = location.pretty()
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

    private suspend fun geocodeLocation(location: GeoPoint): String? {
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

    private fun saveLocation(
        prefix: String,
        location: GeoPoint?,
    ) {
        prefs.edit {
            if (location == null) {
                remove(prefix + "_lat")
                remove(prefix + "_lon")
            } else {
                putLong(prefix + "_lat", java.lang.Double.doubleToRawLongBits(location.latitude))
                putLong(prefix + "_lon", java.lang.Double.doubleToRawLongBits(location.longitude))
            }
        }
    }

    fun loadLocation(prefix: String): GeoPoint? {
        val latBits = prefs.getLong(prefix + "_lat", Long.MIN_VALUE)
        val lngBits = prefs.getLong(prefix + "_lon", Long.MIN_VALUE)

        if (latBits == Long.MIN_VALUE || lngBits == Long.MIN_VALUE) {
            return null
        }

        val location =
            GeoPoint(
                latitude = java.lang.Double.longBitsToDouble(latBits),
                longitude = java.lang.Double.longBitsToDouble(lngBits),
            )
        return location
    }

    fun loadLastLocation(): Pair<GeoPoint, GeoPoint?>? =
        loadLocation(PREF_CURRENT_LOCATION)?.let {
            it to loadLocation(PREF_TARGET_LOCATION)
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

    fun getCurrentLocation(): GeoPoint? = fixedLocation ?: currentLocation

    fun getTargetLocation(): GeoPoint? = targetLocation

    @Composable
    fun rememberLocationRefresher(): () -> Unit {
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

    fun stateFromIntent(intent: Intent) {
        val current = positionFromIntent(intent, EXTRA_CURRENT_PREFIX)
        val target = positionFromIntent(intent, EXTRA_TARGET_PREFIX)
        log("LOCATION SERVICE FROM INTENT $current $target")
        current?.let {
            setFixedLocation(current, target)
        }
    }

    fun stateToIntent(intent: Intent) {
        val current = getCurrentLocation()
        val target = getTargetLocation()
        log("LOCATION SERVICE TO INTENT $current $target")
        positionToIntent(intent, EXTRA_CURRENT_PREFIX, current)
        positionToIntent(intent, EXTRA_TARGET_PREFIX, target)
        intent.putExtra(
            LocationPickerActivity.EXTRA_CURRENT_DESCRIPTION,
            getCurrentLocationDescription(),
        )
    }
}
