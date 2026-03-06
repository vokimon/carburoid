package net.canvoki.shared.test

import org.junit.Assume.assumeTrue

/**
 * Runs [block] and skips the test if it throws an exception of type [E].
 * Otherwise, rethrows any other exception or succeeds.
 */
inline fun <reified E : Throwable> skipOn(block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        if (e is E) {
            assumeTrue("Skipping test due to: ${e::class.simpleName}", false)
        } else {
            throw e
        }
    }
}
