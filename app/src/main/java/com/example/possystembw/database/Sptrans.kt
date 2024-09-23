package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sptrans")
data class Sptrans(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "JOURNALID") val journalId: Int,
    @ColumnInfo(name = "STORENAME") val storeName: String,
    @ColumnInfo(name = "TRANSDATE") val transDate: Long,
    @ColumnInfo(name = "ITEMID") val itemId: String,
    @ColumnInfo(name = "ADJUSTMENT") val adjustment: Double,
    @ColumnInfo(name = "COSTPRICE") val costPrice: Double,
    @ColumnInfo(name = "PRICEUNIT") val priceUnit: Double,
    @ColumnInfo(name = "SALESAMOUNT") val salesAmount: Double,
    @ColumnInfo(name = "INVENTONHAND") val inventOnHand: Int,
    @ColumnInfo(name = "COUNTED") val counted: Int,
    @ColumnInfo(name = "REASONREFRECID") val reasonRefRecId: String?,
    @ColumnInfo(name = "VARIANTID") val variantId: String?,
    @ColumnInfo(name = "POSTED") val posted: Boolean,
    @ColumnInfo(name = "POSTEDDATETIME") val postedDateTime: Long,
    @ColumnInfo(name = "UNITID") val unitId: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "COUNT") val count: Int,
    @ColumnInfo(name = "REMARKS") val remarks: String?
)
