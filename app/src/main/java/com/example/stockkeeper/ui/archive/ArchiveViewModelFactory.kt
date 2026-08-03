package com.example.stockkeeper.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockkeeper.data.repository.StockRepository

class ArchiveViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ArchiveViewModel(repository) as T
}
