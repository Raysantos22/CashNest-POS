package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.ProductVisibility
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductVisibilityDao {
    @Query("SELECT * FROM product_visibility WHERE productId = :productId AND platform = :platform LIMIT 1")
    suspend fun getVisibility(productId: Int, platform: String = "PURCHASE"): ProductVisibility?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisibility(visibility: ProductVisibility)

    @Query("UPDATE product_visibility SET is_hidden = :isHidden WHERE productId = :productId AND platform = :platform")
    suspend fun updateVisibility(productId: Int, isHidden: Boolean, platform: String = "PURCHASE")

    @Query("SELECT * FROM product_visibility WHERE is_hidden = 1 AND platform = :platform")
    fun getHiddenProducts(platform: String = "PURCHASE"): Flow<List<ProductVisibility>>

    @Query("SELECT * FROM product_visibility WHERE is_hidden = 1 AND platform = :platform")
    suspend fun getHiddenProductsSync(platform: String = "PURCHASE"): List<ProductVisibility>

    @Query("DELETE FROM product_visibility WHERE productId = :productId AND platform = :platform")
    suspend fun deleteVisibility(productId: Int, platform: String = "PURCHASE")

    @Query("DELETE FROM product_visibility")
    suspend fun deleteAll()

    @Query("SELECT * FROM product_visibility")
    fun getAllVisibilityRecords(): Flow<List<ProductVisibility>>
}