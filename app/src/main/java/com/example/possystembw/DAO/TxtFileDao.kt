package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Txtfile

@Dao
interface TxtfileDao {
    @Insert
    suspend fun insert(txtFile: Txtfile)

    @Query("SELECT * FROM txtfile")
    suspend fun getAll(): List<Txtfile>

    @Query("SELECT * FROM txtfile WHERE id = :id")
    suspend fun getById(id: Int): Txtfile?

    @Query("DELETE FROM txtfile")
    suspend fun deleteAll()
}
