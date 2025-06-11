package com.example.possystembw.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.RetrofitClient
import com.example.possystembw.adapter.MixMatchItemsAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.DiscountRepository
import com.example.possystembw.data.RequestRepository
import com.example.possystembw.database.Discount
import com.example.possystembw.database.Product
import com.example.possystembw.ui.ViewModel.DiscountRequest
import com.example.possystembw.ui.ViewModel.DiscountViewModel
import com.example.possystembw.ui.ViewModel.DiscountViewModelFactory
import com.example.possystembw.ui.ViewModel.MixMatchItemRequest
import com.example.possystembw.ui.ViewModel.MixMatchLineGroupRequest
import com.example.possystembw.ui.ViewModel.MixMatchRequest
import com.example.possystembw.ui.ViewModel.ProductViewModel
import com.example.possystembw.ui.ViewModel.RequestItem
import kotlinx.coroutines.launch


import androidx.lifecycle.lifecycleScope
import com.example.possystembw.adapter.ProductSearchAdapter


class RequestActivity : AppCompatActivity() {
    private lateinit var requestRepository: RequestRepository
    lateinit var itemsAdapter: MixMatchItemsAdapter
    val selectedItems = mutableListOf<MixMatchItemRequest>()
    private lateinit var productViewModel: ProductViewModel
    private fun initializeViewModel() {
        productViewModel = ViewModelProvider(
            this,
            ProductViewModel.ProductViewModelFactory(application)
        )[ProductViewModel::class.java]
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)

        setupToolbar()
        setupRequestButtons()

    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Create Request"

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    //    private fun openLineVisibilityScreen() {
//        val intent = Intent(this, LineTransactionVisibilityActivity::class.java)
//        startActivity(intent)
//    }

    private fun openProductVisibilityScreen() {
        val intent = Intent(this, ProductVisibilityActivity::class.java)
        startActivity(intent)
    }

    private fun setupRequestButtons() {
        findViewById<Button>(R.id.btnManageVisibility).setOnClickListener {
            openProductVisibilityScreen()
        }
//        findViewById<Button>(R.id.btnManageVisibility1).setOnClickListener {
//            openLineVisibilityScreen()
//        }
        findViewById<Button>(R.id.btnAddItem).setOnClickListener {
            showAddItemDialog()
        }

        findViewById<Button>(R.id.btnEditItem).setOnClickListener {
            showEditItemDialog()
        }

        findViewById<Button>(R.id.btnUpdateItem).setOnClickListener {
            showUpdateItemDialog()
        }

        findViewById<Button>(R.id.btnCreateMixMatch).setOnClickListener {
            showCreateMixMatchDialog()
        }

        findViewById<Button>(R.id.btnAddDiscount).setOnClickListener {
            showAddDiscountDialog()
        }

        findViewById<Button>(R.id.btnEditDiscount).setOnClickListener {
            showEditDiscountDialog()
        }
    }
    private fun showHideShowDialog(product: Product) {
        lifecycleScope.launch {
            val isHidden = productViewModel.isProductHidden(product.id)
            val action = if (isHidden) "Show" else "Hide"

            AlertDialog.Builder(this@RequestActivity)
                .setTitle("$action Product")
                .setMessage("Do you want to $action '${product.itemName}'?")
                .setPositiveButton(action) { _, _ ->
                    if (isHidden) {
                        productViewModel.showProduct(product.id)
                        Toast.makeText(this@RequestActivity, "Product shown", Toast.LENGTH_SHORT).show()
                    } else {
                        productViewModel.hideProduct(product.id)
                        Toast.makeText(this@RequestActivity, "Product hidden", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // Method to show list of hidden products with option to unhide
    private fun showHiddenProductsDialog() {
        lifecycleScope.launch {
            productViewModel.getHiddenProducts().collect { hiddenVisibilities ->
                if (hiddenVisibilities.isEmpty()) {
                    Toast.makeText(this@RequestActivity, "No hidden products", Toast.LENGTH_SHORT).show()
                    return@collect
                }

                val hiddenProductNames = mutableListOf<String>()
                val hiddenProducts = mutableListOf<Product>()

                hiddenVisibilities.forEach { visibility ->
                    val product = productViewModel.getProductById(visibility.productId)
                    product?.let {
                        hiddenProductNames.add(it.itemName)
                        hiddenProducts.add(it)
                    }
                }

                AlertDialog.Builder(this@RequestActivity)
                    .setTitle("Hidden Products")
                    .setItems(hiddenProductNames.toTypedArray()) { _, which ->
                        val selectedProduct = hiddenProducts[which]
                        showUnhideConfirmDialog(selectedProduct)
                    }
                    .setNegativeButton("Close", null)
                    .show()
            }
        }
    }

    private fun showUnhideConfirmDialog(product: Product) {
        AlertDialog.Builder(this)
            .setTitle("Show Product")
            .setMessage("Do you want to show '${product.itemName}'?")
            .setPositiveButton("Show") { _, _ ->
                productViewModel.showProduct(product.id)
                Toast.makeText(this, "Product is now visible", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    // Dialog methods will be implemented next
    private fun showAddItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_item, null)
        val itemNameInput = dialogView.findViewById<EditText>(R.id.etItemName)
        val itemGroupInput = dialogView.findViewById<EditText>(R.id.etItemGroup)
        val priceInput = dialogView.findViewById<EditText>(R.id.etPrice)
        val costInput = dialogView.findViewById<EditText>(R.id.etCost)

        AlertDialog.Builder(this)
            .setTitle("Add New Item")
            .setView(dialogView)
            .setPositiveButton("Submit Request") { dialog, _ ->
                // Validate inputs
                if (itemNameInput.text.isNotEmpty() && priceInput.text.isNotEmpty()) {
                    // Create item request object
                    val newItemRequest = RequestItem(
                        itemName = itemNameInput.text.toString(),
                        itemGroup = itemGroupInput.text.toString(),
                        price = priceInput.text.toString().toDoubleOrNull() ?: 0.0,
                        cost = costInput.text.toString().toDoubleOrNull() ?: 0.0,
                        requestType = "add_item",
                        status = "pending"
                    )

                    // Submit request
                    submitItemRequest(newItemRequest)
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditItemDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_item, null)

        val searchInput = dialogView.findViewById<EditText>(R.id.etSearchItem)
        val productsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.rvProducts)

        // Set up product list
        val productViewModel = ViewModelProvider(
            this,
            ProductViewModel.ProductViewModelFactory(application)
        )[ProductViewModel::class.java]

        // Setup recycler view with adapter
        val productsAdapter = ProductSearchAdapter { product ->
            // When a product is selected from the list, show details dialog
            showEditItemDetailsDialog(product)
        }

        productsRecyclerView.layoutManager = LinearLayoutManager(this)
        productsRecyclerView.adapter = productsAdapter

        // Initial load of all products
        productViewModel.allProducts.observe(this) { products ->
            productsAdapter.submitList(products)
        }

        // Search functionality
        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isEmpty()) {
                    // If search is empty, show all products
                    productViewModel.allProducts.value?.let {
                        productsAdapter.submitList(it)
                    }
                } else {
                    // Filter products based on search query
                    productViewModel.allProducts.value?.let { allProducts ->
                        val filteredProducts = allProducts.filter { product ->
                            product.itemName.contains(query, ignoreCase = true) ||
                                    product.itemid.contains(query, ignoreCase = true) ||
                                    product.itemGroup.contains(query, ignoreCase = true)
                        }
                        productsAdapter.submitList(filteredProducts)
                    }
                }
            }
        })

        AlertDialog.Builder(this)
            .setTitle("Search Item")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showEditItemDetailsDialog(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_item, null)

        // Initialize fields with product data
        dialogView.findViewById<EditText>(R.id.etItemName).setText(product.itemName)
        dialogView.findViewById<EditText>(R.id.etItemGroup).setText(product.itemGroup)
        dialogView.findViewById<EditText>(R.id.etPrice).setText(product.price.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Item Details")
            .setView(dialogView)
            .setPositiveButton("Submit Request") { dialog, _ ->
                // Get updated fields
                val updatedName = dialogView.findViewById<EditText>(R.id.etItemName).text.toString()
                val updatedGroup = dialogView.findViewById<EditText>(R.id.etItemGroup).text.toString()
                val updatedPrice = dialogView.findViewById<EditText>(R.id.etPrice).text.toString().toDoubleOrNull() ?: product.price

                // Validate inputs
                if (updatedName.isEmpty() || updatedGroup.isEmpty()) {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // Create edit request
                val editRequest = RequestItem(
                    itemId = product.itemid,
                    itemName = updatedName,
                    itemGroup = updatedGroup,
                    price = updatedPrice,
                    cost = product.cost, // Keep the original cost
                    requestType = "edit_item",
                    status = "pending"
                )

                submitItemRequest(editRequest)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun showUpdateItemDialog() {
        // Implementation coming in next steps
    }

    private fun showCreateMixMatchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_mix_match_unified, null)

        // Basic info section
        val descriptionInput = dialogView.findViewById<EditText>(R.id.etDescription)
        val discountTypeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerDiscountType)
        val valueInput = dialogView.findViewById<EditText>(R.id.etDiscountValue)
        val itemsNeededInput = dialogView.findViewById<EditText>(R.id.etItemsNeeded)

        // Items section
        val searchInput = dialogView.findViewById<EditText>(R.id.etSearchItem)
        val selectedItemsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.rvSelectedItems)
        val tvTotalBeforeDiscount = dialogView.findViewById<TextView>(R.id.tvTotalBeforeDiscount)
        val tvTotalAfterDiscount = dialogView.findViewById<TextView>(R.id.tvTotalAfterDiscount)
        val tvDiscountSummary = dialogView.findViewById<TextView>(R.id.tvDiscountSummary)

        // Set up adapter for discount type spinner
        val discountTypes = arrayOf("Deal Price", "Percentage Discount", "Amount Discount")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, discountTypes)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        discountTypeSpinner.adapter = spinnerAdapter

        // Create adapter for selected items
        val selectedItems = mutableListOf<MixMatchItemRequest>()
        lateinit var itemsAdapter: MixMatchItemsAdapter

        itemsAdapter = MixMatchItemsAdapter(
            selectedItems,
            onRemoveClick = { item ->
                selectedItems.remove(item)
                itemsAdapter.notifyDataSetChanged()
                updateTotals(selectedItems, discountTypeSpinner.selectedItemPosition,
                    valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                    tvTotalBeforeDiscount, tvTotalAfterDiscount, tvDiscountSummary)
            },
            onQuantityChanged = { _, _ ->
                updateTotals(selectedItems, discountTypeSpinner.selectedItemPosition,
                    valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                    tvTotalBeforeDiscount, tvTotalAfterDiscount, tvDiscountSummary)
            }
        )

        selectedItemsRecyclerView.layoutManager = LinearLayoutManager(this)
        selectedItemsRecyclerView.adapter = itemsAdapter

        // Set up change listeners for automatic total recalculation
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateTotals(selectedItems, discountTypeSpinner.selectedItemPosition,
                    valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                    tvTotalBeforeDiscount, tvTotalAfterDiscount, tvDiscountSummary)
            }
        }

        valueInput.addTextChangedListener(textWatcher)

        discountTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateTotals(selectedItems, position,
                    valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                    tvTotalBeforeDiscount, tvTotalAfterDiscount, tvDiscountSummary)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Get the product view model
        val productViewModel = ViewModelProvider(
            this,
            ProductViewModel.ProductViewModelFactory(application)
        )[ProductViewModel::class.java]

        // Create the products recycler view (similar to Edit Item functionality)
        val productsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.rvProducts)
        val productsAdapter = ProductSearchAdapter { product ->
            // Check if product already exists in the list
            val existingItem = selectedItems.find { it.itemId == product.itemid }
            if (existingItem != null) {
                // Increment quantity instead of adding duplicate
                val index = selectedItems.indexOf(existingItem)
                val updatedItem = existingItem.copy(qty = existingItem.qty + 1)
                selectedItems[index] = updatedItem
            } else {
                // Add new product
                val mixMatchItem = MixMatchItemRequest(
                    itemId = product.itemid,
                    itemName = product.itemName,
                    qty = 1, // Default quantity
                    price = product.price
                )
                selectedItems.add(mixMatchItem)
            }

            itemsAdapter.notifyDataSetChanged()
            updateTotals(selectedItems, discountTypeSpinner.selectedItemPosition,
                valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                tvTotalBeforeDiscount, tvTotalAfterDiscount, tvDiscountSummary)
        }

        productsRecyclerView.layoutManager = LinearLayoutManager(this)
        productsRecyclerView.adapter = productsAdapter

        // Initial load of all products
        productViewModel.allProducts.observe(this) { products ->
            productsAdapter.submitList(products)
        }

        // Search functionality - just like in Edit Item
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                if (query.isEmpty()) {
                    // If search is empty, show all products
                    productViewModel.allProducts.value?.let {
                        productsAdapter.submitList(it)
                    }
                } else {
                    // Filter products based on search query
                    productViewModel.allProducts.value?.let { allProducts ->
                        val filteredProducts = allProducts.filter { product ->
                            product.itemName.contains(query, ignoreCase = true) ||
                                    product.itemid.contains(query, ignoreCase = true) ||
                                    product.itemGroup.contains(query, ignoreCase = true)
                        }
                        productsAdapter.submitList(filteredProducts)
                    }
                }
            }
        })

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Mix & Match Promotion")
            .setView(dialogView)
            .setPositiveButton("Submit Request", null) // Will override this below
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Override the positive button click to add validation
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (descriptionInput.text.isEmpty() || valueInput.text.isEmpty() || itemsNeededInput.text.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Please add at least one item", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get selected discount type value
            val discountType = when (discountTypeSpinner.selectedItemPosition) {
                0 -> 1 // Deal Price
                1 -> 2 // Percentage Discount
                2 -> 3 // Amount Discount
                else -> 1
            }

            // Create mix match object
            val mixMatch = MixMatchRequest(
                description = descriptionInput.text.toString(),
                discountType = discountType,
                discountValue = valueInput.text.toString().toDoubleOrNull() ?: 0.0,
                itemsNeeded = itemsNeededInput.text.toString().toIntOrNull() ?: 0,
                lineGroups = mutableListOf(), // Will be filled next
                requestType = "create_mix_match",
                status = "pending"
            )

            // Add items to the mix match request
            val lineGroup = MixMatchLineGroupRequest(
                lineGroup = "1", // Default line group
                description = "Default group",
                noOfItemsNeeded = mixMatch.itemsNeeded,
                discountLines = selectedItems
            )

            mixMatch.lineGroups.add(lineGroup)

            // Submit the request
            submitMixMatchRequest(mixMatch)
            dialog.dismiss()
        }
    }
    private fun showProductSelectionDialog(
        products: List<Product>,
        onItemSelected: (Product) -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_select_product, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvProducts)

        // Setup adapter for products
        val adapter = ProductSearchAdapter(onItemSelected)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        adapter.submitList(products)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Select Product")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .create()

        // Setup item click listener that will close the dialog
        adapter.setOnItemClickListener { product ->
            onItemSelected(product)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Helper function to update totals
    private fun updateTotals(
        items: List<MixMatchItemRequest>,
        discountTypePosition: Int,
        discountValue: Double,
        tvTotalBeforeDiscount: TextView,
        tvTotalAfterDiscount: TextView,
        tvDiscountSummary: TextView
    ) {
        if (items.isEmpty()) {
            tvTotalBeforeDiscount.text = "Total: ₱0.00"
            tvTotalAfterDiscount.text = "After Discount: ₱0.00"
            tvDiscountSummary.text = "Discount: None"
            return
        }

        // Calculate total before discount
        val totalBeforeDiscount = items.sumOf { it.price * it.qty }
        tvTotalBeforeDiscount.text = String.format("Total: ₱%.2f", totalBeforeDiscount)

        // Calculate total after discount based on discount type
        val totalAfterDiscount = when (discountTypePosition) {
            0 -> { // Deal Price
                if (discountValue > 0) discountValue else totalBeforeDiscount
            }
            1 -> { // Percentage Discount
                val percentage = if (discountValue > 100) 100.0 else discountValue
                totalBeforeDiscount * (1 - percentage / 100)
            }
            2 -> { // Amount Discount
                val discount = if (discountValue > totalBeforeDiscount) totalBeforeDiscount else discountValue
                totalBeforeDiscount - discount
            }
            else -> totalBeforeDiscount
        }

        tvTotalAfterDiscount.text = String.format("After Discount: ₱%.2f", totalAfterDiscount)

        // Display discount summary
        val discountAmount = totalBeforeDiscount - totalAfterDiscount
        val discountSummary = when (discountTypePosition) {
            0 -> String.format("Deal Price: ₱%.2f (Save ₱%.2f)", discountValue, discountAmount)
            1 -> String.format("Discount: %.0f%% (Save ₱%.2f)", discountValue, discountAmount)
            2 -> String.format("Discount: ₱%.2f", discountAmount)
            else -> "No discount"
        }

        tvDiscountSummary.text = discountSummary
    }

    private fun showItemSelectionDialogForMixMatch(
        products: List<Product>,
        onItemSelected: (Product) -> Unit
    ) {
        val items = products.map { it.itemName }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Item")
            .setItems(items) { dialog, which ->
                onItemSelected(products[which])
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddDiscountDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_discount, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.etDiscountName)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerDiscountType)
        val valueInput = dialogView.findViewById<EditText>(R.id.etDiscountValue)

        // Set up adapter for discount type spinner
        val discountTypes = arrayOf("Percentage", "Fixed Amount")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, discountTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Add New Discount")
            .setView(dialogView)
            .setPositiveButton("Submit Request") { dialog, _ ->
                if (nameInput.text.isNotEmpty() && valueInput.text.isNotEmpty()) {
                    // Create discount request
                    val discountRequest = DiscountRequest(
                        discountName = nameInput.text.toString(),
                        discountType = if (typeSpinner.selectedItemPosition == 0) "percentage" else "fixed",
                        parameter = valueInput.text.toString().toIntOrNull() ?: 0,
                        requestType = "add_discount",
                        status = "pending"
                    )

                    submitDiscountRequest(discountRequest)
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showEditDiscountDialog() {
        // First show a dialog to select the discount
        val discountRepository = DiscountRepository(
            RetrofitClient.discountApiService,
            AppDatabase.getDatabase(application).discountDao()
        )

        val discountViewModel = ViewModelProvider(
            this,
            DiscountViewModelFactory(discountRepository)
        )[DiscountViewModel::class.java]

        discountViewModel.fetchDiscounts()

        discountViewModel.discounts.observe(this) { discounts ->
            if (discounts.isNotEmpty()) {
                val items = discounts.map { it.DISCOFFERNAME }.toTypedArray()

                AlertDialog.Builder(this)
                    .setTitle("Select Discount to Edit")
                    .setItems(items) { dialog, which ->
                        showEditDiscountDetailsDialog(discounts[which])
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                Toast.makeText(this, "No discounts found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDiscountDetailsDialog(discount: Discount) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_discount, null)

        val nameInput = dialogView.findViewById<EditText>(R.id.etDiscountName)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.spinnerDiscountType)
        val valueInput = dialogView.findViewById<EditText>(R.id.etDiscountValue)

        // Set up adapter for discount type spinner
        val discountTypes = arrayOf("Percentage", "Fixed Amount")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, discountTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        // Initialize fields with discount data
        nameInput.setText(discount.DISCOFFERNAME)
        valueInput.setText(discount.PARAMETER.toString())
        typeSpinner.setSelection(if (discount.DISCOUNTTYPE == "percentage") 0 else 1)

        AlertDialog.Builder(this)
            .setTitle("Edit Discount")
            .setView(dialogView)
            .setPositiveButton("Submit Request") { dialog, _ ->
                if (nameInput.text.isNotEmpty() && valueInput.text.isNotEmpty()) {
                    // Create edit discount request
                    val discountRequest = DiscountRequest(
                        id = discount.id,
                        discountName = nameInput.text.toString(),
                        discountType = if (typeSpinner.selectedItemPosition == 0) "percentage" else "fixed",
                        parameter = valueInput.text.toString().toIntOrNull() ?: 0,
                        requestType = "edit_discount",
                        status = "pending"
                    )

                    submitDiscountRequest(discountRequest)
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Request submission methods
    private fun submitItemRequest(request: RequestItem) {
        lifecycleScope.launch {
            try {
                val result = requestRepository.submitItemRequest(request)
                result.onSuccess {
                    Toast.makeText(
                        this@RequestActivity,
                        "Request submitted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun submitMixMatchRequest(request: MixMatchRequest) {
        lifecycleScope.launch {
            try {
                val result = requestRepository.submitMixMatchRequest(request)
                result.onSuccess {
                    Toast.makeText(
                        this@RequestActivity,
                        "Mix & Match request submitted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun submitDiscountRequest(request: DiscountRequest) {
        lifecycleScope.launch {
            try {
                val result = requestRepository.submitDiscountRequest(request)
                result.onSuccess {
                    Toast.makeText(
                        this@RequestActivity,
                        "Discount request submitted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@RequestActivity, "Error: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}