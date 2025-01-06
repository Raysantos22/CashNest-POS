package com.example.possystembw.DAO

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.possystembw.database.PrinterSettings
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PrinterSettingsDao {
    @Query("SELECT * FROM printer_settings")
    fun getAllPrinters(): Flow<List<PrinterSettings>>

    @Query("SELECT * FROM printer_settings WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultPrinter(): PrinterSettings?

    @Query("SELECT * FROM printer_settings WHERE windowId = :windowId")
    suspend fun getPrinterForWindow(windowId: Int): PrinterSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrinter(printer: PrinterSettings)

    @Delete
    suspend fun deletePrinter(printer: PrinterSettings)

    @Query("UPDATE printer_settings SET isDefault = 0")
    suspend fun clearDefaultPrinter()

    @Query("UPDATE printer_settings SET isDefault = 1 WHERE id = :printerId")
    suspend fun setDefaultPrinter(printerId: Int)

    @Query("UPDATE printer_settings SET lastConnected = :date WHERE id = :printerId")
    suspend fun updateLastConnected(printerId: Int, date: Date)
}