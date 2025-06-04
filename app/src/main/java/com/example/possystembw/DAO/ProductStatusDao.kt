package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.ProductStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductStatusDao {
    @Query("SELECT * FROM product_status WHERE productId = :productId")
    suspend fun getProductStatus(productId: String): ProductStatus?

    @Query("SELECT * FROM product_status")
    fun getAllProductStatuses(): Flow<List<ProductStatus>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProductStatus(productStatus: ProductStatus)

    @Update
    suspend fun updateProductStatus(productStatus: ProductStatus)

    @Query("UPDATE product_status SET isEnabled = :isEnabled, updatedAt = :updatedAt WHERE productId = :productId")
    suspend fun updateProductStatusById(productId: String, isEnabled: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM product_status WHERE productId = :productId")
    suspend fun deleteProductStatus(productId: String)

    @Query("DELETE FROM product_status")
    suspend fun deleteAllProductStatuses()
}
