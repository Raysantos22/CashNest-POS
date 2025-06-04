package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_status")
data class ProductStatus(
    @PrimaryKey val productId: String, // Use itemid from Product as key
    @ColumnInfo(name = "isEnabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long = System.currentTimeMillis()
)
