package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.possystembw.database.Barcodes

@Dao
interface BarcodesDao {
    @Insert
    suspend fun insert(barcode: Barcodes)

    @Query("SELECT * FROM barcodes")
    suspend fun getAll(): List<Barcodes>

    @Query("SELECT * FROM barcodes WHERE id = :id")
    suspend fun getById(id: Int): Barcodes?

    @Query("DELETE FROM barcodes")
    suspend fun deleteAll()
}
