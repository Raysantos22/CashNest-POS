package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Rboinventitemretailgroups

@Dao
interface RboinventitemretailgroupsDao {
    @Insert
    suspend fun insert(group: Rboinventitemretailgroups)

    @Query("SELECT * FROM rboinventitemretailgroups")
    suspend fun getAll(): List<Rboinventitemretailgroups>

    @Query("SELECT * FROM rboinventitemretailgroups WHERE groupId = :id")
    suspend fun getById(id: Int): Rboinventitemretailgroups?

    @Query("DELETE FROM rboinventitemretailgroups")
    suspend fun deleteAll()
}
