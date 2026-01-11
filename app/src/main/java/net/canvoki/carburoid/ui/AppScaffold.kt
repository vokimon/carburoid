package net.canvoki.carburoid.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.canvoki.carburoid.ui.settings.ThemeSettings
import net.canvoki.carburoid.ui.usermessage.UserMessageSnackbarHost

@Composable
fun AppScaffold(
    topBar: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = ThemeSettings.effectiveColorScheme()) {
        Scaffold(
            topBar = topBar,
            snackbarHost = { UserMessageSnackbarHost() },
        ) { padding ->
            Column(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(padding),
            ) {
                content()
            }
        }
    }
}
