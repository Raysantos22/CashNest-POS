//package com.example.possystembw.ui.ViewModel
//
//import android.util.Log
//import com.example.possystembw.DAO.TransactionSyncRequest
//import com.example.possystembw.data.TransactionRepository
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//
//class TransactionSyncService(private val repository: TransactionRepository) {
//    private var syncJob: Job? = null
//
//    fun startSyncService(scope: CoroutineScope) {
//        stopSyncService() // Stop any existing sync job
//
//        syncJob = scope.launch {
//            while (isActive) {
//                try {
//                    val unsyncedSummaries = repository.transactionDao.getUnsyncedTransactionSummaries()
//                    if (unsyncedSummaries.isNotEmpty()) {
//                        Log.d("SyncService", "Found ${unsyncedSummaries.size} unsynced transactions")
//
//                        unsyncedSummaries.forEach { summary ->
//                            try {
//                                val unsyncedRecords = repository.transactionDao.getUnsyncedTransactionRecords(summary.transactionId)
//
//                                // Create sync request
//                                val request = TransactionSyncRequest(
//                                    transactionSummary = repository.createTransactionSummaryRequest(summary),
//                                    transactionRecords = unsyncedRecords.map { record ->
//                                        repository.createTransactionRecordRequest(record, summary)
//                                    }
//                                )
//
//                                // Attempt sync
//                                val response = repository.api.syncTransaction(request)
//
//                                if (response.isSuccessful && response.body() != null) {
//                                    repository.transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
//                                    repository.transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
//                                    Log.d("SyncService", "Successfully synced transaction ${summary.transactionId}")
//                                } else {
//                                    Log.e("SyncService", "Failed to sync transaction ${summary.transactionId}: ${response.message()}")
//                                }
//                            } catch (e: Exception) {
//                                Log.e("SyncService", "Error syncing transaction ${summary.transactionId}", e)
//                            }
//                        }
//                    }
//
//                    // Wait before next sync attempt
//                    delay(30000) // Check every 30 seconds
//                } catch (e: Exception) {
//                    Log.e("SyncService", "Error in sync service", e)
//                    delay(30000) // Wait before retrying on error
//                }
//            }
//        }
//    }
//
//    fun stopSyncService() {
//        syncJob?.cancel()
//        syncJob = null
//    }
//}


//working
//package com.example.possystembw.ui.ViewModel
//
//import android.util.Log
//import com.example.possystembw.DAO.TransactionSyncRequest
//import com.example.possystembw.data.TransactionRepository
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Job
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.isActive
//import kotlinx.coroutines.launch
//import java.util.concurrent.ConcurrentHashMap
//
//class TransactionSyncService(private val repository: TransactionRepository) {
//    private var syncJob: Job? = null
//
//    // Reduced cooldown for testing - 30 seconds instead of 5 minutes
//    private val SYNC_COOLDOWN_PERIOD = 30 * 1000L // 30 seconds cooldown
//
//    // Tracking recently synced transactions to prevent immediate resyncs
//    private val recentlySyncedTransactions = ConcurrentHashMap<String, Long>()
//
//    fun startSyncService(scope: CoroutineScope) {
//        stopSyncService() // Stop any existing sync job
//
//        syncJob = scope.launch {
//            while (isActive) {
//                try {
//                    // Clean up old entries from recently synced transactions
//                    cleanUpRecentlySyncedTransactions()
//
//                    val unsyncedSummaries = repository.transactionDao.getUnsyncedTransactionSummaries()
//                    if (unsyncedSummaries.isNotEmpty()) {
//                        Log.d("SyncService", "Found ${unsyncedSummaries.size} unsynced transactions")
//
//                        unsyncedSummaries.forEach { summary ->
//                            // Log the detailed sync attempt information
//                            Log.d("SyncService", "Attempting to sync transaction ${summary.transactionId}")
//                            Log.d("SyncService", "Recently synced check: ${isRecentlySynced(summary.transactionId)}")
//
//                            // Skip if recently synced
//                            if (isRecentlySynced(summary.transactionId)) {
//                                Log.e("SyncService", "DUPLICATE PREVENTION: Skipping recently synced transaction ${summary.transactionId}")
//                                return@forEach
//                            }
//
//                            try {
//                                val unsyncedRecords = repository.transactionDao.getUnsyncedTransactionRecords(summary.transactionId)
//
//                                // Create sync request with additional safeguards
//                                val request = TransactionSyncRequest(
//                                    transactionSummary = repository.createTransactionSummaryRequest(summary),
//                                    transactionRecords = unsyncedRecords.map { record ->
//                                        repository.createTransactionRecordRequest(record, summary)
//                                    }
//                                )
//
//                                // Attempt sync with duplicate prevention
//                                val response = repository.api.syncTransaction(request)
//
//                                if (response.isSuccessful && response.body() != null) {
//                                    // Mark as synced and add to recently synced list
//                                    repository.transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
//                                    repository.transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
//
//                                    // Add to recently synced to prevent immediate resync
//                                    addToRecentlySynced(summary.transactionId)
//
//                                    Log.d("SyncService", "Successfully synced transaction ${summary.transactionId}")
//                                } else {
//                                    Log.e("SyncService", "Failed to sync transaction ${summary.transactionId}: ${response.message()}")
//                                }
//                            } catch (e: Exception) {
//                                Log.e("SyncService", "Error syncing transaction ${summary.transactionId}", e)
//                            }
//                        }
//                    }
//
//                    // Wait before next sync attempt
//                    delay(30000) // Check every 30 seconds
//                } catch (e: Exception) {
//                    Log.e("SyncService", "Error in sync service", e)
//                    delay(30000) // Wait before retrying on error
//                }
//            }
//        }
//    }
//
//    // Add a transaction to recently synced list
//    private fun addToRecentlySynced(transactionId: String) {
//        val currentTime = System.currentTimeMillis()
//        recentlySyncedTransactions[transactionId] = currentTime
//        Log.d("SyncService", "Added to recently synced: $transactionId at $currentTime")
//    }
//
//    // Check if a transaction was recently synced
//    private fun isRecentlySynced(transactionId: String): Boolean {
//        val lastSyncTime = recentlySyncedTransactions[transactionId]
//        val isRecent = lastSyncTime != null &&
//                System.currentTimeMillis() - lastSyncTime < SYNC_COOLDOWN_PERIOD
//
//        Log.d("SyncService", "Checking if $transactionId is recently synced:")
//        Log.d("SyncService", "Last sync time: $lastSyncTime")
//        Log.d("SyncService", "Current time: ${System.currentTimeMillis()}")
//        Log.d("SyncService", "Time difference: ${System.currentTimeMillis() - (lastSyncTime ?: 0)}")
//        Log.d("SyncService", "Cooldown period: $SYNC_COOLDOWN_PERIOD")
//        Log.d("SyncService", "Is recently synced: $isRecent")
//
//        return isRecent
//    }
//
//    // Clean up old entries from recently synced transactions
//    private fun cleanUpRecentlySyncedTransactions() {
//        val currentTime = System.currentTimeMillis()
//        val beforeSize = recentlySyncedTransactions.size
//        recentlySyncedTransactions.entries.removeIf {
//            val shouldRemove = currentTime - it.value > SYNC_COOLDOWN_PERIOD
//            if (shouldRemove) {
//                Log.d("SyncService", "Removing old entry: ${it.key}")
//            }
//            shouldRemove
//        }
//        val afterSize = recentlySyncedTransactions.size
//        Log.d("SyncService", "Cleaned up recently synced transactions. Before: $beforeSize, After: $afterSize")
//    }
//
//    fun stopSyncService() {
//        syncJob?.cancel()
//        syncJob = null
//    }
//}
package com.example.possystembw.ui.ViewModel

import android.util.Log
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class TransactionSyncService(private val repository: TransactionRepository) {
    private var syncJob: Job? = null

    // Reduced cooldown for testing - 30 seconds instead of 5 minutes
    private val SYNC_COOLDOWN_PERIOD = 30 * 1000L // 30 seconds cooldown

    // Tracking recently synced transactions to prevent immediate resyncs
    private val recentlySyncedTransactions = ConcurrentHashMap<String, Long>()

    fun startSyncService(scope: CoroutineScope) {
        stopSyncService() // Stop any existing sync job

        syncJob = scope.launch {
            while (isActive) {
                try {
                    // Clean up old entries from recently synced transactions
                    cleanUpRecentlySyncedTransactions()

                    val unsyncedSummaries = repository.transactionDao.getUnsyncedTransactionSummaries()
                    if (unsyncedSummaries.isNotEmpty()) {
                        Log.d("SyncService", "Found ${unsyncedSummaries.size} unsynced transactions")

                        unsyncedSummaries.forEach { summary ->
                            // Log the detailed sync attempt information
                            Log.d("SyncService", "Attempting to sync transaction ${summary.transactionId}")
                            Log.d("SyncService", "Recently synced check: ${isRecentlySynced(summary.transactionId)}")

                            // Skip if recently synced
                            if (isRecentlySynced(summary.transactionId)) {
                                Log.e("SyncService", "DUPLICATE PREVENTION: Skipping recently synced transaction ${summary.transactionId}")
                                return@forEach
                            }

                            try {
                                val unsyncedRecords = repository.transactionDao.getUnsyncedTransactionRecords(summary.transactionId)

                                // Create sync request with additional safeguards
                                val request = TransactionSyncRequest(
                                    transactionSummary = repository.createTransactionSummaryRequest(summary),
                                    transactionRecords = unsyncedRecords.map { record ->
                                        repository.createTransactionRecordRequest(record, summary)
                                    }
                                )

                                // Attempt sync with duplicate prevention
                                val response = repository.api.syncTransaction(request)

                                if (response.isSuccessful && response.body() != null) {
                                    // Mark as synced and add to recently synced list
                                    repository.transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                                    repository.transactionDao.markTransactionRecordsAsSynced(summary.transactionId)

                                    // Add to recently synced to prevent immediate resync
                                    addToRecentlySynced(summary.transactionId)

                                    Log.d("SyncService", "Successfully synced transaction ${summary.transactionId}")
                                } else {
                                    Log.e("SyncService", "Failed to sync transaction ${summary.transactionId}: ${response.message()}")
                                }
                            } catch (e: Exception) {
                                Log.e("SyncService", "Error syncing transaction ${summary.transactionId}", e)
                            }
                        }
                    }

                    // Wait before next sync attempt
                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e("SyncService", "Error in sync service", e)
                    delay(30000) // Wait before retrying on error
                }
            }
        }
    }

    // Add a transaction to recently synced list
    private fun addToRecentlySynced(transactionId: String) {
        val currentTime = System.currentTimeMillis()
        recentlySyncedTransactions[transactionId] = currentTime
        Log.d("SyncService", "Added to recently synced: $transactionId at $currentTime")
    }

    // Check if a transaction was recently synced
    private fun isRecentlySynced(transactionId: String): Boolean {
        val lastSyncTime = recentlySyncedTransactions[transactionId]
        val isRecent = lastSyncTime != null &&
                System.currentTimeMillis() - lastSyncTime < SYNC_COOLDOWN_PERIOD

        Log.d("SyncService", "Checking if $transactionId is recently synced:")
        Log.d("SyncService", "Last sync time: $lastSyncTime")
        Log.d("SyncService", "Current time: ${System.currentTimeMillis()}")
        Log.d("SyncService", "Time difference: ${System.currentTimeMillis() - (lastSyncTime ?: 0)}")
        Log.d("SyncService", "Cooldown period: $SYNC_COOLDOWN_PERIOD")
        Log.d("SyncService", "Is recently synced: $isRecent")

        return isRecent
    }

    // Clean up old entries from recently synced transactions
    private fun cleanUpRecentlySyncedTransactions() {
        val currentTime = System.currentTimeMillis()
        val beforeSize = recentlySyncedTransactions.size
        recentlySyncedTransactions.entries.removeIf {
            val shouldRemove = currentTime - it.value > SYNC_COOLDOWN_PERIOD
            if (shouldRemove) {
                Log.d("SyncService", "Removing old entry: ${it.key}")
            }
            shouldRemove
        }
        val afterSize = recentlySyncedTransactions.size
        Log.d("SyncService", "Cleaned up recently synced transactions. Before: $beforeSize, After: $afterSize")
    }

    fun stopSyncService() {
        syncJob?.cancel()
        syncJob = null
    }
}