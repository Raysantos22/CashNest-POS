package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.WindowTableApi
import com.example.possystembw.DAO.WindowTableDao
import com.example.possystembw.database.WindowTable
import kotlinx.coroutines.flow.Flow

class WindowTableRepository(
    private val windowTableDao: WindowTableDao,
    private val windowTableApi: WindowTableApi
) {
    val allWindowTables: Flow<List<WindowTable>> = windowTableDao.getAllWindowTables()

    suspend fun refreshWindowTables() {
        try {
            val response = windowTableApi.getWindowTables()
            if (response.isSuccessful) {
                response.body()?.let { windowTableResponse ->
                    val windowTables = windowTableResponse.windowtables // Extract the array from the wrapper
                    Log.d(
                        "WindowTableRepository",
                        "Received ${windowTables.size} window tables from API"
                    )
                    windowTableDao.deleteAll()
                    windowTableDao.insertAll(windowTables)
                    Log.d(
                        "WindowTableRepository",
                        "Successfully updated local database with new window tables"
                    )
                } ?: Log.e("WindowTableRepository", "Received null body from API")
            } else {
                Log.e(
                    "WindowTableRepository",
                    "Error refreshing window tables: ${response.code()} - ${response.message()}"
                )
                throw Exception("API error: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e("WindowTableRepository", "Exception refreshing window tables", e)
            throw e
        }
    }

    suspend fun loadFromLocalDatabase() {
        try {
            Log.d("WindowTableRepository", "Loading window tables from local database")
            val localTables = windowTableDao.getAllWindowTablesOneShot()
            Log.d("WindowTableRepository", "Loaded ${localTables.size} window tables from local database")

            if (localTables.isEmpty()) {
                Log.w("WindowTableRepository", "No window tables found in local database")
                throw Exception("No data available in local database")
            }
        } catch (e: Exception) {
            Log.e("WindowTableRepository", "Error loading from local database", e)
            throw e
        }
    }
}
