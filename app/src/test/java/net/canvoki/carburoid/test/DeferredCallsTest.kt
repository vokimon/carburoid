package net.canvoki.carburoid.test

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.Ignore
import net.canvoki.carburoid.test.yieldUntilIdle


class DeferredCallsTest {

    interface Api {
        suspend operator fun invoke(): String
    }

    @Test
    fun `deferred are resolved independently`() = runTest {
        // Mock the API
        val api = mockk<Api>()

        val (deferred1, deferred2) = deferredCalls({api()}, 2)

        val call1 = async { api() }
        val call2 = async { api() }

        deferred1.complete("First")
        deferred2.complete("Second")
        yieldUntilIdle()

        assertEquals("First", call1.await())
        assertEquals("Second", call2.await())
    }

    @Test
    fun `exhausting deferreds, throws`() = runTest {
        val api = mockk<Api>()

        val (deferred1, deferred2) = deferredCalls({api()}, 2)
        deferred1.complete("Done 1")
        deferred2.complete("Done 2")

        val call1 = async { api() }.await()
        val call2 = async { api() }.await()
        val call3 = async {
            val exception = assertFailsWith<IllegalStateException> {
                api()
            }
            assertEquals("No more deferreds available", exception.message)
        }
        call3.await()
    }

    @Test
    fun `side effects are run after yieldUntilIdle`() = runTest {
        val api = mockk<Api>()
        val (deferred) = deferredCalls({ api() }, 1)

        var sideEffect: String? = null

        // First async call with side effect 1
        val call = async {
            val result = api()
            sideEffect = "done: $result"
        }

        assertNull(sideEffect)

        deferred.complete("Hello")
        assertNull(sideEffect)

        yieldUntilIdle()
        assertEquals("done: Hello", sideEffect)
    }


}
