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
    @Query("SELECT * FROM manufacturers WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findManufacturerByName(name: String): ManufacturerEntity?

    @Insert
    suspend fun insertManufacturer(manufacturer: ManufacturerEntity): Long

    @Update
    suspend fun updateManufacturer(manufacturer: ManufacturerEntity)

    @Query("SELECT * FROM manufacturers ORDER BY name COLLATE NOCASE")
    fun observeManufacturers(): Flow<List<ManufacturerEntity>>

    @Query(
        """
        SELECT * FROM manufacturers
        WHERE :query = '' OR name LIKE :query || '%' COLLATE NOCASE
        ORDER BY name COLLATE NOCASE
        LIMIT :limit
        """,
    )
    fun searchManufacturers(query: String, limit: Int = 50): Flow<List<ManufacturerEntity>>

    @Insert
    suspend fun insertLocation(location: StorageLocationEntity): Long

    @Update
    suspend fun updateLocation(location: StorageLocationEntity)

    @Query("SELECT * FROM storage_locations ORDER BY rack COLLATE NOCASE, shelf COLLATE NOCASE")
    fun observeLocations(): Flow<List<StorageLocationEntity>>

    @Query("SELECT * FROM storage_locations WHERE rack = :rack COLLATE NOCASE AND shelf = :shelf COLLATE NOCASE LIMIT 1")
    suspend fun findLocation(rack: String, shelf: String): StorageLocationEntity?

    @Insert
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("SELECT * FROM customers WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findCustomerByName(name: String): CustomerEntity?

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("SELECT * FROM customers ORDER BY name COLLATE NOCASE")
    fun observeCustomers(): Flow<List<CustomerEntity>>
}
