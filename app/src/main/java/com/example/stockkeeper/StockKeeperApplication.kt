package com.example.stockkeeper

import android.app.Application
import com.example.stockkeeper.data.local.StockKeeperDatabase
import com.example.stockkeeper.data.repository.StockRepository

class StockKeeperApplication : Application() {
    val database: StockKeeperDatabase by lazy {
        StockKeeperDatabase.getInstance(this)
    }

    val stockRepository: StockRepository by lazy {
        StockRepository(database)
    }
}
