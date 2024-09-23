package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Sptrans

@Dao
interface SptransDao {
    @Insert
    suspend fun insert(spTrans: Sptrans)

    @Query("SELECT * FROM sptrans")
    suspend fun getAll(): List<Sptrans>

    @Query("SELECT * FROM sptrans WHERE id = :id")
    suspend fun getById(id: Int): Sptrans?

    @Query("DELETE FROM sptrans")
    suspend fun deleteAll()
}
