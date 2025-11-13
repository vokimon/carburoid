package net.canvoki.carburoid.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log
import java.util.Locale

object LanguageSettings {
    private const val SYSTEM_LANGUAGE = "system"
    private const val KEY = "app_language"
    private var systemLocale: Locale? = null

    private var availableLanguagesCache: List<LanguageOption>? = null

    data class LanguageOption(
        val code: String,
        val name: String, // Nom en el seu propi idioma
    )

    fun registerIn(screen: PreferenceScreen) {
        val context = screen.context
        val languagePref = screen.findPreference<ListPreference>(KEY) ?: return

        updateSummary(languagePref, context)
        updateEntries(languagePref, context)

        languagePref.setOnPreferenceChangeListener { _: Preference, newValue: Any ->
            val languageCode = newValue as String
            setPreferencesLanguage(context, languageCode)
            apply(context)
            updateEntries(languagePref, context)
            updateSummary(languagePref, context)
            true
        }
    }

    private fun getAvailableLanguages(context: Context): List<LanguageOption> {
        val systemLanguageOption = LanguageOption(
            SYSTEM_LANGUAGE,
            context.getString(R.string.language_system_default),
        )
        val availableLanguages = availableLanguagesCache
        if (availableLanguages != null) {
            return listOf(systemLanguageOption) + availableLanguages
        }

        val supportedCodes = context.resources.getStringArray(R.array.supported_language_codes).toList()

        val languages = mutableListOf<LanguageOption>()

        for (code in supportedCodes) {
            val localizedContext = createLocalizedContext(context, code)
            val languageName = localizedContext.getString(R.string.language_name)
            languages.add(LanguageOption(code, languageName))
        }

        availableLanguagesCache = languages
        return listOf(systemLanguageOption) + languages
    }

    private fun createLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale.forLanguageTag(languageCode)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    private fun updateSummary(preference: ListPreference, context: Context) {
        val currentCode = getPreferencesLanguage(context)
        val languages = getAvailableLanguages(context)
        val currentLanguage = languages.find { it.code == currentCode }

        preference.summary = currentLanguage?.name ?: context.getString(R.string.language_system_default)
    }

    private fun updateEntries(preference: ListPreference, context: Context) {
        val languages = getAvailableLanguages(context)
        preference.entries = languages.map { it.name }.toTypedArray()
        preference.entryValues = languages.map { it.code }.toTypedArray()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    private fun setPreferencesLanguage(context: Context, languageCode: String) {
        getPrefs(context).edit {
            putString(KEY, languageCode)
        }
    }

    private fun getPreferencesLanguage(context: Context): String {
        val prefs = getPrefs(context)
        return prefs.getString(KEY, SYSTEM_LANGUAGE) ?: SYSTEM_LANGUAGE
    }

    private fun holdSystemLocale(context: Context) {
        // Just rely on the first call, before ever changing the locale
        if (systemLocale != null) return

        systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ can get systemLocales from LocaleManager
            val localeManager = context.getSystemService(LocaleManager::class.java)
            localeManager?.systemLocales?.get(0) ?: Locale.getDefault()
        } else {
            // Android 8.0+ up to 12: get the system configuration locales
            val locales = Resources.getSystem().configuration.locales
            if (!locales.isEmpty) locales[0] else Locale.getDefault()
        }
    }

    private fun getConfiguredLocale(context: Context): Locale {
        val lang = getPreferencesLanguage(context)
        if (lang == SYSTEM_LANGUAGE) return systemLocale!!
        return Locale.forLanguageTag(lang)
    }

    fun apply(context: Context) {
        // Retrieve system language before we apply any language
        holdSystemLocale(context)
        val locale = getConfiguredLocale(context)
        log("Applying language ${locale.language}")
        Locale.setDefault(locale)
        val locales = LocaleListCompat.create(locale)
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
