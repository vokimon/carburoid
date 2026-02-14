package net.canvoki.carburoid.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.canvoki.carburoid.location.LocationSelector
import net.canvoki.carburoid.product.CategorizedProductSelector
import net.canvoki.shared.component.Flow
import net.canvoki.shared.component.isLandscape

@Composable
fun Selectors() {
    val isHorizontal = isLandscape()

    Flow(
        horizontal = isHorizontal,
        gap = if (isHorizontal) 4.dp else 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        val weightIfHorizontal =
            if (isHorizontal) {
                Modifier.weight(1f)
            } else {
                Modifier
            }
        CategorizedProductSelector(
            modifier =
                Modifier
                    .padding(bottom = 8.dp)
                    .then(weightIfHorizontal),
        )
        LocationSelector(
            modifier =
                Modifier
                    .padding(bottom = 8.dp)
                    .then(weightIfHorizontal),
        )
    }
}
