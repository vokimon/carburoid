package net.canvoki.shared.test

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/**
 * Drains all immediately runnable coroutines in the test dispatcher.
 * Safe, stable, non-experimental replacement for advanceUntilIdle().
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
fun TestScope.yieldUntilIdle() {
    advanceUntilIdle()
}
