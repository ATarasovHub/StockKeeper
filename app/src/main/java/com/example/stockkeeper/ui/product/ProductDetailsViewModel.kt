package com.example.stockkeeper.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.local.model.StockTransactionDetails
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
class ProductDetailsViewModel(
    private val productId: Long,
    private val repository: StockRepository,
) : ViewModel() {
    private val manufacturerQuery = MutableStateFlow("")

    val product: StateFlow<ProductStockItem?> = repository.observeProduct(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<StockTransactionDetails>> = repository.observeProductHistory(productId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val events = MutableSharedFlow<ProductEvent>()

    val manufacturerSuggestions: StateFlow<List<ManufacturerEntity>> = manufacturerQuery
        .debounce(200)
        .flatMapLatest { repository.searchManufacturers(it, limit = 20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locations: StateFlow<List<StorageLocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun searchManufacturers(value: String) {
        manufacturerQuery.value = value
    }

    fun update(
        article: String,
        name: String,
        manufacturer: String?,
        rack: String?,
        shelf: String?,
        note: String?,
        photoPath: String?,
    ) = launch(ProductEvent.Updated) {
        repository.updateProduct(productId, article, name, manufacturer, rack, shelf, note, photoPath)
    }

    fun receive(quantity: Int) = launch(ProductEvent.OperationSaved) {
        repository.receive(productId, quantity)
    }

    fun sell(quantity: Int, customer: String) = launch(ProductEvent.OperationSaved) {
        repository.sellToCustomer(productId, quantity, customer)
    }

    fun writeOff(quantity: Int, reason: String) = launch(ProductEvent.OperationSaved) {
        repository.writeOff(productId, quantity, reason)
    }

    fun archive() = launch(ProductEvent.Archived) {
        repository.archiveProduct(productId)
    }

    private fun launch(success: ProductEvent, block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { events.emit(success) }
                .onFailure { events.emit(ProductEvent.Error(it.message.orEmpty())) }
        }
    }
}

sealed interface ProductEvent {
    data object Updated : ProductEvent
    data object OperationSaved : ProductEvent
    data object Archived : ProductEvent
    data class Error(val message: String) : ProductEvent
}
