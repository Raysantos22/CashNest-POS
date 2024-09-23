package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "zread")
data class ZRead(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val zReportId: String,
    val date: String,
    val time: String,
    val totalTransactions: Int,
    val totalAmount: Double
)
