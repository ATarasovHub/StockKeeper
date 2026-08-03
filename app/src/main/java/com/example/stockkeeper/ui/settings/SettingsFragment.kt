package com.example.stockkeeper.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import com.example.stockkeeper.R
import com.example.stockkeeper.settings.AppSettings
import com.google.android.material.button.MaterialButtonToggleGroup

class SettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val languageGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.languageGroup)
        val languageButton = when (AppSettings.language(requireContext())) {
            AppSettings.LANGUAGE_UKRAINIAN -> R.id.ukrainianButton
            else -> R.id.englishButton
        }
        languageGroup.check(languageButton)
        languageGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || checkedId == languageButton) return@addOnButtonCheckedListener
            val language = if (checkedId == R.id.ukrainianButton) {
                AppSettings.LANGUAGE_UKRAINIAN
            } else {
                AppSettings.LANGUAGE_ENGLISH
            }
            AppSettings.setLanguage(requireContext(), language)
        }

        val themeGroup = view.findViewById<RadioGroup>(R.id.themeGroup)
        val themeButton = when (AppSettings.theme(requireContext())) {
            AppSettings.THEME_LIGHT -> R.id.lightTheme
            AppSettings.THEME_DARK -> R.id.darkTheme
            else -> R.id.systemTheme
        }
        themeGroup.check(themeButton)
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == themeButton) return@setOnCheckedChangeListener
            val theme = when (checkedId) {
                R.id.lightTheme -> AppSettings.THEME_LIGHT
                R.id.darkTheme -> AppSettings.THEME_DARK
                else -> AppSettings.THEME_SYSTEM
            }
            AppSettings.setTheme(requireContext(), theme)
        }
    }
}
