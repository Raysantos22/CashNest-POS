package com.example.possystembw.data

import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.DAO.LineTransactionVisibilityDao
import com.example.possystembw.database.LineTransactionVisibility
import kotlinx.coroutines.flow.Flow

class LineTransactionVisibilityRepository(
    private val visibilityDao: LineTransactionVisibilityDao // Now uses the correct DAO
) {
    suspend fun hideLineTransaction(itemId: String) {
        val existing = visibilityDao.getVisibility(itemId)
        if (existing != null) {
            visibilityDao.updateVisibility(itemId, true)
        } else {
            visibilityDao.insertVisibility(LineTransactionVisibility(itemId, true))
        }
    }

    suspend fun showLineTransaction(itemId: String) {
        val existing = visibilityDao.getVisibility(itemId)
        if (existing != null) {
            visibilityDao.updateVisibility(itemId, false)
        } else {
            visibilityDao.insertVisibility(LineTransactionVisibility(itemId, false))
        }
    }

    suspend fun isLineTransactionHidden(itemId: String): Boolean {
        return visibilityDao.getVisibility(itemId)?.isHidden ?: false
    }

    fun getHiddenLineTransactions(): Flow<List<LineTransactionVisibility>> {
        return visibilityDao.getHiddenLineTransactions()
    }
}
// 4. Data class for Line Transaction with Visibility
data class LineTransactionWithVisibility(
    val lineTransaction: LineTransaction,
    val isVisible: Boolean
)