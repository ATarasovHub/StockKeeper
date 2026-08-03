package com.example.stockkeeper.data.local.converter

import androidx.room.TypeConverter
import com.example.stockkeeper.data.local.entity.StockTransactionType

class DatabaseConverters {
    @TypeConverter
    fun transactionTypeToString(value: StockTransactionType): String = value.name

    @TypeConverter
    fun stringToTransactionType(value: String): StockTransactionType =
        StockTransactionType.valueOf(value)
}
