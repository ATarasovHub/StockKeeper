package com.example.stockkeeper.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = ProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CustomerEntity::class,
            parentColumns = ["id"],
            childColumns = ["customer_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["product_id"]),
        Index(value = ["customer_id"]),
        Index(value = ["occurred_at"]),
        Index(value = ["type"]),
    ],
)
data class StockTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "product_id")
    val productId: Long,
    val type: StockTransactionType,
    @ColumnInfo(name = "quantity_delta")
    val quantityDelta: Int,
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    @ColumnInfo(name = "customer_id")
    val customerId: Long? = null,
    val reason: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

enum class StockTransactionType {
    RECEIPT,
    SALE,
    WRITE_OFF,
    ADJUSTMENT,
}
