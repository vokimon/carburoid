package net.canvoki.carburoid.location

import android.app.Activity
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import net.canvoki.carburoid.R

class LocationPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CURRENT_LAT = "current_lat"
        const val EXTRA_CURRENT_LON = "current_lon"
        const val EXTRA_CURRENT_DESCRIPTION = "current_description"

        const val EXTRA_SELECTED_LAT = "selected_lat"
        const val EXTRA_SELECTED_LON = "selected_lon"
    }

    private lateinit var searchEditText: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var resultsList: ListView

    private val geocoder by lazy { Geocoder(this, Locale.getDefault()) }
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        searchEditText = findViewById(R.id.search_edit_text)
        progressBar = findViewById(R.id.progress_bar)
        resultsList = findViewById(R.id.results_list)

        // Pre-fill with current location description (optional)
        intent.getStringExtra(EXTRA_CURRENT_DESCRIPTION)?.let { desc ->
            if (desc != "Location not available") {
                searchEditText.setText(desc)
            }
        }

        // Debounced search
        searchEditText.doAfterTextChanged { text ->
            searchJob?.cancel()
            if (text.isNullOrBlank()) return@doAfterTextChanged
            searchJob = lifecycleScope.launch {
                searchLocations(text.toString())
            }
        }
    }

    private suspend fun searchLocations(query: String) {
        withContext(Dispatchers.IO) {
            try {
                val results = geocoder.getFromLocationName(query, 5)
                withContext(Dispatchers.Main) {
                    showResults(results ?: emptyList())
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showError("Geocoding failed")
                }
            }
        }
    }

    private fun showResults(addresses: List<Address>) {
        progressBar.visibility = View.GONE
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, addresses.map { it.getAddressLine(0) })
        resultsList.adapter = adapter
        resultsList.setOnItemClickListener { _, _, position, _ ->
            val address = addresses[position]
            val resultIntent = Intent().apply {
                putExtra(EXTRA_SELECTED_LAT, address.latitude)
                putExtra(EXTRA_SELECTED_LON, address.longitude)
            }
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

