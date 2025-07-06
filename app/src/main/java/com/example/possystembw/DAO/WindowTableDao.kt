package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.WindowTable
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowTableDao {
    @Insert
    suspend fun insert(windowTable: WindowTable)

    @Query("SELECT * FROM windowtable")
    suspend fun getAll(): List<WindowTable>

    @Query("SELECT * FROM windowtable")
    fun getAllWindowTables(): Flow<List<WindowTable>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(windowTables: List<WindowTable>)

    @Query("SELECT * FROM windowtable")
    suspend fun getAllWindowTablesOneShot(): List<WindowTable>

    @Query("DELETE FROM windowtable")
    suspend fun deleteAll()


}


