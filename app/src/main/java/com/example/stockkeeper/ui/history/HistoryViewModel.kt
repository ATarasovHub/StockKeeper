package com.example.stockkeeper.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.model.StockTransactionDetails
import com.example.stockkeeper.data.repository.StockRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HistoryViewModel(repository: StockRepository) : ViewModel() {
    val operations: StateFlow<List<StockTransactionDetails>> = repository.observeOutgoingHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
