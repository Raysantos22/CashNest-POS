package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.CategoryApi
import com.example.possystembw.DAO.CategoryDao
import com.example.possystembw.database.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CategoryRepository(
    private val categoryApi: CategoryApi,
    private val categoryDao: CategoryDao
) {
    // Get categories with offline support
    fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories()
            .map { categories ->
                // Ensure "All" category is always present
                val hasAllCategory = categories.any { it.name == "All" }
                if (!hasAllCategory) {
                    val allCategory = Category(name = "All")
                    listOf(allCategory) + categories
                } else {
                    categories
                }
            }

    // Refresh categories from API with error handling
    suspend fun refreshCategories() {
        try {
            val response = categoryApi.getCategories()
            if (response.isSuccessful && response.body() != null) {
                val remoteCategories = response.body() // Extract the list of categories
                if (remoteCategories != null) {
                    categoryDao.insertCategories(remoteCategories)
                }
            } else {
                // Handle the case where the response is not successful
                Log.e("CategoryRepository", "Failed to fetch categories: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("CategoryRepository", "Error refreshing categories", e)
            // If no categories exist locally, insert default ones
            if (categoryDao.getCategoryCount() == 0) {
                val defaultCategories = listOf(
                    Category(name = "All"),
                    Category(name = "Mix & Match")
                )
                categoryDao.insertCategories(defaultCategories)
            }
            // Continue with local data if refresh fails
        }
    }

    // Add method to ensure default categories exist
    suspend fun ensureDefaultCategories() {
        val categoryCount = categoryDao.getCategoryCount()
        if (categoryCount == 0) {
            val defaultCategories = listOf(
                Category(name = "All"),
                Category(name = "Mix & Match")
            )
            categoryDao.insertCategories(defaultCategories)
        }
    }
}