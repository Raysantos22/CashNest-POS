package com.example.possystembw.data

import com.example.possystembw.DAO.NumberSequenceDao
import com.example.possystembw.database.NumberSequence
import java.util.Date

class NumberSequenceRepository(private val numberSequenceDao: NumberSequenceDao) {
    suspend fun initializeSequence(
        type: String,
        storeId: String,
        startValue: Long = 1,
        paddingLength: Int = 9
    ) {
        val existingSequence = numberSequenceDao.getSequence(type, storeId)
        if (existingSequence == null) {
            val sequence = NumberSequence(
                sequenceType = type,
                storeId = storeId,
                currentValue = startValue,
                paddingLength = paddingLength,
                storeKey = generateStoreKey(storeId, startValue.toString().padStart(paddingLength, '0'))
            )
            numberSequenceDao.insert(sequence)
        }
    }

    private fun generateStoreKey(storeId: String, transactionId: String): String {
        return "$transactionId-$storeId"
    }

    suspend fun getNextNumber(type: String, storeId: String): String {
        val sequence = numberSequenceDao.getSequence(type, storeId) ?: run {
            initializeSequence(type, storeId)
            numberSequenceDao.getSequence(type, storeId)
        } ?: throw IllegalStateException("Could not initialize sequence: $type for store: $storeId")

        val currentValue = sequence.currentValue
        val formattedNumber = currentValue.toString().padStart(sequence.paddingLength, '0')

        // Update store key before incrementing
        val newStoreKey = generateStoreKey(storeId, formattedNumber)
        numberSequenceDao.updateStoreKey(type, storeId, newStoreKey)

        // Increment the sequence
        numberSequenceDao.incrementSequence(type, storeId, sequence.increment)

        return formattedNumber
    }

    suspend fun getCurrentStoreKey(type: String, storeId: String): String {
        val sequence = numberSequenceDao.getSequence(type, storeId)
            ?: throw IllegalStateException("No sequence found for type: $type and store: $storeId")
        return sequence.storeKey
    }


    suspend fun resetSequence(type: String, storeId: String, newValue: Long = 1) {
        numberSequenceDao.resetSequence(type, storeId, newValue, Date())
    }

    suspend fun getCurrentValue(type: String, storeId: String): Long? {
        return numberSequenceDao.getCurrentValue(type, storeId)
    }
}