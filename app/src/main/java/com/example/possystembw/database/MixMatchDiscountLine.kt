package com.example.possystembw.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "mix_match_discount_lines")
data class MixMatchDiscountLine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val lineGroupId: Int,
    val itemId: String,
    val discType: Int,
    val dealPriceOrDiscPct: Double,
    val qty: Int
)
data class MixMatchWithDetails(
    @Embedded val mixMatch: MixMatch,
    @Relation(
        entity = MixMatchLineGroup::class,
        parentColumn = "id",
        entityColumn = "mixMatchId"
    )
    val lineGroups: List<LineGroupWithDiscounts>
)

data class LineGroupWithDiscounts(
    @Embedded val lineGroup: MixMatchLineGroup,
    @Relation(
        parentColumn = "id",
        entityColumn = "lineGroupId"
    )
    val discountLines: List<MixMatchDiscountLine>
)
