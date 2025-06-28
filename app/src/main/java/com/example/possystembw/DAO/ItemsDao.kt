package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Items

@Dao
interface ItemsDao {
    @Insert
    suspend fun insert(item: Items)

    @Query("SELECT * FROM items")
    suspend fun getAll(): List<Items>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getById(id: Int): Items?

    @Query("DELETE FROM items")
    suspend fun deleteAll()


}
