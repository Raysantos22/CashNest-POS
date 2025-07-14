package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.possystembw.database.StockCountingEntity

@Dao
interface StockCountingDao {
    @Query("SELECT * FROM stock_counting WHERE storeId = :storeId")
    suspend fun getStockCountingByStore(storeId: String): List<StockCountingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stockCounting: List<StockCountingEntity>)

    @Query("DELETE FROM stock_counting WHERE storeId = :storeId")
    suspend fun deleteByStore(storeId: String)

    @Query("DELETE FROM stock_counting")
    suspend fun deleteAllStockCounting()

    @Query("UPDATE stock_counting SET posted = 1 WHERE journalId = :journalId")
    suspend fun markAsPosted(journalId: Long)

    @Query("DELETE FROM stock_counting WHERE journalId = :journalId")
    suspend fun deleteStockCountingByJournal(journalId: Long)
    @Transaction
    suspend fun updateStockCounting(storeId: String, stockCounting: List<StockCountingEntity>) {
        deleteByStore(storeId)
        insertAll(stockCounting)
    }
}