package com.example.stockkeeper.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.stockkeeper.data.local.entity.CustomerEntity
import com.example.stockkeeper.data.local.entity.ManufacturerEntity
import com.example.stockkeeper.data.local.entity.StorageLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DirectoryDao {
    @Insert
    suspend fun insertManufacturer(manufacturer: ManufacturerEntity): Long

    @Update
    suspend fun updateManufacturer(manufacturer: ManufacturerEntity)

    @Query("SELECT * FROM manufacturers ORDER BY name COLLATE NOCASE")
    fun observeManufacturers(): Flow<List<ManufacturerEntity>>

    @Insert
    suspend fun insertLocation(location: StorageLocationEntity): Long

    @Update
    suspend fun updateLocation(location: StorageLocationEntity)

    @Query("SELECT * FROM storage_locations ORDER BY rack COLLATE NOCASE, shelf COLLATE NOCASE")
    fun observeLocations(): Flow<List<StorageLocationEntity>>

    @Insert
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeCustomers(): Flow<List<CustomerEntity>>
}
