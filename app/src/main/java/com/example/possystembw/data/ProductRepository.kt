package com.example.possystembw.data

import android.app.Application
import android.util.Log
import com.example.possystembw.DAO.CategoryApi
import com.example.possystembw.DAO.CategoryDao
import com.example.possystembw.DAO.LineTransactionVisibilityDao
import com.example.possystembw.DAO.ProductDao
import com.example.possystembw.database.Product
import com.example.possystembw.DAO.ProductApi
import com.example.possystembw.DAO.ProductVisibilityDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import com.example.possystembw.database.Category  // Make sure to import the correct Category
import com.example.possystembw.database.ProductVisibility
import com.example.possystembw.ui.SessionManager
import kotlinx.coroutines.flow.combine


class ProductRepository(
    private val productDao: ProductDao,
    private val categoryDao: CategoryDao,
    private val productApi: ProductApi,
    private val categoryApi: CategoryApi,
    private val application: Application,
    private val visibilityDao: ProductVisibilityDao, // Product visibility DAO
    private val lineTransactionVisibilityDao: LineTransactionVisibilityDao // Add this if your ProductVisibilityRepository needs it
) {

    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()

    private val visibilityRepository = ProductVisibilityRepository(visibilityDao, lineTransactionVisibilityDao)

    suspend fun hideProduct(productId: Int) {
        visibilityRepository.hideProduct(productId)
    }

    fun getVisibleProducts(): Flow<List<Product>> {
        return combine(
            productDao.getAllProducts(),
            visibilityDao.getHiddenProducts()
        ) { allProducts, hiddenVisibilities ->
            val hiddenIds = hiddenVisibilities.map { it.productId }.toSet()
            allProducts.filter { product -> product.id !in hiddenIds }
        }
    }

    suspend fun showProduct(productId: Int) {
        visibilityRepository.showProduct(productId)
    }

    suspend fun isProductHidden(productId: Int): Boolean {
        return visibilityRepository.isProductHidden(productId)
    }

    fun getHiddenProducts(): Flow<List<ProductVisibility>> {
        return visibilityRepository.getHiddenProducts()
    }

    // Update getAlignedProducts to use visible products
    fun getAlignedVisibleProducts(): Flow<Map<Category, List<Product>>> {
        return combine(allCategories, getVisibleProducts()) { categories, products ->
            val alignedMap = mutableMapOf<Category, List<Product>>()

            // Always add all visible products to "All" category
            alignedMap[Category(-1, "All")] = products

            // Group products by their categories
            categories
                .filter { it.name != "All" }
                .forEach { category ->
                    val categoryProducts = products.filter { product ->
                        product.itemGroup.equals(category.name, ignoreCase = true)
                    }
                    if (categoryProducts.isNotEmpty()) {
                        alignedMap[category] = categoryProducts
                    }
                }

            // Add uncategorized products to "Uncategorized" category
            val uncategorizedProducts = products.filter { product ->
                categories.none { category ->
                    product.itemGroup.equals(category.name, ignoreCase = true)
                }
            }
            if (uncategorizedProducts.isNotEmpty()) {
                alignedMap[Category(-3, "Uncategorized")] = uncategorizedProducts
            }

            alignedMap
        }
    }


    private fun getCurrentStoreId(): String {
        return SessionManager.getCurrentUser()?.storeid
            ?: throw IllegalStateException("No store ID available")
    }

    suspend fun refreshProducts() {
        withContext(Dispatchers.IO) {
            try {
                val storeId = getCurrentStoreId()
                val response = productApi.getAllProducts(storeId)
                if (response.isSuccessful) {
                    val remoteProducts = response.body() ?: emptyList()
                    productDao.deleteAll()
                    productDao.insertAll(remoteProducts)
                } else {
                    // Handle error
                    throw Exception("Failed to fetch products: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error refreshing products", e)
                throw e
            }
        }
    }

    suspend fun createProduct(product: Product) {
        withContext(Dispatchers.IO) {
            val response = productApi.createProduct(product)
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.data is Product) {
                        productDao.insertProduct(apiResponse.data)
                    }
                }
            } else {
                // Handle error
            }
        }
    }


    private suspend fun ensureDefaultCategories() {
        val defaultCategories = listOf(
            Category(groupId = -1L, name = "All"),
            Category(groupId = -2L, name = "Mix & Match"),
            Category(groupId = -3L, name = "Uncategorized")
        )

        defaultCategories.forEach { category ->
            if (categoryDao.getCategoryByName(category.name.trim()) == null) {
                categoryDao.insertCategory(category)
            }
        }
    }
    suspend fun refreshProductsAndCategories() {
        withContext(Dispatchers.IO) {
            try {
                // Ensure default categories exist
                ensureDefaultCategories()

                try {
                    val storeId = getCurrentStoreId()
                    val productsResponse = productApi.getAllProducts(storeId)
                    val categoriesResponse = categoryApi.getCategories()

                    if (productsResponse.isSuccessful && categoriesResponse.isSuccessful) {
                        val remoteProducts = productsResponse.body() ?: emptyList()
                        val remoteCategories = categoriesResponse.body() ?: emptyList()

                        // Clear existing non-default categories
                        categoryDao.deleteNonDefaultCategories()

                        // Process categories and products
                        processCategoriesAndProducts(remoteCategories, remoteProducts)
                    } else {
                        Log.e(
                            "ProductRepository",
                            "API call failed: ${
                                productsResponse.errorBody()
                                    ?.string() ?: categoriesResponse.errorBody()?.string()
                            }"
                        )
                    }
                } catch (e: Exception) {
                    Log.e("ProductRepository", "API call failed, using local data", e)
                }
            } catch (e: Exception) {
                Log.e("ProductRepository", "Error in refreshProductsAndCategories", e)
                throw e
            }
        }
    }

    private suspend fun processCategoriesAndProducts(
        remoteCategories: List<Category>,
        remoteProducts: List<Product>
    ) {
        // Create a map to store unique categories
        val uniqueCategories = mutableMapOf<String, Long>()

        // Add default categories to the map
        categoryDao.getAllCategoriesSync().forEach { category ->
            uniqueCategories[category.name.lowercase().trim()] = category.groupId
        }

        // Process remote categories
        remoteCategories
            .distinctBy { it.name.lowercase().trim() }
            .forEach { category ->
                val categoryName = category.name.lowercase().trim()
                if (!uniqueCategories.containsKey(categoryName)) {
                    val id = categoryDao.insertCategory(Category(name = category.name.trim()))
                    uniqueCategories[categoryName] = id
                }
            }

        // Get uncategorized category ID
        val uncategorizedId = uniqueCategories["uncategorized"] ?: -3L

        // Process products with proper category IDs
        val processedProducts = remoteProducts.map { product ->
            val categoryName = product.itemGroup.trim().lowercase()
            val categoryId = uniqueCategories[categoryName] ?: uncategorizedId
            product.copy(categoryId = categoryId)
        }

        // Update products
        productDao.deleteAll()
        productDao.insertAll(processedProducts)
    }

    fun getAlignedProducts(): Flow<Map<Category, List<Product>>> {
        return combine(allCategories, allProducts) { categories, products ->
            val alignedMap = mutableMapOf<Category, List<Product>>()

            // Always add all products to "All" category
            alignedMap[Category(-1, "All")] = products

            // Group products by their categories
            categories
                .filter { it.name != "All" }
                .forEach { category ->
                    val categoryProducts = products.filter { product ->
                        product.itemGroup.equals(category.name, ignoreCase = true)
                    }
                    if (categoryProducts.isNotEmpty()) {
                        alignedMap[category] = categoryProducts
                    }
                }

            // Add uncategorized products to "Uncategorized" category
            val uncategorizedProducts = products.filter { product ->
                categories.none { category ->
                    product.itemGroup.equals(category.name, ignoreCase = true)
                }
            }
            if (uncategorizedProducts.isNotEmpty()) {
                alignedMap[Category(-3, "Uncategorized")] = uncategorizedProducts
            }

            alignedMap
        }
    }


    suspend fun updateProduct(product: Product) {
        withContext(Dispatchers.IO) {
            val response = productApi.updateProduct(product.id, product)
            if (response.isSuccessful) {
                // Assuming you want to update the local database only if the API call was successful
                productDao.updateProduct(product)
            } else {
                // Handle error
            }
        }
    }

    suspend fun deleteAllProducts() {
        withContext(Dispatchers.IO) {
            productApi.deleteAllProducts()
            productDao.deleteAll()
        }
    }

    suspend fun getProductById(id: Int): Product? {
        return productDao.getProductById(id)
    }

    fun getProductsByCategory(categoryName: String): Flow<List<Product>> {
        return productDao.getProductsByCategory(categoryName)
    }

//    suspend fun insertAllProductsFromApi(): Result<List<Product>> = withContext(Dispatchers.IO) {
//        try {
//            // Step 1: Ensure default categories exist first
//            ensureDefaultCategories()
//
//            // Step 2: Get all unique categories from products
//            val storeId = getCurrentStoreId()
//            val productsResponse = productApi.getAllProducts(storeId)
//            if (!productsResponse.isSuccessful) {
//                return@withContext Result.failure(Exception("Failed to fetch products"))
//            }
//
//            val products = productsResponse.body() ?: emptyList()
//            if (products.isEmpty()) {
//                return@withContext Result.failure(Exception("No products received from API"))
//            }
//
//            // Rest of the method remains the same...
//            // Step 3: Extract and create unique categories
//            val uniqueCategories = products
//                .map { it.itemGroup.trim() }
//                .distinct()
//                .filter { it.isNotBlank() }
//
//            // Step 4: Insert categories and build category mapping
//            val categoryMap = mutableMapOf<String, Long>()
//
//            // Add existing categories to map
//            categoryDao.getAllCategoriesSync().forEach { category ->
//                categoryMap[category.name.lowercase().trim()] = category.groupId
//            }
//
//            // Insert new categories
//            uniqueCategories.forEach { categoryName ->
//                if (!categoryMap.containsKey(categoryName.lowercase())) {
//                    val id = categoryDao.insertCategory(Category(name = categoryName.trim()))
//                    categoryMap[categoryName.lowercase()] = id
//                }
//            }
//
//            // Get uncategorized category ID
//            val uncategorizedId = categoryDao.getCategoryByName("Uncategorized")?.groupId ?: -3L
//
//            // Step 5: Process products with proper category IDs
//            val processedProducts = products.map { product ->
//                val categoryName = product.itemGroup.trim().lowercase()
//                val categoryId = categoryMap[categoryName] ?: uncategorizedId
//                product.copy(categoryId = categoryId)
//            }
//
//            // Step 6: Insert processed products
//            productDao.deleteAll()
//            productDao.insertAll(processedProducts)
//
//            return@withContext Result.success(processedProducts)
//        } catch (e: Exception) {
//            Log.e("ProductRepository", "Error inserting products", e)
//            return@withContext Result.failure(e)
//        }
//    }
suspend fun insertAllProductsFromApi(): Result<List<Product>> = withContext(Dispatchers.IO) {
    try {
        // Step 1: Store current visibility settings before clearing products
        val currentVisibilitySettings = mutableMapOf<String, Boolean>()
        val existingProducts = productDao.getAllProductsSync() // You'll need to add this method
        existingProducts.forEach { product ->
            val isHidden = visibilityDao.getVisibility(product.id)?.isHidden ?: false
            // Use itemid as the key since it's more stable than auto-generated id
            currentVisibilitySettings[product.itemid] = isHidden
        }

        // Step 2: Ensure default categories exist first
        ensureDefaultCategories()

        // Step 3: Get all unique categories from products
        val storeId = getCurrentStoreId()
        val productsResponse = productApi.getAllProducts(storeId)
        if (!productsResponse.isSuccessful) {
            return@withContext Result.failure(Exception("Failed to fetch products"))
        }

        val products = productsResponse.body() ?: emptyList()
        if (products.isEmpty()) {
            return@withContext Result.failure(Exception("No products received from API"))
        }

        // Step 4: Extract and create unique categories
        val uniqueCategories = products
            .map { it.itemGroup.trim() }
            .distinct()
            .filter { it.isNotBlank() }

        // Step 5: Insert categories and build category mapping
        val categoryMap = mutableMapOf<String, Long>()

        // Add existing categories to map
        categoryDao.getAllCategoriesSync().forEach { category ->
            categoryMap[category.name.lowercase().trim()] = category.groupId
        }

        // Insert new categories
        uniqueCategories.forEach { categoryName ->
            if (!categoryMap.containsKey(categoryName.lowercase())) {
                val id = categoryDao.insertCategory(Category(name = categoryName.trim()))
                categoryMap[categoryName.lowercase()] = id
            }
        }

        // Get uncategorized category ID
        val uncategorizedId = categoryDao.getCategoryByName("Uncategorized")?.groupId ?: -3L

        // Step 6: Process products with proper category IDs
        val processedProducts = products.map { product ->
            val categoryName = product.itemGroup.trim().lowercase()
            val categoryId = categoryMap[categoryName] ?: uncategorizedId
            product.copy(categoryId = categoryId)
        }

        // Step 7: Clear old data
        visibilityDao.deleteAll() // Clear old visibility settings
        productDao.deleteAll()

        // Step 8: Insert processed products
        productDao.insertAll(processedProducts)

        // Step 9: Restore visibility settings based on itemid
        val newProducts = productDao.getAllProductsSync()
        newProducts.forEach { product ->
            val wasHidden = currentVisibilitySettings[product.itemid] ?: false
            if (wasHidden) {
                visibilityDao.insertVisibility(ProductVisibility(product.id, true))
            }
        }

        return@withContext Result.success(processedProducts)
    } catch (e: Exception) {
        Log.e("ProductRepository", "Error inserting products", e)
        return@withContext Result.failure(e)
    }
}
}
