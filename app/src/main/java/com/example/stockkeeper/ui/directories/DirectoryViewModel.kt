package com.example.stockkeeper.ui.directories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.example.stockkeeper.data.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class DirectoryType { MANUFACTURERS, CUSTOMERS, LOCATIONS }

sealed interface DirectoryEntry {
    val id: Long
    val title: String
    val subtitle: String

    data class Manufacturer(val value: ManufacturerEntity) : DirectoryEntry {
        override val id = value.id
        override val title = value.name
        override val subtitle = value.note.orEmpty()
    }

    data class Customer(val value: CustomerEntity) : DirectoryEntry {
        override val id = value.id
        override val title = value.name
        override val subtitle = listOfNotNull(value.contactInfo, value.note).joinToString(" · ")
    }

    data class Location(val value: StorageLocationEntity) : DirectoryEntry {
        override val id = value.id
        override val title = value.label ?: "${value.rack} · ${value.shelf}"
        override val subtitle = "${value.rack} · ${value.shelf}" + value.note?.let { " · $it" }.orEmpty()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryViewModel(private val repository: StockRepository) : ViewModel() {
    val type = MutableStateFlow(DirectoryType.MANUFACTURERS)
    val entries = type.flatMapLatest { selected ->
        when (selected) {
            DirectoryType.MANUFACTURERS -> repository.observeManufacturers().map { list -> list.map(DirectoryEntry::Manufacturer) }
            DirectoryType.CUSTOMERS -> repository.observeCustomers().map { list -> list.map(DirectoryEntry::Customer) }
            DirectoryType.LOCATIONS -> repository.observeLocations().map { list -> list.map(DirectoryEntry::Location) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun save(entry: DirectoryEntry?, fields: List<String>) {
        when (type.value) {
            DirectoryType.MANUFACTURERS -> repository.saveManufacturer(entry?.id ?: 0, fields[0], fields[3])
            DirectoryType.CUSTOMERS -> repository.saveCustomer(entry?.id ?: 0, fields[0], fields[1], fields[3])
            DirectoryType.LOCATIONS -> repository.saveLocation(entry?.id ?: 0, fields[0], fields[1], fields[2], fields[3])
        }
    }

    suspend fun delete(entry: DirectoryEntry) {
        when (entry) {
            is DirectoryEntry.Manufacturer -> repository.deleteManufacturer(entry.value)
            is DirectoryEntry.Customer -> repository.deleteCustomer(entry.value)
            is DirectoryEntry.Location -> repository.deleteLocation(entry.value)
        }
    }
}
