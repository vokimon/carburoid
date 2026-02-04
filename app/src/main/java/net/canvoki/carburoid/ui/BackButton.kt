package net.canvoki.carburoid.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.canvoki.carburoid.R

@Composable
fun BackButton(modifier: Modifier = Modifier) {
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    IconButton(
        modifier = modifier,
        onClick = { backDispatcher?.onBackPressed() },
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.menu_back),
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }
}
