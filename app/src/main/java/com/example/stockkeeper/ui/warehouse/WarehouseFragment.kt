package com.example.stockkeeper.ui.warehouse

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.ui.common.StockViewModelFactory
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class WarehouseFragment : Fragment() {
    private val viewModel: WarehouseViewModel by viewModels {
        val app = requireActivity().application as StockKeeperApplication
        StockViewModelFactory(app.stockRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_warehouse, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ProductAdapter(::showProductActions)
        val list = view.findViewById<RecyclerView>(R.id.productList)
        val empty = view.findViewById<View>(R.id.emptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter

        view.findViewById<TextInputEditText>(R.id.searchInput).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(value: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(value: CharSequence?, start: Int, before: Int, count: Int) {
                    viewModel.search(value?.toString().orEmpty())
                }
                override fun afterTextChanged(value: Editable?) = Unit
            },
        )
        view.findViewById<ExtendedFloatingActionButton>(R.id.addProductButton)
            .setOnClickListener { showAddProductDialog(view) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { products ->
                        adapter.submitList(products)
                        empty.isVisible = products.isEmpty()
                        list.isVisible = products.isNotEmpty()
                    }
                }
                launch {
                    viewModel.messages.collect { result ->
                        result.fold(
                            onSuccess = { Snackbar.make(view, R.string.operation_saved, Snackbar.LENGTH_SHORT).show() },
                            onFailure = { error ->
                                Snackbar.make(
                                    view,
                                    getString(R.string.product_add_failed, error.message.orEmpty()),
                                    Snackbar.LENGTH_LONG,
                                ).show()
                            },
                        )
                    }
                }
            }
        }
    }

    private fun showProductActions(product: ProductStockItem) {
        val actions = arrayOf(
            getString(R.string.receive),
            getString(R.string.sell),
            getString(R.string.write_off),
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${product.name} · ${getString(R.string.quantity_format, product.quantity)}")
            .setItems(actions) { _, index -> showOperationDialog(product, index) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showOperationDialog(product: ProductStockItem, operation: Int) {
        val content = layoutInflater.inflate(R.layout.dialog_stock_operation, null)
        val customerLayout = content.findViewById<TextInputLayout>(R.id.customerLayout)
        val reasonLayout = content.findViewById<TextInputLayout>(R.id.reasonLayout)
        customerLayout.isVisible = operation == 1
        reasonLayout.isVisible = operation == 2
        val title = when (operation) {
            0 -> R.string.receive
            1 -> R.string.sell
            else -> R.string.write_off
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val quantityLayout = content.findViewById<TextInputLayout>(R.id.operationQuantityLayout)
                val quantity = content.text(R.id.operationQuantityInput).toIntOrNull()
                val customer = content.text(R.id.customerInput)
                val reason = content.text(R.id.reasonInput)
                quantityLayout.error = if (quantity == null || quantity <= 0) getString(R.string.invalid_quantity) else null
                customerLayout.error = if (operation == 1 && customer.isBlank()) getString(R.string.required_field) else null
                reasonLayout.error = if (operation == 2 && reason.isBlank()) getString(R.string.required_field) else null
                if (quantity == null || quantity <= 0 || operation == 1 && customer.isBlank() || operation == 2 && reason.isBlank()) {
                    return@setOnClickListener
                }
                when (operation) {
                    0 -> viewModel.receive(product.id, quantity)
                    1 -> viewModel.sell(product.id, quantity, customer, null)
                    2 -> viewModel.writeOff(product.id, quantity, reason)
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun showAddProductDialog(anchor: View) {
        val content = layoutInflater.inflate(R.layout.dialog_add_product, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_product)
            .setView(content)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val articleLayout = content.findViewById<TextInputLayout>(R.id.articleLayout)
                val nameLayout = content.findViewById<TextInputLayout>(R.id.nameLayout)
                val quantityLayout = content.findViewById<TextInputLayout>(R.id.quantityLayout)
                val article = content.text(R.id.articleInput)
                val name = content.text(R.id.nameInput)
                val quantity = content.text(R.id.quantityInput).toIntOrNull()

                articleLayout.error = if (article.isBlank()) getString(R.string.required_field) else null
                nameLayout.error = if (name.isBlank()) getString(R.string.required_field) else null
                quantityLayout.error = if (quantity == null || quantity < 0) getString(R.string.invalid_quantity) else null
                if (article.isBlank() || name.isBlank() || quantity == null || quantity < 0) return@setOnClickListener

                viewModel.addProduct(
                    article = article,
                    name = name,
                    manufacturer = content.text(R.id.manufacturerInput),
                    rack = content.text(R.id.rackInput),
                    shelf = content.text(R.id.shelfInput),
                    note = content.text(R.id.noteInput),
                    initialQuantity = quantity,
                )
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun View.text(id: Int): String =
        findViewById<TextInputEditText>(id).text?.toString()?.trim().orEmpty()
}
