package com.example.stockkeeper.ui.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.stockkeeper.data.repository.StockRepository

class ProductDetailsViewModelFactory(
    private val productId: Long,
    private val repository: StockRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        ProductDetailsViewModel(productId, repository) as T
}
