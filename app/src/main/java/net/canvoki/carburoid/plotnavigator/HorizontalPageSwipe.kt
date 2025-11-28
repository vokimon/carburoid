package net.canvoki.carburoid.plotnavigator

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sign

fun Modifier.horizontalSwipe(
    swipeThreshold: Float = 50f,
    longPressMillis: Long = 400,
    minRepeatDelay: Long = 300,
    maxRepeatDelay: Long = 1000,
    onStep: (Int) -> Unit,
): Modifier =
    composed {
        val controller = remember { StepwiseScrollController() }
        var shortSwipeFired by remember { mutableStateOf(false) }

        // --- LONG PRESS REPEAT LOOP ---
        LaunchedEffect(controller.active, controller.longPressActive, controller.direction) {
            while (controller.active && controller.longPressActive && controller.direction != 0) {
                val delayMs =
                    calculateDelay(
                        deltaX = controller.lastDeltaX,
                        minDelay = minRepeatDelay,
                        maxDelay = maxRepeatDelay,
                    )
                onStep(controller.direction)
                delay(delayMs)
            }
        }

        pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val startX = down.position.x

                controller.active = true
                controller.longPressActive = false
                controller.direction = 0
                controller.lastDeltaX = 0f
                shortSwipeFired = false

                val downTime = System.currentTimeMillis()

                horizontalDrag(down.id) { change: PointerInputChange ->
                    val totalDeltaX = change.position.x - startX
                    controller.lastDeltaX = totalDeltaX
                    controller.direction = totalDeltaX.sign.toInt()

                    val now = System.currentTimeMillis()
                    val elapsed = now - downTime

                    // Long press activation (only if not short swipe)
                    if (!shortSwipeFired && !controller.longPressActive && elapsed >= longPressMillis) {
                        controller.longPressActive = true
                    }

                    change.consume()
                }

                // --- ON FINGER UP ---
                val elapsed = System.currentTimeMillis() - downTime

                // If NOT long press and displacement above threshold → short swipe
                if (!controller.longPressActive &&
                    abs(controller.lastDeltaX) >= swipeThreshold
                ) {
                    val dir = controller.lastDeltaX.sign.toInt()
                    if (dir != 0) {
                        onStep(dir)
                        shortSwipeFired = true
                    }
                }

                // Reset controller at end of gesture
                controller.active = false
                controller.longPressActive = false
                controller.direction = 0
                controller.lastDeltaX = 0f
            }
        }
    }

// -------------------------------------------
// Internal controller
// -------------------------------------------
private class StepwiseScrollController {
    var direction: Int by mutableStateOf(0)
    var active: Boolean by mutableStateOf(false)
    var longPressActive: Boolean by mutableStateOf(false)
    var lastDeltaX: Float by mutableStateOf(0f)
}

private fun calculateDelay(
    deltaX: Float,
    minDelay: Long,
    maxDelay: Long,
): Long {
    val proportion = (abs(deltaX) / 200f).coerceIn(0f, 1f)
    return (maxDelay - (maxDelay - minDelay) * proportion).toLong()
}
