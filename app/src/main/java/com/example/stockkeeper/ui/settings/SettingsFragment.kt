package com.example.stockkeeper.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.R
import com.example.stockkeeper.data.backup.BackupManager
import com.example.stockkeeper.settings.AppSettings
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {
    private lateinit var exportButton: MaterialButton
    private lateinit var importButton: MaterialButton

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { exportBackup(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.import_confirm_title)
                .setMessage(R.string.import_confirm_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.import_data) { _, _ -> importBackup(selectedUri) }
                .show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        exportButton = view.findViewById(R.id.exportButton)
        importButton = view.findViewById(R.id.importButton)
        exportButton.setOnClickListener {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            exportLauncher.launch("stockkeeper-backup-$date.zip")
        }
        importButton.setOnClickListener {
            importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
        }

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

    private fun exportBackup(uri: android.net.Uri) {
        setBackupButtonsEnabled(false)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { backupManager().export(uri) }
                .onSuccess { view?.let { Snackbar.make(it, R.string.export_success, Snackbar.LENGTH_LONG).show() } }
                .onFailure { showFailure(it) }
            setBackupButtonsEnabled(true)
        }
    }

    private fun importBackup(uri: android.net.Uri) {
        setBackupButtonsEnabled(false)
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching { backupManager().import(uri) }
                .onSuccess {
                    Toast.makeText(requireContext(), R.string.import_success, Toast.LENGTH_LONG).show()
                    requireActivity().recreate()
                }
                .onFailure {
                    showFailure(it)
                    setBackupButtonsEnabled(true)
                }
        }
    }

    private fun backupManager(): BackupManager = BackupManager(
        requireActivity().application as StockKeeperApplication,
    )

    private fun setBackupButtonsEnabled(enabled: Boolean) {
        exportButton.isEnabled = enabled
        importButton.isEnabled = enabled
    }

    private fun showFailure(error: Throwable) {
        view?.let {
            Snackbar.make(it, getString(R.string.backup_failed, error.message.orEmpty()), Snackbar.LENGTH_LONG).show()
        }
    }
}
