package com.example.possystembw.data

import android.util.Log
import com.example.possystembw.DAO.NumberSequenceDao
import com.example.possystembw.DAO.NumberSequenceRequest
import com.example.possystembw.DAO.NumberSequenceService
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.database.NumberSequence

import com.example.possystembw.database.NumberSequenceEntity
import java.util.Date

class NumberSequenceRepository(private val numberSequenceDao: NumberSequenceDao) {
    suspend fun initializeSequence(
        type: String,
        startValue: Long = 1,
        prefix: String = "",
        suffix: String = "",
        paddingLength: Int = 9,
        increment: Int = 1
    ) {
        val sequence = NumberSequence(
            sequenceType = type,
            currentValue = startValue,
            prefix = prefix,
            suffix = suffix,
            paddingLength = paddingLength,
            increment = increment
        )
        numberSequenceDao.insert(sequence)
    }

    suspend fun getNextNumber(type: String): String {
        val sequence = numberSequenceDao.getSequence(type) ?: run {
            initializeSequence(type)
            numberSequenceDao.getSequence(type)
        } ?: throw IllegalStateException("Could not initialize sequence: $type")

        numberSequenceDao.incrementSequence(type, sequence.increment)

        return buildString {
            append(sequence.prefix)
            append(sequence.currentValue.toString().padStart(sequence.paddingLength, '0'))
            append(sequence.suffix)
        }
    }

    suspend fun resetSequence(type: String, newValue: Long = 1) {
        numberSequenceDao.resetSequence(type, newValue, Date())
    }

    suspend fun getCurrentValue(type: String): Long? {
        return numberSequenceDao.getSequence(type)?.currentValue
    }
}
