package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Controls

@Dao
interface ControlsDao {
    @Insert
    suspend fun insert(control: Controls)

    @Query("SELECT * FROM controls")
    suspend fun getAll(): List<Controls>

    @Query("SELECT * FROM controls WHERE id = :id")
    suspend fun getById(id: Int): Controls?

    @Query("DELETE FROM controls")
    suspend fun deleteAll()
}
