package com.example.stockkeeper.ui.directories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.stockkeeper.R
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class DirectoriesFragment : Fragment() {
    private val viewModel by viewModels<DirectoryViewModel> {
        val app = requireActivity().application as StockKeeperApplication
        DirectoryViewModelFactory(app.stockRepository)
    }
    private val adapter = DirectoryAdapter(::showEditor, ::confirmDelete)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_directories, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val list = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.directoryList)
        val empty = view.findViewById<View>(R.id.directoryEmptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.setHasFixedSize(true)
        list.adapter = adapter

        view.findViewById<MaterialButtonToggleGroup>(R.id.directoryTypeGroup)
            .addOnButtonCheckedListener { _, checkedId, checked ->
                if (!checked) return@addOnButtonCheckedListener
                viewModel.type.value = when (checkedId) {
                    R.id.customersButton -> DirectoryType.CUSTOMERS
                    R.id.locationsButton -> DirectoryType.LOCATIONS
                    else -> DirectoryType.MANUFACTURERS
                }
            }
        view.findViewById<ExtendedFloatingActionButton>(R.id.addDirectoryButton)
            .setOnClickListener { showEditor(null) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.entries.collect { entries ->
                    adapter.submitList(entries)
                    empty.isVisible = entries.isEmpty()
                }
            }
        }
    }

    private fun showEditor(entry: DirectoryEntry?) {
        val content = layoutInflater.inflate(R.layout.dialog_directory_entry, null)
        val fields = listOf<TextInputLayout>(
            content.findViewById(R.id.directoryField1),
            content.findViewById(R.id.directoryField2),
            content.findViewById(R.id.directoryField3),
            content.findViewById(R.id.directoryField4),
        )
        configureFields(fields, entry)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (entry == null) R.string.add_directory_entry else R.string.edit_directory_entry)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                fields.forEach { it.error = null }
                val values = fields.map { it.editText?.text?.toString().orEmpty() }
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { viewModel.save(entry, values) }
                        .onSuccess {
                            dialog.dismiss()
                            view?.let { anchor -> Snackbar.make(anchor, R.string.directory_saved, Snackbar.LENGTH_SHORT).show() }
                        }
                        .onFailure { fields.first().error = it.message ?: getString(R.string.required_field) }
                }
            }
        }
        dialog.show()
    }

    private fun configureFields(fields: List<TextInputLayout>, entry: DirectoryEntry?) {
        fields.forEach { it.isVisible = true }
        when (viewModel.type.value) {
            DirectoryType.MANUFACTURERS -> {
                fields[0].hint = getString(R.string.manufacturer_name)
                fields[1].isVisible = false
                fields[2].isVisible = false
                fields[3].hint = getString(R.string.note_optional)
                (entry as? DirectoryEntry.Manufacturer)?.value?.let {
                    fields[0].editText?.setText(it.name)
                    fields[3].editText?.setText(it.note)
                }
            }
            DirectoryType.CUSTOMERS -> {
                fields[0].hint = getString(R.string.customer_company)
                fields[1].hint = getString(R.string.contact_optional)
                fields[2].isVisible = false
                fields[3].hint = getString(R.string.note_optional)
                (entry as? DirectoryEntry.Customer)?.value?.let {
                    fields[0].editText?.setText(it.name)
                    fields[1].editText?.setText(it.contactInfo)
                    fields[3].editText?.setText(it.note)
                }
            }
            DirectoryType.LOCATIONS -> {
                fields[0].hint = getString(R.string.rack)
                fields[1].hint = getString(R.string.shelf)
                fields[2].hint = getString(R.string.location_label_optional)
                fields[3].hint = getString(R.string.note_optional)
                (entry as? DirectoryEntry.Location)?.value?.let {
                    fields[0].editText?.setText(it.rack)
                    fields[1].editText?.setText(it.shelf)
                    fields[2].editText?.setText(it.label)
                    fields[3].editText?.setText(it.note)
                }
            }
        }
    }

    private fun confirmDelete(entry: DirectoryEntry) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_directory_title)
            .setMessage(getString(R.string.delete_directory_message, entry.title))
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    runCatching { viewModel.delete(entry) }
                        .onSuccess { view?.let { Snackbar.make(it, R.string.directory_deleted, Snackbar.LENGTH_SHORT).show() } }
                        .onFailure { error ->
                            view?.let { Snackbar.make(it, error.message.orEmpty(), Snackbar.LENGTH_LONG).show() }
                        }
                }
            }
            .show()
    }
}
