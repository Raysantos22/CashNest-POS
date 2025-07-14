package com.example.possystembw.data

import com.example.possystembw.DAO.LineTransactionVisibilityDao
import com.example.possystembw.DAO.ProductVisibilityDao
import com.example.possystembw.database.LineTransactionVisibility
import com.example.possystembw.database.ProductVisibility
import kotlinx.coroutines.flow.Flow


//import com.example.possystembw.DAO.ProductVisibilityDao
//import com.example.possystembw.database.ProductVisibility
//import kotlinx.coroutines.flow.Flow

class ProductVisibilityRepository(private val visibilityDao: ProductVisibilityDao) {

    suspend fun hideProduct(productId: Int, platform: String = "PURCHASE") {
        val existing = visibilityDao.getVisibility(productId, platform)
        if (existing != null) {
            visibilityDao.updateVisibility(productId, true, platform)
        } else {
            // FIXED: Use correct constructor
            visibilityDao.insertVisibility(
                ProductVisibility(
                    id = 0, // Auto-generated
                    productId = productId,
                    isHidden = true,
                    platform = platform
                )
            )
        }
    }

    suspend fun showProduct(productId: Int, platform: String = "PURCHASE") {
        visibilityDao.deleteVisibility(productId, platform)
    }

    suspend fun isProductHidden(productId: Int, platform: String = "PURCHASE"): Boolean {
        return visibilityDao.getVisibility(productId, platform)?.isHidden ?: false
    }

    fun getHiddenProducts(platform: String = "PURCHASE"): Flow<List<ProductVisibility>> {
        return visibilityDao.getHiddenProducts(platform)
    }

    suspend fun getHiddenProductsSync(platform: String = "PURCHASE"): List<ProductVisibility> {
        return visibilityDao.getHiddenProductsSync(platform)
    }
}
