package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Details

@Dao
interface DetailsDao {
    @Insert
    suspend fun insert(details: Details)

    @Query("SELECT * FROM details")
    suspend fun getAll(): List<Details>

    @Query("SELECT * FROM details WHERE id = :id")
    suspend fun getById(id: Int): Details?

    @Query("DELETE FROM details")
    suspend fun deleteAll()
}
