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
    @Query("SELECT * FROM number_sequences WHERE sequenceType = :type")
    suspend fun getSequence(type: String): NumberSequence?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sequence: NumberSequence)

    @Update
    suspend fun update(sequence: NumberSequence)

    @Query("UPDATE number_sequences SET currentValue = :value, lastResetDate = :resetDate WHERE sequenceType = :type")
    suspend fun resetSequence(type: String, value: Long, resetDate: Date)

    @Query("UPDATE number_sequences SET currentValue = currentValue + :increment WHERE sequenceType = :type")
    suspend fun incrementSequence(type: String, increment: Int = 1)

    @Query("SELECT * FROM number_sequences")
    suspend fun getAllSequences(): List<NumberSequence>
}
