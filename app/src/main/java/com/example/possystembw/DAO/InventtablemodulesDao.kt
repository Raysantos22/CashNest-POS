package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventtablemodules

@Dao
interface InventtablemodulesDao {
    @Insert
    suspend fun insert(inventTableModules: Inventtablemodules)

    @Query("SELECT * FROM inventtablemodules")
    suspend fun getAll(): List<Inventtablemodules>

    @Query("SELECT * FROM inventtablemodules WHERE id = :id")
    suspend fun getById(id: Int): Inventtablemodules?

    @Query("DELETE FROM inventtablemodules")
    suspend fun deleteAll()
}
