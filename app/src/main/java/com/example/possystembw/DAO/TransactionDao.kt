package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.possystembw.database.Product
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
    @Query("SELECT * FROM transaction_summary LIMIT 30")
 fun getRecentTransactions(): List<TransactionSummary>

    @Query("SELECT * FROM transaction_summary WHERE store = :storeId ORDER BY createddate DESC")
    suspend fun getTransactionsByStore(storeId: String): List<TransactionSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionRecord>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionRecord)

    @Query("DELETE FROM transactions WHERE transaction_id = :transactionId")
    suspend fun deleteTransactionsByTransactionId(transactionId: String)

    @Query("DELETE FROM transactions WHERE window_number = :windowNumber AND partial_payment_amount > 0")
    suspend fun deletePartialPaymentTransactions(windowNumber: Int)

    @Query("SELECT * FROM transaction_summary")
    suspend fun getAllTransactionSummaries(): List<TransactionSummary>

    @Query("SELECT * FROM transactions WHERE transaction_Id = :transactionId")
    suspend fun getItemsForTransaction(transactionId: String): List<TransactionRecord>

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

    // FIXED: Use string comparison for dates
    @Query("SELECT * FROM transaction_summary WHERE createddate >= :dateString")
    suspend fun getAllTransactionsSince(dateString: String): List<TransactionSummary>

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

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transactions WHERE DATE(createddate) = :currentDate")
    suspend fun getAllTransactionsForDate(currentDate: String): List<TransactionRecord>

    @Query("UPDATE transaction_summary SET syncStatus = :status WHERE transaction_id = :transactionId")
    suspend fun updateTransactionSummarySync(transactionId: String, status: Boolean)

    @Query("UPDATE transactions SET syncstatusrecord = :status WHERE transaction_id = :transactionId")
    suspend fun updateTransactionRecordsSync(transactionId: String, status: Boolean)

    @Query("SELECT * FROM transaction_summary WHERE syncStatus = 0")
    suspend fun getUnsyncedTransactionSummaries(): List<TransactionSummary>

    @Query("SELECT * FROM transactions WHERE syncstatusrecord = 0 AND transaction_id = :transactionId")
    suspend fun getUnsyncedTransactionRecords(transactionId: String): List<TransactionRecord>

    @Query("UPDATE transaction_summary SET syncStatus = 1 WHERE transaction_id = :transactionId")
    suspend fun markTransactionSummaryAsSynced(transactionId: String)

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

    @Delete
    suspend fun deleteTransactionSummary(transactionSummary: TransactionSummary)

    @Delete
    suspend fun deleteTransactionRecord(transactionRecord: TransactionRecord)

    @Query("DELETE FROM transaction_summary WHERE transaction_id = :id")
    suspend fun deleteTransactionSummaryById(id: String)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionRecordById(id: String)

    @Query("DELETE FROM transaction_summary WHERE DATE(createdDate) < :cutoffDate")
    suspend fun deleteTransactionSummariesOlderThan(cutoffDate: String)

    @Query("DELETE FROM transactions WHERE DATE(createdDate) < :cutoffDate")
    suspend fun deleteTransactionRecordsOlderThan(cutoffDate: String)

    @Query("DELETE FROM transaction_summary")
    suspend fun clearAllTransactionSummaries()

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactionRecords()


    @Query("SELECT * FROM transaction_summary WHERE zReportId IS NULL")
    suspend fun getAllUnprocessedTransactions(): List<TransactionSummary>

    @Query("UPDATE transaction_summary SET zReportId = :zReportId WHERE zReportId IS NULL")
    suspend fun markTransactionsAsProcessed(zReportId: String)

    @Query("SELECT * FROM transaction_summary WHERE zReportId = :zReportId")
    suspend fun getTransactionsByZReport(zReportId: String): List<TransactionSummary>

    @Query("UPDATE transaction_summary SET zReportId = :zReportId WHERE store = :storeId AND (zReportId IS NULL OR zReportId = '')")
    suspend fun updateTransactionsZReportId(storeId: String, zReportId: String)

    @Query("SELECT * FROM transaction_summary WHERE refundReceiptId = :originalTransactionId")
    suspend fun findReturnTransactionsForOriginal(originalTransactionId: String): List<TransactionSummary>

//     FIXED: Use string date range comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate ORDER BY createddate DESC")
     fun getTransactionsByDateRange(startDate: String, endDate: String): List<TransactionSummary>
    @Query("SELECT * FROM transaction_summary WHERE DATE(createddate) = :dateString ORDER BY createddate DESC")
     fun getTransactionsByDate(dateString: String): List<TransactionSummary>

    @Query("SELECT * FROM transaction_summary WHERE createddate LIKE :datePattern ORDER BY createddate DESC")
     fun getTransactionsByDatePattern(datePattern: String): List<TransactionSummary>
    // FIXED: Use string date comparison
    @Query("SELECT * FROM transactions WHERE createddate >= :dateString")
    suspend fun getTransactionRecordsSince(dateString: String): List<TransactionRecord>

    @Query("SELECT * FROM products WHERE itemid = :itemId LIMIT 1")
    suspend fun getProductByItemId(itemId: String): Product?

    @Query("SELECT * FROM transaction_summary WHERE syncStatus = 1")
    suspend fun getSyncedTransactionSummaries(): List<TransactionSummary>

    @Query("SELECT * FROM transaction_summary WHERE zreportid = :zReportId ORDER BY createddate ASC")
    suspend fun getTransactionsByZReportId(zReportId: String): List<TransactionSummary>

    @Query("SELECT DISTINCT zreportid FROM transaction_summary WHERE zreportid IS NOT NULL AND zreportid != ''")
    suspend fun getAllZReadIds(): List<String?>

    // FIXED: Use string date comparison
    @Query("SELECT COUNT(*) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate AND zReportId IS NOT NULL AND zReportId != ''")
    suspend fun countTransactionsWithZReportByDateRange(startDate: String, endDate: String): Int

    @Query("SELECT MAX(CAST(zReportId AS INTEGER)) FROM transaction_summary WHERE zReportId IS NOT NULL AND zReportId != '' AND zReportId GLOB '[0-9]*'")
    suspend fun getMaxZReportIdFromTransactions(): Int?

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate AND (zReportId IS NULL OR zReportId = '') AND transactionStatus = 1 ORDER BY createddate ASC")
    suspend fun getTransactionsWithoutZReportByDateRange(startDate: String, endDate: String): List<TransactionSummary>

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate AND zReportId IS NOT NULL AND zReportId != '' AND transactionStatus = 1 ORDER BY createddate ASC")
    suspend fun getTransactionsWithZReportByDateRange(startDate: String, endDate: String): List<TransactionSummary>

    @Query("SELECT COUNT(*) FROM transaction_summary WHERE (zReportId IS NULL OR zReportId = '') AND transactionStatus = 1")
    suspend fun countTransactionsWithoutZReport(): Int

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transaction_summary WHERE DATE(createddate) < DATE('now') AND (zReportId IS NULL OR zReportId = '') AND transactionStatus = 1 ORDER BY createddate ASC")
    suspend fun getOldTransactionsWithoutZReport(): List<TransactionSummary>

    @Transaction
    suspend fun updateTransactionsWithZReportId(transactionIds: List<String>, zReportId: String) {
        transactionIds.forEach { transactionId ->
            updateTransactionZReportId(transactionId, zReportId)
        }
    }

    @Query("UPDATE transaction_summary SET zReportId = :zReportId WHERE transaction_id = :transactionId")
    suspend fun updateTransactionZReportId(transactionId: String, zReportId: String)

    @Query("SELECT MAX(CAST(zreportid AS INTEGER)) FROM transaction_summary WHERE zreportid IS NOT NULL AND zreportid != '' AND zreportid GLOB '[0-9]*'")
    suspend fun getMaxZReadId(): Int?

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate AND (zReportId IS NULL OR zReportId = '') AND transactionStatus = 1")
    suspend fun getUnprocessedTransactionsByDateRange(startDate: String, endDate: String): List<TransactionSummary>

    // FIXED: Use string date comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate AND zReportId IS NOT NULL AND zReportId != ''")
    suspend fun getProcessedTransactionsByDateRange(startDate: String, endDate: String): List<TransactionSummary>

    @Query("SELECT MAX(CAST(receiptId AS INTEGER)) FROM transaction_summary WHERE receiptId GLOB '[0-9]*'")
    suspend fun getMaxORNumber(): Int?

    @Query("UPDATE transaction_summary SET receiptId = :orNumber WHERE transaction_id = :transactionId")
    suspend fun updateReceiptId(transactionId: String, orNumber: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceTransactionSummaries(transactions: List<TransactionSummary>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceAll(records: List<TransactionRecord>)

    @Update
    suspend fun updateTransactionRecord(record: TransactionRecord)



    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactionRecords()



}