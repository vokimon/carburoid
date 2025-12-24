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
import net.canvoki.carburoid.ui.VerticalScrollbar
import net.canvoki.carburoid.ui.settings.ThemeSettings

@Composable
fun PullOnRefresh(
    isDownloading: Boolean,
    onRefresh: () -> Unit,
    listState: LazyListState,
    content: @Composable () -> Unit,
) {
    val pullRefreshState = rememberPullToRefreshState()

    Box(Modifier.fillMaxSize()) {
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.refreshing_data),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        CircularProgressIndicator()
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
    processing: Boolean = false,
    onStationClicked: (GasStation) -> Unit,
) {
    val listState = rememberLazyListState()

    PullOnRefresh(
        isDownloading = downloading,
        onRefresh = onRefresh,
        listState = listState,
    ) {
        if (processing) {
            LoadingPlaceholder(Modifier.fillMaxSize())
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                if (stations.isEmpty()) {
                    item {
                        NoStationsPlaceholder(
                            Modifier
                                .fillParentMaxSize()
                                .padding(
                                    start = 16.dp,
                                    top = 0.dp,
                                    end = 16.dp,
                                    bottom = 64.dp,
                                ),
                        )
                    }
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
}

/** Wraps the StationList Composable to have theme when
 * included inside a view.
 */
@Composable
private fun StationListWrapper(
    stations: List<GasStation>,
    isDownloading: Boolean,
    isProcessing: Boolean = false,
    onStationClicked: (GasStation) -> Unit,
    onRefresh: () -> Unit,
) {
    MaterialTheme(
        colorScheme = ThemeSettings.effectiveColorScheme(),
    ) {
        StationList(
            stations = stations,
            downloading = isDownloading,
            processing = isProcessing,
            onRefresh = onRefresh,
            onStationClicked = { s ->
                onStationClicked(s)
            },
        )
    }
}

class StationListView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : FrameLayout(context, attrs) {
        private val composeView = ComposeView(context)

        init {
            composeView.layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            addView(composeView)
            updateContent()
        }

        var stations: List<GasStation> = emptyList()
            set(value) {
                field = value
                updateContent()
            }

        var onStationClicked: ((GasStation) -> Unit) = {}
            set(value) {
                field = value
                updateContent()
            }

        var onRefresh: (() -> Unit) = {}
            set(value) {
                field = value
                updateContent()
            }

        var isDownloading: Boolean = false
            set(value) {
                field = value
                updateContent()
            }

        var isProcessing: Boolean = false
            set(value) {
                field = value
                updateContent()
            }

        private fun updateContent() {
            composeView.setContent {
                StationListWrapper(
                    stations = stations,
                    onStationClicked = { station -> onStationClicked(station) },
                    onRefresh = { onRefresh() },
                    isDownloading = isDownloading,
                    isProcessing = isProcessing,
                )
            }
        }
    }
