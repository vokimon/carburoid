package net.canvoki.carburoid.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.ui.settings.ThemeSettings
import net.canvoki.carburoid.plotnavigator.GasStationCard

@Composable
fun StationList(
    stations: List<GasStation>,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    // State remembers position and caches elements
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = refreshing,
        onRefresh = onRefresh,
    ) {
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                items = stations,
                key = { it.id },
                contentType = {"station"},
            ) { station ->
                GasStationCard(station)
            }
        }
    }
}

@Composable
private fun StationListWrapper(stations: List<GasStation>) {
    MaterialTheme(
        colorScheme = ThemeSettings.effectiveColorScheme(),
    ) {
        StationList(stations, refreshing=false, onRefresh={})
    }
}

class StationListView
@JvmOverloads
constructor(
    context: Context,
    attrs: AttributeSet? = null,
): FrameLayout(context, attrs) {

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

    var onStationClick: ((GasStation) -> Unit)? = null
        set(value) {
            field = value
            updateContent()
        }

    private fun updateContent() {
        composeView.setContent {
            StationListWrapper(stations)
        }
    }
}
