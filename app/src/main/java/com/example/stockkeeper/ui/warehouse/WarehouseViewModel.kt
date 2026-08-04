package com.example.stockkeeper.ui.warehouse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WarehouseViewModel(
    private val repository: StockRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val manufacturerId = MutableStateFlow<Long?>(null)
    private val rack = MutableStateFlow("")
    private val shelf = MutableStateFlow("")
    private val availability = MutableStateFlow(0)
    private val manufacturerQuery = MutableStateFlow("")
    private val productFormManufacturerQuery = MutableStateFlow("")
    val messages = MutableSharedFlow<Result<Unit>>()

    val products: StateFlow<List<ProductStockItem>> = combine(
        query.debounce(200),
        manufacturerId,
        rack,
        shelf,
        availability,
    ) { searchQuery, selectedManufacturer, selectedRack, selectedShelf, selectedAvailability ->
        StockFilters(searchQuery, selectedManufacturer, selectedRack, selectedShelf, selectedAvailability)
    }
        .flatMapLatest { filters ->
            repository.observeStock(
                filters.query,
                filters.manufacturerId,
                filters.rack,
                filters.shelf,
                filters.availability,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val searchCandidates: StateFlow<List<String>> = repository.observeSearchCandidates()
        .map { values ->
            values.filter(String::isNotBlank).distinctBy(String::lowercase)
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val manufacturerSuggestions: StateFlow<List<ManufacturerEntity>> = manufacturerQuery
        .debounce(200)
        .flatMapLatest { repository.searchManufacturers(it, limit = 50) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val productFormManufacturerSuggestions: StateFlow<List<ManufacturerEntity>> = productFormManufacturerQuery
        .debounce(200)
        .flatMapLatest { repository.searchManufacturers(it, limit = 20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val locations: StateFlow<List<StorageLocationEntity>> = repository.observeLocations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun searchProductFormManufacturers(value: String) {
        productFormManufacturerQuery.value = value
    }

    fun search(value: String) {
        query.value = value
    }

    fun filterManufacturer(id: Long?) {
        manufacturerId.value = id
    }

    fun filterRack(value: String) {
        rack.value = value
    }

    fun filterShelf(value: String) {
        shelf.value = value
    }

    fun filterAvailability(value: Int) {
        availability.value = value
    }

    fun searchManufacturers(value: String) {
        manufacturerQuery.value = value
    }

    fun addProduct(
        article: String,
        name: String,
        photoPath: String?,
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
                        photoPath = photoPath,
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

private data class StockFilters(
    val query: String,
    val manufacturerId: Long?,
    val rack: String,
    val shelf: String,
    val availability: Int,
)
