package net.canvoki.carburoid
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.lifecycle.lifecycleScope
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.distances.DistanceFromAddress
import net.canvoki.carburoid.distances.DistanceFromCurrentPosition
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.network.GasStationApiFactory
import net.canvoki.carburoid.ui.GasStationAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (checkLocationPermission()) {
            getLastLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun setFallbackLocation() {
        val madrid = Location("").apply {
            latitude = 40.4168
            longitude = -3.7038
        }
        CurrentDistancePolicy.setMethod(DistanceFromAddress(madrid))
    }

    private fun log(message: String) {
        Log.d("Carburoid", message)
    }

    private fun showToast(message: String) {
        log(message)
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun emptyViewStatus(
        message: String?
    ) {
        lifecycleScope.launch {
            _emptyViewStatus(message)
        }
    }

    private suspend fun _emptyViewStatus(
        message: String?
    ) {

        withContext(Dispatchers.Main) {
            val emptyView = findViewById<TextView>(R.id.text_empty)
            if (message == null) {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
            } else {
                log(message)
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
                emptyView.text = message
            }
        }
    }

    private fun getLastLocation() {
        if (!checkLocationPermission()) {
            setFallbackLocation() // ✅ Set fallback if permission not granted
            loadGasStations()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = location
                    CurrentDistancePolicy.setMethod(DistanceFromCurrentPosition(location))
                } else {
                    setFallbackLocation()
                    showToast("Location not available — using Madrid")
                }
                loadGasStations()
            }
            .addOnFailureListener {
                setFallbackLocation()
                showToast("Failed to get location — using Madrid")
                loadGasStations()
            }
    }

    private fun loadGasStations() {
        lifecycleScope.launch {
            try {
                emptyViewStatus("Loading Gas Stations...")
                val response = GasStationApiFactory.create().getGasStations()
                showToast("Downloaded ${response.stations.size} stations")

                val stationsWithCoords = response.stations//.filter { it.latitude != null && it.longitude != null }
                log("With coords: ${stationsWithCoords.size}")

                val sortedStations = stationsWithCoords
                    .filter { CurrentDistancePolicy.getDistance(it) != null }
                    .sortedBy { CurrentDistancePolicy.getDistance(it) }
                    .toMutableList()
                    .apply {
                        addAll(stationsWithCoords.filter { CurrentDistancePolicy.getDistance(it) == null })
                    }

                log("Final list: ${sortedStations.size} stations")

                emptyViewStatus(if (sortedStations.isEmpty()) "Failed to load stations" else null)

                val adapter = GasStationAdapter(sortedStations)
                recyclerView.adapter = adapter

            } catch (e: Exception) {
                showToast("Download failed: ${e.message}")
                e.printStackTrace()
                emptyViewStatus("Failed to load stations: ${e.message}")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation()
            } else {
                setFallbackLocation()
                showToast("Location permission denied — using Madrid")
                loadGasStations()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private var currentLocation: Location? = null
    }
}
