package net.canvoki.carburoid.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.distances.CurrentDistancePolicy

class GasStationAdapter(private val stations: List<GasStation>) :
    RecyclerView.Adapter<GasStationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_name)
        val address: TextView = view.findViewById(R.id.text_address)
        val location: TextView = view.findViewById(R.id.text_location)
        val price: TextView = view.findViewById(R.id.text_price)
        val distance: TextView = view.findViewById(R.id.text_distance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gas_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = stations[position]
        holder.name.text = station.name ?: "Unknown"
        holder.address.text = station.address ?: "No address"

        // Combine city and province
        val location = listOfNotNull(station.city, station.state).joinToString(" - ")
        holder.location.text = if (location.isNotEmpty()) location else "Location unknown"

        val price = toSpanishFloat(station.priceGasoleoA) ?: "???"
        holder.price.text = "${price}€/l"

        val distance = CurrentDistancePolicy.getDistance(station)
        holder.distance.text = distance?.let { "%.1f km".format(it / 1000) } ?: "Distance N/A"
    }

    override fun getItemCount() = stations.size
}
