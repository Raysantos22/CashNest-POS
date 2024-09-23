package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "number_sequence")
data class NumberSequenceEntity(
    @PrimaryKey
    @ColumnInfo(name = "number_sequence") val numberSequence: String,
    @ColumnInfo(name = "next_rec") val nextRec: Int,
    @ColumnInfo(name = "cart_next_rec") val cartNextRec: Int,
    @ColumnInfo(name = "bundle_next_rec") val bundleNextRec: Int,
    @ColumnInfo(name = "discount_next_rec") val discountNextRec: Int,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "waste_rec") val wasteRec: Int,
    @ColumnInfo(name = "to_next_rec") val toNextRec: Int,
    @ColumnInfo(name = "stock_next_rec") val stockNextRec: Int,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "last_sync_attempt") val lastSyncAttempt: Long? = null
)