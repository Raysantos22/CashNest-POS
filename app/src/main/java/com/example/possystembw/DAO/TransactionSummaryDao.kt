package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.TransactionSummary
import kotlinx.coroutines.flow.Flow
import java.util.Date

//@Dao
//interface TransactionSummaryDao {
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(transactionSummary: TransactionSummary)
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(transactionSummaries: List<TransactionSummary>)
//    @Update
//    suspend fun update(transactionSummary: TransactionSummary)
//    @Delete
//    suspend fun delete(transactionSummary: TransactionSummary)
//    @Query("SELECT * FROM transaction_summary WHERE transaction_id = :transactionId")
//    suspend fun getTransactionSummaryById(transactionId: String): TransactionSummary?
//    @Query("SELECT * FROM transaction_summary")
//    fun getAllTransactionSummaries(): Flow<List<TransactionSummary>>
//    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
//    fun getTransactionSummariesByDateRange(startDate: Date, endDate: Date): Flow<List<TransactionSummary>>
//    @Query("SELECT * FROM transaction_summary WHERE store = :store")
//    fun getTransactionSummariesByStore(store: String): Flow<List<TransactionSummary>>
//    @Query("SELECT * FROM transaction_summary WHERE staff = :staff")
//    fun getTransactionSummariesByStaff(staff: String): Flow<List<TransactionSummary>>
//    @Query("SELECT SUM(netamount) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
//    suspend fun getTotalNetAmountByDateRange(startDate: Date, endDate: Date): Double?
//    @Query("SELECT SUM(grossamount) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
//    suspend fun getTotalGrossAmountByDateRange(startDate: Date, endDate: Date): Double?
//    @Query("SELECT COUNT(*) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
//    suspend fun getTransactionCountByDateRange(startDate: Date, endDate: Date): Int
//    @Query("DELETE FROM transaction_summary")
//    suspend fun deleteAllTransactionSummaries()
//    @Query("DELETE FROM transaction_summary WHERE createddate < :date")
//    suspend fun deleteTransactionSummariesOlderThan(date: Date)
//    @Query("UPDATE transaction_summary SET receiptid = :returnReceiptId WHERE transaction_id = :transactionId")
//    suspend fun updateReturnReceiptId(transactionId: String, returnReceiptId: String)
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertTransactionSummary(transactionSummary: TransactionSummary): Long
//}
@Dao
interface TransactionSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transactionSummary: TransactionSummary)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactionSummaries: List<TransactionSummary>)

    @Update
    suspend fun update(transactionSummary: TransactionSummary)

    @Delete
    suspend fun delete(transactionSummary: TransactionSummary)

    @Query("SELECT * FROM transaction_summary WHERE transaction_id = :transactionId")
    suspend fun getTransactionSummaryById(transactionId: String): TransactionSummary?

    @Query("SELECT * FROM transaction_summary")
    fun getAllTransactionSummaries(): Flow<List<TransactionSummary>>



    // FIXED: Use string date range comparison
    @Query("SELECT * FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
    fun getTransactionSummariesByDateRange(startDate: String, endDate: String): Flow<List<TransactionSummary>>

    @Query("SELECT * FROM transaction_summary WHERE store = :store")
    fun getTransactionSummariesByStore(store: String): Flow<List<TransactionSummary>>

    @Query("SELECT * FROM transaction_summary WHERE staff = :staff")
    fun getTransactionSummariesByStaff(staff: String): Flow<List<TransactionSummary>>

    // FIXED: Use string date range comparison
    @Query("SELECT SUM(netamount) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
    suspend fun getTotalNetAmountByDateRange(startDate: String, endDate: String): Double?

    // FIXED: Use string date range comparison
    @Query("SELECT SUM(grossamount) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
    suspend fun getTotalGrossAmountByDateRange(startDate: String, endDate: String): Double?

    // FIXED: Use string date range comparison
    @Query("SELECT COUNT(*) FROM transaction_summary WHERE createddate BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: String, endDate: String): Int

    @Query("DELETE FROM transaction_summary")
    suspend fun deleteAllTransactionSummaries()

    // FIXED: Use string date comparison
    @Query("DELETE FROM transaction_summary WHERE createddate < :dateString")
    suspend fun deleteTransactionSummariesOlderThan(dateString: String)

    @Query("UPDATE transaction_summary SET receiptid = :returnReceiptId WHERE transaction_id = :transactionId")
    suspend fun updateReturnReceiptId(transactionId: String, returnReceiptId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionSummary(transactionSummary: TransactionSummary): Long
}