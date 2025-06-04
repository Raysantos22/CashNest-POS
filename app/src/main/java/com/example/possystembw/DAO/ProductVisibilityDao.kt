package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.ProductVisibility
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVisibilityDao {
    @Query("SELECT * FROM product_visibility WHERE productId = :productId LIMIT 1")
    suspend fun getVisibility(productId: Int): ProductVisibility?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisibility(visibility: ProductVisibility)

    @Query("UPDATE product_visibility SET is_hidden = :isHidden WHERE productId = :productId")
    suspend fun updateVisibility(productId: Int, isHidden: Boolean)

    @Query("SELECT * FROM product_visibility WHERE is_hidden = 1")
    fun getHiddenProducts(): Flow<List<ProductVisibility>>

    @Query("DELETE FROM product_visibility WHERE productId = :productId")
    suspend fun deleteVisibility(productId: Int)

    @Query("DELETE FROM product_visibility")
    suspend fun deleteAll()

    // Add this method to get all visibility records
    @Query("SELECT * FROM product_visibility")
    fun getAllVisibilityRecords(): Flow<List<ProductVisibility>>


}
