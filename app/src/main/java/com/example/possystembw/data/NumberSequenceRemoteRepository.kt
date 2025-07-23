package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.NumberSequenceApi
import com.example.possystembw.DAO.NumberSequenceRemoteDao
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.RetrofitClient.numberSequenceApi
import com.example.possystembw.database.NumberSequenceRemoteEntity
import com.example.possystembw.ui.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NumberSequenceRemoteRepository(
    private val numberSequenceApi: NumberSequenceApi,
    private val numberSequenceRemoteDao: NumberSequenceRemoteDao,
    private val transactionDao: TransactionDao  // Add TransactionDao to check existing transactions
) {
    private var currentSequence: Int = 1

    /**
     * Initialize or sync the number sequence by checking existing transactions
     */
    suspend fun initializeNumberSequence(storeId: String): Result<Boolean> {
        return try {
            Log.d("NumberSequence", "Initializing number sequence for store: $storeId")

            // Get the highest transaction ID from existing transactions
            val lastTransactionNumber = getLastTransactionNumber(storeId)
            Log.d("NumberSequence", "Last transaction number found: $lastTransactionNumber")

            // Get or create number sequence entity
            var numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)

            if (numberSequence == null) {
                // Create new sequence starting from last transaction + 1
                val nextSequence = lastTransactionNumber + 1
                Log.d("NumberSequence", "Creating new sequence starting from: $nextSequence")

                numberSequence = NumberSequenceRemoteEntity(
                    storeId = storeId,
                    numberSequence = "TXN",
                    nextRec = nextSequence,
                    cartNextRec = 1,
                    bundleNextRec = 1,
                    discountNextRec = 1,
                    wasteRec = 1,
                    toNextRec = 1,
                    stockNextRec = 1,
                    createdAt = getCurrentDateString(),
                    updatedAt = getCurrentDateString()
                )
                numberSequenceRemoteDao.insertNumberSequence(numberSequence)
            } else {
                // Update existing sequence if it's behind the actual last transaction
                if (numberSequence.nextRec <= lastTransactionNumber) {
                    val newNextRec = lastTransactionNumber + 1
                    Log.d("NumberSequence", "Updating sequence from ${numberSequence.nextRec} to $newNextRec")

                    val updatedEntity = numberSequence.copy(
                        nextRec = newNextRec,
                        updatedAt = getCurrentDateString()
                    )
                    numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
                }
            }

            currentSequence = numberSequence.nextRec
            Result.success(true)
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error initializing number sequence", e)
            Result.failure(e)
        }
    }

    /**
     * Get the last (highest) transaction number from existing transactions
     */
    private suspend fun getLastTransactionNumber(storeId: String): Int {
        return try {
            Log.d("NumberSequence", "Getting last transaction number for store: $storeId")

            // Get ALL transactions for the store
            val transactions = transactionDao.getTransactionsByStore(storeId)
            Log.d("NumberSequence", "Found ${transactions.size} total transactions")

            if (transactions.isEmpty()) {
                Log.d("NumberSequence", "No existing transactions found")
                return 0
            }

            var maxTransactionNumber = 0
            var transactionsProcessed = 0

            transactions.forEach { transaction ->
                try {
                    // Extract numeric part from transaction ID
                    val numericPart = transaction.transactionId.filter { it.isDigit() }
                    if (numericPart.isNotEmpty()) {
                        val transactionNumber = numericPart.toInt()
                        if (transactionNumber > maxTransactionNumber) {
                            maxTransactionNumber = transactionNumber
                            Log.d("NumberSequence", "New max found: $transactionNumber (from ${transaction.transactionId})")
                        }
                        transactionsProcessed++
                    }
                } catch (e: Exception) {
                    Log.w("NumberSequence", "Could not parse transaction ID: ${transaction.transactionId}")
                }
            }

            Log.d("NumberSequence", "Processed $transactionsProcessed transactions")
            Log.d("NumberSequence", "Highest transaction number: $maxTransactionNumber")

            maxTransactionNumber
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error getting last transaction number", e)
            0
        }
    }

    /**
     * Get the last (highest) receipt ID from existing transactions
     */
    private suspend fun getLastReceiptNumber(storeId: String): Int {
        return try {
            val transactions = transactionDao.getTransactionsByStore(storeId)

            if (transactions.isEmpty()) {
                return 0
            }

            var maxReceiptNumber = 0

            transactions.forEach { transaction ->
                try {
                    // Extract numeric part from receipt ID
                    val numericPart = transaction.receiptId.filter { it.isDigit() }
                    if (numericPart.isNotEmpty()) {
                        val receiptNumber = numericPart.toInt()
                        if (receiptNumber > maxReceiptNumber) {
                            maxReceiptNumber = receiptNumber
                        }
                    }
                } catch (e: Exception) {
                    Log.w("NumberSequence", "Could not parse receipt ID: ${transaction.receiptId}")
                }
            }

            Log.d("NumberSequence", "Highest receipt number found: $maxReceiptNumber")
            maxReceiptNumber
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error getting last receipt number", e)
            0
        }
    }

    suspend fun getNextTransactionNumber(storeId: String): String {
        // Ensure sequence is initialized and up-to-date
        initializeNumberSequence(storeId)

        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            ?: throw IllegalStateException("No number sequence found for store $storeId")

        // Get and increment the sequence
        val nextNumber = numberSequence.nextRec
        val formattedNumber = String.format("%09d", nextNumber)
        val transactionNumber = formattedNumber

        // Update the sequence
        val updatedEntity = numberSequence.copy(
            nextRec = nextNumber + 1,
            updatedAt = getCurrentDateString()
        )
        numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
        currentSequence = nextNumber + 1

        Log.d(
            "NumberSequence",
            "Generated transaction number: $transactionNumber, Next sequence: ${nextNumber + 1}"
        )
        return transactionNumber
    }

    /**
     * Sync number sequence with server after checking local transactions
     */
    suspend fun syncNumberSequenceWithServer(storeId: String): Result<Boolean> {
        return try {
            // First, ensure our local sequence is up-to-date
            initializeNumberSequence(storeId)

            val localSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
                ?: throw IllegalStateException("No local sequence found")

            // Send updated sequence to server
            val response = numberSequenceApi.updateNumberSequence(storeId, localSequence.nextRec)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("NumberSequence", "Successfully synced sequence with server: ${localSequence.nextRec}")
                Result.success(true)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("NumberSequence", "Failed to sync with server: $errorMessage")
                Result.failure(Exception("Failed to sync with server: $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error syncing with server", e)
            Result.failure(e)
        }
    }

    suspend fun updateCartSequenceOnTransaction(storeId: String): Result<Boolean> {
        return try {
            // Get current sequence from local database
            val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
                ?: throw IllegalStateException("No number sequence found for store $storeId")

            // Increment cart sequence locally
            val newCartNextRec = numberSequence.cartNextRec + 1

            Log.d("CartSequence", "Transaction completed - updating cart sequence from ${numberSequence.cartNextRec} to $newCartNextRec")

            // Update local database first
            val updatedEntity = numberSequence.copy(
                cartNextRec = newCartNextRec,
                updatedAt = getCurrentDateString()
            )
            numberSequenceRemoteDao.updateNumberSequence(updatedEntity)

            // Send current cartNextRec to your existing API
            val response = numberSequenceApi.updateNumberSequence(storeId, newCartNextRec)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("CartSequence", "✅ Successfully sent cart sequence $newCartNextRec to server after transaction")
                Result.success(true)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("CartSequence", "❌ Failed to send cart sequence to server: $errorMessage")
                // Keep local update even if server fails
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e("CartSequence", "Error updating cart sequence after transaction", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentCartSequence(storeId: String): Int {
        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            ?: throw IllegalStateException("No number sequence found for store $storeId")

        return numberSequence.cartNextRec
    }
    suspend fun forceInitializeAndUpdate(storeId: String): Result<Boolean> {
        return try {
            Log.d("NumberSequence", "=== FORCE INITIALIZE AND UPDATE ===")
            Log.d("NumberSequence", "Store ID: $storeId")

            // Step 1: Get the highest transaction number from existing transactions
            val lastTransactionNumber = getLastTransactionNumber(storeId)
            Log.d("NumberSequence", "Last transaction number found: $lastTransactionNumber")

            // Step 2: Calculate what the next sequence should be
            val nextSequence = lastTransactionNumber + 1
            Log.d("NumberSequence", "Next sequence should be: $nextSequence")

            // Step 3: Get current sequence from database
            val currentSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            Log.d("NumberSequence", "Current sequence in DB: ${currentSequence?.nextRec}")

            // Step 4: Force update the sequence regardless of current value
            if (currentSequence != null) {
                // Update existing sequence
                val updatedEntity = currentSequence.copy(
                    nextRec = nextSequence,
                    cartNextRec = nextSequence,
                    updatedAt = getCurrentDateString()
                )
                numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
                Log.d("NumberSequence", "Updated existing sequence to: $nextSequence")
            } else {
                // Create new sequence
                val newEntity = NumberSequenceRemoteEntity(
                    storeId = storeId,
                    numberSequence = "TXN",
                    nextRec = nextSequence,
                    cartNextRec =nextSequence,
                    bundleNextRec = nextSequence,
                    discountNextRec = nextSequence,
                    wasteRec = nextSequence,
                    toNextRec = 1,
                    stockNextRec = 1,
                    createdAt = getCurrentDateString(),
                    updatedAt = getCurrentDateString()
                )
                numberSequenceRemoteDao.insertNumberSequence(newEntity)
                Log.d("NumberSequence", "Created new sequence starting at: $nextSequence")
            }

            // Step 5: Sync with server
            Log.d("NumberSequence", "Syncing sequence $nextSequence with server...")
            val syncResult = numberSequenceApi.updateNumberSequence(storeId, nextSequence)

            if (syncResult.isSuccessful && syncResult.body()?.success == true) {
                Log.d("NumberSequence", "✅ Successfully synced sequence with server")
            } else {
                Log.w("NumberSequence", "⚠️ Server sync failed but local update succeeded")
            }

            Log.d("NumberSequence", "=== FORCE INITIALIZE COMPLETE ===")
            Result.success(true)

        } catch (e: Exception) {
            Log.e("NumberSequence", "❌ Error in force initialize", e)
            Result.failure(e)
        }
    }

    suspend fun updateCartSequence(storeId: String): Result<Boolean> {
        return try {
            // Get current sequence from local database
            val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
                ?: throw IllegalStateException("No number sequence found for store $storeId")

            // Increment the cart sequence
            val newCartNextRec = numberSequence.cartNextRec + 1

            Log.d("CartSequence", "Updating cart sequence for store: $storeId from ${numberSequence.cartNextRec} to $newCartNextRec")

            // Update local database first
            val updatedEntity = numberSequence.copy(
                cartNextRec = newCartNextRec,
                updatedAt = getCurrentDateString()
            )
            numberSequenceRemoteDao.updateNumberSequence(updatedEntity)

            // Update remote server using existing API endpoint
            val response = numberSequenceApi.updateNumberSequence(storeId, newCartNextRec)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("CartSequence", "Successfully updated cart sequence for store: $storeId to $newCartNextRec")
                Result.success(true)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("CartSequence", "Failed to update cart sequence: $errorMessage")
                // Note: We keep the local update even if remote fails
                Result.success(true) // Still return success since local update worked
            }
        } catch (e: Exception) {
            Log.e("CartSequence", "Error updating cart sequence for store: $storeId", e)
            Result.failure(e)
        }
    }

    /**
     * Gets the next cart sequence number without incrementing
     */
    suspend fun getNextCartSequenceNumber(storeId: String): String {
        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            ?: throw IllegalStateException("No number sequence found for store $storeId")

        val nextCartNumber = numberSequence.cartNextRec
        return String.format("%09d", nextCartNumber)
    }

    suspend fun fetchAndUpdateNumberSequence(storeId: String): Result<NumberSequenceValue> {
        return try {
            Log.d("NumberSequence", "=== FETCH AND UPDATE NUMBER SEQUENCE ===")
            Log.d("NumberSequence", "Store ID: $storeId")

            // Step 1: Get the highest transaction number from existing transactions
            val lastTransactionNumber = getLastTransactionNumber(storeId)
            Log.d("NumberSequence", "Last transaction number found: $lastTransactionNumber")

            // Step 2: Calculate what the next sequence should be
            val nextSequence = lastTransactionNumber + 1
            Log.d("NumberSequence", "Next sequence should be: $nextSequence")

            // Step 3: Check if we have a local sequence
            val existingSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)

            if (existingSequence != null) {
                Log.d("NumberSequence", "Found existing local sequence: ${existingSequence.nextRec}")

                // Update existing sequence to match the calculated next sequence
                val correctedNextRec = maxOf(existingSequence.nextRec, nextSequence)
                Log.d("NumberSequence", "Using corrected sequence: $correctedNextRec")

                if (correctedNextRec != existingSequence.nextRec) {
                    Log.d("NumberSequence", "Updating sequence from ${existingSequence.nextRec} to $correctedNextRec")
                    val updatedEntity = existingSequence.copy(
                        nextRec = correctedNextRec,
                        updatedAt = getCurrentDateString()
                    )
                    numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
                }

                val updatedValue = NumberSequenceValue(
                    numberSequence = existingSequence.numberSequence,
                    nextRec = correctedNextRec.toLong(),
                    cartNextRec = existingSequence.cartNextRec.toLong(),
                    bundleNextRec = existingSequence.bundleNextRec.toLong(),
                    discountNextRec = existingSequence.discountNextRec.toLong(),
                    storeId = existingSequence.storeId,
                    createdAt = existingSequence.createdAt,
                    updatedAt = getCurrentDateString(),
                    wasteRec = existingSequence.wasteRec.toLong(),
                    toNextRec = existingSequence.toNextRec.toLong(),
                    stockNextRec = existingSequence.stockNextRec.toLong()
                )

                // Sync the corrected sequence with server
                if (correctedNextRec != existingSequence.nextRec) {
                    try {
                        val syncResult = numberSequenceApi.updateNumberSequence(storeId, correctedNextRec)
                        if (syncResult.isSuccessful && syncResult.body()?.success == true) {
                            Log.d("NumberSequence", "✅ Successfully synced corrected sequence with server")
                        } else {
                            Log.w("NumberSequence", "⚠️ Server sync failed but local update succeeded")
                        }
                    } catch (e: Exception) {
                        Log.w("NumberSequence", "Server sync error: ${e.message}")
                    }
                }

                return Result.success(updatedValue)
            }

            // Step 4: No local sequence exists, fetch from API and correct it
            Log.d("NumberSequence", "No local sequence found, fetching from API...")
            val response = numberSequenceApi.getNumberSequence(storeId)

            if (response.isSuccessful) {
                val responseBody = response.body()
                val numberSequenceValues = responseBody?.get("nubersequencevalues")

                if (numberSequenceValues.isNullOrEmpty()) {
                    Log.w("NumberSequence", "No number sequence found from API, creating new one")

                    // Create completely new sequence
                    val entity = NumberSequenceRemoteEntity(
                        numberSequence = "TXN",
                        storeId = storeId,
                        nextRec = nextSequence,
                        cartNextRec = 1,
                        bundleNextRec = 1,
                        discountNextRec = 1,
                        wasteRec = 1,
                        toNextRec = 1,
                        stockNextRec = 1,
                        createdAt = getCurrentDateString(),
                        updatedAt = getCurrentDateString()
                    )

                    numberSequenceRemoteDao.insertNumberSequence(entity)
                    currentSequence = nextSequence

                    val newValue = NumberSequenceValue(
                        numberSequence = "TXN",
                        nextRec = nextSequence.toLong(),
                        cartNextRec = 1L,
                        bundleNextRec = 1L,
                        discountNextRec = 1L,
                        storeId = storeId,
                        createdAt = getCurrentDateString(),
                        updatedAt = getCurrentDateString(),
                        wasteRec = 1L,
                        toNextRec = 1L,
                        stockNextRec = 1L
                    )

                    Result.success(newValue)
                } else {
                    val numberSequenceValue = numberSequenceValues.first()
                    Log.d("NumberSequence", "API returned sequence: ${numberSequenceValue.nextRec}")

                    // Use the higher value between API and calculated sequence
                    val correctedNextRec = maxOf(numberSequenceValue.nextRec.toInt(), nextSequence)
                    Log.d("NumberSequence", "Using corrected sequence: $correctedNextRec (API: ${numberSequenceValue.nextRec}, Calculated: $nextSequence)")

                    val entity = NumberSequenceRemoteEntity(
                        numberSequence = numberSequenceValue.numberSequence,
                        storeId = numberSequenceValue.storeId,
                        nextRec = correctedNextRec,
                        cartNextRec = numberSequenceValue.cartNextRec.toInt(),
                        bundleNextRec = numberSequenceValue.bundleNextRec.toInt(),
                        discountNextRec = numberSequenceValue.discountNextRec.toInt(),
                        wasteRec = numberSequenceValue.wasteRec.toInt(),
                        toNextRec = numberSequenceValue.toNextRec.toInt(),
                        stockNextRec = numberSequenceValue.stockNextRec.toInt(),
                        createdAt = numberSequenceValue.createdAt,
                        updatedAt = getCurrentDateString()
                    )

                    numberSequenceRemoteDao.insertNumberSequence(entity)
                    currentSequence = correctedNextRec

                    // If we corrected the sequence, sync it back to server
                    if (correctedNextRec != numberSequenceValue.nextRec.toInt()) {
                        Log.d("NumberSequence", "Syncing corrected sequence back to server: $correctedNextRec")
                        try {
                            val syncResult = numberSequenceApi.updateNumberSequence(storeId, correctedNextRec)
                            if (syncResult.isSuccessful && syncResult.body()?.success == true) {
                                Log.d("NumberSequence", "✅ Successfully synced corrected sequence with server")
                            } else {
                                Log.w("NumberSequence", "⚠️ Server sync failed but local update succeeded")
                            }
                        } catch (e: Exception) {
                            Log.w("NumberSequence", "Server sync error: ${e.message}")
                        }
                    }

                    // Return updated value with corrected sequence
                    val updatedValue = numberSequenceValue.copy(nextRec = correctedNextRec.toLong())
                    Result.success(updatedValue)
                }
            } else {
                Log.e("NumberSequence", "Failed to fetch from API: ${response.code()} - ${response.message()}")

                // Create local sequence as fallback
                val entity = NumberSequenceRemoteEntity(
                    numberSequence = "TXN",
                    storeId = storeId,
                    nextRec = nextSequence,
                    cartNextRec = nextSequence,
                    bundleNextRec = nextSequence,
                    discountNextRec = nextSequence,
                    wasteRec = 1,
                    toNextRec = 1,
                    stockNextRec = 1,
                    createdAt = getCurrentDateString(),
                    updatedAt = getCurrentDateString()
                )

                numberSequenceRemoteDao.insertNumberSequence(entity)
                currentSequence = nextSequence

                val fallbackValue = NumberSequenceValue(
                    numberSequence = "TXN",
                    nextRec = nextSequence.toLong(),
                    cartNextRec = nextSequence.toLong(),
                    bundleNextRec = nextSequence.toLong(),
                    discountNextRec = 1L,
                    storeId = storeId,
                    createdAt = getCurrentDateString(),
                    updatedAt = getCurrentDateString(),
                    wasteRec = 1L,
                    toNextRec = 1L,
                    stockNextRec = 1L
                )

                Result.success(fallbackValue)
            }
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error in fetchAndUpdateNumberSequence", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentStoreKey(storeId: String): String {
        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            ?: throw IllegalStateException("No number sequence found for store: $storeId")

        // Get current (not incremented) number
        val currentNumber = (numberSequence.nextRec - 1).coerceAtLeast(0)
        return String.format("%09d", currentNumber)
    }

    suspend fun updateRemoteNextRec(storeId: String, nextRec: Int): Result<Boolean> {
        return try {
            Log.d("NumberSequence", "Attempting to update sequence for store: $storeId with nextRec: $nextRec")

            // Call the API with path parameters
            val response = numberSequenceApi.updateNumberSequence(storeId, nextRec)

            if (response.isSuccessful && response.body()?.success == true) {
                Log.d("NumberSequence", "Successfully updated sequence for store: $storeId")

                // Also update local database
                val localSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
                if (localSequence != null) {
                    val updatedEntity = localSequence.copy(
                        nextRec = nextRec,
                        updatedAt = getCurrentDateString()
                    )
                    numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
                }

                Result.success(true)
            } else {
                val errorMessage = response.errorBody()?.string() ?: "Unknown error"
                Log.e("NumberSequence", "Failed to update sequence: $errorMessage")
                Result.failure(Exception("Failed to update sequence: ${response.code()} - $errorMessage"))
            }
        } catch (e: Exception) {
            Log.e("NumberSequence", "Error updating sequence", e)
            Result.failure(e)
        }
    }

    private fun getCurrentDateString(): String {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
            format.format(java.util.Date())
        } catch (e: Exception) {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
        }
    }
}