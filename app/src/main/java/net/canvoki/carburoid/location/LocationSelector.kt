package net.canvoki.carburoid.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log

class LocationSelector
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val textInputLayout: TextInputLayout
        private val textInputEditText: TextInputEditText
        private val locationIcon = ContextCompat.getDrawable(context, R.drawable.ic_my_location)
        private val progressIcon = ContextCompat.getDrawable(context, R.drawable.ic_refresh)

        init {
            View.inflate(context, R.layout.location_selector, this)

            textInputLayout = findViewById(R.id.text_input_layout)
            textInputEditText = findViewById(R.id.text_input_edit_text)
        }

        fun bind(
            activity: ComponentActivity,
            service: LocationService,
        ) {
            setLocationDescription(service.getCurrentLocationDescription())
            activity.lifecycleScope.launch {
                service.descriptionUpdated.collect { description ->
                    setLocationDescription(description)
                }
            }

            textInputLayout.setStartIconOnClickListener {
                log("REFRESHING ON ICON PRESS")
                textInputLayout.startIconDrawable = progressIcon
                service.refreshLocation()
            }

            val launcher =
                activity.registerForActivityResult(
                    ActivityResultContracts.StartActivityForResult(),
                ) { result ->
                    if (result.resultCode == Activity.RESULT_OK) {
                        val lat = result.data?.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LAT, 0.0)
                        val lon = result.data?.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LON, 0.0)
                        if (lat != null && lon != null) {
                            val newLocation =
                                Location("user_picked").apply {
                                    latitude = lat
                                    longitude = lon
                                }
                            service.setFixedLocation(newLocation)
                        }
                    }
                }

            textInputLayout.setEndIconOnClickListener {
                val current = service.getCurrentLocation()
                val intent =
                    Intent(activity, LocationPickerActivity::class.java).apply {
                        putExtra(LocationPickerActivity.EXTRA_CURRENT_LAT, current?.latitude)
                        putExtra(LocationPickerActivity.EXTRA_CURRENT_LON, current?.longitude)
                        putExtra(
                            LocationPickerActivity.EXTRA_CURRENT_DESCRIPTION,
                            service.getCurrentLocationDescription(),
                        )
                    }
                launcher.launch(intent)
            }
        }

        private fun setLocationDescription(description: String) {
            textInputLayout.startIconDrawable = locationIcon
            textInputEditText.setText(description)
            log("Updating description to $description")
        }

        private fun setOnEditClickListener(listener: () -> Unit) {
            textInputLayout.setEndIconOnClickListener { listener() }
        }
    }
