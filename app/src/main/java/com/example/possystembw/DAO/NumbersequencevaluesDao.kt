package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Numbersequencevalues

@Dao
interface NumbersequencevaluesDao {
    @Insert
    suspend fun insert(numberSequenceValue: Numbersequencevalues)

    @Query("SELECT * FROM numbersequencevalues")
    suspend fun getAll(): List<Numbersequencevalues>

    @Query("SELECT * FROM numbersequencevalues WHERE id = :id")
    suspend fun getById(id: Int): Numbersequencevalues?

    @Query("DELETE FROM numbersequencevalues")
    suspend fun deleteAll()
}
