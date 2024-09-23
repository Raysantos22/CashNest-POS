package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.ARApi
import com.example.possystembw.DAO.ARDao
import com.example.possystembw.database.AR
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ARRepository(private val arApi: ARApi, private val arDao: ARDao) {
    val allARTypes: Flow<List<AR>> = arDao.getAllARTypes()

    suspend fun refreshARTypes() {
        withContext(Dispatchers.IO) {
            try {
                val arTypes = arApi.getARTypes()
                // Store new AR types in database
                arDao.deleteAllARTypes()
                arDao.insertARTypes(arTypes)
            } catch (e: Exception) {
                Log.e("ARRepository", "Error fetching AR types from API: ${e.message}")
                // Don't throw exception - let the Flow continue providing cached data
            }
        }
    }

    // Add method to get AR types directly from database
    suspend fun getLocalARTypes(): List<AR> = withContext(Dispatchers.IO) {
        try {
            arDao.getAllARTypesImmediate() // You'll need to add this method to your DAO
        } catch (e: Exception) {
            Log.e("ARRepository", "Error getting local AR types: ${e.message}")
            emptyList()
        }
    }
}

