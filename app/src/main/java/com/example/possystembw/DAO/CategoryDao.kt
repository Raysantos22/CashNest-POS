package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(category: Category)

  /*  @Query("SELECT * FROM category")
    fun getAllCategories(): Flow<List<Category>>*/

    @Query("SELECT * FROM category ORDER BY name ASC")
    fun getAllCategories(): Flow<List<Category>>
    @Query("SELECT * FROM category WHERE groupId = :id")
    suspend fun getCategoryById(id: Long): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<Category>)
    @Query("DELETE FROM category")
    suspend fun deleteAllCategories()
    @Query("SELECT COUNT(*) FROM category")
    suspend fun getCategoryCount(): Int
    @Update
    suspend fun updateCategory(category: Category)
    @Query("SELECT * FROM category WHERE name = :name LIMIT 1")
    suspend fun getCategoryByName(name: String): Category?
    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("DELETE FROM category")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("SELECT * FROM category")
    fun getAllCategoriesSync(): List<Category>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Query("DELETE FROM category WHERE groupId > 0") // Keeps default categories (negative IDs)
    suspend fun deleteNonDefaultCategories()

}