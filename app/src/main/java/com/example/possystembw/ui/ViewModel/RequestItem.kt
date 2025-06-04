package com.example.possystembw.ui.ViewModel// Request Models (RequestModels.kt)

data class RequestItem(
    val itemId: String = "",
    val itemName: String,
    val itemGroup: String,
    val price: Double,
    val cost: Double = 0.0,
    val requestType: String, // "add_item", "edit_item", "update_item"
    val status: String // "pending", "approved", "rejected"
)

data class MixMatchRequest(
    val id: String = "",
    val description: String,
    val discountType: Int, // 1: Deal Price, 2: Percentage Discount, 3: Amount Discount
    val discountValue: Double,
    val itemsNeeded: Int,
    val lineGroups: MutableList<MixMatchLineGroupRequest>,
    val requestType: String, // "create_mix_match"
    val status: String // "pending", "approved", "rejected"
)

data class MixMatchLineGroupRequest(
    val lineGroup: String,
    val description: String,
    val noOfItemsNeeded: Int,
    val discountLines: List<MixMatchItemRequest>
)

data class MixMatchItemRequest(
    val itemId: String,
    val itemName: String,
    val qty: Int,
    val price: Double = 0.0 // Added price field for tracking totals
)

data class DiscountRequest(
    val id: Int = 0,
    val discountName: String,
    val discountType: String, // "percentage" or "fixed"
    val parameter: Int,
    val requestType: String, // "add_discount", "edit_discount"
    val status: String // "pending", "approved", "rejected"
)