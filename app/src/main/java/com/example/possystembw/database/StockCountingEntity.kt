package com.example.possystembw.database

import androidx.room.Entity
import com.example.possystembw.DAO.StockCountingData

@Entity(tableName = "stock_counting", primaryKeys = ["journalId", "storeId"])
data class StockCountingEntity(
    val journalId: Long,
    val storeId: String,
    val description: String,
    val quantity: String?,  // Make nullable
    val amount: String?,    // Make nullable
    val posted: Int,
    val updatedAt: String?,
    val journalType: Int,
    val createdDateTime: String
)

// Extension function to convert API response to Entity with null safety
