package net.canvoki.carburoid.ui

import java.time.Instant
import java.time.ZoneId
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import net.canvoki.carburoid.R
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.model.OpeningStatus
import net.canvoki.carburoid.json.toSpanishFloat
import net.canvoki.carburoid.distances.CurrentDistancePolicy
import net.canvoki.carburoid.log




class GasStationAdapter(
    private var context: Context,
    private var stations: List<GasStation>,
    private val onStationClick: (GasStation) -> Unit = {},
):
    RecyclerView.Adapter<GasStationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_name)
        val address: TextView = view.findViewById(R.id.text_address)
        val location: TextView = view.findViewById(R.id.text_location)
        val price: TextView = view.findViewById(R.id.text_price)
        val distance: TextView = view.findViewById(R.id.text_distance)
        val openStatus: TextView = view.findViewById(R.id.text_open_status)
    }

    fun updateData(newData: List<GasStation>) {
        stations = newData
        notifyDataSetChanged() // Rebind all items
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_gas_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = stations[position]

        holder.itemView.setOnClickListener {
            //log("Clicked on ${station.name}")
            onStationClick(station)
        }

        holder.name.text = station.name ?: context.getString(R.string.station_no_name)
        holder.address.text = station.address ?: context.getString(R.string.station_no_address)

        // Combine city and province
        val location = listOfNotNull(station.city, station.state).joinToString(" - ")
        holder.location.text = if (location.isNotEmpty()) location else context.getString(R.string.station_no_city)

        val price = station.price?.let { "%.3f €".format(it)}
        holder.price.text = if (station.isPublicPrice) price else "*" + price

        val distance = CurrentDistancePolicy.getDistance(station)
        holder.distance.text = distance?.let { "%.1f km".format(it / 1000) } ?: "?? km"

        val status = station.openStatus(Instant.now())
        // TODO: Take the threshod from settings
        holder.openStatus.text = status.forHumans(context)
    }



    override fun getItemCount() = stations.size
}
