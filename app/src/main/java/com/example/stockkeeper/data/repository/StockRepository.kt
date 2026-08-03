package com.example.stockkeeper.data.repository

import androidx.room.withTransaction
import com.example.stockkeeper.data.local.StockKeeperDatabase
import com.example.stockkeeper.data.local.entity.ProductEntity
import com.example.stockkeeper.data.local.entity.StockTransactionEntity
import com.example.stockkeeper.data.local.entity.StockTransactionType
import com.example.stockkeeper.data.local.model.ProductStockItem
import com.example.stockkeeper.data.local.model.StockTransactionDetails
import kotlinx.coroutines.flow.Flow

class StockRepository(
    private val database: StockKeeperDatabase,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
) {
    private val productDao = database.productDao()
    private val transactionDao = database.stockTransactionDao()

    fun observeStock(
        query: String = "",
        manufacturerId: Long? = null,
    ): Flow<List<ProductStockItem>> = productDao.observeStock(query.trim(), manufacturerId)

    fun observeProductHistory(productId: Long): Flow<List<StockTransactionDetails>> =
        transactionDao.observeProductHistory(productId)

    fun observeOutgoingHistory(): Flow<List<StockTransactionDetails>> =
        transactionDao.observeOutgoingHistory()

    suspend fun createProduct(
        article: String,
        name: String,
        photoPath: String? = null,
        manufacturerId: Long? = null,
        locationId: Long? = null,
        note: String? = null,
        initialQuantity: Int = 0,
        receivedAt: Long = currentTimeMillis(),
    ): Long = database.withTransaction {
        val cleanArticle = article.trim()
        val cleanName = name.trim()
        require(cleanArticle.isNotEmpty()) { "Article is required" }
        require(cleanName.isNotEmpty()) { "Product name is required" }
        require(initialQuantity >= 0) { "Initial quantity must not be negative" }
        require(productDao.findByArticle(cleanArticle) == null) {
            "A product with article '$cleanArticle' already exists"
        }

        val now = currentTimeMillis()
        val productId = productDao.insert(
            ProductEntity(
                article = cleanArticle,
                name = cleanName,
                photoPath = photoPath,
                manufacturerId = manufacturerId,
                locationId = locationId,
                note = note?.trim()?.takeIf(String::isNotEmpty),
                createdAt = now,
                updatedAt = now,
            ),
        )

        if (initialQuantity > 0) {
            transactionDao.insert(
                StockTransactionEntity(
                    productId = productId,
                    type = StockTransactionType.RECEIPT,
                    quantityDelta = initialQuantity,
                    occurredAt = receivedAt,
                    createdAt = now,
                ),
            )
        }
        productId
    }

    suspend fun receive(
        productId: Long,
        quantity: Int,
        occurredAt: Long = currentTimeMillis(),
        note: String? = null,
    ): Long = database.withTransaction {
        requireProduct(productId)
        StockMovementValidator.requirePositiveQuantity(quantity)
        insertMovement(productId, StockTransactionType.RECEIPT, quantity, occurredAt, reason = note)
    }

    suspend fun sell(
        productId: Long,
        quantity: Int,
        customerId: Long,
        occurredAt: Long = currentTimeMillis(),
        note: String? = null,
    ): Long = database.withTransaction {
        requireProduct(productId)
        require(customerId > 0) { "Customer is required for a sale" }
        ensureCanRemove(productId, quantity)
        insertMovement(
            productId = productId,
            type = StockTransactionType.SALE,
            quantityDelta = -quantity,
            occurredAt = occurredAt,
            customerId = customerId,
            reason = note,
        )
    }

    suspend fun writeOff(
        productId: Long,
        quantity: Int,
        reason: String,
        occurredAt: Long = currentTimeMillis(),
    ): Long = database.withTransaction {
        requireProduct(productId)
        require(reason.isNotBlank()) { "Write-off reason is required" }
        ensureCanRemove(productId, quantity)
        insertMovement(
            productId = productId,
            type = StockTransactionType.WRITE_OFF,
            quantityDelta = -quantity,
            occurredAt = occurredAt,
            reason = reason.trim(),
        )
    }

    suspend fun adjust(
        productId: Long,
        quantityDelta: Int,
        reason: String,
        occurredAt: Long = currentTimeMillis(),
    ): Long = database.withTransaction {
        requireProduct(productId)
        StockMovementValidator.requireNonZeroAdjustment(quantityDelta)
        require(reason.isNotBlank()) { "Adjustment reason is required" }
        if (quantityDelta < 0) {
            StockMovementValidator.requireSufficientStock(
                currentBalance = transactionDao.getBalance(productId),
                requestedQuantity = -quantityDelta,
            )
        }
        insertMovement(
            productId = productId,
            type = StockTransactionType.ADJUSTMENT,
            quantityDelta = quantityDelta,
            occurredAt = occurredAt,
            reason = reason.trim(),
        )
    }

    private suspend fun requireProduct(productId: Long) {
        requireNotNull(productDao.findById(productId)) { "Product $productId does not exist" }
    }

    private suspend fun ensureCanRemove(productId: Long, quantity: Int) {
        StockMovementValidator.requireSufficientStock(
            currentBalance = transactionDao.getBalance(productId),
            requestedQuantity = quantity,
        )
    }

    private suspend fun insertMovement(
        productId: Long,
        type: StockTransactionType,
        quantityDelta: Int,
        occurredAt: Long,
        customerId: Long? = null,
        reason: String? = null,
    ): Long = transactionDao.insert(
        StockTransactionEntity(
            productId = productId,
            type = type,
            quantityDelta = quantityDelta,
            occurredAt = occurredAt,
            customerId = customerId,
            reason = reason?.trim()?.takeIf(String::isNotEmpty),
            createdAt = currentTimeMillis(),
        ),
    )
}
