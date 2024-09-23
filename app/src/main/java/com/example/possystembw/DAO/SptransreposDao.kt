package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Sptransrepos

@Dao
interface SptransreposDao {
    @Insert
    suspend fun insert(spTransRepos: Sptransrepos)

    @Query("SELECT * FROM sptransrepos")
    suspend fun getAll(): List<Sptransrepos>

    @Query("SELECT * FROM sptransrepos WHERE id = :id")
    suspend fun getById(id: Int): Sptransrepos?

    @Query("DELETE FROM sptransrepos")
    suspend fun deleteAll()
}
