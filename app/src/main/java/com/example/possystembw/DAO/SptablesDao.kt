package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Sptables

@Dao
interface SptablesDao {
    @Insert
    suspend fun insert(spTable: Sptables)

    @Query("SELECT * FROM sptables")
    suspend fun getAll(): List<Sptables>

    @Query("SELECT * FROM sptables WHERE journalId = :id")
    suspend fun getById(id: Int): Sptables?

    @Query("DELETE FROM sptables")
    suspend fun deleteAll()
}
