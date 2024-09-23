package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Rbospecialgroups

@Dao
interface RbospecialgroupsDao {
    @Insert
    suspend fun insert(group: Rbospecialgroups)

    @Query("SELECT * FROM rbospecialgroups")
    suspend fun getAll(): List<Rbospecialgroups>

    @Query("SELECT * FROM rbospecialgroups WHERE groupId = :id")
    suspend fun getById(id: Int): Rbospecialgroups?

    @Query("DELETE FROM rbospecialgroups")
    suspend fun deleteAll()
}
