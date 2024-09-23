package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "importproducts")
data class Importproducts(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "itemid") val itemId: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "searchalias") val searchAlias: String,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "retailgroup") val retailGroup: String,
    @ColumnInfo(name = "retaildepartment") val retailDepartment: String,
    @ColumnInfo(name = "salestaxgroup") val salesTaxGroup: String,
    @ColumnInfo(name = "costprice") val costPrice: Double,
    @ColumnInfo(name = "salesprice") val salesPrice: Double,
    @ColumnInfo(name = "barcodesetup") val barcodeSetup: String,
    @ColumnInfo(name = "barcode") val barcode: String,
    @ColumnInfo(name = "barcodeunit") val barcodeUnit: String,
    @ColumnInfo(name = "activestatus") val activeStatus: Boolean
)
