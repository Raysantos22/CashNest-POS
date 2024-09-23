package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventjournaltrans

@Dao
interface InventjournaltransDao {
    @Insert
    suspend fun insert(inventJournalTrans: Inventjournaltrans)

    @Query("SELECT * FROM inventjournaltrans")
    suspend fun getAll(): List<Inventjournaltrans>

    @Query("SELECT * FROM inventjournaltrans WHERE JOURNALID = :journalId")
    suspend fun getByJournalId(journalId: String): List<Inventjournaltrans>

    @Query("DELETE FROM inventjournaltrans")
    suspend fun deleteAll()
}
