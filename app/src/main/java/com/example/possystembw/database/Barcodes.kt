package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "barcodes")
data class Barcodes(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "description") val description: String?,
    @ColumnInfo(name = "isuse") val isUse: Boolean,
    @ColumnInfo(name = "generateby") val generatedBy: String,
    @ColumnInfo(name = "generatedate") val generateDate: Long,
    @ColumnInfo(name = "modifiedby") val modifiedBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
