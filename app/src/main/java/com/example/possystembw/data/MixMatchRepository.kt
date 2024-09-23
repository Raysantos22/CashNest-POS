package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.MixMatchApi
import com.example.possystembw.DAO.MixMatchDao
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchDiscountLine
import com.example.possystembw.database.MixMatchLineGroup
import com.example.possystembw.database.MixMatchWithDetails

class MixMatchRepository(
    private val mixMatchDao: MixMatchDao,
    private val mixMatchApi: MixMatchApi
) {
    suspend fun refreshMixMatches() {
        try {
            val mixMatches = mixMatchApi.getMixMatches()

            // Clear existing data before inserting new data
            mixMatchDao.deleteAllMixMatches()
            mixMatchDao.deleteAllLineGroups()
            mixMatchDao.deleteAllDiscountLines()

            // Insert new data
            mixMatches.forEach { response ->
                // Insert main mix match
                val mixMatch = MixMatch(
                    id = response.id,
                    description = response.description,
                    discountType = response.discounttype,
                    dealPriceValue = response.dealpricevalue,
                    discountPctValue = response.discountpctvalue,
                    discountAmountValue = response.discountamountvalue
                )
                mixMatchDao.insertMixMatch(mixMatch)

                // Insert line groups and their discount lines
                response.line_groups.forEach { lineGroupResponse ->
                    val lineGroup = MixMatchLineGroup(
                        mixMatchId = response.id,
                        lineGroup = lineGroupResponse.linegroup,
                        description = lineGroupResponse.description,
                        noOfItemsNeeded = lineGroupResponse.noofitemsneeded
                    )

                    val lineGroupId = mixMatchDao.insertLineGroup(lineGroup)

                    // Insert discount lines
                    lineGroupResponse.discount_lines.forEach { discountLine ->
                        val mixMatchDiscountLine = MixMatchDiscountLine(
                            lineGroupId = lineGroupId.toInt(),
                            itemId = discountLine.itemid,
                            discType = discountLine.disctype,
                            dealPriceOrDiscPct = discountLine.dealpriceordiscpct,
                            qty = discountLine.qty
                        )
                        mixMatchDao.insertDiscountLine(mixMatchDiscountLine)
                    }
                }
            }
        } catch (e: Exception) {
            // If API fails, just use existing local data
            Log.e("MixMatchRepository", "Error refreshing mix matches from API", e)
            // Don't throw the exception - this allows offline operation
        }
    }

    suspend fun getAllMixMatches(): List<MixMatchWithDetails> {
        return mixMatchDao.getAllMixMatches()
    }
}
