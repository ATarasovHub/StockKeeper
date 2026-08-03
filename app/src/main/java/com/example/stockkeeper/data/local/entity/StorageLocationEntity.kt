package com.example.stockkeeper.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "storage_locations",
    indices = [Index(value = ["rack", "shelf"], unique = true)],
)
data class StorageLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rack: String,
    val shelf: String,
    val label: String? = null,
    val note: String? = null,
)
