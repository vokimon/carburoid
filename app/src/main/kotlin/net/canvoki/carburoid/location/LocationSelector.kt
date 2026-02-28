package net.canvoki.carburoid.location

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.canvoki.carburoid.CarburoidApplication
import net.canvoki.carburoid.R
import net.canvoki.shared.log

@Composable
fun LocationSelector(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as CarburoidApplication
    val service = app.locationService
    val refreshCurrentLocation = service.rememberLocationRefresher()

    val descriptionFlowValue by service.descriptionUpdated.collectAsStateWithLifecycle(
        initialValue = service.getCurrentLocationDescription(),
    )

    var description by remember { mutableStateOf(descriptionFlowValue) }
    var refreshing by remember { mutableStateOf(false) }

    // TODO: Fix: use eventFlow not value update.
    // refreshing has to be updated, not when the value changes,
    // but when the value arribes event if it is the same.
    LaunchedEffect(descriptionFlowValue) {
        description = descriptionFlowValue
        refreshing = false
    }

    LaunchedEffect(Unit) {
        // Only use device location if no fixed location exists
        if (service.getCurrentLocation() == null) {
            refreshing = true
            refreshCurrentLocation()
        }
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            log("RETURNING FROM LocationPickerActivity $result")
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                intent?.let {
                    service.stateFromIntent(it)
                }
            }
        }

    TextField(
        value = description,
        onValueChange = {},
        readOnly = true,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
        label = {
            Text(stringResource(R.string.location_selector_hint))
        },
        leadingIcon = {
            IconButton(
                onClick = {
                    log("REFRESHING ON ICON PRESS")
                    refreshing = true
                    refreshCurrentLocation()
                },
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (refreshing) R.drawable.ic_refresh else R.drawable.ic_my_location,
                        ),
                    contentDescription = stringResource(R.string.location_selector_locate_device),
                )
            }
        },
        trailingIcon = {
            IconButton(
                onClick = {
                    val intent =
                        Intent(app, LocationPickerActivity::class.java)
                    service.stateToIntent(intent)
                    launcher.launch(intent)
                },
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = stringResource(R.string.location_selector_locate_device),
                )
            }
        },
        minLines = 1,
        maxLines = 1,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = {
            Text(
                text = stringResource(R.string.location_selector_hint),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
    )
}
