package com.example.possystembw.ui.ViewModel

import android.util.Log
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.data.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TransactionSyncService(private val repository: TransactionRepository) {
    private var syncJob: Job? = null
    
    fun startSyncService(scope: CoroutineScope) {
        stopSyncService() // Stop any existing sync job
        
        syncJob = scope.launch {
            while (isActive) {
                try {
                    val unsyncedSummaries = repository.transactionDao.getUnsyncedTransactionSummaries()
                    if (unsyncedSummaries.isNotEmpty()) {
                        Log.d("SyncService", "Found ${unsyncedSummaries.size} unsynced transactions")
                        
                        unsyncedSummaries.forEach { summary ->
                            try {
                                val unsyncedRecords = repository.transactionDao.getUnsyncedTransactionRecords(summary.transactionId)
                                
                                // Create sync request
                                val request = TransactionSyncRequest(
                                    transactionSummary = repository.createTransactionSummaryRequest(summary),
                                    transactionRecords = unsyncedRecords.map { record ->
                                        repository.createTransactionRecordRequest(record, summary)
                                    }
                                )

                                // Attempt sync
                                val response = repository.api.syncTransaction(request)
                                
                                if (response.isSuccessful && response.body() != null) {
                                    repository.transactionDao.markTransactionSummaryAsSynced(summary.transactionId)
                                    repository.transactionDao.markTransactionRecordsAsSynced(summary.transactionId)
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
    
    fun stopSyncService() {
        syncJob?.cancel()
        syncJob = null
    }
}