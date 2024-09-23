package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventtablemodules")
data class Inventtablemodules(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "itemid") val itemId: String,
    @ColumnInfo(name = "moduletype") val moduleType: String,
    @ColumnInfo(name = "unitid") val unitId: String,
    @ColumnInfo(name = "price") val price: Double,
    @ColumnInfo(name = "priceunit") val priceUnit: String,
    @ColumnInfo(name = "priceincltax") val priceInclTax: Boolean,
    @ColumnInfo(name = "quantity") val quantity: Int,
    @ColumnInfo(name = "lowestqty") val lowestQty: Int,
    @ColumnInfo(name = "highestqty") val highestQty: Int,
    @ColumnInfo(name = "blocked") val blocked: Boolean,
    @ColumnInfo(name = "inventlocationid") val inventLocationId: String,
    @ColumnInfo(name = "pricedate") val priceDate: Long,
    @ColumnInfo(name = "taxitemgroupid") val taxItemGroupId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
