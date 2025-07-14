package com.example.possystembw.DAO

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.example.possystembw.database.LineTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Dao
interface LineTransactionDao {
    @Query("SELECT * FROM line_transactions WHERE journalId = :journalId")
    suspend fun getLineTransactionsByJournal(journalId: String): List<LineTransactionEntity>

    @Query("DELETE FROM line_transactions WHERE journalId = :journalId")
    suspend fun deleteLineTransactionsByJournal(journalId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLineTransactions(transactions: List<LineTransactionEntity>)

    @Query("SELECT * FROM line_transactions WHERE journalId = :journalId AND syncStatus = 0")
    suspend fun getUnsyncedTransactions(journalId: String): List<LineTransactionEntity>

    @Query("UPDATE line_transactions SET syncStatus = 0 WHERE journalId = :journalId AND itemId = :itemId")
    suspend fun markAsUnsynced(journalId: String, itemId: String)

    @Query("UPDATE line_transactions SET syncStatus = 1 WHERE journalId = :journalId AND itemId = :itemId")
    suspend fun markAsSynced(journalId: String, itemId: String)

    @Query("SELECT COUNT(*) FROM line_transactions WHERE journalId = :journalId AND syncStatus = 0")
    suspend fun getUnsyncedCount(journalId: String): Int
    @Query("DELETE FROM line_transactions WHERE journalId = :journalId")
    suspend fun deleteByJournal(journalId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LineTransactionEntity>)


    @Query("DELETE FROM line_transactions")
    suspend fun deleteAllLineTransactions()

//    @Query("DELETE FROM line_transactions WHERE journalId = :journalId")
//    suspend fun deleteLineTransactionsByJournal(journalId: String)

    @Query("SELECT * FROM line_transactions WHERE journalId = :journalId")
    fun getLineTransactionsByJournalLiveData(journalId: String): LiveData<List<LineTransactionEntity>>

    @RawQuery
    suspend fun getTodayFilteredItemsRaw(query: SupportSQLiteQuery): List<LineTransaction>

    @Query("UPDATE line_transactions SET posted = 1, syncStatus = 1 WHERE journalId = :journalId")
    suspend fun markAllAsPosted(journalId: String)

    @Query("UPDATE line_transactions SET posted = 1, syncStatus = 1, postedDateTime = :currentDateTime WHERE journalId = :journalId")
    suspend fun markAllAsPostedWithDateTime(journalId: String, currentDateTime: String)

    // Also add this to your StockCountingDao if you have one


    // Alternative: Use a regular @Query (even better)
// Method to get all line transactions for debugging
    @Query("SELECT * FROM line_transactions WHERE journalId = :journalId")
    suspend fun getAllLineTransactions(journalId: String): List<LineTransactionEntity>

    // Method to get all transactions for a specific date for debugging

    // Fixed main query - removed posted requirement and improved logic
    @Query("""
    SELECT DISTINCT b.* 
    FROM line_transactions b 
    LEFT JOIN transactions a ON a.itemid = b.itemId 
        AND DATE(datetime(a.timestamp/1000, 'unixepoch')) = :currentDate
    WHERE b.journalId = :journalId
    AND (
        (a.quantity IS NOT NULL AND a.quantity != 0) 
        OR (CAST(b.receivedCount AS REAL) - CAST(b.adjustment AS REAL) != 0)
        OR (CAST(b.wasteCount AS REAL) != 0)
        OR (CAST(b.transferCount AS REAL) != 0)
        OR (CAST(b.counted AS REAL) != 0)
        OR (b.syncStatus = 0)
    )
""")
    suspend fun getItemsWithTransactions(
        journalId: String,
        currentDate: String
    ): List<LineTransactionEntity>

    // Simplified alternative query - check for any activity regardless of posted status
    @Query("""
    SELECT b.* 
    FROM line_transactions b 
    WHERE b.journalId = :journalId
    AND (
        (CAST(b.receivedCount AS REAL) - CAST(b.adjustment AS REAL) != 0)
        OR (CAST(b.wasteCount AS REAL) != 0)
        OR (CAST(b.counted AS REAL) != 0)
        OR (CAST(b.transferCount AS REAL) != 0)
        OR (b.syncStatus = 0)
    )
""")
    suspend fun getItemsWithQuantities(journalId: String): List<LineTransactionEntity>

    // Additional debugging query to check transactions table
    @Query("""
    SELECT COUNT(*) FROM transactions 
    WHERE itemid IS NOT NULL 
    AND quantity != 0
    AND DATE(datetime(timestamp/1000, 'unixepoch')) = :currentDate
""")
    suspend fun getTransactionCountForDate(currentDate: String): Int

    // Query to check if there are any matching itemIds between tables
    @Query("""
    SELECT COUNT(*) FROM line_transactions b 
    INNER JOIN transactions a ON a.itemid = b.itemId 
    WHERE b.journalId = :journalId
""")
    suspend fun getMatchingItemCount(journalId: String): Int

    // Query to find specific itemId in line_transactions
    @Query("""
    SELECT * FROM line_transactions 
    WHERE itemId = :itemId AND journalId = :journalId
""")
    suspend fun findLineTransactionByItemId(itemId: String, journalId: String): LineTransactionEntity?

    // Query to get all unique itemIds from line_transactions for a journal
    @Query("""
    SELECT DISTINCT itemId FROM line_transactions 
    WHERE journalId = :journalId
    ORDER BY itemId
""")
    suspend fun getAllItemIdsForJournal(journalId: String): List<String>



    @Query("""
        UPDATE line_transactions 
        SET adjustment = :adjustment,
            receivedCount = :receivedCount,
            transferCount = :transferCount,
            wasteCount = :wasteCount,
            counted = :counted,
            wasteType = :wasteType,
            syncStatus = :syncStatus,
            variantId = :variantId,
            updatedAt = :updatedAt
        WHERE journalId = :journalId AND itemId = :itemId
    """)
    suspend fun updateSpecificItem(
        journalId: String,
        itemId: String,
        adjustment: String,
        receivedCount: String,
        transferCount: String?,
        wasteCount: String,
        counted: String,
        wasteType: String?,
        syncStatus: Int,
        variantId: String?,
        updatedAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
    )
    @Query("""
    UPDATE line_transactions 
    SET adjustment = :adjustment,
        receivedCount = :receivedCount,
        transferCount = :transferCount,
        wasteCount = :wasteCount,
        counted = :counted,
        wasteType = :wasteType,
        variantId = :variantId,
        syncStatus = :syncStatus,
        updatedAt = datetime('now')
    WHERE journalId = :journalId AND itemId = :itemId
""")
    suspend fun updateSpecificItemData(
        journalId: String,
        itemId: String,
        adjustment: String,
        receivedCount: String,
        transferCount: String,
        wasteCount: String,
        counted: String,
        wasteType: String?,
        variantId: String?,
        syncStatus: Int
    )
    /**
     * Get count of items that have been modified but not yet synced
     */
    @Query("""
        SELECT COUNT(*) FROM line_transactions 
        WHERE journalId = :journalId 
        AND syncStatus = 0 
        AND (
            CAST(adjustment AS REAL) > 0 OR
            CAST(receivedCount AS REAL) > 0 OR
            CAST(transferCount AS REAL) > 0 OR
            CAST(wasteCount AS REAL) > 0 OR
            CAST(counted AS REAL) > 0 OR
            (wasteType IS NOT NULL AND wasteType != 'none' AND wasteType != 'Select type')
        )
    """)
    suspend fun getModifiedUnsyncedCount(journalId: String): Int

    /**
     * Get items that are marked as unsynced and have actual values
     */
    @Query("""
        SELECT * FROM line_transactions 
        WHERE journalId = :journalId 
        AND syncStatus = 0 
        AND (
            CAST(adjustment AS REAL) > 0 OR
            CAST(receivedCount AS REAL) > 0 OR
            CAST(transferCount AS REAL) > 0 OR
            CAST(wasteCount AS REAL) > 0 OR
            CAST(counted AS REAL) > 0 OR
            (wasteType IS NOT NULL AND wasteType != 'none' AND wasteType != 'Select type')
        )
    """)
    suspend fun getModifiedUnsyncedItems(journalId: String): List<LineTransactionEntity>


@Transaction
suspend fun updateSpecificItems(entities: List<LineTransactionEntity>) {
    entities.forEach { entity ->
        updateSpecificItem(
            journalId = entity.journalId,
            itemId = entity.itemId,
            adjustment = entity.adjustment,
            receivedCount = entity.receivedCount,
            transferCount = entity.transferCount,
            wasteCount = entity.wasteCount,
            counted = entity.counted,
            wasteType = entity.wasteType,
            syncStatus = entity.syncStatus,
            variantId = entity.variantId
        )
    }
}
@Query("UPDATE line_transactions SET syncStatus = :syncStatus WHERE journalId = :journalId AND itemId = :itemId")
    suspend fun updateSyncStatus(journalId: String, itemId: String, syncStatus: Int)
    @Transaction
    suspend fun saveLineTransactionsWithTransaction(journalId: String, entities: List<LineTransactionEntity>) {
        // Delete existing entries for this journal
        deleteByJournal(journalId)
        // Insert new entities
        insertAll(entities)
    }

}