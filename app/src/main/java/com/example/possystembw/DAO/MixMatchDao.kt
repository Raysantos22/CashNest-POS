package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchDiscountLine
import com.example.possystembw.database.MixMatchLineGroup
import com.example.possystembw.database.MixMatchWithDetails

@Dao
interface MixMatchDao {
    @Transaction
    @Query("SELECT * FROM mix_match")
    suspend fun getAllMixMatches(): List<MixMatchWithDetails>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMixMatch(mixMatch: MixMatch)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineGroup(lineGroup: MixMatchLineGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiscountLine(discountLine: MixMatchDiscountLine)

    @Query("DELETE FROM mix_match")
    suspend fun deleteAllMixMatches()

    @Query("DELETE FROM mix_match_line_groups")
    suspend fun deleteAllLineGroups()

    @Query("DELETE FROM mix_match_discount_lines")
    suspend fun deleteAllDiscountLines()
    @Transaction
    suspend fun insertFullMixMatch(
        mixMatchResponse: MixMatchApiResponse
    ) {
        // Insert main mix match record
        val mixMatch = MixMatch(
            id = mixMatchResponse.id,
            description = mixMatchResponse.description,
            discountType = mixMatchResponse.discounttype,
            dealPriceValue = mixMatchResponse.dealpricevalue,
            discountPctValue = mixMatchResponse.discountpctvalue,
            discountAmountValue = mixMatchResponse.discountamountvalue
        )
        insertMixMatch(mixMatch)

        // Insert line groups and their discount lines
        mixMatchResponse.line_groups.forEach { lineGroupResponse ->
            val lineGroup = MixMatchLineGroup(
                mixMatchId = mixMatch.id,
                lineGroup = lineGroupResponse.linegroup,
                description = lineGroupResponse.description,
                noOfItemsNeeded = lineGroupResponse.noofitemsneeded
            )
            val lineGroupId = insertLineGroup(lineGroup)

            // Insert discount lines for this line group
            lineGroupResponse.discount_lines.forEach { discountLineResponse ->
                val discountLine = MixMatchDiscountLine(
                    lineGroupId = lineGroupId.toInt(),
                    itemId = discountLineResponse.itemid,
                    discType = discountLineResponse.disctype,
                    dealPriceOrDiscPct = discountLineResponse.dealpriceordiscpct,
                    qty = discountLineResponse.qty
                )
                insertDiscountLine(discountLine)
            }
        }
    }
}
