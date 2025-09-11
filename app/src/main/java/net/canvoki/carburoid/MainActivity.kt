package net.canvoki.carburoid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.network.GasStationApiFactory
import net.canvoki.carburoid.ui.GasStationAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            try {
                val response = GasStationApiFactory.create().getGasStations()
                val adapter = GasStationAdapter(response.stations)
                recyclerView.adapter = adapter
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                e.printStackTrace()
            }
        }
    }
}
