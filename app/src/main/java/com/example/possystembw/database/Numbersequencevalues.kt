package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "numbersequencevalues")
data class Numbersequencevalues(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "NUMBERSEQUENCE") val numberSequence: String,
    @ColumnInfo(name = "NEXTREC") val nextRec: Int,
    @ColumnInfo(name = "STOREID") val storeId: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
