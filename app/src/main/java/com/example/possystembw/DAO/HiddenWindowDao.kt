package com.example.possystembw.DAO

import androidx.room.*
import com.example.possystembw.database.HiddenWindow
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenWindowDao {
    @Query("SELECT * FROM hidden_windows")
    fun getAllHiddenWindows(): Flow<List<HiddenWindow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHiddenWindow(hiddenWindow: HiddenWindow)

    @Delete
    suspend fun deleteHiddenWindow(hiddenWindow: HiddenWindow)

    @Query("DELETE FROM hidden_windows WHERE windowId = :windowId")
    suspend fun deleteHiddenWindowByWindowId(windowId: Int)

    @Query("DELETE FROM hidden_windows WHERE windowTableId = :windowTableId")
    suspend fun deleteHiddenWindowByWindowTableId(windowTableId: Int)

    @Query("SELECT * FROM hidden_windows WHERE windowId = :windowId")
    suspend fun getHiddenWindowByWindowId(windowId: Int): HiddenWindow?

    @Query("SELECT * FROM hidden_windows WHERE windowTableId = :windowTableId")
    suspend fun getHiddenWindowByWindowTableId(windowTableId: Int): HiddenWindow?
}