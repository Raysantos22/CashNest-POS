package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.WindowApiService
import com.example.possystembw.DAO.WindowDao
import com.example.possystembw.RetrofitClient
import com.example.possystembw.database.Product
import com.example.possystembw.database.Window
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


class WindowRepository(
    private val windowDao: WindowDao,
    private val apiService: WindowApiService
) {
    val allWindows: Flow<List<Window>> = windowDao.getAllWindows()

    suspend fun refreshWindows() {
        try {
            val apiWindows = apiService.getAllWindows()
            Log.d("WindowRepository", "Received from API: ${apiWindows.joinToString { it.description }}")
            val validWindows = apiWindows.filter { !it.description.isNullOrBlank() }
            windowDao.deleteAll()
            windowDao.insertAll(validWindows)
        } catch (e: Exception) {
            Log.e("WindowRepository", "Failed to refresh windows", e)
            throw Exception("Failed to refresh windows: ${e.message}")
        }
    }

    suspend fun loadFromLocalDatabase() {
        try {
            Log.d("WindowRepository", "Loading windows from local database")
            // This will trigger the Flow to emit the latest data from the local database
            val localWindows = windowDao.getAllWindowsOneShot()
            Log.d("WindowRepository", "Loaded ${localWindows.size} windows from local database")

            if (localWindows.isEmpty()) {
                Log.w("WindowRepository", "No windows found in local database")
                throw Exception("No data available in local database")
            }
        } catch (e: Exception) {
            Log.e("WindowRepository", "Error loading from local database", e)
            throw e
        }
    }

    suspend fun getWindowsAlignedWithTable(tableId: Int): List<Window> {
        return windowDao.getWindowsAlignedWithTable(tableId)
    }

    suspend fun getWindowById(id: Int): Window? {
        return withContext(Dispatchers.IO) {
            windowDao.getWindowById(id)
        }
    }

    suspend fun insert(window: Window) {
        withContext(Dispatchers.IO) {
            windowDao.insert(window)
        }
    }

    suspend fun update(window: Window) {
        withContext(Dispatchers.IO) {
            windowDao.update(window)
        }
    }

    suspend fun delete(window: Window) {
        withContext(Dispatchers.IO) {
            windowDao.delete(window)
        }
    }
    suspend fun getSortedWindowsForProduct(product: Product): List<Window> {
        val windows = windowDao.getAllWindowsOneShot()
        return windows.filter { window ->
            when {
                // Check if window is GrabFood and product has GrabFood price
                window.description.contains("GRABFOOD", ignoreCase = true) ->
                    product.grabfood > 0

                // Check if window is FoodPanda and product has FoodPanda price
                window.description.contains("FOODPANDA", ignoreCase = true) ->
                    product.foodpanda > 0

                // Check if window is ManilaRate and product has Manila price
                window.description.contains("MANILARATE", ignoreCase = true) ->
                    product.manilaprice > 0

                // If window is Purchase and product only has regular price
                window.description.contains("PURCHASE", ignoreCase = true) ->
                    product.price > 0 && product.grabfood == 0.0 &&
                            product.foodpanda == 0.0 && product.manilaprice == 0.0

                else -> false
            }
        }
    }

    suspend fun getProductPriceForWindow(product: Product, window: Window): Double {
        return when {
            window.description.contains("GRABFOOD", ignoreCase = true) -> product.grabfood
            window.description.contains("FOODPANDA", ignoreCase = true) -> product.foodpanda
            window.description.contains("MANILARATE", ignoreCase = true) -> product.manilaprice
            else -> product.price
        }
    }

}