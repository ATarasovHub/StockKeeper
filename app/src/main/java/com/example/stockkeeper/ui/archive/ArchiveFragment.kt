package com.example.stockkeeper.ui.archive

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
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.ui.warehouse.ProductAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ArchiveFragment : Fragment() {
    private val viewModel: ArchiveViewModel by viewModels {
        val app = requireActivity().application as StockKeeperApplication
        ArchiveViewModelFactory(app.stockRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_archive, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = ProductAdapter(::confirmRestore)
        val list = view.findViewById<RecyclerView>(R.id.archiveList)
        val empty = view.findViewById<View>(R.id.archiveEmptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.setHasFixedSize(true)
        list.adapter = adapter
        view.findViewById<TextInputEditText>(R.id.archiveSearchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = viewModel.search(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) = Unit
        })
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.products.collect { products ->
                        adapter.submitList(products)
                        list.isVisible = products.isNotEmpty()
                        empty.isVisible = products.isEmpty()
                    }
                }
                launch {
                    viewModel.messages.collect { result ->
                        result.fold(
                            onSuccess = { Snackbar.make(view, R.string.product_restored, Snackbar.LENGTH_SHORT).show() },
                            onFailure = { Snackbar.make(view, it.message.orEmpty(), Snackbar.LENGTH_LONG).show() },
                        )
                    }
                }
            }
        }
    }

    private fun confirmRestore(product: ProductStockItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restore_product_title)
            .setMessage(R.string.restore_product_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.restore) { _, _ -> viewModel.restore(product.id) }
            .show()
    }
}
