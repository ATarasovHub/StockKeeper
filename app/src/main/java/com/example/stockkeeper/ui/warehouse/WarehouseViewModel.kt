package com.example.stockkeeper.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WarehouseViewModel(
    private val repository: StockRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    val messages = MutableSharedFlow<Result<Unit>>()

    val products: StateFlow<List<ProductStockItem>> = query
        .debounce(200)
        .flatMapLatest(repository::observeStock)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun search(value: String) {
        query.value = value
    }

    fun addProduct(
        article: String,
        name: String,
        manufacturer: String?,
        rack: String?,
        shelf: String?,
        note: String?,
        initialQuantity: Int,
    ) {
        viewModelScope.launch {
            messages.emit(
                runCatching {
                    repository.createProductFromForm(
                        article = article,
                        name = name,
                        manufacturerName = manufacturer,
                        rack = rack,
                        shelf = shelf,
                        note = note,
                        initialQuantity = initialQuantity,
                    )
                }.map { Unit },
            )
        }
    }

    fun receive(productId: Long, quantity: Int) = runOperation {
        repository.receive(productId, quantity)
    }

    fun sell(productId: Long, quantity: Int, customer: String, note: String?) = runOperation {
        repository.sellToCustomer(productId, quantity, customer, note = note)
    }

    fun writeOff(productId: Long, quantity: Int, reason: String) = runOperation {
        repository.writeOff(productId, quantity, reason)
    }

    private fun runOperation(block: suspend () -> Long) {
        viewModelScope.launch {
            messages.emit(runCatching { block() }.map { Unit })
        }
    }
}
