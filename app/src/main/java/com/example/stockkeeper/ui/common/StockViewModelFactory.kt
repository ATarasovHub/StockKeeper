package com.example.stockkeeper.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockkeeper.data.repository.StockRepository
import com.example.stockkeeper.ui.history.HistoryViewModel
import com.example.stockkeeper.ui.warehouse.WarehouseViewModel

class StockViewModelFactory(
    private val repository: StockRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(WarehouseViewModel::class.java) ->
            WarehouseViewModel(repository) as T
        modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
            HistoryViewModel(repository) as T
        else -> error("Unknown ViewModel: ${modelClass.name}")
    }
}
