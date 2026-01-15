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
import net.canvoki.carburoid.log
import net.canvoki.carburoid.ui.settings.ThemeSettings

@Composable
fun LocationSelector(
    activity: ComponentActivity,
    modifier: Modifier = Modifier,
) {
    val app = LocalContext.current.applicationContext as CarburoidApplication
    val service = app.locationService
    val refreshAction = service.rememberLocationController()

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
            refreshAction.invoke()
        }
    }

    val launcher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val lat = data?.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LAT, 0.0)
                val lon = data?.getDoubleExtra(LocationPickerActivity.EXTRA_SELECTED_LON, 0.0)
                if (lat != null && lon != null) {
                    val newLocation =
                        Location("user_picked").apply {
                            latitude = lat
                            longitude = lon
                        }
                    service.setFixedLocation(newLocation)
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
                    refreshAction.invoke()
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
                    val current = service.getCurrentLocation()
                    val intent =
                        Intent(activity, LocationPickerActivity::class.java).apply {
                            putExtra(LocationPickerActivity.EXTRA_CURRENT_LAT, current?.latitude)
                            putExtra(LocationPickerActivity.EXTRA_CURRENT_LON, current?.longitude)
                            putExtra(
                                LocationPickerActivity.EXTRA_CURRENT_DESCRIPTION,
                                service.getCurrentLocationDescription(),
                            )
                        }
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
