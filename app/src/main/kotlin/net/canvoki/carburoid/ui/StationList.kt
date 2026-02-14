package net.canvoki.carburoid.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.plotnavigator.GasStationCard
import net.canvoki.carburoid.ui.DownloadingPill
import net.canvoki.shared.component.VerticalScrollbar
import net.canvoki.shared.settings.ThemeSettings

@Composable
fun PullOnRefresh(
    isDownloading: Boolean,
    onRefresh: () -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier) {
        if (isDownloading) {
            content()
            DownloadingPill()
        } else {
            PullToRefreshBox(
                state = pullRefreshState,
                isRefreshing = false,
                onRefresh = onRefresh,
            ) {
                content()
            }
        }
        VerticalScrollbar(
            state = listState,
            modifier =
                Modifier
                    .width(4.dp)
                    .padding(vertical = 16.dp)
                    .align(Alignment.CenterEnd),
            thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

@Composable
fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_local_gas_station),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(0.1f),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.refreshing_data),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            CircularProgressIndicator()
        }
    }
}

@Composable
fun NoStationsPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_local_gas_station),
            contentDescription = null,
            modifier =
                Modifier
                    .fillMaxSize()
                    .alpha(0.1f),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.no_gas_stations),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
        )
    }
}

@Composable
fun StationList(
    stations: List<GasStation>,
    downloading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    processing: Boolean = false,
    onStationClicked: (GasStation) -> Unit,
) {
    val listState = rememberLazyListState()

    PullOnRefresh(
        isDownloading = downloading,
        onRefresh = onRefresh,
        listState = listState,
        modifier = modifier,
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (processing) {
                // Out of LazyColumn to disable reloading
                item { LoadingPlaceholder(Modifier.fillMaxSize().fillParentMaxSize()) }
            } else if (stations.isEmpty()) {
                item { NoStationsPlaceholder(Modifier.fillMaxSize().fillParentMaxSize()) }
            } else {
                items(
                    items = stations,
                    key = { it.id },
                    contentType = { "station" },
                ) { station ->
                    GasStationCard(station, onClick = {
                        onStationClicked(station)
                    })
                }
            }
        }
    }
}
