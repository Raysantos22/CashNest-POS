package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rboinventables")
data class Rboinventables(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "itemid") val itemId: String,
    @ColumnInfo(name = "itemtype") val itemType: String,
    @ColumnInfo(name = "itemgroup") val itemGroup: String,
    @ColumnInfo(name = "itemdepartment") val itemDepartment: String,
    @ColumnInfo(name = "zeropricevalid") val zeroPriceValid: Boolean,
    @ColumnInfo(name = "dateblocked") val dateBlocked: Long?,
    @ColumnInfo(name = "datetobeblocked") val dateToBeBlocked: Long?,
    @ColumnInfo(name = "blockedonpos") val blockedOnPos: Boolean,
    @ColumnInfo(name = "Activeondelivery") val activeOnDelivery: Boolean,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "datetoactivateitem") val dateToActivateItem: Long?,
    @ColumnInfo(name = "mustselectuom") val mustSelectUOM: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "PRODUCTION") val production: String,
    @ColumnInfo(name = "moq") val moq: Int,
    @ColumnInfo(name = "fgcount") val fgCount: Int,
    @ColumnInfo(name = "TRANSPARENTSTOCKS") val transparentStocks: Int,
    @ColumnInfo(name = "stocks") val stocks: Int
)
