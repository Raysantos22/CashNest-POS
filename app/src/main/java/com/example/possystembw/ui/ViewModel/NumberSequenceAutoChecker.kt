package com.example.possystembw.ui.ViewModel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.RetrofitClient
import com.example.possystembw.ui.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NumberSequenceAutoChecker(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) {
    private var numberSequenceRemoteRepository: NumberSequenceRemoteRepository? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "NumberSequenceChecker"
        private var lastCheckTime = 0L
        private const val CHECK_INTERVAL = 5000L // 5 seconds between checks for testing
    }

    /**
     * Initialize the checker (call this once per activity)
     */
    fun initialize() {
        if (isInitialized) return

        try {
            val database = AppDatabase.getDatabase(context)
            val numberSequenceApi = RetrofitClient.numberSequenceApi
            val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
            val transactionDao = database.transactionDao()

            numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
                numberSequenceApi,
                numberSequenceRemoteDao,
                transactionDao
            )

            isInitialized = true
            Log.d(TAG, "✅ NumberSequenceAutoChecker initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize NumberSequenceAutoChecker", e)
        }
    }

    /**
     * Check and update sequence if needed (call this in activity lifecycle methods)
     */
    fun checkAndUpdateSequence(showToast: Boolean = false) {
        if (!isInitialized) {
            Log.w(TAG, "Checker not initialized, initializing now...")
            initialize()
        }

        // Prevent too frequent checks
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCheckTime < CHECK_INTERVAL) {
            Log.d(TAG, "Skipping check, too soon since last check")
            return
        }
        lastCheckTime = currentTime

        val repository = numberSequenceRemoteRepository ?: return

        lifecycleOwner.lifecycleScope.launch {
            try {
                val currentUser = SessionManager.getCurrentUser()
                val storeId = currentUser?.storeid

                if (storeId.isNullOrEmpty()) {
                    Log.w(TAG, "No store ID available")
                    return@launch
                }

                Log.d(TAG, "Checking number sequence for store: $storeId")

                // Get current sequence and last transaction
                val lastTransactionNumber = getLastTransactionNumber(repository, storeId)
                val currentSequence = getCurrentSequence(repository, storeId)

                Log.d(TAG, "Last transaction: $lastTransactionNumber, Current sequence: $currentSequence")

                // Check if sequence needs updating
                val expectedNextSequence = lastTransactionNumber + 1

                if (currentSequence < expectedNextSequence) {
                    Log.d(TAG, "Sequence needs updating: $currentSequence -> $expectedNextSequence")

                    val result = repository.forceInitializeAndUpdate(storeId)

                    result.onSuccess {
                        Log.d(TAG, "✅ Sequence updated successfully")
                        if (showToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Transaction numbering synchronized", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "❌ Failed to update sequence", error)
                        if (showToast) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Sequence update failed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.d(TAG, "✅ Sequence is up to date")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during sequence check", e)
            }
        }
    }

    /**
     * Force update sequence (for manual triggers)
     */
    fun forceUpdateSequence(showToast: Boolean = true) {
        if (!isInitialized) initialize()

        val repository = numberSequenceRemoteRepository ?: return

        lifecycleOwner.lifecycleScope.launch {
            try {
                val currentUser = SessionManager.getCurrentUser()
                val storeId = currentUser?.storeid

                if (storeId.isNullOrEmpty()) {
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No store ID available", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }

                Log.d(TAG, "Force updating sequence for store: $storeId")

                val result = repository.forceInitializeAndUpdate(storeId)

                result.onSuccess {
                    Log.d(TAG, "✅ Force update successful")
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Sequence updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.onFailure { error ->
                    Log.e(TAG, "❌ Force update failed", error)
                    if (showToast) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Update failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during force update", e)
            }
        }
    }

    private suspend fun getLastTransactionNumber(repository: NumberSequenceRemoteRepository, storeId: String): Int {
        return try {
            val database = AppDatabase.getDatabase(context)
            val transactions = database.transactionDao().getTransactionsByStore(storeId)

            var maxNumber = 0
            transactions.forEach { transaction ->
                try {
                    val numericPart = transaction.transactionId.filter { it.isDigit() }
                    if (numericPart.isNotEmpty()) {
                        val number = numericPart.toInt()
                        if (number > maxNumber) {
                            maxNumber = number
                        }
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
            maxNumber
        } catch (e: Exception) {
            0
        }
    }

    private suspend fun getCurrentSequence(repository: NumberSequenceRemoteRepository, storeId: String): Int {
        return try {
            val database = AppDatabase.getDatabase(context)
            val sequence = database.numberSequenceRemoteDao().getNumberSequenceByStoreId(storeId)
            sequence?.nextRec ?: 1
        } catch (e: Exception) {
            1
        }
    }
}

// Extension function to make it even easier to use
fun LifecycleOwner.setupNumberSequenceChecker(context: Context): NumberSequenceAutoChecker {
    return NumberSequenceAutoChecker(context, this).apply {
        initialize()
    }
}