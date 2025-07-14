package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

//@Entity(tableName = "product_visibility")
//data class ProductVisibility(
//    @PrimaryKey val productId: Int,
//    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false
//)
@Entity(tableName = "product_visibility")
data class ProductVisibility(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "productId") val productId: Int,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false,
    @ColumnInfo(name = "platform") val platform: String = "GENERAL" // GENERAL, FOODPANDA, GRABFOOD, MANILARATE
)