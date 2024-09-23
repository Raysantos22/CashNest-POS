package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventitembarcodes")
data class Inventitembarcodes(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "ITEMBARCODE") val itemBarcode: String,
    @ColumnInfo(name = "ITEMID") val itemId: String,
    @ColumnInfo(name = "BARCODESETUPID") val barcodeSetupId: String,
    @ColumnInfo(name = "DESCRIPTION") val description: String,
    @ColumnInfo(name = "QTY") val quantity: Int,
    @ColumnInfo(name = "UNITID") val unitId: String,
    @ColumnInfo(name = "RBOVARIANTID") val rboVariantId: String,
    @ColumnInfo(name = "BLOCKED") val blocked: Boolean,
    @ColumnInfo(name = "MODIFIEDBY") val modifiedBy: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
