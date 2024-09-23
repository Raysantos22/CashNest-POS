package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Inventitembarcodes

@Dao
interface InventitembarcodesDao {
    @Insert
    suspend fun insert(inventItemBarcode: Inventitembarcodes)

    @Query("SELECT * FROM inventitembarcodes")
    suspend fun getAll(): List<Inventitembarcodes>

    @Query("SELECT * FROM inventitembarcodes WHERE ITEMID = :itemId")
    suspend fun getByItemId(itemId: String): List<Inventitembarcodes>

    @Query("DELETE FROM inventitembarcodes")
    suspend fun deleteAll()
}
