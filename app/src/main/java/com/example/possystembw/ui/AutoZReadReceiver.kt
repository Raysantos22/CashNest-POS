package com.example.possystembw.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.DAO.ZReadDao
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.ZRead
import com.example.possystembw.database.TransactionSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AutoZReadReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        when (intent.action) {
            "AUTO_ZREAD_CHECK" -> {
                performAutomaticZReadCheck(context)
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Reschedule the alarm after device reboot
                rescheduleAlarmAfterReboot(context)
            }
        }
    }

    private fun performAutomaticZReadCheck(context: Context) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val transactionDao = database.transactionDao()
                val zReadDao = database.zReadDao()

                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = philippinesTimeZone
                }.format(yesterday)

                val existingZRead = zReadDao.getZReadByDate(yesterdayString)

                if (existingZRead != null) {
                    Log.d("AutoZRead", "Z-Read already exists for $yesterdayString")
                    return@launch
                }

                val yesterdayStart = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val yesterdayEnd = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time

                // FIXED: Convert Date to String for DAO call
                val transactions = transactionDao.getTransactionsByDateRange(
                    formatDateToString(yesterdayStart),
                    formatDateToString(yesterdayEnd)
                ).filter { it.transactionStatus == 1 }

                if (transactions.isEmpty()) {
                    Log.d("AutoZRead", "No transactions found for $yesterdayString")
                    return@launch
                }

                val transactionsWithoutZRead = transactions.filter { it.zReportId.isNullOrEmpty() }

                if (transactionsWithoutZRead.isEmpty()) {
                    Log.d("AutoZRead", "All transactions already have Z-Read for $yesterdayString")
                    return@launch
                }

                generateAutomaticZReadSilent(context, transactionsWithoutZRead, yesterdayString, transactionDao, zReadDao)

            } catch (e: Exception) {
                Log.e("AutoZRead", "Error in automatic Z-Read check", e)
            }
        }
    }
    fun formatDateToString(date: Date): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.format(date)
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    private suspend fun generateAutomaticZReadSilent(
        context: Context,
        transactions: List<TransactionSummary>,
        dateString: String,
        transactionDao: TransactionDao,
        zReadDao: ZReadDao
    ) {
        try {
            // Generate Z-Report ID
            val zReportId = generateZReportId(context, transactionDao)

            // Update transactions with Z-Report ID
            transactions.forEach { transaction ->
                val updatedTransaction = transaction.copy(zReportId = zReportId)
                transactionDao.updateTransactionSummary(updatedTransaction)
            }

            // Save Z-Read record
            val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                timeZone = philippinesTimeZone
            }.format(Date())

            val zReadRecord = ZRead(
                zReportId = zReportId,
                date = dateString,
                time = currentTime,
                totalTransactions = transactions.size,
                totalAmount = transactions.sumOf { it.netAmount }
            )
            zReadDao.insert(zReadRecord)

            Log.i("AutoZRead", "Automatic Z-Read #$zReportId generated silently for $dateString with ${transactions.size} transactions")

        } catch (e: Exception) {
            Log.e("AutoZRead", "Error generating automatic Z-Read", e)
        }
    }

    private suspend fun generateZReportId(context: Context, transactionDao: TransactionDao): String {
        return withContext(Dispatchers.IO) {
            try {
                // Get the maximum Z-Read ID from the database
                val maxZReadId = transactionDao.getMaxZReadId() ?: 0

                // Also check SharedPreferences for consistency
                val sharedPreferences = context.getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                val sharedPrefNumber = sharedPreferences.getInt("lastSequentialNumber", 0)

                // Use the higher number to ensure we don't have conflicts
                val nextNumber = maxOf(maxZReadId, sharedPrefNumber) + 1

                // Update SharedPreferences to keep it in sync
                with(sharedPreferences.edit()) {
                    putInt("lastSequentialNumber", nextNumber)
                    apply()
                }

                Log.d("ZRead", "Generated Z-Report ID: $nextNumber (DB Max: $maxZReadId, SharedPref: $sharedPrefNumber)")

                String.format("%09d", nextNumber)
            } catch (e: Exception) {
                Log.e("ZRead", "Error generating Z-Report ID", e)
                // Fallback to SharedPreferences only
                val sharedPreferences = context.getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                val currentNumber = sharedPreferences.getInt("lastSequentialNumber", 0)
                val nextNumber = currentNumber + 1

                with(sharedPreferences.edit()) {
                    putInt("lastSequentialNumber", nextNumber)
                    apply()
                }

                String.format("%09d", nextNumber)
            }
        }
    }

    private fun rescheduleAlarmAfterReboot(context: Context) {
        // You can implement this to reschedule the alarm after device reboot
        // For now, just log that we received the boot event
        Log.d("AutoZRead", "Device rebooted, alarm rescheduling may be needed")
    }
}