package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.Product
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

        @Query("SELECT * FROM products")
        fun getAllProducts(): Flow<List<Product>>
        @Query("SELECT * FROM products")
        suspend fun getAllProductsSync(): List<Product>
        @Query("SELECT COUNT(*) FROM products")
        suspend fun getProductCount(): Int

        @Query("SELECT * FROM products WHERE itemgroup = :categoryName")
        fun getProductsByCategory(categoryName: String): Flow<List<Product>>

        @Insert(onConflict = OnConflictStrategy.IGNORE)
        suspend fun insertProduct(product: Product)

        @Update
        suspend fun updateProduct(product: Product)

        @Query("DELETE FROM products")
        suspend fun deleteAll()

        @Query("SELECT * FROM products WHERE itemid = :id")
        suspend fun getProductById(id: Int): Product?

        @Query("SELECT * FROM products WHERE itemname LIKE :name")
        fun getProductsByName(name: String): Flow<List<Product>>


        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insertAll(products: List<Product>)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(product: Product)

        @Update
        suspend fun update(product: Product)

        @Query("DELETE FROM products")
        suspend fun deleteAllProducts()

        @Query("SELECT * FROM products WHERE itemid = :itemId LIMIT 1")
        suspend fun getProductByItemId(itemId: String): Product?
}





