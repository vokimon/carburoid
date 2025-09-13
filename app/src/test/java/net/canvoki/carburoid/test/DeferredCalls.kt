package net.canvoki.carburoid.test

import io.mockk.coEvery
import kotlinx.coroutines.CompletableDeferred

fun <T> deferredCalls(
    mockedCall: suspend () -> T,
    count: Int
): List<CompletableDeferred<T>> {
    require(count > 0) { "You must request at least one deferred" }

    val deferreds = List(count) { CompletableDeferred<T>() }
    val iterator = deferreds.iterator()

    coEvery { mockedCall() } coAnswers {
        if (iterator.hasNext()) iterator.next().await()
        else throw IllegalStateException("No more deferreds available")
    }

    return deferreds
}
