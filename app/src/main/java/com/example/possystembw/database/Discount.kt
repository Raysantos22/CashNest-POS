package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

//@Entity(tableName = "discounts")
//data class Discount(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val DISCOFFERNAME: String,
//    val PARAMETER: Int,
//    val DISCOUNTTYPE: String // "percentage" or "fixed"
//)

//@Entity(tableName = "discounts")
//data class Discount(
//    @PrimaryKey(autoGenerate = true) val id: Int = 0,
//    val DISCOFFERNAME: String,
//    val PARAMETER: Int, // Default price parameter (keeping for backward compatibility)
//    val DISCOUNTTYPE: String, // "percentage" or "fixed"
//
//    // New parameters for different pricing tiers
//    val GRABFOOD_PARAMETER: Int? = null,
//    val FOODPANDA_PARAMETER: Int? = null,
//    val MANILAPRICE_PARAMETER: Int? = null
//)

@Entity(tableName = "discounts")
data class Discount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val DISCOFFERNAME: String,
    val PARAMETER: Int, // Default price parameter (keeping for backward compatibility)
    val DISCOUNTTYPE: String, // "percentage" or "fixed"

    // Existing parameters for different pricing tiers
    val GRABFOOD_PARAMETER: Int? = null,
    val FOODPANDA_PARAMETER: Int? = null,
    val MANILAPRICE_PARAMETER: Int? = null,

    // NEW PARAMETERS FOR NEW PRICING TIERS
    val MALLPRICE_PARAMETER: Int? = null,
    val GRABFOODMALL_PARAMETER: Int? = null,
    val FOODPANDAMALL_PARAMETER: Int? = null
)