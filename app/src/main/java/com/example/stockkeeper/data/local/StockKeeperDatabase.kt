package com.example.stockkeeper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.stockkeeper.data.local.converter.DatabaseConverters
import com.example.stockkeeper.data.local.dao.DirectoryDao
import com.example.stockkeeper.data.local.dao.ProductDao
import com.example.stockkeeper.data.local.dao.StockTransactionDao
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.ProductEntity
import com.example.stockkeeper.data.local.entity.StockTransactionEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity

@Database(
    entities = [
        ProductEntity::class,
        ManufacturerEntity::class,
        StorageLocationEntity::class,
        CustomerEntity::class,
        StockTransactionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class StockKeeperDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    abstract fun directoryDao(): DirectoryDao

    abstract fun stockTransactionDao(): StockTransactionDao

    companion object {
        const val DATABASE_NAME = "stockkeeper.db"
        const val DATABASE_VERSION = 1

        @Volatile
        private var instance: StockKeeperDatabase? = null

        fun getInstance(context: Context): StockKeeperDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StockKeeperDatabase::class.java,
                    DATABASE_NAME,
                ).build().also { instance = it }
            }

        fun closeInstance() = synchronized(this) {
            instance?.close()
            instance = null
        }
    }
}
