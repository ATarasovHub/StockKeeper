package com.example.stockkeeper.ui.directories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockkeeper.data.repository.StockRepository

class DirectoryViewModelFactory(private val repository: StockRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = DirectoryViewModel(repository) as T
}
