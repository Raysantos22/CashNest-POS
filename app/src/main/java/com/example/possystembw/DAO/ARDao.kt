package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.AR
import kotlinx.coroutines.flow.Flow

@Dao
interface ARDao {
    @Query("SELECT * FROM ar_table")
    fun getAllARTypes(): Flow<List<AR>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertARTypes(arTypes: List<AR>)

    @Query("DELETE FROM ar_table")
    suspend fun deleteAllARTypes()

    @Query("SELECT * FROM ar_table")
    suspend fun getAllARTypesImmediate(): List<AR>
}

