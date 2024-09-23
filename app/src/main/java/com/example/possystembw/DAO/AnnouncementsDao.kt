package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Announcements

@Dao
interface AnnouncementsDao {
    @Insert
    suspend fun insert(announcement: Announcements)

    @Query("SELECT * FROM announcements")
    suspend fun getAll(): List<Announcements>

    @Query("SELECT * FROM announcements WHERE id = :id")
    suspend fun getById(id: Int): Announcements?

    @Query("DELETE FROM announcements")
    suspend fun deleteAll()
}
