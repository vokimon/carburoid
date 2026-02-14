package net.canvoki.carburoid.distances

import android.location.Location
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.canvoki.shared.test.yieldUntilIdle
import org.junit.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class CurrentDistancePolicyTest {
    @Test
    fun `test distance policy method change triggers notification`() =
        runTest {
            val mockLocation = mockk<Location>(relaxed = true)
            every { mockLocation.latitude } returns 40.7128
            every { mockLocation.longitude } returns -74.0060
            val collector = mutableListOf<Unit>()

            val collectorJob =
                launch {
                    CurrentDistancePolicy.methodChanged.collect {
                        collector.add(Unit)
                    }
                }

            yieldUntilIdle()

            assertTrue(collector.isEmpty(), "Collector should have no events.")

            val newDistanceMethod = DistanceFromAddress(mockLocation)
            CurrentDistancePolicy.setMethod(newDistanceMethod)
            yieldUntilIdle()

            assertTrue(collector.isNotEmpty(), "Collector was not notified when the method changed.")

            collectorJob.cancel()
        }
}
