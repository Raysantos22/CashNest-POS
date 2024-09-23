package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventjournaltransrepos

@Dao
interface InventjournaltransreposDao {
    @Insert
    suspend fun insert(inventJournalTransRepos: Inventjournaltransrepos)

    @Query("SELECT * FROM inventjournaltransrepos")
    suspend fun getAll(): List<Inventjournaltransrepos>

    @Query("SELECT * FROM inventjournaltransrepos WHERE JOURNALID = :journalId")
    suspend fun getByJournalId(journalId: String): List<Inventjournaltransrepos>

    @Query("DELETE FROM inventjournaltransrepos")
    suspend fun deleteAll()
}
