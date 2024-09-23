package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Rboinventables

@Dao
interface RboinventablesDao {
    @Insert
    suspend fun insert(rboinventables: Rboinventables)

    @Query("SELECT * FROM rboinventables")
    suspend fun getAll(): List<Rboinventables>

    @Query("SELECT * FROM rboinventables WHERE id = :id")
    suspend fun getById(id: Int): Rboinventables?

    @Query("DELETE FROM rboinventables")
    suspend fun deleteAll()
}
