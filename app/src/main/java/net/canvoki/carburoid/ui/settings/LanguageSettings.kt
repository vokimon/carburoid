package net.canvoki.carburoid.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import net.canvoki.carburoid.R
import net.canvoki.carburoid.log
import net.canvoki.carburoid.ui.settings.ListPreference
import net.canvoki.carburoid.ui.settings.rememberMutablePreference
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

    fun initialize(context: Context) {
        apply(context)
    }

    private fun getAvailableLanguages(context: Context): List<LanguageOption> {
        val systemLanguageOption =
            LanguageOption(
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
            languages.add(LanguageOption(code, languageName(context, code)))
        }

        availableLanguagesCache = languages
        return listOf(systemLanguageOption) + languages
    }

    private fun languageName(
        context: Context,
        code: String,
    ) = context
        .createConfigurationContext(
            Configuration().apply {
                setLocale(Locale.forLanguageTag(code))
            },
        ).getString(R.string.language_name)

    private fun getPrefs(context: Context): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private fun setPreferencesLanguage(
        context: Context,
        languageCode: String,
    ) {
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

        systemLocale =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ can get systemLocales from LocaleManager
                val localeManager = context.getSystemService(LocaleManager::class.java)
                localeManager?.systemLocales?.get(0) ?: Locale.getDefault()
            } else {
                // Android 8.0+ up to 12: get the system configuration locales
                val locales = Resources.getSystem().configuration.locales
                if (!locales.isEmpty) locales[0] else Locale.getDefault()
            }
    }

    fun apply(context: Context) {
        // Retrieve system language before we apply any language
        holdSystemLocale(context)
        val lang = getPreferencesLanguage(context)
        val locales =
            if (lang == SYSTEM_LANGUAGE) {
                LocaleListCompat.getEmptyLocaleList() // let system decide
            } else {
                LocaleListCompat.create(Locale.forLanguageTag(lang))
            }

        AppCompatDelegate.setApplicationLocales(locales)
    }

    @Composable
    fun rememberLanguage() = rememberMutablePreference(KEY, SYSTEM_LANGUAGE)

    @Composable
    fun Preference() {
        val context = LocalContext.current
        val resources = context.resources

        var currentValue by rememberLanguage()

        val systemOption = "system" to stringResource(R.string.language_system_default)
        val supportedCodes =
            remember(resources) {
                resources.getStringArray(R.array.supported_language_codes).toList()
            }

        val options =
            remember(supportedCodes) {
                val languageOptions =
                    supportedCodes.map { code ->
                        code to languageName(context, code)
                    }
                listOf(systemOption) + languageOptions
            }

        val title = stringResource(R.string.settings_language_title)
        val summary =
            options.find { it.first == currentValue }?.second
                ?: stringResource(R.string.language_system_default)

        ListPreference(
            title = title,
            summary = summary,
            icon = R.drawable.ic_translate,
            options = options,
            value = currentValue,
            onChange = { newValue ->
                currentValue = newValue
                apply(context)
            },
        )
    }
}
