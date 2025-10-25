package net.canvoki.carburoid.deeplink

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import net.canvoki.carburoid.location.GeoPoint
import net.canvoki.carburoid.MainActivity
import net.canvoki.carburoid.log

class DeepLinkHandler : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inputText = when (intent.action) {
            Intent.ACTION_VIEW -> {
                val uri: Uri? = intent.data
                uri?.toString()
            }
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)
            }
            else -> null
        }

        val geoPoint = GeoPoint.fromText(inputText)

        startActivity(Intent(this, MainActivity::class.java).apply {
            if (geoPoint != null) {
                putExtra(MainActivity.EXTRA_LOCATION, geoPoint.toAndroidLocation())
            }
            putExtra(MainActivity.EXTRA_SOURCE, intent.action)
        })
        lifecycleScope.launch {
            finish()
        }
    }
}
