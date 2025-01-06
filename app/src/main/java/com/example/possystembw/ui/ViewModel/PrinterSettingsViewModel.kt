package com.example.possystembw.ui.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.possystembw.DAO.PrinterSettingsDao
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.PrinterSettings
import kotlinx.coroutines.flow.Flow

class PrinterSettingsViewModel(application: Application) : AndroidViewModel(application) {
    val dao: PrinterSettingsDao = AppDatabase.getDatabase(application).printerSettingsDao()
    val allPrinters: Flow<List<PrinterSettings>> = dao.getAllPrinters()
    
    suspend fun addPrinter(name: String, macAddress: String, isDefault: Boolean = false, windowId: Int? = null) {
        if (isDefault) {
            dao.clearDefaultPrinter()
        }
        dao.insertPrinter(PrinterSettings(
            printerName = name,
            macAddress = macAddress,
            isDefault = isDefault,
            windowId = windowId
        ))
    }

    suspend fun deletePrinter(printer: PrinterSettings) {
        dao.deletePrinter(printer)
    }

    suspend fun setDefaultPrinter(printerId: Int) {
        dao.clearDefaultPrinter()
        dao.setDefaultPrinter(printerId)
    }

    suspend fun getDefaultPrinter(): PrinterSettings? {
        return dao.getDefaultPrinter()
    }

    suspend fun getPrinterForWindow(windowId: Int): PrinterSettings? {
        return dao.getPrinterForWindow(windowId)
    }
}