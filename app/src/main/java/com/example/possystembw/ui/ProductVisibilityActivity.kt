package com.example.possystembw.ui

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.R
import com.example.possystembw.adapter.ProductVisibilityAdapter
import com.example.possystembw.database.Category
import com.example.possystembw.database.Product
import com.example.possystembw.ui.ViewModel.ProductViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductVisibilityActivity : AppCompatActivity() {
    private lateinit var productViewModel: ProductViewModel
    private lateinit var adapter: ProductVisibilityAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnToggleAllOn: Button
    private lateinit var btnToggleAllOff: Button
    private lateinit var categoryButtonsContainer: LinearLayout

    private var selectedCategory: Category? = null
    private var allProductsWithVisibility: List<ProductWithVisibility> = emptyList()
    private var categories: List<Category> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_visibility)

        setupToolbar()
        initializeViews()
        initializeViewModel()
        setupRecyclerView()
        setupToggleButtons()
        observeData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Show/Hide Products"
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts)
        searchView = findViewById(R.id.searchView)
        btnToggleAllOn = findViewById(R.id.btnToggleAllOn)
        btnToggleAllOff = findViewById(R.id.btnToggleAllOff)
        categoryButtonsContainer = findViewById(R.id.categoryButtonsContainer)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts()
                return true
            }
        })
    }

    private fun initializeViewModel() {
        productViewModel = ViewModelProvider(
            this,
            ProductViewModel.ProductViewModelFactory(application)
        )[ProductViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = ProductVisibilityAdapter { product, isEnabled ->
            toggleProductVisibility(product, isEnabled)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add item decoration for better spacing
        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = spacing
            }
        })
    }

    private fun setupToggleButtons() {
        btnToggleAllOn.setOnClickListener {
            toggleAllProducts(true)
            updateToggleButtonStates()
        }

        btnToggleAllOff.setOnClickListener {
            toggleAllProducts(false)
            updateToggleButtonStates()
        }

        updateToggleButtonStates()
    }

    private fun updateToggleButtonStates() {
        val currentList = adapter.currentList
        val visibleCount = currentList.count { it.isVisible }
        val totalCount = currentList.size

        // Update button appearances based on current state
        if (visibleCount == totalCount) {
            btnToggleAllOn.alpha = 0.5f
            btnToggleAllOff.alpha = 1.0f
        } else if (visibleCount == 0) {
            btnToggleAllOn.alpha = 1.0f
            btnToggleAllOff.alpha = 0.5f
        } else {
            btnToggleAllOn.alpha = 1.0f
            btnToggleAllOff.alpha = 1.0f
        }
    }

    private fun observeData() {
        // Observe aligned products for categories

        lifecycleScope.launch {
            productViewModel.alignedProducts.collect { alignedProducts ->
                Log.d("ProductVisibility", "Aligned products received: ${alignedProducts.size} categories")
                alignedProducts.forEach { (category, products) ->
                    Log.d("ProductVisibility", "Category: ${category.name}, Products: ${products.size}")
                }

                categories = alignedProducts.keys.toList().sortedWith(
                    compareBy<Category> {
                        when (it.name) {
                            "All" -> 0
                            "Uncategorized" -> Int.MAX_VALUE
                            else -> 1
                        }
                    }.thenBy { it.name }
                )

                Log.d("ProductVisibility", "Categories for buttons: ${categories.map { it.name }}")
                setupCategoryButtons()
            }
        }

        // Observe all products (including hidden ones) for visibility management
        productViewModel.allProducts.observe(this) { products ->
            Log.d("ProductVisibility", "All products received: ${products.size}")
            lifecycleScope.launch {
                val productsWithVisibility = products.map { product ->
                    val isVisible = !productViewModel.isProductHidden(product.id)
                    ProductWithVisibility(product, isVisible)
                }
                allProductsWithVisibility = productsWithVisibility
                Log.d("ProductVisibility", "Products with visibility: ${allProductsWithVisibility.size}")
                filterProducts()
            }

        }
    }

    private fun setupCategoryButtons() {
        categoryButtonsContainer.removeAllViews()
        Log.d("ProductVisibility", "Setting up ${categories.size} category buttons")

        categories.forEach { category ->
            val button = createCategoryButton(category)
            categoryButtonsContainer.addView(button)
        }
    }

    private fun createCategoryButton(category: Category): Button {
        val button = Button(this).apply {
            text = category.name
            textSize = 14f
            setPadding(32, 16, 32, 16)

            // Set initial appearance
            updateCategoryButtonAppearance(this, category == selectedCategory)

            setOnClickListener {
                selectCategory(category)
            }

            // Add margin
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            layoutParams = params
        }

        return button
    }

    // FIXED: Different backgrounds for selected and unselected states
    private fun updateCategoryButtonAppearance(button: Button, isSelected: Boolean) {
        if (isSelected) {
            // Use a selected background (you might need to create this drawable)
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            button.elevation = 8f
        } else {
            // Use an unselected background
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            button.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.elevation = 2f
            // Add border for unselected state
            button.background = ContextCompat.getDrawable(this, R.drawable.category_button_unselected)
        }
    }

    private fun selectCategory(category: Category) {
        val previousCategory = selectedCategory
        selectedCategory = if (selectedCategory == category) null else category

        Log.d("ProductVisibility", "Selected category: ${selectedCategory?.name ?: "None"}")

        // Update button appearances
        for (i in 0 until categoryButtonsContainer.childCount) {
            val button = categoryButtonsContainer.getChildAt(i) as Button
            val buttonCategory = categories[i]
            updateCategoryButtonAppearance(button, buttonCategory == selectedCategory)
        }

        filterProducts()
    }

    private fun filterProducts() {
        val query = searchView.query?.toString()
        Log.d("ProductVisibility", "Filtering products - Query: '$query', Selected Category: '${selectedCategory?.name}'")

        val filteredList = allProductsWithVisibility.filter { productWithVisibility ->
            val product = productWithVisibility.product

            val matchesSearch = query.isNullOrBlank() ||
                    product.itemName.contains(query, ignoreCase = true) ||
                    product.itemGroup.contains(query, ignoreCase = true)

            val matchesCategory = selectedCategory?.let { category ->
                when (category.name) {
                    "All" -> true
                    "Uncategorized" -> {
                        // Check if product doesn't belong to any known category
                        val knownCategories = categories.filter { it.name != "All" && it.name != "Uncategorized" }
                        knownCategories.none { cat ->
                            product.itemGroup.equals(cat.name, ignoreCase = true)
                        }
                    }
                    else -> product.itemGroup.equals(category.name, ignoreCase = true)
                }
            } ?: true

            val result = matchesSearch && matchesCategory
            Log.d("ProductVisibility", "Product: ${product.itemName}, Group: ${product.itemGroup}, Matches: $result")
            result
        }

        Log.d("ProductVisibility", "Filtered ${filteredList.size} products from ${allProductsWithVisibility.size} total")
        adapter.submitList(filteredList)
        updateToggleButtonStates()
        updateResultsInfo(filteredList.size, allProductsWithVisibility.size)
    }

    private fun updateResultsInfo(filtered: Int, total: Int) {
        supportActionBar?.subtitle = if (filtered == total) {
            "$total products"
        } else {
            "$filtered of $total products"
        }
    }

    private fun toggleProductVisibility(product: Product, isEnabled: Boolean) {
        if (isEnabled) {
            productViewModel.showProduct(product.id)
        } else {
            productViewModel.hideProduct(product.id)
        }

        // Update local list immediately for better UX
        allProductsWithVisibility = allProductsWithVisibility.map { productWithVisibility ->
            if (productWithVisibility.product.id == product.id) {
                productWithVisibility.copy(isVisible = isEnabled)
            } else {
                productWithVisibility
            }
        }

        filterProducts()
    }

    private fun toggleAllProducts(showAll: Boolean) {
        val currentList = adapter.currentList

        lifecycleScope.launch {
            // Update database
            currentList.forEach { productWithVisibility ->
                val product = productWithVisibility.product
                if (showAll) {
                    productViewModel.showProduct(product.id)
                } else {
                    productViewModel.hideProduct(product.id)
                }
            }

            // Update local list immediately
            allProductsWithVisibility = allProductsWithVisibility.map { productWithVisibility ->
                val shouldUpdate = currentList.any { it.product.id == productWithVisibility.product.id }
                if (shouldUpdate) {
                    productWithVisibility.copy(isVisible = showAll)
                } else {
                    productWithVisibility
                }
            }

            // Update UI immediately
            withContext(Dispatchers.Main) {
                filterProducts()

                // Show feedback
                val message = if (showAll) "All products shown" else "All products hidden"
                Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}

data class ProductWithVisibility(
    val product: Product,
    val isVisible: Boolean
)
data class LineTransactionWithVisibility(
    val lineTransaction: LineTransaction,
    val isVisible: Boolean
)