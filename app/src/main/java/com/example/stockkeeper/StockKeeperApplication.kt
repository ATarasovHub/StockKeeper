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

    val database: StockKeeperDatabase by lazy {
        StockKeeperDatabase.getInstance(this)
    }

    val stockRepository: StockRepository by lazy {
        StockRepository(database)
    }
}
