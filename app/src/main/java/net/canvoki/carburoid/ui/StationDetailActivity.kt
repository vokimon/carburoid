package net.canvoki.carburoid.ui

import java.time.Instant
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.databinding.ActivityStationDetailBinding

class StationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /*

        val stationId = intent.getStringExtra("station_id") ?: return
        val station = findStationById(stationId) ?: return // ← you implement this

        binding.textName.text = station.name
        binding.textPrice.text = station.price?.let { "${String.format("%.3f", it)} €" } ?: "No price"
        binding.textDistance.text = station.distanceInMeters?.let { "%.1f km".format(it / 1000f) } ?: "Unknown distance"

        val status = station.openStatus(Instant.now())
        if (status != null) {
            val statusText = formatOpeningStatus(this, status, station.timeZone(), 60)
            binding.textOpenStatus.text = statusText
            val colorAttr = if (status.isOpen) R.attr.colorSuccess else R.attr.colorError
            val typedValue = TypedValue()
            theme.resolveAttribute(colorAttr, typedValue, true)
            binding.textOpenStatus.setTextColor(typedValue.data)
            binding.textOpenStatus.visibility = View.VISIBLE
        } else {
            binding.textOpenStatus.visibility = View.GONE
        }

        binding.textAddress.text = station.address
        binding.textCityState.text = "${station.city}, ${station.state}"

        binding.textOpeningHours.text = station.openingHours?.toString() ?: "No hours"

        // Other prices
        val otherPrices = station.prices.filter { it.key != ProductManager.getCurrent() }
        if (otherPrices.isNotEmpty()) {
            binding.labelOtherPrices.visibility = View.VISIBLE
            binding.containerOtherPrices.visibility = View.VISIBLE

            for ((product, price) in otherPrices) {
                val textView = TextView(this)
                textView.text = "$product: ${String.format("%.3f", price)} €"
                textView.setTextAppearance(R.style.TextAppearance_Material3_BodyMedium)
                binding.containerOtherPrices.addView(textView)
            }
        }
        */
    }

    private fun findStationById(id: String): GasStation? {
        // ← Implement this — e.g., from repository or passed as Parcelable
        return null
    }
}
