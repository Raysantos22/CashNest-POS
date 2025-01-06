package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.possystembw.database.NumberSequenceRemoteEntity

@Dao
interface NumberSequenceRemoteDao {
    @Query("SELECT * FROM number_sequence_remote WHERE storeId = :storeId")
    suspend fun getNumberSequenceByStoreId(storeId: String): NumberSequenceRemoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNumberSequence(numberSequenceValue: NumberSequenceRemoteEntity)

    @Update
    suspend fun updateNumberSequence(numberSequenceEntity: NumberSequenceRemoteEntity)


    @Query("DELETE FROM number_sequence_remote WHERE storeId = :storeId")
    suspend fun deleteNumberSequenceByStoreId(storeId: String)


}