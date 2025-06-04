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
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductVisibilityActivity : AppCompatActivity() {
    private lateinit var productViewModel: ProductViewModel
    private lateinit var adapter: ProductVisibilityAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var cardToggleAllOn: CardView
    private lateinit var cardToggleAllOff: CardView
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
        cardToggleAllOn = findViewById(R.id.cardToggleAllOn)
        cardToggleAllOff = findViewById(R.id.cardToggleAllOff)
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
        cardToggleAllOn.setOnClickListener {
            toggleAllProducts(true)
        }

        cardToggleAllOff.setOnClickListener {
            toggleAllProducts(false)
        }
    }

    private fun updateToggleButtonStates() {
        val currentList = adapter.currentList
        val visibleCount = currentList.count { it.isVisible }
        val totalCount = currentList.size

        if (visibleCount == totalCount) {
            cardToggleAllOn.alpha = 0.5f
            cardToggleAllOff.alpha = 1.0f
        } else if (visibleCount == 0) {
            cardToggleAllOn.alpha = 1.0f
            cardToggleAllOff.alpha = 0.5f
        } else {
            cardToggleAllOn.alpha = 1.0f
            cardToggleAllOff.alpha = 1.0f
        }
    }

    private fun observeData() {
        // Observe categories from all aligned products
        lifecycleScope.launch {
            productViewModel.allAlignedProducts.collect { allAlignedProducts ->
                Log.d("ProductVisibility", "All aligned products received: ${allAlignedProducts.size} categories")

                categories = allAlignedProducts.keys.toList().sortedWith(
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

        // FIXED: Observe all products and their visibility status
        lifecycleScope.launch {
            combine(
                productViewModel.allProducts.asFlow(),
                productViewModel.getHiddenProducts()
            ) { allProducts, hiddenProducts ->
                val hiddenProductIds = hiddenProducts.map { it.productId }.toSet()

                allProducts.map { product ->
                    ProductWithVisibility(
                        product = product,
                        isVisible = product.id !in hiddenProductIds
                    )
                }
            }.collect { productsWithVisibility ->
                allProductsWithVisibility = productsWithVisibility
                Log.d("ProductVisibility", "Updated products with visibility: ${allProductsWithVisibility.size}")
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

    private fun createCategoryButton(category: Category): android.widget.Button {
        val button = android.widget.Button(this).apply {
            text = category.name
            textSize = 14f
            setPadding(32, 16, 32, 16)

            updateCategoryButtonAppearance(this, category == selectedCategory)

            setOnClickListener {
                selectCategory(category)
            }

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

    private fun updateCategoryButtonAppearance(button: android.widget.Button, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            button.elevation = 8f
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            button.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.elevation = 2f
            button.background = ContextCompat.getDrawable(this, R.drawable.category_button_unselected)
        }
    }

    private fun selectCategory(category: Category) {
        selectedCategory = if (selectedCategory == category) null else category

        Log.d("ProductVisibility", "Selected category: ${selectedCategory?.name ?: "None"}")

        for (i in 0 until categoryButtonsContainer.childCount) {
            val button = categoryButtonsContainer.getChildAt(i) as android.widget.Button
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
                        val knownCategories = categories.filter { it.name != "All" && it.name != "Uncategorized" }
                        knownCategories.none { cat ->
                            product.itemGroup.equals(cat.name, ignoreCase = true)
                        }
                    }
                    else -> product.itemGroup.equals(category.name, ignoreCase = true)
                }
            } ?: true

            matchesSearch && matchesCategory
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
        lifecycleScope.launch {
            if (isEnabled) {
                productViewModel.showProduct(product.id)
            } else {
                productViewModel.hideProduct(product.id)
            }

            // The UI will automatically update through the Flow observation
            // No need to manually update allProductsWithVisibility here
        }
    }

    private fun toggleAllProducts(showAll: Boolean) {
        val currentList = adapter.currentList

        lifecycleScope.launch {
            currentList.forEach { productWithVisibility ->
                val product = productWithVisibility.product
                if (showAll) {
                    productViewModel.showProduct(product.id)
                } else {
                    productViewModel.hideProduct(product.id)
                }
            }

            withContext(Dispatchers.Main) {
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