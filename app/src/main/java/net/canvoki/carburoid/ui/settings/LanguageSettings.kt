package net.canvoki.carburoid.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import net.canvoki.carburoid.R
import java.util.Locale

object LanguageSettings {
    private const val KEY = "app_language"

    // Cache per evitar recàlculs
    private var availableLanguagesCache: List<LanguageOption>? = null

    data class LanguageOption(
        val code: String,
        val name: String, // Nom en el seu propi idioma
    )

    fun initializeLanguage(context: Context) {
        val languageCode = getLanguagePreference(context)
        var locale = setApplicationLanguage(context, languageCode)
        setActivityLanguage(context, locale)
    }

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val languagePref = screen.findPreference<ListPreference>(KEY) ?: return

        updateSummary(languagePref, context)
        updateEntries(languagePref, context)

        languagePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val languageCode = newValue as String
            setLanguagePreference(context, languageCode)
            var locale = setApplicationLanguage(context, languageCode)
            setActivityLanguage(context, locale)

            (context as? AppCompatActivity)?.recreate()

            updateSummary(languagePref, context)
            true
        }
    }

    private fun getAvailableLanguages(context: Context): List<LanguageOption> {
        val availableLanguages = availableLanguagesCache
        if (availableLanguages != null) {
            return availableLanguages
        }

        val supportedCodes = context.resources.getStringArray(R.array.supported_language_codes).toList()

        val languages = mutableListOf<LanguageOption>()
        languages.add(LanguageOption("system", context.getString(R.string.language_system_default)))

        for (code in supportedCodes) {
            val localizedContext = createLocalizedContext(context, code)
            val languageName = localizedContext.getString(R.string.language_name)
            languages.add(LanguageOption(code, languageName))
        }

        availableLanguagesCache = languages
        return languages
    }

    private fun createLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    private fun updateSummary(preference: ListPreference, context: Context) {
        val currentCode = getLanguagePreference(context)
        val languages = getAvailableLanguages(context)
        val currentLanguage = languages.find { it.code == currentCode }

        preference.summary = currentLanguage?.name ?: context.getString(R.string.language_system_default)
    }

    private fun updateEntries(preference: ListPreference, context: Context) {
        val languages = getAvailableLanguages(context)
        preference.entries = languages.map { it.name }.toTypedArray()
        preference.entryValues = languages.map { it.code }.toTypedArray()
    }

    private fun setLanguagePreference(context: Context, languageCode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit {
            putString(KEY, languageCode)
        }
    }

    fun getLanguagePreference(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY, "system") ?: "system"
    }

    fun getConfiguredLanguageCode(context: Context): String {
        val preference = getLanguagePreference(context)
        if (preference!="system") {
            return preference
        }
        return getDeviceLocale().language
    }

    private fun setApplicationLanguage(context: Context, languageCode: String): Locale {
        val locale = when (languageCode) {
            "system" -> getDeviceLocale()
            else -> Locale.forLanguageTag(languageCode)
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.createConfigurationContext(config)
        return locale
    }

    fun setActivityLanguage(context: Context, languageCode: String) {
        val locale = when (languageCode) {
            "system" -> Resources.getSystem().configuration.locales[0]
            else -> Locale.forLanguageTag(languageCode)
        }

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        val metrics = context.resources.displayMetrics
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, metrics)
    }

    fun setActivityLanguage(context: Context, locale: Locale) {
        // Configure locale for the resources to come (not existing ones)
        val config = context.resources.configuration
        config.setLocale(locale)

        val metrics = context.resources.displayMetrics
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, metrics)
    }

    private fun getDeviceLocale(): Locale {
        val locales = Resources.getSystem().configuration.locales
        return if (locales.isEmpty) Locale.getDefault() else locales[0]
    }

    fun getApplicationLanguage(): String {
        return Locale.getDefault().language
    }
}
