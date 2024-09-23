package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventtables

@Dao
interface InventtablesDao {
    @Insert
    suspend fun insert(inventtables: Inventtables)

    @Query("SELECT * FROM inventtables")
    suspend fun getAll(): List<Inventtables>

    @Query("SELECT * FROM inventtables WHERE itemGroupId = :id")
    suspend fun getById(id: Int): Inventtables?

    @Query("DELETE FROM inventtables")
    suspend fun deleteAll()
}
