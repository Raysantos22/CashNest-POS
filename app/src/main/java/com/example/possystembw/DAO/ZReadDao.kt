package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.ZRead

@Dao
interface ZReadDao {
    @Query("SELECT * FROM zread ORDER BY date DESC LIMIT 1")
    suspend fun getLastZRead(): ZRead?

    @Query("DELETE FROM zread")
    suspend fun deleteAll()

    @Query("DELETE FROM zread WHERE date = :date")
    suspend fun deleteByDate(date: String)
//
//    @Query("SELECT * FROM zread WHERE date = :date LIMIT 1")
//    suspend fun getZReadByDate(date: String): ZRead?
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insert(zRead: ZRead)

    // Add this method to check if Z-Read exists for today
    @Query("SELECT COUNT(*) > 0 FROM zread WHERE date = :date")
    suspend fun hasZReadForDate(date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zRead: ZRead)

    @Query("SELECT * FROM zread WHERE date = :date LIMIT 1")
    suspend fun getZReadByDate(date: String): ZRead?

    @Query("SELECT * FROM zread ORDER BY date DESC, time DESC")
    suspend fun getAllZReads(): List<ZRead>

    @Query("SELECT MAX(CAST(zReportId AS INTEGER)) FROM zread WHERE zReportId GLOB '[0-9]*'")
    suspend fun getMaxZReportId(): Int?

    @Query("SELECT COUNT(*) FROM zread WHERE date = :date")
    suspend fun getZReadCountForDate(date: String): Int

    @Query("SELECT * FROM zread WHERE zReportId = :zReportId LIMIT 1")
    suspend fun getZReadByReportId(zReportId: String): ZRead?
}