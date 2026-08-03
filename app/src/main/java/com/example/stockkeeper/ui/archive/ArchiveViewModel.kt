package com.example.stockkeeper.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.repository.StockRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class ArchiveViewModel(private val repository: StockRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val messages = MutableSharedFlow<Result<Unit>>()
    val products: StateFlow<List<ProductStockItem>> = query
        .debounce(200)
        .flatMapLatest(repository::observeArchivedStock)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) {
        query.value = value
    }

    fun restore(productId: Long) {
        viewModelScope.launch {
            messages.emit(runCatching { repository.restoreProduct(productId) })
        }
    }
}
