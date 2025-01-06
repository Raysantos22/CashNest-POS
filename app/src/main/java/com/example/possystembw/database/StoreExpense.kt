package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "store_expenses")
data class StoreExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expenseType: String,
    val amount: Double,
    val receivedBy: String,
    val approvedBy: String,
    val effectDate: String,
    val storeId: String,
    val syncStatus: Int = 0, // 0: Not synced, 1: Synced
    val timestamp: Long = System.currentTimeMillis()
)
