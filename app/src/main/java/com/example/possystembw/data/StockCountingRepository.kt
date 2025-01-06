package com.example.possystembw.data

import com.example.possystembw.DAO.StockCountingApi
import com.example.possystembw.DAO.StockCountingDao
import com.example.possystembw.DAO.StockCountingData
import com.example.possystembw.database.StockCountingEntity

class StockCountingRepository(
    private val api: StockCountingApi,
    private val dao: StockCountingDao
) {
    suspend fun getStockCounting(storeId: String): Result<List<StockCountingEntity>> = runCatching {
        val response = api.getStockCounting(storeId)
        if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody != null && responseBody.success) {
                val stockCountingEntities = responseBody.data.map { it.toEntity() }
                dao.updateStockCounting(storeId, stockCountingEntities)
                stockCountingEntities
            } else {
                throw Exception("Failed to fetch stock counting data: ${response.message()}")
            }
        } else {
            throw Exception("API call failed with code: ${response.code()}")
        }
    }

    suspend fun getLocalStockCounting(storeId: String): List<StockCountingEntity> {
        return dao.getStockCountingByStore(storeId)
    }
}

// Extension function to convert API response to Entity

fun StockCountingData.toEntity() = StockCountingEntity(
    journalId = journalId,
    storeId = storeId,
    description = description,
    quantity = quantity ?: "0",  // Provide default value if null
    amount = amount ?: "0.00",   // Provide default value if null
    posted = posted,
    updatedAt = updatedAt,
    journalType = journalType,
    createdDateTime = createdDateTime
)