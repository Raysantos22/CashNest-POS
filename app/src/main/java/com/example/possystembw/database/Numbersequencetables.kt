package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "numbersequencetables")
data class Numbersequencetables(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "NUMBERSEQUENCE") val numberSequence: String,
    @ColumnInfo(name = "TXT") val txt: String,
    @ColumnInfo(name = "LOWEST") val lowest: Int,
    @ColumnInfo(name = "HIGHEST") val highest: Int,
    @ColumnInfo(name = "BLOCKED") val blocked: Boolean,
    @ColumnInfo(name = "STOREID") val storeId: Int,
    @ColumnInfo(name = "CANBEDELETED") val canBeDeleted: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
