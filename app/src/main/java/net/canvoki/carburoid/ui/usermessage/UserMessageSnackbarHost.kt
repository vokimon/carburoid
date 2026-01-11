package net.canvoki.carburoid.ui.usermessage

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun UserMessageSnackbarHost() {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        UserMessageBus.messages.collect { message ->
            when (message) {
                is UserMessage.Info -> {
                    snackbarHostState.showSnackbar(message.message)
                }
                is UserMessage.Suggestion -> {
                    val result =
                        snackbarHostState.showSnackbar(
                            message = message.message,
                            actionLabel = message.actionLabel,
                            duration = SnackbarDuration.Indefinite,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        message.action()
                    }
                }
            }
        }
    }

    SnackbarHost(hostState = snackbarHostState)
}
