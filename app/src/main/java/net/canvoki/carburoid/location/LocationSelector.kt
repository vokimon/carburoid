package net.canvoki.carburoid.location

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
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

    init {
        View.inflate(context, R.layout.location_selector, this)

        textInputLayout = findViewById(R.id.text_input_layout)
        textInputEditText = findViewById(R.id.text_input_edit_text)
    }

    fun setLocationDescription(description: String) {
        textInputEditText.setText(description)
        log("Updating description to $description")
    }

    fun setOnEditClickListener(listener: () -> Unit) {
        textInputLayout.setEndIconOnClickListener { listener() }
    }
}
