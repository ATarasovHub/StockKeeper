package com.example.stockkeeper.data.local.model

data class ProductStockItem(
    val id: Long,
    val article: String,
    val name: String,
    val photoPath: String?,
    val manufacturerId: Long?,
    val manufacturerName: String?,
    val locationId: Long?,
    val locationLabel: String?,
    val rack: String?,
    val shelf: String?,
    val note: String?,
    val quantity: Int,
)
