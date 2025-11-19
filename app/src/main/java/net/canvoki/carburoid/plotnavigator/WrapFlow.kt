package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
//import androidx.compose.foundation.layout.crossAxisSpacing
//import androidx.compose.foundation.layout.mainAxisSpacing
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp



@Composable
private fun FlowRowScope(
    content: @Composable () -> Unit,
    minItemWidth: Dp
) {
    Layout(
        content = content,
        modifier = Modifier
    ) { measurables, constraints ->
        // Simplement assignem minItemWidth a cada fill
        val placeables = measurables.map { measurable ->
            val itemConstraints = constraints.copy(
                minWidth = minItemWidth.roundToPx()
            )
            measurable.measure(itemConstraints)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            // FlowRow farà el posicionament real — només fem forwarding
            placeables.forEach { placeable ->
                placeable.placeRelative(0, 0)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapFlow(
    modifier: Modifier = Modifier,
    minItemWidth: Dp = 300.dp,
    content: @Composable () -> Unit
) {
    FlowRow(
        modifier = modifier,
        maxItemsInEachRow = Int.MAX_VALUE,
    ) {
        FlowRowScope(content = content, minItemWidth = minItemWidth)
    }
}
