package com.example.stockkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.stockkeeper.data.local.entity.StockTransactionEntity
import com.example.stockkeeper.data.local.model.StockTransactionDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface StockTransactionDao {
    @Insert
    suspend fun insert(transaction: StockTransactionEntity): Long

    @Query(
        "SELECT CAST(COALESCE(SUM(quantity_delta), 0) AS INTEGER) " +
            "FROM stock_transactions WHERE product_id = :productId",
    )
    suspend fun getBalance(productId: Long): Int

    @Query(
        """
        SELECT
            t.id,
            t.product_id AS productId,
            p.article,
            p.name AS productName,
            t.type,
            t.quantity_delta AS quantityDelta,
            t.occurred_at AS occurredAt,
            t.customer_id AS customerId,
            c.name AS customerName,
            t.reason,
            t.created_at AS createdAt
        FROM stock_transactions t
        INNER JOIN products p ON p.id = t.product_id
        LEFT JOIN customers c ON c.id = t.customer_id
        WHERE t.product_id = :productId
        ORDER BY t.occurred_at DESC, t.id DESC
        """,
    )
    fun observeProductHistory(productId: Long): Flow<List<StockTransactionDetails>>

    @Query(
        """
        SELECT
            t.id,
            t.product_id AS productId,
            p.article,
            p.name AS productName,
            t.type,
            t.quantity_delta AS quantityDelta,
            t.occurred_at AS occurredAt,
            t.customer_id AS customerId,
            c.name AS customerName,
            t.reason,
            t.created_at AS createdAt
        FROM stock_transactions t
        INNER JOIN products p ON p.id = t.product_id
        LEFT JOIN customers c ON c.id = t.customer_id
        WHERE t.type IN ('SALE', 'WRITE_OFF')
        ORDER BY t.occurred_at DESC, t.id DESC
        """,
    )
    fun observeOutgoingHistory(): Flow<List<StockTransactionDetails>>
}
