package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.NumberSequenceApi
import com.example.possystembw.DAO.NumberSequenceRemoteDao
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.RetrofitClient.numberSequenceApi
import com.example.possystembw.database.NumberSequenceRemoteEntity
import com.example.possystembw.ui.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NumberSequenceRemoteRepository(
    private val numberSequenceApi: NumberSequenceApi,
    private val numberSequenceRemoteDao: NumberSequenceRemoteDao
) {
    private var currentSequence: Int = 1

    suspend fun getNextTransactionNumber(storeId: String): String {
        val numberSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)
            ?: throw IllegalStateException("No number sequence found for store $storeId")

        // Get and increment the sequence
        val nextNumber = numberSequence.nextRec
        val formattedNumber = String.format("%09d", nextNumber)
        val transactionNumber = "$formattedNumber"

        // Update the sequence
        val updatedEntity = numberSequence.copy(
            nextRec = nextNumber + 1
        )
        numberSequenceRemoteDao.updateNumberSequence(updatedEntity)
        currentSequence = nextNumber + 1

        Log.d(
            "NumberSequence",
            "Generated transaction number: $transactionNumber, Next sequence: ${nextNumber + 1}"
        )
        return transactionNumber
    }

    suspend fun fetchAndUpdateNumberSequence(storeId: String): Result<NumberSequenceValue> {
        return try {
            // First check if we have a local sequence
            val existingSequence = numberSequenceRemoteDao.getNumberSequenceByStoreId(storeId)

            // If we have a local sequence, use its nextRec value
            if (existingSequence != null) {
                val localNextRec = existingSequence.nextRec
                val updatedValue = NumberSequenceValue(
                    numberSequence = existingSequence.numberSequence,
                    nextRec = localNextRec.toLong(),
                    cartNextRec = existingSequence.cartNextRec.toLong(),
                    bundleNextRec = existingSequence.bundleNextRec.toLong(),
                    discountNextRec = existingSequence.discountNextRec.toLong(),
                    storeId = existingSequence.storeId,
                    createdAt = existingSequence.createdAt,
                    updatedAt = existingSequence.updatedAt,
                    wasteRec = existingSequence.wasteRec.toLong(),
                    toNextRec = existingSequence.toNextRec.toLong(),
                    stockNextRec = existingSequence.stockNextRec.toLong()
                )
                return Result.success(updatedValue)
            }

            // If no local sequence, fetch from API and initialize
            val response = numberSequenceApi.getNumberSequence(storeId)

            if (response.isSuccessful) {
                val responseBody = response.body()
                val numberSequenceValues = responseBody?.get("nubersequencevalues")

                if (numberSequenceValues.isNullOrEmpty()) {
                    Result.failure(Exception("No number sequence found for store $storeId"))
                } else {
                    val numberSequenceValue = numberSequenceValues.first()

                    // Initialize with sequence 1
                    val entity = NumberSequenceRemoteEntity(
                        numberSequence = numberSequenceValue.numberSequence,
                        storeId = numberSequenceValue.storeId,
                        nextRec = numberSequenceValue.cartNextRec.toInt(),  // Start with 1
                        cartNextRec = numberSequenceValue.cartNextRec.toInt(),
                        bundleNextRec = numberSequenceValue.bundleNextRec.toInt(),
                        discountNextRec = numberSequenceValue.discountNextRec.toInt(),
                        wasteRec = numberSequenceValue.wasteRec.toInt(),
                        toNextRec = numberSequenceValue.toNextRec.toInt(),
                        stockNextRec = numberSequenceValue.stockNextRec.toInt(),
                        createdAt = numberSequenceValue.createdAt,
                        updatedAt = numberSequenceValue.updatedAt
                    )

                    numberSequenceRemoteDao.insertNumberSequence(entity)
                    currentSequence = 1
                    Result.success(numberSequenceValue)
                }
            } else {
                Result.failure(Exception("Failed to fetch number sequence: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
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
}

