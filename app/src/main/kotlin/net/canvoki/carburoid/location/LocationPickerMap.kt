package net.canvoki.carburoid.location

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import net.canvoki.carburoid.FeatureFlags
import net.canvoki.carburoid.R
import net.canvoki.shared.log
import net.canvoki.shared.settings.ThemeSettings
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.offset
import org.maplibre.compose.expressions.value.SymbolAnchor
import org.maplibre.compose.expressions.value.SymbolOverlap
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.material3.ScaleBar
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun LocationPickerMap(
    currentPosition: Position,
    targetPosition: Position?,
    onCurrentPositionChanged: (Position) -> Unit,
    onTargetPositionChanged: (Position?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val markerScaling = 2f
    //"https://tiles.openfreemap.org/styles/positron"
    //"https://tiles.openfreemap.org/styles/liberty"
    //"https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
    //"https://basemaps.cartocdn.com/gl/voyager-gl-style/style.json"
    val styleUrl =
        if (ThemeSettings.isDarkTheme()) {
            "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
        } else {
            "https://tiles.openfreemap.org/styles/liberty"
        }

    val cameraState =
        rememberCameraState(
            CameraPosition(
                target = currentPosition,
                zoom = 15.0,
            ),
        )
    val styleState = rememberStyleState()

    val targetPositionRef = remember { mutableStateOf(targetPosition) }
    LaunchedEffect(targetPosition) {
        targetPositionRef.value = targetPosition
    }

    fun offsetInMarker(
        offset: DpOffset,
        position: Position?,
    ): Boolean {
        if (position == null) return false
        val markerOffset = cameraState.projection?.screenLocationFromPosition(position) ?: return false
        val left = -6.dp * markerScaling + markerOffset.x
        val right = 18.dp * markerScaling + markerOffset.x
        val top = -24.dp * markerScaling + markerOffset.y
        val bottom = 0.dp * markerScaling + markerOffset.y
        if (offset.x !in left..right) return false
        if (offset.y !in top..bottom) return false
        return true
    }

    LaunchedEffect(currentPosition) {
        cameraState.animateTo(
            finalPosition =
                cameraState.position.copy(
                    target = currentPosition,
                ),
            duration = 500.milliseconds,
        )
    }
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = cameraState,
            styleState = styleState,
            options =
                MapOptions(
                    gestureOptions =
                        GestureOptions(
                            isTiltEnabled = true,
                            isZoomEnabled = true,
                            isRotateEnabled = false,
                            isScrollEnabled = true,
                        ),
                    ornamentOptions = OrnamentOptions.OnlyLogo,
                ),
            onMapClick = { pos, offset ->
                if (offsetInMarker(offset, targetPositionRef.value)) {
                    onTargetPositionChanged(null)
                } else {
                    onCurrentPositionChanged(pos)
                }
                ClickResult.Consume
            },
            onMapLongClick = { pos, offset ->
                onTargetPositionChanged(pos)
                ClickResult.Consume
            },
        ) {
            val points by remember(currentPosition) {
                derivedStateOf {
                    FeatureCollection(
                        listOf(
                            Feature(
                                geometry = Point(currentPosition),
                                properties = kotlinx.serialization.json.JsonObject(emptyMap()),
                            ),
                        ),
                    )
                }
            }
            val source = rememberGeoJsonSource(data = GeoJsonData.Features(points))
            SymbolLayer(
                id = "current_position",
                source = source,
                iconImage = image(painterResource(R.drawable.ic_emoji_people)),
                iconSize = const(markerScaling),
                iconAnchor = const(SymbolAnchor.Bottom),
                iconOverlap = const(SymbolOverlap.Always),
                iconAllowOverlap = const(true),
            )
            if (FeatureFlags.routeDeviation && targetPosition != null) {
                val pos: Position = targetPosition

                val targetPoints =
                    FeatureCollection(
                        Feature(
                            geometry =
                                Point(
                                    latitude = pos.latitude,
                                    longitude = pos.longitude,
                                ),
                            properties = kotlinx.serialization.json.JsonObject(emptyMap()),
                        ),
                    )
                SymbolLayer(
                    id = "target_position",
                    source = rememberGeoJsonSource(data = GeoJsonData.Features(targetPoints)),
                    iconImage = image(painterResource(R.drawable.ic_sports_score)),
                    iconSize = const(markerScaling),
                    iconAnchor = const(org.maplibre.compose.expressions.value.SymbolAnchor.BottomLeft),
                    iconOverlap = const(org.maplibre.compose.expressions.value.SymbolOverlap.Always),
                    iconAllowOverlap = const(true),
                    iconPadding = const(PaddingValues.Absolute(10.dp)),
                    iconOffset = offset(-5.dp, 4.dp),
                )
            }
        }
        Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            ScaleBar(
                cameraState.metersPerDpAtTarget,
                modifier = Modifier.align(Alignment.TopStart),
                alignment = Alignment.Start,
                haloWidth = 2.dp,
            )
            ExpandingAttributionButton(
                cameraState = cameraState,
                styleState = styleState,
                modifier = Modifier.align(Alignment.BottomEnd),
                contentAlignment = Alignment.BottomEnd,
            )
        }
    }
}
