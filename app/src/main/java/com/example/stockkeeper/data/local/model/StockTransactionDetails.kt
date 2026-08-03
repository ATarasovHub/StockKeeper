package com.example.stockkeeper.data.local.model

import com.example.stockkeeper.data.local.entity.StockTransactionType

data class StockTransactionDetails(
    val id: Long,
    val productId: Long,
    val article: String,
    val productName: String,
    val type: StockTransactionType,
    val quantityDelta: Int,
    val occurredAt: Long,
    val customerId: Long?,
    val customerName: String?,
    val reason: String?,
    val createdAt: Long,
)
