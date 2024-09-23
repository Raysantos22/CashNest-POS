package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Partycakes

@Dao
interface PartycakesDao {
    @Insert
    suspend fun insert(partyCakes: Partycakes)

    @Query("SELECT * FROM partycakes")
    suspend fun getAll(): List<Partycakes>

    @Query("SELECT * FROM partycakes WHERE id = :id")
    suspend fun getById(id: Int): Partycakes?

    @Query("DELETE FROM partycakes")
    suspend fun deleteAll()
}
