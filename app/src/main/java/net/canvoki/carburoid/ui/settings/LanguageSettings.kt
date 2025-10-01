package net.canvoki.carburoid.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import java.util.Locale
import net.canvoki.carburoid.R



object LanguageSettings {
    private const val KEY = "app_language"

    // Cache per evitar recàlculs
    private var availableLanguagesCache: List<LanguageOption>? = null

    data class LanguageOption(
        val code: String,
        val name: String  // Nom en el seu propi idioma
    )

    private fun getAvailableLanguages(context: Context): List<LanguageOption> {
        val availableLanguages = availableLanguagesCache
        if (availableLanguages!= null) {
            return availableLanguages
        }

        // Llegeix els codis d'idiomes des dels recursos
        val supportedCodes = context.resources.getStringArray(R.array.supported_language_codes).toList()

        val languages = mutableListOf<LanguageOption>()

        // Afegeix l'opció "Sistema"
        languages.add(LanguageOption("system", context.getString(R.string.language_system_default)))

        // Afegeix els idiomes suportats amb els seus noms en el seu propi idioma
        for (code in supportedCodes) {
            val localizedContext = createLocalizedContext(context, code)
            val languageName = localizedContext.getString(R.string.language_name)
            languages.add(LanguageOption(code, languageName))
        }

        availableLanguagesCache = languages
        return languages
    }

    private fun createLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val languagePref = screen.findPreference<ListPreference>(KEY) ?: return

        updateSummary(languagePref, context)
        updateEntries(languagePref, context)  // Actualitza dinàmicament

        languagePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val languageCode = newValue as String
            saveAndApplyLanguage(context, languageCode)

            (context as? AppCompatActivity)?.recreate()
            updateSummary(languagePref, context)
            true
        }
    }

    private fun updateEntries(preference: ListPreference, context: Context) {
        val languages = getAvailableLanguages(context)
        preference.entries = languages.map { it.name }.toTypedArray()
        preference.entryValues = languages.map { it.code }.toTypedArray()
    }

    private fun saveAndApplyLanguage(context: Context, languageCode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(KEY, languageCode).apply()

        updateLanguage(context, languageCode)
    }

    private fun updateLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            "system" -> getDeviceLocale()
            else -> Locale(languageCode)
        }

        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)

        val metrics = context.resources.displayMetrics
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, metrics)
    }

    private fun getDeviceLocale(): Locale {
        val deviceLanguage = Locale.getDefault().language
        return Locale(deviceLanguage)
    }

    private fun updateSummary(preference: ListPreference, context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val currentCode = prefs.getString(KEY, "system") ?: "system"

        val languages = getAvailableLanguages(context)
        val currentLanguage = languages.find { it.code == currentCode }

        preference.summary = currentLanguage?.name ?: context.getString(R.string.language_system_default)
    }

    fun applyLanguage(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val languageCode = prefs.getString(KEY, "system") ?: "system"
        updateLanguage(context, languageCode)
    }
}
