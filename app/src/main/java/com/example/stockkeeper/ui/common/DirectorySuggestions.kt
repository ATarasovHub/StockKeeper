package com.example.stockkeeper.ui.common

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.stockkeeper.R
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Wires the manufacturer/rack/shelf fields of dialog_add_product.xml to directory-backed
 * autocomplete suggestions, mirroring the search-suggestion pattern used in WarehouseFragment.
 */
fun Fragment.bindDirectorySuggestions(
    content: View,
    manufacturerSuggestions: StateFlow<List<ManufacturerEntity>>,
    locations: StateFlow<List<StorageLocationEntity>>,
    onManufacturerQueryChanged: (String) -> Unit,
): Job {
    val manufacturerInput = content.findViewById<MaterialAutoCompleteTextView>(R.id.manufacturerInput)
    val rackInput = content.findViewById<MaterialAutoCompleteTextView>(R.id.rackInput)
    val shelfInput = content.findViewById<MaterialAutoCompleteTextView>(R.id.shelfInput)

    manufacturerInput.addTextChangedListener(
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) =
                onManufacturerQueryChanged(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        },
    )
    manufacturerInput.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus) onManufacturerQueryChanged(manufacturerInput.text?.toString().orEmpty())
    }

    return viewLifecycleOwner.lifecycleScope.launch {
        launch {
            manufacturerSuggestions.collect { suggestions ->
                manufacturerInput.setAdapter(
                    ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, suggestions.map { it.name }),
                )
                if (manufacturerInput.hasFocus() && suggestions.isNotEmpty()) manufacturerInput.showDropDown()
            }
        }
        launch {
            locations.collect { values ->
                val racks = values.map { it.rack }.distinct().sortedBy { it.lowercase() }
                val shelves = values.map { it.shelf }.distinct().sortedBy { it.lowercase() }
                rackInput.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, racks))
                shelfInput.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, shelves))
            }
        }
    }
}

/** Wires the sale customer field to the customer directory. */
fun Fragment.bindCustomerSuggestions(
    content: View,
    customers: StateFlow<List<CustomerEntity>>,
): Job {
    val customerInput = content.findViewById<MaterialAutoCompleteTextView>(R.id.customerInput)
    customerInput.setOnFocusChangeListener { _, hasFocus ->
        if (hasFocus && customerInput.adapter?.count != 0) customerInput.showDropDown()
    }

    return viewLifecycleOwner.lifecycleScope.launch {
        customers.collect { values ->
            customerInput.setAdapter(
                ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, values.map { it.name }),
            )
            if (customerInput.hasFocus() && values.isNotEmpty()) customerInput.showDropDown()
        }
    }
}
