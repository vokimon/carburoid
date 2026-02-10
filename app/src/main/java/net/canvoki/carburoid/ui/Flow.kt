package net.canvoki.carburoid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
@ReadOnlyComposable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
}

interface FlowScope {
    fun Modifier.weight(
        weight: Float,
        fill: Boolean = false,
    ): Modifier
}

@Composable
fun Flow(
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
    gap: Dp = 0.dp,
    content: @Composable FlowScope.() -> Unit,
) {
    if (horizontal) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            val rowScope: RowScope = this

            val flowScope =
                object : FlowScope {
                    override fun Modifier.weight(
                        weight: Float,
                        fill: Boolean,
                    ): Modifier =
                        with(rowScope) {
                            Modifier.weight(weight, fill)
                        }
                }
            flowScope.content()
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            val columnScope: ColumnScope = this
            val flowScope =
                object : FlowScope {
                    override fun Modifier.weight(
                        weight: Float,
                        fill: Boolean,
                    ): Modifier =
                        with(columnScope) {
                            Modifier.weight(weight, fill)
                        }
                }
            flowScope.content()
        }
    }
}
