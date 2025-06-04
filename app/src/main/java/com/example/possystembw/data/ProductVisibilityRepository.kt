package com.example.possystembw.data

import com.example.possystembw.DAO.LineTransactionVisibilityDao
import com.example.possystembw.DAO.ProductVisibilityDao
import com.example.possystembw.database.LineTransactionVisibility
import com.example.possystembw.database.ProductVisibility
import kotlinx.coroutines.flow.Flow

class ProductVisibilityRepository(
    private val visibilityDao: ProductVisibilityDao,
    private val lineTransactionVisibilityDao: LineTransactionVisibilityDao

) {
    suspend fun hideProduct(productId: Int) {
        val existing = visibilityDao.getVisibility(productId)
        if (existing != null) {
            visibilityDao.updateVisibility(productId, true)
        } else {
            visibilityDao.insertVisibility(ProductVisibility(productId, true))
        }
    }
    
    suspend fun showProduct(productId: Int) {
        val existing = visibilityDao.getVisibility(productId)
        if (existing != null) {
            visibilityDao.updateVisibility(productId, false)
        } else {
            visibilityDao.insertVisibility(ProductVisibility(productId, false))
        }
    }
    
    suspend fun isProductHidden(productId: Int): Boolean {
        return visibilityDao.getVisibility(productId)?.isHidden ?: false
    }
    suspend fun hideLineTransaction(itemId: String) {
        val existing = lineTransactionVisibilityDao.getVisibility(itemId)
        if (existing != null) {
            lineTransactionVisibilityDao.updateVisibility(itemId, true)
        } else {
            lineTransactionVisibilityDao.insertVisibility(LineTransactionVisibility(itemId, true))
        }
    }

    suspend fun showLineTransaction(itemId: String) {
        val existing = lineTransactionVisibilityDao.getVisibility(itemId)
        if (existing != null) {
            lineTransactionVisibilityDao.updateVisibility(itemId, false)
        } else {
            lineTransactionVisibilityDao.insertVisibility(LineTransactionVisibility(itemId, false))
        }
    }

    suspend fun isLineTransactionHidden(itemId: String): Boolean {
        return lineTransactionVisibilityDao.getVisibility(itemId)?.isHidden ?: false
    }

    fun getHiddenLineTransactions(): Flow<List<LineTransactionVisibility>> {
        return lineTransactionVisibilityDao.getHiddenLineTransactions()
    }

    fun getHiddenProducts(): Flow<List<ProductVisibility>> {
        return visibilityDao.getHiddenProducts()
    }
}