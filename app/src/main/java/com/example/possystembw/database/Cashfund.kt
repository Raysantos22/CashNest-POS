package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cash_fund")
data class Cashfund(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "cash_fund") val cashFund: Double,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "date") val date: String // You might consider using LocalDate or Date type
)
