package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bundles")
data class ProductBundle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "start_date") val startDate: Long,
    @ColumnInfo(name = "end_date") val endDate: Long,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "discount_type") val discountType: String = "", // "PERCENTAGE", "FIXED", "DEAL"
    @ColumnInfo(name = "percentage_value") val percentageValue: Double? = null, // Used when discount_type is "PERCENTAGE"
    @ColumnInfo(name = "discount_amount") val discountAmount: Double? = null, // Used when discount_type is "FIXED"
    @ColumnInfo(name = "deal_price") val dealPrice: Double? = null  // Used when discount_type is "DEAL"
)