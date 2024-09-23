package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discounts")
data class Discount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val DISCOFFERNAME: String,
    val PARAMETER: Int,
    val DISCOUNTTYPE: String // "percentage" or "fixed"
)