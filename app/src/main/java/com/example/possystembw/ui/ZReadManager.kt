package com.example.possystembw.ui

import android.util.Log
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.ZReadDao
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.ZRead
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZReadManager(
    private val transactionDao: TransactionDao,
    private val zReadDao: ZReadDao,
    private val transactionRepository: TransactionRepository
) {

    /**
     * Get the next Z-Report ID by checking both ZRead table and TransactionSummary table
     */
    suspend fun getNextZReportId(): String {
        return withContext(Dispatchers.IO) {
            // Get max from both ZRead table and TransactionSummary table
            val maxFromZRead = zReadDao.getMaxZReportId() ?: 0
            val maxFromTransactions = transactionDao.getMaxZReportIdFromTransactions() ?: 0

            // Take the higher value and increment
            val nextNumber = maxOf(maxFromZRead, maxFromTransactions) + 1

            Log.d("ZReadManager", "Max from ZRead: $maxFromZRead, Max from Transactions: $maxFromTransactions, Next: $nextNumber")

            String.format("%09d", nextNumber)
        }
    }

    /**
     * Auto-assign Z-Report IDs to old transactions without them (BIR compliance)
     */
    suspend fun autoAssignZReportIds(): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                val oldTransactions = transactionDao.getOldTransactionsWithoutZReport()

                if (oldTransactions.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val assignedZReportIds = mutableListOf<String>()

                // Group transactions by date
                val transactionsByDate = oldTransactions
                    .filter { it.createdDate != null }
                    .groupBy {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.createdDate!!)
                    }


                for ((date, transactions) in transactionsByDate) {
                    // Check if Z-Read already exists for this date
                    val existingZRead = zReadDao.getZReadByDate(date)

                    if (existingZRead == null) {
                        // Generate new Z-Report ID for this date
                        val zReportId = getNextZReportId()

                        // Update transactions with Z-Report ID
                        val transactionIds = transactions.map { it.transactionId }
                        transactionDao.updateTransactionsWithZReportId(transactionIds, zReportId)

                        // Create Z-Read record
                        val zRead = ZRead(
                            zReportId = zReportId,
                            date = date,
                            time = "23:59:59", // End of day for auto-generated
                            totalTransactions = transactions.size,
                            totalAmount = transactions.sumOf { it.netAmount }
                        )
                        zReadDao.insert(zRead)

                        assignedZReportIds.add(zReportId)

                        Log.d("ZReadManager", "Auto-assigned Z-Report ID $zReportId to ${transactions.size} transactions for date $date")

                        // Try to sync with server
                        val storeId = SessionManager.getCurrentUser()?.storeid
                        if (storeId != null) {
                            try {
                                transactionRepository.updateTransactionsZReport(storeId, zReportId)
                            } catch (e: Exception) {
                                Log.w("ZReadManager", "Failed to sync auto Z-Report to server: ${e.message}")
                            }
                        }
                    }
                }

                Result.success(assignedZReportIds)

            } catch (e: Exception) {
                Log.e("ZReadManager", "Error in auto-assigning Z-Report IDs", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Check if there are transactions that need Z-Report IDs
     */
    suspend fun hasTransactionsNeedingZReport(): Boolean {
        return withContext(Dispatchers.IO) {
            val count = transactionDao.countTransactionsWithoutZReport()
            count > 0
        }
    }
}