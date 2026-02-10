package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log
import net.canvoki.carburoid.model.GasStation
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.ui.Flow
import net.canvoki.carburoid.ui.Selectors
import net.canvoki.carburoid.ui.isLandscape

@Composable
fun ButtonCloser(
    index: Int,
    setIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.button_closer)
    Button(
        onClick = { setIndex(index - 1) },
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_keyboard_arrow_left),
            contentDescription = label,
        )
        Text(label)
    }
}

@Composable
fun ButtonCheaper(
    index: Int,
    setIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.button_cheaper)
    Button(
        onClick = { setIndex(index + 1) },
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_keyboard_arrow_right),
            contentDescription = label,
        )
        Text(label)
    }
}

@Composable
fun GasStationScatterPlot(
    items: List<GasStation>,
    allItems: List<GasStation>,
    modifier: Modifier = Modifier,
) {
    val currentItems by rememberUpdatedState(items)
    val currentAllItems by rememberUpdatedState(allItems)
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedItem by remember {
        derivedStateOf {
            currentItems.getOrNull(selectedIndex)
        }
    }
    val product by rememberUpdatedState(ProductManager.getCurrent())

    LaunchedEffect(currentItems.map { it.id }) {
        selectedIndex = 0
    }

    fun selectIndex(index: Int) {
        selectedIndex = index.coerceIn(0..currentItems.lastIndex)
    }

    Material2KoalaTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            Selectors()
            Flow(horizontal = isLandscape()) {
                ScatterPlot(
                    items = currentItems,
                    allItems = currentAllItems,
                    getX = { station: GasStation -> station.distanceInMeters?.div(1000.0f) },
                    getY = { station: GasStation -> station.prices[product]?.toFloat() },
                    selectedIndex = selectedIndex,
                    onIndexSelected = ::selectIndex,
                    modifier = if (isLandscape()) Modifier.fillMaxWidth(0.5f) else Modifier.weight(1f),
                )
                Column(
                    modifier = if (isLandscape()) Modifier.fillMaxWidth() else Modifier,
                ) {
                    GasStationCard(
                        selectedItem,
                        modifier = if (isLandscape()) Modifier.weight(1f) else Modifier,
                    )
                    Flow(
                        horizontal = true,
                        gap = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        ButtonCloser(
                            index = selectedIndex,
                            setIndex = ::selectIndex,
                            modifier = Modifier.weight(1f),
                        )
                        ButtonCheaper(
                            index = selectedIndex,
                            setIndex = ::selectIndex,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
