package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["groupId"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "itemid") val itemid: String = "",
    @ColumnInfo(name = "activeondelivery") val activeOnDelivery: Boolean = false,
    @ColumnInfo(name = "itemname") val itemName: String = "",
    @ColumnInfo(name = "itemgroup") val itemGroup: String = "",
    @ColumnInfo(name = "specialgroup") val specialGroup: String = "",
    @ColumnInfo(name = "production") val production: String = "",
    @ColumnInfo(name = "moq") val moq: Long = 0,
    @ColumnInfo(name = "price") val price: Double = 0.0,
    @ColumnInfo(name = "cost") val cost: Double = 0.0,
    @ColumnInfo(name = "barcode") val barcode: Long = 0,
    @ColumnInfo(name = "categoryId")val categoryId: Long,
    @ColumnInfo(name = "foodpanda")val foodpanda: Double,
    @ColumnInfo(name = "grabfood")val grabfood: Double,
    @ColumnInfo(name = "manilaprice")val manilaprice: Double
)

//    @ColumnInfo(name = "discountType") val discountType: String = "",
//    @ColumnInfo(name = "discountValue") val discountValue: Double = 0.0

