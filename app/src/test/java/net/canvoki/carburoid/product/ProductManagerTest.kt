package net.canvoki.carburoid.product

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import net.canvoki.carburoid.product.ProductManager
import net.canvoki.carburoid.test.yieldUntilIdle
import org.junit.Test
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductManagerTest {
    @Test
    fun `test product change triggers reloadStations`() =
        runTest {
            val collector = mutableListOf<Unit>()

            val collectorJob =
                launch {
                    ProductManager.productChanged.collect {
                        collector.add(Unit)
                    }
                }

            yieldUntilIdle()
            assertTrue(collector.isEmpty(), "Collector should be empty before product change.")

            ProductManager.setCurrent("Gasolina 95 E10")

            yieldUntilIdle()

            assertTrue(collector.isNotEmpty(), "Collector was not notified when product changed.")

            collectorJob.cancel()
        }
}
