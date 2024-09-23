package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.WindowApiService
import com.example.possystembw.DAO.WindowDao
import com.example.possystembw.RetrofitClient
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
            // TODO: Implement API call to insert window on the server
        }
    }

    suspend fun update(window: Window) {
        withContext(Dispatchers.IO) {
            windowDao.update(window)
            // TODO: Implement API call to update window on the server
        }
    }

    suspend fun delete(window: Window) {
        withContext(Dispatchers.IO) {
            windowDao.delete(window)
            // TODO: Implement API call to delete window on the server
        }
    }
}