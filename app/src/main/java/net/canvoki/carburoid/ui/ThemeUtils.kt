package net.canvoki.carburoid.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

fun applyPreferencesTheme(context: Context) {
    val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
    val themeMode = prefs.getString("dark_mode", "auto")
    when (themeMode) {
        "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        "auto" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
