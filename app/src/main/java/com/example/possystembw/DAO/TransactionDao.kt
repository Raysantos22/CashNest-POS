package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>
    @Query("SELECT * FROM transaction_summary WHERE transaction_id LIKE :query OR staff LIKE :query OR receiptId LIKE :query")
    suspend fun searchTransactions(query: String): List<TransactionSummary>
    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionById(transactionId: String): TransactionRecord?

    @Query("SELECT * FROM transaction_summary WHERE store = :storeId ORDER BY createdDate DESC")
    suspend fun getTransactionsByStore(storeId: String): List<TransactionSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionRecord)
    @Query("DELETE FROM transactions WHERE transaction_id = :transactionId")
    suspend fun deleteTransactionsByTransactionId(transactionId: String)
    @Query("DELETE FROM transactions WHERE window_number = :windowNumber AND partial_payment_amount > 0")
    suspend fun deletePartialPaymentTransactions(windowNumber: Int)
    // Add these new methods
    @Query("SELECT * FROM transaction_summary")
    suspend fun getAllTransactionSummaries(): List<TransactionSummary>
    @Query("SELECT * FROM transactions WHERE transaction_Id = :transactionId")
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionRecord>
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTransactionSummary(transactionSummary: TransactionSummary): Long

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionItems(transactionId: String): List<TransactionRecord>

    @Query("UPDATE transaction_summary SET refundreceiptid = :returnReceiptId WHERE transaction_id = :transactionId")
    suspend fun updateReturnReceiptId(transactionId: String, returnReceiptId: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionRecords(records: List<TransactionRecord>)
    @Update
    suspend fun updateTransactionSummary(transactionSummary: TransactionSummary)
    @Update
    suspend fun updateTransactionRecords(records: List<TransactionRecord>)
    @Query("SELECT * FROM transaction_summary WHERE createdDate >= :timestamp")
    suspend fun getAllTransactionsSince(timestamp: Long): List<TransactionSummary>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransactionRecord(transactionRecord: TransactionRecord): Long

    @Query("SELECT * FROM transactions WHERE transaction_id = :transactionId")
    suspend fun getTransactionRecordsByTransactionId(transactionId: String): List<TransactionRecord>

    @Query("SELECT * FROM transaction_summary WHERE transaction_id = :transactionId")
    suspend fun getTransactionSummary(transactionId: String): TransactionSummary?

    @Query("SELECT * FROM transaction_summary WHERE syncStatus = 0")
    suspend fun getUnsyncedTransactions(): List<TransactionSummary>

    @Query("UPDATE transaction_summary SET syncStatus = :status WHERE transaction_id = :transactionId")
    suspend fun updateSyncStatus(transactionId: String, status: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionSummary(transaction: TransactionSummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionSummaries(summaries: List<TransactionSummary>)

    @Query("UPDATE transaction_summary SET refundreceiptid = :refundReceiptId WHERE transaction_id = :transactionId")
    suspend fun updateRefundReceiptId(transactionId: String, refundReceiptId: String)

    @Query("UPDATE transaction_summary SET syncStatus = :status WHERE transaction_id = :transactionId")
    suspend fun updateTransactionStatus(transactionId: String, status: Int)

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionRecords(): List<TransactionRecord>

//    @Query("DELETE FROM transactions")
//    suspend fun deleteAllTransactions()
//
//    @Query("DELETE FROM transaction_summary")
//    suspend fun deleteAllTransactionSummaries()


//    @Query("SELECT * FROM transaction_summary WHERE syncStatus = 0")
//    suspend fun getUnsyncedTransactionSummaries(): List<TransactionSummary>
//
//    // Get all unsynced transaction records for a specific transaction
//    @Query("SELECT * FROM transactions WHERE syncstatusrecord = 0 AND transaction_id = :transactionId")
//    suspend fun getUnsyncedTransactionRecords(transactionId: String): List<TransactionRecord>

    // Update sync status for transaction summary
    @Query("UPDATE transaction_summary SET syncStatus = :status WHERE transaction_id = :transactionId")
    suspend fun updateTransactionSummarySync(transactionId: String, status: Boolean)

    // Update sync status for transaction records
    @Query("UPDATE transactions SET syncstatusrecord = :status WHERE transaction_id = :transactionId")
    suspend fun updateTransactionRecordsSync(transactionId: String, status: Boolean)


    @Query("SELECT * FROM transaction_summary WHERE syncStatus = 0")
    suspend fun getUnsyncedTransactionSummaries(): List<TransactionSummary>

    // Get only unsynced transaction records for a specific transaction
    @Query("SELECT * FROM transactions WHERE syncstatusrecord = 0 AND transaction_id = :transactionId")
    suspend fun getUnsyncedTransactionRecords(transactionId: String): List<TransactionRecord>

    // Update sync status for transaction summary
    @Query("UPDATE transaction_summary SET syncStatus = 1 WHERE transaction_id = :transactionId")
    suspend fun markTransactionSummaryAsSynced(transactionId: String)

    // Update sync status for transaction records
    @Query("UPDATE transactions SET syncstatusrecord = 1 WHERE transaction_id = :transactionId")
    suspend fun markTransactionRecordsAsSynced(transactionId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM transaction_summary WHERE transaction_id = :transactionId AND syncStatus = 1)")
    suspend fun isTransactionSynced(transactionId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE transaction_id = :transactionId AND syncstatusrecord = 1)")
    suspend fun areTransactionRecordsSynced(transactionId: String): Boolean

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transaction_summary")
    suspend fun deleteAllTransactionSummaries()

    @Query("SELECT * FROM transaction_summary WHERE zReportId IS NULL")
    suspend fun getAllUnprocessedTransactions(): List<TransactionSummary>

    // Mark transactions as processed in Z-Read
    @Query("UPDATE transaction_summary SET zReportId = :zReportId WHERE zReportId IS NULL")
    suspend fun markTransactionsAsProcessed(zReportId: String)

    // Optional: Get all transactions for a specific Z-Report
    @Query("SELECT * FROM transaction_summary WHERE zReportId = :zReportId")
    suspend fun getTransactionsByZReport(zReportId: String): List<TransactionSummary>

}


