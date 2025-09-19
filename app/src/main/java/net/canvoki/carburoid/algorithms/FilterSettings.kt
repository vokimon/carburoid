package net.canvoki.carburoid.algorithms

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.Flow
import net.canvoki.carburoid.log


object FilterSettings {

    private const val PREFS_NAME = "app_settings"
    private const val KEY_HIDE_EXPENSIVE = "hide_expensive_further"
    private const val ONLY_PUBLIC_PRICES = "only_public_prices"

    private val relevantKeys = setOf(
        KEY_HIDE_EXPENSIVE,
        ONLY_PUBLIC_PRICES,
    )

    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val changes: Flow<Unit> get() = _changes.asSharedFlow()

    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun registerIn(screen: PreferenceScreen) {
        log("registerIn FilterSettings")
        val context = screen.context
        val prefs = preferences(context)

        //unregister(context) // ✅ Safe unregister in case it's already registered

        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            log("Changed $key")
            if (key in relevantKeys) {
                _changes.tryEmit(Unit)
            }
        }

        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(context: Context) {
        listener?.let {
            preferences(context).unregisterOnSharedPreferenceChangeListener(it)
            listener = null
        }
    }

    fun config(context: Context): FilterConfig {
        val prefs = preferences(context)
        return FilterConfig(
            hideExpensiveFurther = prefs.getBoolean(KEY_HIDE_EXPENSIVE, false),
            onlyPublicPrices = prefs.getBoolean(ONLY_PUBLIC_PRICES, false),
        )
    }

    private fun preferences(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }
}
