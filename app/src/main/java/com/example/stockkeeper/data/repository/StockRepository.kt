package com.example.stockkeeper.data.repository

import androidx.room.withTransaction
import com.example.stockkeeper.data.local.StockKeeperDatabase
import com.example.stockkeeper.data.local.entity.ProductEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import com.example.stockkeeper.data.local.entity.CustomerEntity
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
    private val directoryDao = database.directoryDao()

    fun observeStock(
        query: String = "",
        manufacturerId: Long? = null,
    ): Flow<List<ProductStockItem>> = productDao.observeStock(query.trim(), manufacturerId)

    fun observeManufacturers(): Flow<List<ManufacturerEntity>> =
        directoryDao.observeManufacturers()

    fun searchManufacturers(query: String, limit: Int = 50): Flow<List<ManufacturerEntity>> =
        directoryDao.searchManufacturers(query.trim(), limit)

    fun observeProductHistory(productId: Long): Flow<List<StockTransactionDetails>> =
        transactionDao.observeProductHistory(productId)

    fun observeProduct(productId: Long): Flow<ProductStockItem?> =
        productDao.observeProduct(productId)

    fun observeOutgoingHistory(): Flow<List<StockTransactionDetails>> =
        transactionDao.observeOutgoingHistory()

    fun observeArchivedStock(query: String = ""): Flow<List<ProductStockItem>> =
        productDao.observeArchivedStock(query.trim())

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

    suspend fun createProductFromForm(
        article: String,
        name: String,
        photoPath: String? = null,
        manufacturerName: String?,
        rack: String?,
        shelf: String?,
        note: String?,
        initialQuantity: Int,
    ): Long = database.withTransaction {
        val cleanManufacturer = manufacturerName?.trim().orEmpty()
        val manufacturerId = cleanManufacturer.takeIf(String::isNotEmpty)?.let { value ->
            directoryDao.findManufacturerByName(value)?.id
                ?: directoryDao.insertManufacturer(ManufacturerEntity(name = value))
        }

        val cleanRack = rack?.trim().orEmpty()
        val cleanShelf = shelf?.trim().orEmpty()
        val locationId = if (cleanRack.isNotEmpty() || cleanShelf.isNotEmpty()) {
            directoryDao.findLocation(cleanRack, cleanShelf)?.id
                ?: directoryDao.insertLocation(
                    StorageLocationEntity(rack = cleanRack, shelf = cleanShelf),
                )
        } else {
            null
        }

        createProduct(
            article = article,
            name = name,
            photoPath = photoPath,
            manufacturerId = manufacturerId,
            locationId = locationId,
            note = note,
            initialQuantity = initialQuantity,
        )
    }

    suspend fun updateProduct(
        productId: Long,
        article: String,
        name: String,
        manufacturerName: String?,
        rack: String?,
        shelf: String?,
        note: String?,
        photoPath: String?,
    ) = database.withTransaction {
        val current = requireNotNull(productDao.findById(productId)) { "Product $productId does not exist" }
        val cleanArticle = article.trim()
        val cleanName = name.trim()
        require(cleanArticle.isNotEmpty()) { "Article is required" }
        require(cleanName.isNotEmpty()) { "Product name is required" }
        val duplicate = productDao.findByArticle(cleanArticle)
        require(duplicate == null || duplicate.id == productId) {
            "A product with article '$cleanArticle' already exists"
        }

        val cleanManufacturer = manufacturerName?.trim().orEmpty()
        val manufacturerId = cleanManufacturer.takeIf(String::isNotEmpty)?.let { value ->
            directoryDao.findManufacturerByName(value)?.id
                ?: directoryDao.insertManufacturer(ManufacturerEntity(name = value))
        }
        val cleanRack = rack?.trim().orEmpty()
        val cleanShelf = shelf?.trim().orEmpty()
        val locationId = if (cleanRack.isNotEmpty() || cleanShelf.isNotEmpty()) {
            directoryDao.findLocation(cleanRack, cleanShelf)?.id
                ?: directoryDao.insertLocation(StorageLocationEntity(rack = cleanRack, shelf = cleanShelf))
        } else null

        productDao.update(
            current.copy(
                article = cleanArticle,
                name = cleanName,
                manufacturerId = manufacturerId,
                locationId = locationId,
                note = note?.trim()?.takeIf(String::isNotEmpty),
                photoPath = photoPath,
                updatedAt = currentTimeMillis(),
            ),
        )
    }

    suspend fun archiveProduct(productId: Long) = database.withTransaction {
        val current = requireNotNull(productDao.findById(productId)) { "Product $productId does not exist" }
        productDao.update(current.copy(isArchived = true, updatedAt = currentTimeMillis()))
    }

    suspend fun restoreProduct(productId: Long) = database.withTransaction {
        val current = requireNotNull(productDao.findById(productId)) { "Product $productId does not exist" }
        productDao.update(current.copy(isArchived = false, updatedAt = currentTimeMillis()))
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

    suspend fun sellToCustomer(
        productId: Long,
        quantity: Int,
        customerName: String,
        occurredAt: Long = currentTimeMillis(),
        note: String? = null,
    ): Long = database.withTransaction {
        val cleanName = customerName.trim()
        require(cleanName.isNotEmpty()) { "Customer is required for a sale" }
        val customerId = directoryDao.findCustomerByName(cleanName)?.id
            ?: directoryDao.insertCustomer(CustomerEntity(name = cleanName))
        sell(productId, quantity, customerId, occurredAt, note)
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
