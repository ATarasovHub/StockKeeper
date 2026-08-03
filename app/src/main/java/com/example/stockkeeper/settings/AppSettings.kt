package com.example.stockkeeper.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object AppSettings {
    private const val PREFERENCES_NAME = "stockkeeper_settings"
    private const val KEY_LANGUAGE = "language"
    private const val KEY_THEME = "theme"

    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_UKRAINIAN = "uk"
    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun language(context: Context): String = preferences(context)
        .getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH

    fun theme(context: Context): String = preferences(context)
        .getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM

    fun apply(context: Context) {
        applyTheme(theme(context))
        applyLanguage(language(context))
    }

    fun setLanguage(context: Context, language: String) {
        preferences(context).edit().putString(KEY_LANGUAGE, language).apply()
        applyLanguage(language)
    }

    fun setTheme(context: Context, theme: String) {
        preferences(context).edit().putString(KEY_THEME, theme).apply()
        applyTheme(theme)
    }

    private fun applyLanguage(language: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
    }

    private fun applyTheme(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
}
