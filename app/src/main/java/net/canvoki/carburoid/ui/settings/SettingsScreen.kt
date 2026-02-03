package net.canvoki.carburoid.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.log

@Composable
fun SettingsScreen() {
    MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.Red),
        ) {
            log("Composing SettingsScreen")
            Text(
                text = "Settings (Compose)",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            // TODO: Add individual settings here
        }
    }
}
