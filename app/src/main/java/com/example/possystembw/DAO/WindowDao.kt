package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.example.possystembw.database.Window
import kotlinx.coroutines.flow.Flow


@Dao
interface WindowDao {
    @Query("SELECT * FROM windows")
    fun getAllWindows(): Flow<List<Window>>

    @Query("SELECT * FROM windows WHERE id = :id")
    suspend fun getWindowById(id: Int): Window?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(windows: List<Window>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: Window)

    @Update
    suspend fun update(window: Window)

    @Delete
    suspend fun delete(window: Window)

    @Query("DELETE FROM windows")
    suspend fun deleteAll()

    @Query("SELECT * FROM windows WHERE windownum = :tableId")
    suspend fun getWindowsAlignedWithTable(tableId: Int): List<Window>

    @Query("SELECT * FROM windows")
    suspend fun getAllWindowsOneShot(): List<Window>
}
