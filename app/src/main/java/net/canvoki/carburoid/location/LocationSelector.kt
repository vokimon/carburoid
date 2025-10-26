package net.canvoki.carburoid.location

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log

class LocationSelector @JvmOverloads constructor(
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

    fun setService(service: LocationService, scope: CoroutineScope) {
        setLocationDescription(service.getCurrentLocationDescription())
        scope.launch {
            service.descriptionUpdated.collect { description ->
                setLocationDescription(description)
            }
        }

        textInputLayout.setStartIconOnClickListener {
            log("REFRESHING ON ICON PRESS")
            textInputLayout.startIconDrawable = progressIcon
            service.refreshLocation()
        }

    }

    fun setLocationDescription(description: String) {
        textInputLayout.startIconDrawable = locationIcon
        textInputEditText.setText(description)
        log("Updating description to $description")
    }

    fun setOnEditClickListener(listener: () -> Unit) {
        textInputLayout.setEndIconOnClickListener { listener() }
    }
}
