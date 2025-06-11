package com.example.possystembw.ui

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.possystembw.R
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AutoZReadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

        override suspend fun doWork(): Result {
        return try {
            val sharedPrefs = applicationContext.getSharedPreferences("ZReadPrefs", Context.MODE_PRIVATE)
            val lastZReadDate = sharedPrefs.getString("lastZReadDate", "")
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (lastZReadDate != today) {
                // Check if there are any transactions for today
                val database = AppDatabase.getDatabase(applicationContext)
                val transactionDao = database.transactionDao()

                val startOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.time

                val endOfDay = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }.time

                val transactions = transactionDao.getTransactionsByDateRange(startOfDay, endOfDay)

                if (transactions.isNotEmpty()) {
                    // Perform automatic Z-Read
                    performAutomaticZRead(transactions)

                    // Update last Z-Read date
                    sharedPrefs.edit().putString("lastZReadDate", today).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("AutoZReadWorker", "Error performing automatic Z-Read", e)
            Result.failure()
        }
    }

    private suspend fun performAutomaticZRead(transactions: List<TransactionSummary>) {
        try {
            val bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()
            val zReportId = "AUTO_Z${SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())}"

            // Generate Z-Read report
            val reportContent = bluetoothPrinterHelper.buildReadReport(transactions, true, zReportId, null)

            // Print if printer is connected
            if (bluetoothPrinterHelper.isConnected()) {
                bluetoothPrinterHelper.printGenericReceipt(reportContent)
            }

            // Save Z-Read record
            // Implementation to save Z-Read record

            // Send notification
            sendZReadNotification()

        } catch (e: Exception) {
            Log.e("AutoZReadWorker", "Error in automatic Z-Read", e)
        }
    }

    private fun sendZReadNotification() {
        val notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)

        val notification = NotificationCompat.Builder(applicationContext, "zread_channel")
            .setContentTitle("Automatic Z-Read Completed")
            .setContentText("Daily Z-Read has been processed at midnight")
            .setSmallIcon(R.drawable.ic_grid)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager?.notify(1001, notification)
    }
}

// Data classes
data class ZReadRecord(
    val zReadId: String,
    val dateGenerated: Date,
    val totalTransactions: Int,
    val totalGross: Double,
    val totalNet: Double,
    val totalDiscount: Double
)