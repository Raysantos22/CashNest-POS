package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.NumberSequence
import com.example.possystembw.database.NumberSequenceEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface NumberSequenceDao {

    @Query("SELECT * FROM number_sequences WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun getSequence(type: String, storeId: String): NumberSequence?


@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insert(sequence: NumberSequence)

    @Query("UPDATE number_sequences SET currentValue = currentValue + :increment WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun incrementSequence(type: String, storeId: String, increment: Int)

    @Query("UPDATE number_sequences SET currentValue = :newValue, lastResetDate = :resetDate WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun resetSequence(type: String, storeId: String, newValue: Long, resetDate: Date)

    @Query("SELECT currentValue FROM number_sequences WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun getCurrentValue(type: String, storeId: String): Long?


    @Query("UPDATE number_sequences SET storeKey = :storeKey WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun updateStoreKey(type: String, storeId: String, storeKey: String)
    @Update
    suspend fun update(sequence: NumberSequence)

    @Query("UPDATE number_sequences SET currentValue = :newValue WHERE sequenceType = :type AND storeId = :storeId")
    suspend fun updateCurrentValue(type: String, storeId: String, newValue: Long)
}
