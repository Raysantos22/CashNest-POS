package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TenderDeclaration")
data class TenderDeclaration(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "cashAmount") val cashAmount: Double,
    @ColumnInfo(name = "arPayAmount") val arPayAmount: Double,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "time") val time: String,
    @ColumnInfo(name = "arAmounts") val arAmounts: String // Store as JSON string

)