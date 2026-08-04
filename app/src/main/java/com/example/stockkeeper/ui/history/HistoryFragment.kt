package com.example.stockkeeper.ui.history

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
import androidx.recyclerview.widget.RecyclerView
import com.example.stockkeeper.R
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.ui.common.StockViewModelFactory
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private val viewModel: HistoryViewModel by viewModels {
        val app = requireActivity().application as StockKeeperApplication
        StockViewModelFactory(app.stockRepository)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View =
        inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = HistoryAdapter()
        val list = view.findViewById<RecyclerView>(R.id.historyList)
        val empty = view.findViewById<View>(R.id.emptyState)
        list.layoutManager = LinearLayoutManager(requireContext())
        list.setHasFixedSize(true)
        list.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.operations.collect { operations ->
                    adapter.submitList(operations)
                    list.isVisible = operations.isNotEmpty()
                    empty.isVisible = operations.isEmpty()
                }
            }
        }
    }
}
