package com.example.possystembw.data

import com.example.possystembw.DAO.LineTransactionVisibilityDao
import com.example.possystembw.DAO.ProductVisibilityDao
import com.example.possystembw.database.LineTransactionVisibility
import com.example.possystembw.database.ProductVisibility
import kotlinx.coroutines.flow.Flow

class ProductVisibilityRepository(private val visibilityDao: ProductVisibilityDao) {

    suspend fun hideProduct(productId: Int) {
        val existing = visibilityDao.getVisibility(productId)
        if (existing != null) {
            visibilityDao.updateVisibility(productId, true)
        } else {
            visibilityDao.insertVisibility(ProductVisibility(productId, true))
        }
    }

    suspend fun showProduct(productId: Int) {
        visibilityDao.deleteVisibility(productId)
    }

    suspend fun isProductHidden(productId: Int): Boolean {
        return visibilityDao.getVisibility(productId)?.isHidden ?: false
    }

    fun getHiddenProducts(): Flow<List<ProductVisibility>> {
        return visibilityDao.getHiddenProducts()
    }
}