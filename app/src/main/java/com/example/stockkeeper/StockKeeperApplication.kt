package com.example.stockkeeper

import android.app.Application
import com.example.stockkeeper.data.local.StockKeeperDatabase
import com.example.stockkeeper.data.repository.StockRepository
import com.example.stockkeeper.settings.AppSettings

class StockKeeperApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.apply(this)
    }

    @Volatile
    private var databaseInstance: StockKeeperDatabase? = null

    @Volatile
    private var repositoryInstance: StockRepository? = null

    val database: StockKeeperDatabase
        get() = databaseInstance ?: synchronized(this) {
            databaseInstance ?: StockKeeperDatabase.getInstance(this).also { databaseInstance = it }
        }

    val stockRepository: StockRepository
        get() = repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: StockRepository(database).also { repositoryInstance = it }
        }

    fun closeDatabase() = synchronized(this) {
        repositoryInstance = null
        StockKeeperDatabase.closeInstance()
        databaseInstance = null
    }
}
