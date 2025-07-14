package com.example.possystembw.ui

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SearchView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
    private lateinit var tvResultsInfo: TextView
    private var isLoading = false

    private var selectedCategory: Category? = null
    private var selectedVisibilityType: String = "PURCHASE"
    private var allProductsWithVisibility: List<ProductWithVisibility> = emptyList()
    private var categories: List<Category> = emptyList()

    companion object {
        const val VISIBILITY_GENERAL = "PURCHASE"
        const val VISIBILITY_FOODPANDA = "FOODPANDA"
        const val VISIBILITY_GRABFOOD = "GRABFOOD"
        const val VISIBILITY_MANILARATE = "MANILARATE"
        const val VISIBILITY_MALLPRICE = "MALLPRICE"
        const val VISIBILITY_GRABFOODMALL = "GRABFOODMALL"
        const val VISIBILITY_FOODPANDAMALL = "FOODPANDAMALL"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_visibility)

        setupToolbar()
        initializeViews()
        initializeViewModel()
        setupRecyclerView()
        setupToggleButtons()
        setupVisibilityTypeChips()
        observeData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Product Visibility Management"
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
        tvResultsInfo = findViewById(R.id.tvResultsInfo)

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

    private fun setupVisibilityTypeChips() {
        val cardGeneral = findViewById<CardView>(R.id.cardGeneral)
        val cardFoodPanda = findViewById<CardView>(R.id.cardFoodPanda)
        val cardGrabFood = findViewById<CardView>(R.id.cardGrabFood)
        val cardManilaRate = findViewById<CardView>(R.id.cardManilaRate)
        val cardMallPrice = findViewById<CardView>(R.id.cardMallPrice)
        val cardGrabFoodMall = findViewById<CardView>(R.id.cardGrabFoodMall)
        val cardFoodPandaMall = findViewById<CardView>(R.id.cardFoodPandaMall)

        // Ensure all cards are enabled and clickable
        listOf(cardGeneral, cardFoodPanda, cardGrabFood, cardManilaRate, cardMallPrice, cardGrabFoodMall, cardFoodPandaMall).forEach { card ->
            card?.let {
                it.isEnabled = true
                it.isClickable = true
                it.isFocusable = true
            }
        }

        // Set initial selection
        updateCardSelection(cardGeneral, true)

        cardGeneral.setOnClickListener {
            Log.d("CardViewClick", "General/Purchase clicked")
            if (selectedVisibilityType != VISIBILITY_GENERAL) {
                selectedVisibilityType = VISIBILITY_GENERAL
                updateAllCardSelections(cardGeneral, cardFoodPanda, cardGrabFood, cardManilaRate, cardMallPrice, cardGrabFoodMall, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        cardFoodPanda.setOnClickListener {
            Log.d("CardViewClick", "FoodPanda clicked")
            if (selectedVisibilityType != VISIBILITY_FOODPANDA) {
                selectedVisibilityType = VISIBILITY_FOODPANDA
                updateAllCardSelections(cardFoodPanda, cardGeneral, cardGrabFood, cardManilaRate, cardMallPrice, cardGrabFoodMall, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        cardGrabFood.setOnClickListener {
            Log.d("CardViewClick", "GrabFood clicked")
            if (selectedVisibilityType != VISIBILITY_GRABFOOD) {
                selectedVisibilityType = VISIBILITY_GRABFOOD
                updateAllCardSelections(cardGrabFood, cardGeneral, cardFoodPanda, cardManilaRate, cardMallPrice, cardGrabFoodMall, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        cardManilaRate.setOnClickListener {
            Log.d("CardViewClick", "ManilaRate clicked")
            if (selectedVisibilityType != VISIBILITY_MANILARATE) {
                selectedVisibilityType = VISIBILITY_MANILARATE
                updateAllCardSelections(cardManilaRate, cardGeneral, cardFoodPanda, cardGrabFood, cardMallPrice, cardGrabFoodMall, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        // New visibility type buttons - prepared for future use
        cardMallPrice?.setOnClickListener {
            Log.d("CardViewClick", "MallPrice clicked")
            if (selectedVisibilityType != VISIBILITY_MALLPRICE) {
                selectedVisibilityType = VISIBILITY_MALLPRICE
                updateAllCardSelections(cardMallPrice, cardGeneral, cardFoodPanda, cardGrabFood, cardManilaRate, cardGrabFoodMall, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        cardGrabFoodMall?.setOnClickListener {
            Log.d("CardViewClick", "GrabFoodMall clicked")
            if (selectedVisibilityType != VISIBILITY_GRABFOODMALL) {
                selectedVisibilityType = VISIBILITY_GRABFOODMALL
                updateAllCardSelections(cardGrabFoodMall, cardGeneral, cardFoodPanda, cardGrabFood, cardManilaRate, cardMallPrice, cardFoodPandaMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }

        cardFoodPandaMall?.setOnClickListener {
            Log.d("CardViewClick", "FoodPandaMall clicked")
            if (selectedVisibilityType != VISIBILITY_FOODPANDAMALL) {
                selectedVisibilityType = VISIBILITY_FOODPANDAMALL
                updateAllCardSelections(cardFoodPandaMall, cardGeneral, cardFoodPanda, cardGrabFood, cardManilaRate, cardMallPrice, cardGrabFoodMall)
                updateToolbarTitle()
                loadProductsForVisibilityType()
            }
        }
    }

    private fun updateAllCardSelections(selectedCard: CardView, vararg otherCards: CardView?) {
        updateCardSelection(selectedCard, true)
        otherCards.forEach { card ->
            card?.let { updateCardSelection(it, false) }
        }
    }

    private fun updateCardSelection(card: CardView?, isSelected: Boolean) {
        card?.let {
            val textView = it.getChildAt(0) as TextView
            if (isSelected) {
                it.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                it.setCardBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
                textView.setTextColor(ContextCompat.getColor(this, android.R.color.black))
            }
        }
    }

    private fun updateToolbarTitle() {
        val title = when (selectedVisibilityType) {
            VISIBILITY_GENERAL -> "Purchase Product Visibility"
            VISIBILITY_FOODPANDA -> "FoodPanda Visibility"
            VISIBILITY_GRABFOOD -> "GrabFood Visibility"
            VISIBILITY_MANILARATE -> "Manila Rate Visibility"
            VISIBILITY_MALLPRICE -> "Mall Price Visibility"
            VISIBILITY_GRABFOODMALL -> "GrabFood Mall Visibility"
            VISIBILITY_FOODPANDAMALL -> "FoodPanda Mall Visibility"
            else -> "Product Visibility"
        }
        supportActionBar?.title = title
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

        loadProductsForVisibilityType()
    }

    private fun loadProductsForVisibilityType() {
        lifecycleScope.launch {
            showLoadingState(true)
            try {
                when (selectedVisibilityType) {
                    VISIBILITY_GENERAL -> loadGeneralVisibilityProducts()
                    VISIBILITY_FOODPANDA -> loadDeliveryPlatformProducts("foodpanda")
                    VISIBILITY_GRABFOOD -> loadDeliveryPlatformProducts("grabfood")
                    VISIBILITY_MANILARATE -> loadDeliveryPlatformProducts("manilarate")
                    VISIBILITY_MALLPRICE -> loadDeliveryPlatformProducts("mallprice")
                    VISIBILITY_GRABFOODMALL -> loadDeliveryPlatformProducts("grabfoodmall")
                    VISIBILITY_FOODPANDAMALL -> loadDeliveryPlatformProducts("foodpandamall")
                }
            } catch (e: Exception) {
                Log.e("ProductVisibility", "Error loading products", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(recyclerView, "Error loading products", Snackbar.LENGTH_SHORT).show()
                }
            } finally {
                showLoadingState(false)
            }
        }
    }

    private suspend fun loadGeneralVisibilityProducts() {
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
            Log.d("ProductVisibility", "Updated general products with visibility: ${allProductsWithVisibility.size}")
            withContext(Dispatchers.Main) {
                filterProducts()
            }
        }
    }

    private suspend fun loadDeliveryPlatformProducts(platform: String) {
        withContext(Dispatchers.Main) {
            tvResultsInfo.text = "Loading $platform products..."
        }

        val allProducts = productViewModel.allProducts.value ?: emptyList()

        // Filter products that have prices for the selected platform
        val platformProducts = allProducts.filter { product ->
            when (platform) {
                "foodpanda" -> product.foodpanda > 0.0
                "grabfood" -> product.grabfood > 0.0
                "manilarate" -> product.manilaprice > 0.0
                "mallprice" -> {
                    // For future implementation - when you add mallprice field to Product
                    // product.mallprice > 0.0
                    // For now, return empty list as placeholder
                    false
                }
                "grabfoodmall" -> {
                    // For future implementation - when you add grabfoodmall field to Product
                    // product.grabfoodmall > 0.0
                    // For now, return empty list as placeholder
                    false
                }
                "foodpandamall" -> {
                    // For future implementation - when you add foodpandamall field to Product
                    // product.foodpandamall > 0.0
                    // For now, return empty list as placeholder
                    false
                }
                else -> false
            }
        }

        // Get platform-specific hidden products
        val platformName = platform.uppercase()
        val hiddenProducts = try {
            productViewModel.getHiddenProductsForPlatform(platformName)
        } catch (e: Exception) {
            Log.e("ProductVisibility", "Error getting hidden products for $platformName, using general visibility", e)
            productViewModel.getHiddenProducts().value ?: emptyList()
        }
        val hiddenProductIds = hiddenProducts.map { it.productId }.toSet()

        allProductsWithVisibility = platformProducts.map { product ->
            ProductWithVisibility(
                product = product.copy(
                    price = when (platform) {
                        "foodpanda" -> product.foodpanda
                        "grabfood" -> product.grabfood
                        "manilarate" -> product.manilaprice
                        "mallprice" -> {
                            // For future implementation
                            // product.mallprice
                            0.0
                        }
                        "grabfoodmall" -> {
                            // For future implementation
                            // product.grabfoodmall
                            0.0
                        }
                        "foodpandamall" -> {
                            // For future implementation
                            // product.foodpandamall
                            0.0
                        }
                        else -> product.price
                    }
                ),
                isVisible = product.id !in hiddenProductIds
            )
        }

        Log.d("ProductVisibility", "Updated $platform products: ${allProductsWithVisibility.size}")
        withContext(Dispatchers.Main) {
            filterProducts()
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
        Log.d("ProductVisibility", "Filtering products - Query: '$query', Selected Category: '${selectedCategory?.name}', Visibility Type: '$selectedVisibilityType'")

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
        val platformText = when (selectedVisibilityType) {
            VISIBILITY_GENERAL -> "products"
            VISIBILITY_FOODPANDA -> "FoodPanda products"
            VISIBILITY_GRABFOOD -> "GrabFood products"
            VISIBILITY_MANILARATE -> "Manila Rate products"
            VISIBILITY_MALLPRICE -> "Mall Price products"
            VISIBILITY_GRABFOODMALL -> "GrabFood Mall products"
            VISIBILITY_FOODPANDAMALL -> "FoodPanda Mall products"
            else -> "products"
        }

        tvResultsInfo.text = if (filtered == total) {
            "$total $platformText"
        } else {
            "$filtered of $total $platformText"
        }
    }

    private fun toggleProductVisibility(product: Product, isEnabled: Boolean) {
        lifecycleScope.launch {
            try {
                showLoadingState(true)

                // Use platform-specific visibility for delivery platforms
                when (selectedVisibilityType) {
                    VISIBILITY_GENERAL -> {
                        if (isEnabled) {
                            productViewModel.showProduct(product.id)
                        } else {
                            productViewModel.hideProduct(product.id)
                        }
                    }
                    VISIBILITY_FOODPANDA -> {
                        try {
                            if (isEnabled) {
                                productViewModel.showProductOnPlatform(product.id, "FOODPANDA")
                            } else {
                                productViewModel.hideProductFromPlatform(product.id, "FOODPANDA")
                            }
                        } catch (e: Exception) {
                            Log.e("ProductVisibility", "Platform visibility not available, using general", e)
                            if (isEnabled) {
                                productViewModel.showProduct(product.id)
                            } else {
                                productViewModel.hideProduct(product.id)
                            }
                        }
                    }
                    VISIBILITY_GRABFOOD -> {
                        try {
                            if (isEnabled) {
                                productViewModel.showProductOnPlatform(product.id, "GRABFOOD")
                            } else {
                                productViewModel.hideProductFromPlatform(product.id, "GRABFOOD")
                            }
                        } catch (e: Exception) {
                            Log.e("ProductVisibility", "Platform visibility not available, using general", e)
                            if (isEnabled) {
                                productViewModel.showProduct(product.id)
                            } else {
                                productViewModel.hideProduct(product.id)
                            }
                        }
                    }
                    VISIBILITY_MANILARATE -> {
                        try {
                            if (isEnabled) {
                                productViewModel.showProductOnPlatform(product.id, "MANILARATE")
                            } else {
                                productViewModel.hideProductFromPlatform(product.id, "MANILARATE")
                            }
                        } catch (e: Exception) {
                            Log.e("ProductVisibility", "Platform visibility not available, using general", e)
                            if (isEnabled) {
                                productViewModel.showProduct(product.id)
                            } else {
                                productViewModel.hideProduct(product.id)
                            }
                        }
                    }
                }

                kotlinx.coroutines.delay(100)
                loadProductsForVisibilityType()

                val message = when (selectedVisibilityType) {
                    VISIBILITY_GENERAL -> if (isEnabled) "Product shown" else "Product hidden"
                    VISIBILITY_FOODPANDA -> if (isEnabled) "Product shown on FoodPanda" else "Product hidden from FoodPanda"
                    VISIBILITY_GRABFOOD -> if (isEnabled) "Product shown on GrabFood" else "Product hidden from GrabFood"
                    VISIBILITY_MANILARATE -> if (isEnabled) "Product shown on Manila Rate" else "Product hidden from Manila Rate"
                    else -> if (isEnabled) "Product shown" else "Product hidden"
                }

                withContext(Dispatchers.Main) {
                    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProductVisibility", "Error toggling product visibility", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(recyclerView, "Error updating product visibility", Snackbar.LENGTH_SHORT).show()
                }
            } finally {
                showLoadingState(false)
            }
        }
    }

    private fun toggleAllProducts(showAll: Boolean) {
        val currentList = adapter.currentList
        if (currentList.isEmpty()) return

        lifecycleScope.launch {
            try {
                showLoadingState(true)

                currentList.forEach { productWithVisibility ->
                    val product = productWithVisibility.product

                    when (selectedVisibilityType) {
                        VISIBILITY_GENERAL -> {
                            if (showAll) {
                                productViewModel.showProduct(product.id)
                            } else {
                                productViewModel.hideProduct(product.id)
                            }
                        }
                        VISIBILITY_FOODPANDA -> {
                            try {
                                if (showAll) {
                                    productViewModel.showProductOnPlatform(product.id, "FOODPANDA")
                                } else {
                                    productViewModel.hideProductFromPlatform(product.id, "FOODPANDA")
                                }
                            } catch (e: Exception) {
                                if (showAll) {
                                    productViewModel.showProduct(product.id)
                                } else {
                                    productViewModel.hideProduct(product.id)
                                }
                            }
                        }
                        VISIBILITY_GRABFOOD -> {
                            try {
                                if (showAll) {
                                    productViewModel.showProductOnPlatform(product.id, "GRABFOOD")
                                } else {
                                    productViewModel.hideProductFromPlatform(product.id, "GRABFOOD")
                                }
                            } catch (e: Exception) {
                                if (showAll) {
                                    productViewModel.showProduct(product.id)
                                } else {
                                    productViewModel.hideProduct(product.id)
                                }
                            }
                        }
                        VISIBILITY_MANILARATE -> {
                            try {
                                if (showAll) {
                                    productViewModel.showProductOnPlatform(product.id, "MANILARATE")
                                } else {
                                    productViewModel.hideProductFromPlatform(product.id, "MANILARATE")
                                }
                            } catch (e: Exception) {
                                if (showAll) {
                                    productViewModel.showProduct(product.id)
                                } else {
                                    productViewModel.hideProduct(product.id)
                                }
                            }
                        }
                    }
                }

                kotlinx.coroutines.delay(200)
                loadProductsForVisibilityType()

                val message = when (selectedVisibilityType) {
                    VISIBILITY_GENERAL -> if (showAll) "All products shown" else "All products hidden"
                    VISIBILITY_FOODPANDA -> if (showAll) "All products shown on FoodPanda" else "All products hidden from FoodPanda"
                    VISIBILITY_GRABFOOD -> if (showAll) "All products shown on GrabFood" else "All products hidden from GrabFood"
                    VISIBILITY_MANILARATE -> if (showAll) "All products shown on Manila Rate" else "All products hidden from Manila Rate"
                    else -> if (showAll) "All products shown" else "All products hidden"
                }

                withContext(Dispatchers.Main) {
                    Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProductVisibility", "Error toggling all products", e)
                withContext(Dispatchers.Main) {
                    Snackbar.make(recyclerView, "Error updating products", Snackbar.LENGTH_SHORT).show()
                }
            } finally {
                showLoadingState(false)
            }
        }
    }

    private fun showLoadingState(loading: Boolean) {
        isLoading = loading
        Log.d("LoadingState", "Setting loading state: $loading")

        if (loading) {
            tvResultsInfo.text = "Loading..."
        }
    }
}

data class ProductWithVisibility(
    val product: Product,
    val isVisible: Boolean
)