import android.content.Context
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.canvoki.carburoid.algorithms.FilterSettings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

val screen = mock<PreferenceScreen> {
    on { context } doReturn ApplicationProvider.getApplicationContext()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FilterSettingsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun teardown() {
        FilterSettings.unregister(context)
    }

    @Test
    fun `config reads hide_expensive_further correctly`() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("hide_expensive_further", true).commit()

        val config = FilterSettings.config(context)
        assertTrue(config.hideExpensiveFurther)
    }

    @Test
    fun `flow emits on relevant key change`() = runTest {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Register the listener
        FilterSettings.registerIn(FakePreferenceScreen(context))

        // Change the relevant preference
        prefs.edit().putBoolean("hide_expensive_further", false).commit()

        // Should emit Unit
        val emitted = FilterSettings.changes.first()
        assertEquals(Unit, emitted)
    }
}
