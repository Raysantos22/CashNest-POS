package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sptables")
data class Sptables(
    @PrimaryKey(autoGenerate = true) val journalId: Int = 0,
    @ColumnInfo(name = "DESCRIPTION") val description: String,
    @ColumnInfo(name = "POSTED") val posted: Boolean,
    @ColumnInfo(name = "POSTEDDATETIME") val postedDateTime: Long,
    @ColumnInfo(name = "JOURNALTYPE") val journalType: String,
    @ColumnInfo(name = "DELETEPOSTEDLINES") val deletePostedLines: Boolean,
    @ColumnInfo(name = "CREATEDDATETIME") val createdDateTime: Long,
    @ColumnInfo(name = "STOREID") val storeId: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
