package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.TenderDeclaration
import kotlinx.coroutines.flow.Flow

@Dao
interface TenderDeclarationDao {
    @Insert
    suspend fun insert(tenderDeclaration: TenderDeclaration)

    @Query("SELECT * FROM TenderDeclaration")
    fun getAllTenderDeclarations(): Flow<List<TenderDeclaration>>
    @Query("SELECT * FROM tenderDeclaration ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTenderDeclaration(): TenderDeclaration?


    @Query("DELETE FROM tenderDeclaration")
    suspend fun deleteAll()

    // Optional: Delete by date if you want more specific control
    @Query("DELETE FROM tenderDeclaration WHERE date = :date")
    suspend fun deleteByDate(date: String)
// Add other query methods as needed
}
