package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Rbostoretables

@Dao
interface RbostoretablesDao {
    @Insert
    suspend fun insert(store: Rbostoretables)

    @Query("SELECT * FROM rbostoretables")
    suspend fun getAll(): List<Rbostoretables>

    @Query("SELECT * FROM rbostoretables WHERE storeId = :id")
    suspend fun getById(id: Int): Rbostoretables?

    @Query("DELETE FROM rbostoretables")
    suspend fun deleteAll()
}
