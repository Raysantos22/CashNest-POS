package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Numbersequencetables

@Dao
interface NumbersequencetablesDao {
    @Insert
    suspend fun insert(numberSequence: Numbersequencetables)

    @Query("SELECT * FROM numbersequencetables")
    suspend fun getAll(): List<Numbersequencetables>

    @Query("SELECT * FROM numbersequencetables WHERE id = :id")
    suspend fun getById(id: Int): Numbersequencetables?

    @Query("DELETE FROM numbersequencetables")
    suspend fun deleteAll()
}
