package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "product_visibility")
data class ProductVisibility(
    @PrimaryKey val productId: Int,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false
)
