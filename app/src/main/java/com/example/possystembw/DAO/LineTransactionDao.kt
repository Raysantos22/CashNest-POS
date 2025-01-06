package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.possystembw.database.LineTransactionEntity
import kotlinx.coroutines.flow.Flow

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