package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.possystembw.database.Product
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.ProductRepository
import com.example.possystembw.database.Category
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private var repository: ProductRepository
    val allProducts: LiveData<List<Product>>


    private val _operationStatus = MutableLiveData<Result<List<Product>>>()
    val operationStatus: LiveData<Result<List<Product>>> = _operationStatus

    private val _alignedProducts = MutableStateFlow<Map<Category, List<Product>>>(emptyMap())
    val alignedProducts: StateFlow<Map<Category, List<Product>>> = _alignedProducts.asStateFlow()

    private val _selectedCategory = MutableStateFlow<Category?>(null)
    val selectedCategory: StateFlow<Category?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    private val _filteredProducts = MutableStateFlow<List<Product>>(emptyList())
    val filteredProducts: StateFlow<List<Product>> = _filteredProducts.asStateFlow()


    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()


    private val _isInitialized = MutableStateFlow(false)

    init {
        val database = AppDatabase.getDatabase(application)
        val productDao = database.productDao()
        val categoryDao = database.categoryDao()
        val productApi = RetrofitClient.productApi
        val categoryApi = RetrofitClient.categoryApi
        repository = ProductRepository(productDao, categoryDao, productApi, categoryApi)
        allProducts = repository.allProducts.asLiveData()
        loadAlignedProducts()

        viewModelScope.launch {
            combine(
                repository.allProducts,
                _selectedCategory,
                _searchQuery
            ) { products, category, query ->
                products.filter { product ->
                    val matchesCategory = when {
                        category == null -> true
                        category.name == "All" -> true
                        else -> product.itemGroup.equals(category.name, ignoreCase = true)
                    }
                    val matchesSearch = query.isNullOrBlank() ||
                            product.itemName.contains(query, ignoreCase = true)
                    matchesCategory && matchesSearch
                }
            }.catch { e ->
                Log.e("ProductViewModel", "Error filtering products", e)
                emit(emptyList())
            }.collect { filteredList ->
                _filteredProducts.value = filteredList
            }
        }

        // Load aligned products
        loadAlignedProducts()
    }





    fun filterProducts(query: String?) {
        _searchQuery.value = query
    }
    fun refreshProducts() {
        viewModelScope.launch {
            repository.refreshProducts()
        }
    }
    fun refreshEverything() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.refreshProductsAndCategories()
                loadAlignedProducts()
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error refreshing data", e)
                // Handle error appropriately
            } finally {
                _isLoading.value = false
            }
        }
    }


    fun updateProduct(product: Product) = viewModelScope.launch {
        repository.updateProduct(product)
    }

    fun deleteAllProducts() = viewModelScope.launch {
        repository.deleteAllProducts()
    }
    fun getProductByName(name: String): Product? {
        return allProducts.value?.find { it.itemName == name }
    }


    /* fun insertAllProductsFromApi() = viewModelScope.launch {
         _isLoading.value = true
         val result = repository.insertAllProductsFromApi()
         _operationStatus.postValue(result)
         loadAlignedProducts()
         _isLoading.value = false
     }*/


    private suspend fun initializeData() {
        try {
            _isLoading.value = true

            // First load products and categories from local DB
            val initialProducts = repository.allProducts.first()

            // If DB is empty, fetch from API
            if (initialProducts.isEmpty()) {
                repository.refreshProductsAndCategories()
            }

            loadAlignedProducts()
            _isInitialized.value = true
        } finally {
            _isLoading.value = false
        }
    }

    // Modify insertAllProductsFromApi to properly sync with categories
    suspend fun insertAllProductsFromApi() {
        _isLoading.value = true
        try {
            val result = repository.insertAllProductsFromApi()
            result.onSuccess { products ->
                loadAlignedProducts()
                _operationStatus.postValue(Result.success(products))
            }.onFailure { error ->
                _operationStatus.postValue(Result.failure(error))
            }
        } finally {
            _isLoading.value = false
        }
    }

    // New function to load aligned products
    fun loadAlignedProducts() {
        viewModelScope.launch {
            try {
                repository.getAlignedProducts()
                    .catch { e ->
                        Log.e("ProductViewModel", "Error loading aligned products", e)
                        emit(emptyMap())
                    }
                    .collect { aligned ->
                        _alignedProducts.value = aligned
                    }
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error in loadAlignedProducts", e)
                _alignedProducts.value = emptyMap()
            }
        }
    }

    fun getProductById(id: Int): Product? {
        return allProducts.value?.find { it.id == id }
    }
    fun selectCategory(category: Category?) {
        viewModelScope.launch {
            try {
                _selectedCategory.value = category
                val currentProducts = repository.allProducts.first()

                // Show all products if category is null or "All"
                val filteredProducts = when {
                    category == null || category.name == "All" -> currentProducts
                    category.name == "Uncategorized" -> {
                        // Show products that don't match any category
                        currentProducts.filter { product ->
                            alignedProducts.value.keys
                                .filter { it.name != "All" && it.name != "Uncategorized" }
                                .none { cat -> product.itemGroup.equals(cat.name, ignoreCase = true) }
                        }
                    }
                    else -> {
                        // Filter by selected category
                        currentProducts.filter { it.itemGroup.equals(category.name, ignoreCase = true) }
                    }
                }
                _filteredProducts.value = filteredProducts
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error selecting category", e)
            }
        }
    }

    private fun filterProducts(products: List<Product>, category: Category?) {
        _filteredProducts.value = when {
            category == null || category.name == "All" -> products
            else -> products.filter { it.itemGroup.equals(category.name, ignoreCase = true) }
        }
    }

    fun getProductByItemId(itemId: String): Product? {
        return allProducts.value?.find { product ->
            product.id.toString() == itemId ||
                    product.itemid.trim() == itemId.trim() ||
                    product.itemName.trim().equals(itemId.trim(), ignoreCase = true)
        }
    }

    fun refreshProductsAndCategories() = viewModelScope.launch {
        try {
            repository.refreshProductsAndCategories()
        } catch (e: Exception) {
            Log.e("ProductViewModel", "Error refreshing products and categories", e)
            // You might want to update _operationStatus here as well
        }
    }
    fun findProduct(identifier: String?): Product? {
        if (identifier.isNullOrBlank()) return null

        val normalizedIdentifier = identifier.trim()

        return allProducts.value?.find { product ->
            val normalizedItemId = product.itemid.trim()
            normalizedItemId.equals(normalizedIdentifier, ignoreCase = true)
        }
    }


    class ProductViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProductViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ProductViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}