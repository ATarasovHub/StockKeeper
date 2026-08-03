package com.example.stockkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockkeeper.data.local.entity.ProductEntity
import com.example.stockkeeper.data.local.model.ProductStockItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Insert
    suspend fun insert(product: ProductEntity): Long

    @Update
    suspend fun update(product: ProductEntity)

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    suspend fun findById(productId: Long): ProductEntity?

    @Query("SELECT * FROM products WHERE article = :article COLLATE NOCASE LIMIT 1")
    suspend fun findByArticle(article: String): ProductEntity?

    @Query(
        """
        SELECT
            p.id,
            p.article,
            p.name,
            p.photo_path AS photoPath,
            p.manufacturer_id AS manufacturerId,
            m.name AS manufacturerName,
            p.location_id AS locationId,
            l.label AS locationLabel,
            l.rack,
            l.shelf,
            p.note,
            CAST(COALESCE(SUM(t.quantity_delta), 0) AS INTEGER) AS quantity
        FROM products p
        LEFT JOIN manufacturers m ON m.id = p.manufacturer_id
        LEFT JOIN storage_locations l ON l.id = p.location_id
        LEFT JOIN stock_transactions t ON t.product_id = p.id
        WHERE p.is_archived = 0
          AND (:query = '' OR p.article LIKE '%' || :query || '%' COLLATE NOCASE
               OR p.name LIKE '%' || :query || '%' COLLATE NOCASE
               OR m.name LIKE '%' || :query || '%' COLLATE NOCASE)
          AND (:manufacturerId IS NULL OR p.manufacturer_id = :manufacturerId)
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE, p.article COLLATE NOCASE
        """,
    )
    fun observeStock(
        query: String = "",
        manufacturerId: Long? = null,
    ): Flow<List<ProductStockItem>>

    @Query(
        """
        SELECT
            p.id,
            p.article,
            p.name,
            p.photo_path AS photoPath,
            p.manufacturer_id AS manufacturerId,
            m.name AS manufacturerName,
            p.location_id AS locationId,
            l.label AS locationLabel,
            l.rack,
            l.shelf,
            p.note,
            CAST(COALESCE(SUM(t.quantity_delta), 0) AS INTEGER) AS quantity
        FROM products p
        LEFT JOIN manufacturers m ON m.id = p.manufacturer_id
        LEFT JOIN storage_locations l ON l.id = p.location_id
        LEFT JOIN stock_transactions t ON t.product_id = p.id
        WHERE p.id = :productId
        GROUP BY p.id
        """,
    )
    fun observeProduct(productId: Long): Flow<ProductStockItem?>

    @Query(
        """
        SELECT
            p.id,
            p.article,
            p.name,
            p.photo_path AS photoPath,
            p.manufacturer_id AS manufacturerId,
            m.name AS manufacturerName,
            p.location_id AS locationId,
            l.label AS locationLabel,
            l.rack,
            l.shelf,
            p.note,
            CAST(COALESCE(SUM(t.quantity_delta), 0) AS INTEGER) AS quantity
        FROM products p
        LEFT JOIN manufacturers m ON m.id = p.manufacturer_id
        LEFT JOIN storage_locations l ON l.id = p.location_id
        LEFT JOIN stock_transactions t ON t.product_id = p.id
        WHERE p.is_archived = 1
          AND (:query = '' OR p.article LIKE '%' || :query || '%' COLLATE NOCASE
               OR p.name LIKE '%' || :query || '%' COLLATE NOCASE)
        GROUP BY p.id
        ORDER BY p.name COLLATE NOCASE, p.article COLLATE NOCASE
        """,
    )
    fun observeArchivedStock(query: String = ""): Flow<List<ProductStockItem>>
}
