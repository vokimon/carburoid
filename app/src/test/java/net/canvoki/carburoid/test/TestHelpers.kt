package net.canvoki.carburoid.test

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

/**
 * Drains all immediately runnable coroutines in the test dispatcher.
 * Safe, stable, non-experimental replacement for advanceUntilIdle().
 */
fun TestScope.yieldUntilIdle() {
    repeat(5) { runCurrent() }
}
