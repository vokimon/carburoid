package net.canvoki.carburoid.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import net.canvoki.shared.log
import net.canvoki.shared.timeit
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun Geocoder.getFromLocation(
    latitude: Double,
    longitude: Double,
    maxResults: Int,
): List<Address>? =
    suspendCancellableCoroutine { continuation ->
        try {
            getFromLocation(
                latitude,
                longitude,
                maxResults,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) {
                            continuation.resume(addresses?.toList())
                        }
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(
                                RuntimeException("Geocoding failed: $errorMessage"),
                            )
                        }
                    }
                },
            )
        } catch (e: Exception) {
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
    }

suspend fun describeLocation(
    context: Context,
    location: GeoPoint,
): String? {
    return withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        try {
            timeit("GEOCODING $location") {
                val geocoder = Geocoder(context, Locale.getDefault())

                // TODO: Blocking behaviour is deprecated
                @Suppress("DEPRECATION")
                val addresses =
                    geocoder.getFromLocation(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        maxResults = 1,
                    )
                addresses
                    ?.firstOrNull()
                    ?.let { address ->
                        listOfNotNull(
                            address.thoroughfare,
                            address.subThoroughfare,
                            address.locality,
                            address.adminArea,
                            address.countryName,
                        ).filterNot { it.isNullOrBlank() }
                            .joinToString(", ")
                    }.takeUnless { it.isNullOrBlank() }
            }
        } catch (e: Exception) {
            log("Error getting address: ${e.message}")
            null
        }
    }
}
