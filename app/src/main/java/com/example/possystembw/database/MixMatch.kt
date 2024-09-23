package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mix_match")
data class MixMatch(
    @PrimaryKey val id: String,
    val description: String,
    val discountType: Int,
    val dealPriceValue: Double,
    val discountPctValue: Double,
    val discountAmountValue: Double
)