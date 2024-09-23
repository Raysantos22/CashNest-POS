package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventjournaltables

@Dao
interface InventjournaltablesDao {
    @Insert
    suspend fun insert(inventJournalTable: Inventjournaltables)

    @Query("SELECT * FROM Inventjournaltables")
    suspend fun getAll(): List<Inventjournaltables>

    @Query("SELECT * FROM inventjournaltables WHERE JOURNALID = :journalId")
    suspend fun getByJournalId(journalId: String): Inventjournaltables?

    @Query("DELETE FROM Inventjournaltables")
    suspend fun deleteAll()
}
