package com.example.possystembw.data

import com.example.possystembw.DAO.HiddenWindowDao
import com.example.possystembw.database.HiddenWindow
import kotlinx.coroutines.flow.Flow

class WindowVisibilityRepository(private val hiddenWindowDao: HiddenWindowDao) {

    fun getHiddenWindows(): Flow<List<HiddenWindow>> {
        return hiddenWindowDao.getAllHiddenWindows()
    }

    suspend fun hideWindow(windowId: Int) {
        val hiddenWindow = HiddenWindow(windowId = windowId, windowTableId = null)
        hiddenWindowDao.insertHiddenWindow(hiddenWindow)
    }

    suspend fun showWindow(windowId: Int) {
        hiddenWindowDao.deleteHiddenWindowByWindowId(windowId)
    }

    suspend fun hideWindowTable(windowTableId: Int) {
        val hiddenWindow = HiddenWindow(windowId = null, windowTableId = windowTableId)
        hiddenWindowDao.insertHiddenWindow(hiddenWindow)
    }

    suspend fun showWindowTable(windowTableId: Int) {
        hiddenWindowDao.deleteHiddenWindowByWindowTableId(windowTableId)
    }
}