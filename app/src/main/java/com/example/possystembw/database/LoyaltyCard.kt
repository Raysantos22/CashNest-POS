package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "loyalty_cards")
data class LoyaltyCard(
    @PrimaryKey
    val id: Int,
    val cardNumber: String,
    val customerId: Int,
    val customerName: String?,
    val points: Int,
    val pointsFormatted: String,
    val tier: String,
    val status: String,
    val expiryDate: String?,
    val createdAt: String?,
    val isActive: Boolean,
    val cumulativeAmount: Double = 0.0,
    val syncStatus: Int = 1 // 1 for synced, 0 for needs sync
)
