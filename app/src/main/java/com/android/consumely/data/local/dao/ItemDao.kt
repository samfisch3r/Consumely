package com.android.consumely.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.android.consumely.data.local.entity.ItemEntity
import com.android.consumely.data.local.model.ItemWithLocation
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM items WHERE isDeleted = 0 ORDER BY dateAdded DESC")
    fun getAllItemsWithLocationFlow(): Flow<List<ItemWithLocation>>

    @Transaction
    @Query("SELECT * FROM items WHERE locationId = :locationId AND isDeleted = 0 ORDER BY dateAdded DESC")
    fun getItemsByLocationFlow(locationId: String): Flow<List<ItemWithLocation>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    fun getItemWithLocationFlow(id: String): Flow<ItemWithLocation?>

    @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ItemEntity?

    @Query("SELECT * FROM items")
    suspend fun getAllItemsIncludingDeleted(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("UPDATE items SET isDeleted = 1, lastUpdated = :timestamp WHERE id = :id")
    suspend fun softDelete(id: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE items SET quantity = :newQuantity, lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateQuantity(id: String, newQuantity: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE items SET hasNotifiedExpiry = :hasNotified WHERE id = :id")
    suspend fun updateHasNotifiedExpiry(id: String, hasNotified: Boolean = true)
}
