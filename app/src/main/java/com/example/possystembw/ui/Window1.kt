package com.example.possystembw.ui

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.possystembw.R
import com.example.possystembw.adapter.ProductAdapter
import com.example.possystembw.adapter.CartAdapter
import com.example.possystembw.ui.ViewModel.ProductViewModel
import com.example.possystembw.ui.ViewModel.CartViewModel
import com.example.possystembw.ui.ViewModel.CartViewModelFactory
import com.example.possystembw.database.Product
import com.example.possystembw.database.CartItem
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.possystembw.AutoDatabaseTransferManager
import com.example.possystembw.DAO.TransactionDao
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.CashFundRepository
import com.example.possystembw.database.Cashfund
import com.example.possystembw.database.TransactionRecord
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.possystembw.RetrofitClient
import com.example.possystembw.adapter.CategoryAdapter
import com.example.possystembw.adapter.DiscountAdapter
import com.example.possystembw.adapter.PriceOverrideAdapter
import com.example.possystembw.adapter.TransactionAdapter
import com.example.possystembw.data.CartRepository
import com.example.possystembw.data.CategoryRepository
import com.example.possystembw.data.DiscountRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.Category
import com.example.possystembw.database.Discount
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.databinding.ActivityWindow1Binding
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import com.example.possystembw.ui.ViewModel.CategoryViewModel
import com.example.possystembw.ui.ViewModel.CategoryViewModelFactory
import com.example.possystembw.ui.ViewModel.DiscountViewModel
import com.example.possystembw.ui.ViewModel.DiscountViewModelFactory
import com.example.possystembw.ui.ViewModel.TransactionViewModel
import com.example.possystembw.ui.ViewModel.WindowViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import android.Manifest
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.SearchView // Change this import
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.recyclerview.widget.ItemTouchHelper
import com.example.possystembw.DAO.TenderDeclarationDao
import com.example.possystembw.DAO.ZReadDao
import com.example.possystembw.adapter.LineGroupAdapter
import com.example.possystembw.adapter.MixMatchAdapter
import com.example.possystembw.adapter.TransactionItemsAdapter
import com.example.possystembw.data.ARRepository
import com.example.possystembw.data.CustomerRepository
import com.example.possystembw.data.MixMatchRepository
import com.example.possystembw.data.NumberSequenceRepository
import com.example.possystembw.database.Customer
import com.example.possystembw.database.LineGroupWithDiscounts
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchWithDetails
import com.example.possystembw.database.ProductBundle
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.ZRead
import com.example.possystembw.ui.ViewModel.ARViewModel
import com.example.possystembw.ui.ViewModel.ARViewModelFactory
import com.example.possystembw.ui.ViewModel.CustomerViewModel
import com.example.possystembw.ui.ViewModel.CustomerViewModelFactory
import com.example.possystembw.ui.ViewModel.MixMatchViewModel
import com.example.possystembw.ui.ViewModel.MixMatchViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import java.util.UUID
import kotlin.math.roundToInt


class Window1 : AppCompatActivity() {
    private lateinit var binding: ActivityWindow1Binding
    private lateinit var autoDatabaseTransferManager: AutoDatabaseTransferManager
    private lateinit var productViewModel: ProductViewModel
    private lateinit var cartViewModel: CartViewModel
    private lateinit var arViewModel: ARViewModel
    private lateinit var customerViewModel: CustomerViewModel
    private val TAG = "Window1"
    private var windowId: Int = -1
    private lateinit var transactionDao: TransactionDao
    private lateinit var cashFundRepository: CashFundRepository
    private lateinit var database: AppDatabase
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var windowViewModel: WindowViewModel
    private lateinit var productAdapter: ProductAdapter
    private lateinit var discountViewModel: DiscountViewModel
    private var partialPaymentAmount: Double = 0.0
    private var isPartialPaymentJustApplied: Boolean = false
    private var transactionComment: String = ""
    private var lastTransactionId = 0
    private var lastReceiptNumber = 0
    private var lastTransactionNumber = 0
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var transactionAdapter: TransactionAdapter
    private val BLUETOOTH_PERMISSION_REQUEST_CODE = 1
    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private lateinit var zReadButton: Button
    private lateinit var xReadButton: Button
    private var lastZReadTime: Long = 0
    private var isPulloutCashFundProcessed = false
    private var isTenderDeclarationProcessed = false
    private var tenderDeclaration: TenderDeclaration? = null
    private var currentCashFund: Double = 0.0
    private var isZReadPerformed: Boolean = false
    private lateinit var refreshJob: Job
    private lateinit var viewTransactionButton: Button
    private lateinit var returnTransactionButton: Button
    private lateinit var viewModel: TransactionViewModel
    private var isCashFundEntered = false
    private var partialPaymentApplied: Boolean = false
    private lateinit var tenderDeclarationDao: TenderDeclarationDao
    private var returnDialog: AlertDialog? = null
    private var isObserving = false
    private var transactionDetailsDialog: AlertDialog? = null
    private var transactionItemsObserver: Observer<List<TransactionRecord>>? = null
    private lateinit var searchView: SearchView
    private var currentBundle: ProductBundle? = null
    private var currentSelections = mutableMapOf<Int, Product>()
    private lateinit var mixMatchViewModel: MixMatchViewModel
    private val selectedItems = mutableMapOf<String, MutableList<CartItem>>()
    private lateinit var progressDialog: AlertDialog
    private lateinit var zReadDao: ZReadDao
    private lateinit var numberSequenceRepository: NumberSequenceRepository






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWindow1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            initializeDatabase()
            getWindowId()
            initializeRepositories()
            initializeViewModels()
            setupRecyclerViews()
            observeViewModels()
            setupButtonListeners()
            initializeAutoDatabaseTransferManager()
            checkForExistingCashFund()
            setupButtonListeners()
            setupOverlay()
            setupPartialPaymentDisplay()
            setupCommentButton()
            setupDeleteCommentButton()
            setupCommentHandling()
            setupTransactionView()
            checkBluetoothPermissions()
            initializeZXReadButtons()
            loadLastTransactionNumber()
            setupCashManagementButtons()


            bluetoothPrinterHelper = BluetoothPrinterHelper(this)
            setupViewModel()
            setupButtons()
            connectToPrinter()
            setupSearchView()
            productViewModel.selectCategory(null) // Reset category filter
            initializeProgressDialog()
            updateCategoriesAndRecreate()
            observeCategoriesAndProducts()
            initializeMixMatch()
            transactionViewModel.startAutoSync(this)
            zReadDao = database.zReadDao()
            transactionDao = database.transactionDao()
            tenderDeclarationDao = database.tenderDeclarationDao()
            setupSequenceButton()

//            setupNumberSequenceViewModel()


//            setupMixMatchButton() // Add this line


        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate", e)
            Toast.makeText(this, "Initialization Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }


    private fun initializeDatabase() {
        database = AppDatabase.getDatabase(this)
        tenderDeclarationDao = database.tenderDeclarationDao()

    }


    private fun showTransactionListDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_list, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.transactionRecyclerView)
        val searchEditText = dialogView.findViewById<EditText>(R.id.searchEditText)
        val searchButton = dialogView.findViewById<ImageButton>(R.id.searchButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        recyclerView.layoutManager = LinearLayoutManager(this)

        val transactionAdapter = TransactionAdapter { transaction ->
            showTransactionDetailsDialog(transaction)
        }
        recyclerView.adapter = transactionAdapter

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                transactionAdapter.filter(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        searchButton.setOnClickListener {
            transactionAdapter.filter(searchEditText.text.toString())
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setView(dialogView)
            .create()

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.dialog_background)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        transactionViewModel.transactions.observe(this) { transactions ->
            transactionAdapter.setTransactions(transactions)
        }
        transactionViewModel.loadTransactions()

        dialog.show()
    }

    private fun showTransactionDetailsDialog(transaction: TransactionSummary) {
        Log.d(
            TAG,
            "Showing transaction details dialog for transaction ID: ${transaction.transactionId}"
        )

        transactionDetailsDialog?.dismiss()

        val dialogView = layoutInflater.inflate(R.layout.dialog_transaction_details, null)
        val detailsTextView = dialogView.findViewById<TextView>(R.id.transactionDetailsTextView)
        val itemsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.itemsRecyclerView)
        val printButton = dialogView.findViewById<Button>(R.id.printButton)
        val returnButton = dialogView.findViewById<Button>(R.id.returnButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        // Load transaction items
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    transactionViewModel.getTransactionItems(transaction.transactionId)
                }

                // Header details
                val headerDetails = buildString {
                    append("Transaction ID: ${transaction.transactionId}\n")
                    append("Receipt ID: ${transaction.receiptId}\n")
                    append("Staff: ${transaction.staff}\n")
                    append("Store: ${transaction.store}\n")
                    append("\n${"-".repeat(40)}\n\n")
                }

                // Items details
                val itemsDetails = buildString {
                    items.forEach { item ->
                        append("${item.name}\n")
                        append("   ${item.quantity} x ₱${String.format("%.2f", item.price)}")
                        append(" = ₱${String.format("%.2f", item.netAmount)}\n")
                    }
                    append("\n${"-".repeat(40)}\n\n")
                }

                // Payment details
                val paymentDetails = buildString {
                    append("Gross Amount: ₱${String.format("%.2f", transaction.grossAmount)}\n")
                    if (transaction.discountAmount > 0) {
                        append("Discount: ₱${String.format("%.2f", transaction.discountAmount)}\n")
                    }
                    append("VAT Amount: ₱${String.format("%.2f", transaction.vatAmount)}\n")
                    if (transaction.partialPayment > 0) {
                        append(
                            "Partial Payment: ₱${
                                String.format(
                                    "%.2f",
                                    transaction.partialPayment
                                )
                            }\n"
                        )
                    }
                    append("\n${"-".repeat(40)}\n\n")
                    append("Total Amount: ₱${String.format("%.2f", transaction.netAmount)}\n")
                    append("\n${"-".repeat(40)}\n\n")
                    append("Payment Method: ${transaction.paymentMethod}\n")
                    when (transaction.paymentMethod) {
                        "CASH" -> append(
                            "Cash Amount: ₱${
                                String.format(
                                    "%.2f",
                                    transaction.cash
                                )
                            }\n"
                        )

                        "GCASH" -> append(
                            "GCash Amount: ₱${
                                String.format(
                                    "%.2f",
                                    transaction.gCash
                                )
                            }\n"
                        )

                        "PAYMAYA" -> append(
                            "PayMaya Amount: ₱${
                                String.format(
                                    "%.2f",
                                    transaction.payMaya
                                )
                            }\n"
                        )

                        "CARD" -> append(
                            "Card Amount: ₱${
                                String.format(
                                    "%.2f",
                                    transaction.card
                                )
                            }\n"
                        )
                    }
                }

                detailsTextView.text = headerDetails + itemsDetails + paymentDetails
            } catch (e: Exception) {
                Log.e(TAG, "Error loading transaction details", e)
                Toast.makeText(
                    this@Window1,
                    "Error loading transaction details: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        transactionDetailsDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Transaction Details")
            .setView(dialogView)
            .create()

        transactionDetailsDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        // Set up button click listeners
        printButton.setOnClickListener {
            printReceiptWithItems(transaction)
            transactionDetailsDialog?.dismiss()
        }

        returnButton.setOnClickListener {
            transactionDetailsDialog?.dismiss()
            showReturnTransactionDialog(transaction)
        }

        closeButton.setOnClickListener {
            transactionDetailsDialog?.dismiss()
        }

        transactionDetailsDialog?.show()
    }

    private fun printReceiptWithItems(transaction: TransactionSummary) {
        lifecycleScope.launch {
            try {
                val items = withContext(Dispatchers.IO) {
                    transactionViewModel.getTransactionItems(transaction.transactionId)
                }

                if (items.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "No items found for this transaction",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val printerMacAddress =
                    "DC:0D:30:70:09:19"  // Replace with your printer's MAC address

                if (!bluetoothPrinterHelper.isConnected() && !bluetoothPrinterHelper.connect(
                        printerMacAddress
                    )
                ) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Failed to connect to printer",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Generate the receipt content using the new format
                val receiptContent = bluetoothPrinterHelper.generateReceiptContent(
                    transaction,
                    items,
                    BluetoothPrinterHelper.ReceiptType.REPRINT // or REPRINT
                )
                // Write the content to the printer
                bluetoothPrinterHelper.outputStream?.write(receiptContent.toByteArray())
                bluetoothPrinterHelper.outputStream?.flush()

                // Cut the paper
                bluetoothPrinterHelper.cutPaper()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Receipt reprinted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reprinting receipt", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error reprinting receipt: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun setupTransactionView() {
        val transactionRepository = TransactionRepository(database.transactionDao())
        val factory = TransactionViewModel.TransactionViewModelFactory(transactionRepository)
        transactionViewModel =
            ViewModelProvider(this, factory).get(TransactionViewModel::class.java)

        transactionAdapter = TransactionAdapter { transaction ->
            showTransactionDetailsDialog(transaction)
        }

    }

    private fun showReturnTransactionDialog(transaction: TransactionSummary) {
        Log.d(
            TAG,
            "Showing return transaction dialog for transaction ID: ${transaction.transactionId}"
        )

        // Dismiss any existing dialog
        returnDialog?.dismiss()
        returnDialog = null

        lifecycleScope.launch {
            try {
                // Show a loading indicator
                val loadingDialog = AlertDialog.Builder(this@Window1)
                    .setMessage("Loading transaction items...")
                    .setCancelable(false)
                    .create()
                loadingDialog.show()

                // Load transaction items
                val items = withContext(Dispatchers.IO) {
                    transactionViewModel.getTransactionItems(transaction.transactionId)
                }

                loadingDialog.dismiss()

                // Filter out already returned items and items with quantity <= 0
                val returnable = items.filter { item -> item.quantity > 0 && !item.isReturned }

                if (returnable.isEmpty()) {
                    Toast.makeText(
                        this@Window1,
                        "No items available for return in this transaction",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val dialogView = layoutInflater.inflate(R.layout.dialog_return_transaction, null)
                val recyclerView =
                    dialogView.findViewById<RecyclerView>(R.id.returnItemsRecyclerView)
                val remarksEditText =
                    dialogView.findViewById<TextInputEditText>(R.id.remarksEditText)
                val returnButton = dialogView.findViewById<Button>(R.id.returnButton)
                val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
                val warningTextView = dialogView.findViewById<TextView>(R.id.warningTextView)

                // Check for partial payment
                val hasPartialPayment = returnable.any { it.partialPaymentAmount > 0 }

                // Show warning if partial payment exists
                if (hasPartialPayment) {
                    warningTextView.visibility = View.VISIBLE
                    val partialAmount = returnable.sumOf { it.partialPaymentAmount }
                    warningTextView.text = "This transaction has a partial payment of ₱${
                        String.format(
                            "%.2f",
                            partialAmount
                        )
                    }. All items must be returned together."
                }

                val adapter = TransactionItemsAdapter(
                    items = returnable,
                    onItemSelected = { item, isSelected ->
                        updateReturnButtonState(
                            returnButton,
                            getSelectedItems(returnable)
                        )
                    }
                )

                recyclerView.layoutManager = LinearLayoutManager(this@Window1)
                recyclerView.adapter = adapter

                // If there's a partial payment, update the return button state immediately
                if (hasPartialPayment) {
                    updateReturnButtonState(returnButton, returnable)
                } else {
                    updateReturnButtonState(returnButton, emptyList())
                }

                returnDialog = AlertDialog.Builder(this@Window1, R.style.CustomDialogStyle)
                    .setView(dialogView)
                    .create()

                returnDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

                returnButton.setOnClickListener {
                    processReturn(
                        returnDialog!!,
                        transaction,
                        returnable,
                        remarksEditText,
                        returnButton
                    )
                }

                cancelButton.setOnClickListener {
                    returnDialog?.dismiss()
                }

                returnDialog?.show()
                Log.d(TAG, "Return dialog shown for transaction ${transaction.transactionId}")

            } catch (e: Exception) {
                Log.e(TAG, "Error loading transaction items", e)
                Toast.makeText(
                    this@Window1,
                    "Error loading transaction items: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updateReturnButtonState(button: Button, selectedItems: List<TransactionRecord>) {
        button.isEnabled = selectedItems.isNotEmpty()
    }

    private fun getSelectedItems(items: List<TransactionRecord>): List<TransactionRecord> {
        return items.filter { it.isSelected }
    }

    private fun updateReturnButtonText(button: Button, selectedItems: List<TransactionRecord>) {
        button.text = "Process Return"
    }

    private fun processReturn(
        returnDialog: AlertDialog,
        transaction: TransactionSummary,
        returnable: List<TransactionRecord>,
        remarksEditText: TextInputEditText,
        returnButton: Button
    ) {
        val selectedItems = getSelectedItems(returnable)
        val remarks = remarksEditText.text.toString().trim()

        when {
            selectedItems.isEmpty() -> {
                Toast.makeText(this, "Please select items to return", Toast.LENGTH_SHORT).show()
            }

            remarks.isEmpty() -> {
                remarksEditText.error = "Remarks are required"
            }

            else -> {
                lifecycleScope.launch {
                    try {
                        // Show loading dialog
                        val loadingDialog = AlertDialog.Builder(this@Window1)
                            .setMessage("Processing return...")
                            .setCancelable(false)
                            .create()
                        loadingDialog.show()

                        val updatedItems = selectedItems.map { it.copy(isReturned = true) }
                        transactionViewModel.updateTransactionRecords(updatedItems)
                        processReturnTransaction(transaction, selectedItems, remarks)
                        loadingDialog.dismiss()
                        returnDialog.dismiss()
//                        recreate()
                        Toast.makeText(
                            this@Window1,
                            "Return processed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing return", e)
                        Toast.makeText(
                            this@Window1,
                            "Error processing return: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    /*private fun processReturnTransaction(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        remarks: String
    ) {
        lifecycleScope.launch {
            try {
                // Calculate correct amounts for returned items
                val returnedGrossAmount = items.sumByDouble { item ->
                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                    effectivePrice * item.quantity
                }

                val returnedDiscountAmount = items.sumByDouble { item ->
                    when (item.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> {
                            val itemTotal = (if (item.priceOverride!! > 0.0) item.priceOverride else item.price) * item.quantity
                            itemTotal * item.discountRate
                        }
                        "FIXED" -> item.discountAmount * item.quantity
                        "FIXEDTOTAL" -> item.discountAmount
                        else -> 0.0
                    }
                }

                // Calculate net amount first
                val returnedNetAmount = returnedGrossAmount - returnedDiscountAmount

                // Calculate VAT components correctly from net amount
                val returnedVatAmount = returnedNetAmount * 0.12 / 1.12
                val returnedVatableSales = returnedNetAmount / 1.12

                val updatedTransaction = transaction.copy(
                    netAmount = transaction.netAmount - returnedNetAmount,
                    costAmount = transaction.costAmount - items.sumByDouble { it.costAmount ?: 0.0 },
                    grossAmount = transaction.grossAmount - returnedGrossAmount,
                    discountAmount = transaction.discountAmount - returnedDiscountAmount,
                    customerDiscountAmount = transaction.customerDiscountAmount - returnedDiscountAmount,
                    totalDiscountAmount = transaction.totalDiscountAmount - returnedDiscountAmount,
                    numberOfItems = transaction.numberOfItems - items.sumOf { it.quantity.toDouble() },
                    taxIncludedInPrice = transaction.taxIncludedInPrice - returnedVatAmount,
                    vatAmount = transaction.vatAmount - returnedVatAmount,
                    vatableSales = transaction.vatableSales - returnedVatableSales,
                    comment = "${transaction.comment}\nReturn processed: $remarks",
                )

                // Create returned items with correct amounts
                val returnedItems = items.map { item ->
                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                    val itemTotal = effectivePrice * item.quantity
                    val discountAmount = when (item.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> itemTotal * item.discountRate
                        "FIXED" -> item.discountAmount * item.quantity
                        "FIXEDTOTAL" -> item.discountAmount
                        else -> 0.0
                    }
                    val netAmount = itemTotal - discountAmount
                    val vatAmount = netAmount * 0.12 / 1.12
                    val vatableSales = netAmount / 1.12

                    item.copy(
                        quantity = -item.quantity,
                        subtotal = -itemTotal,
                        vatAmount = -vatAmount,
                        discountAmount = -discountAmount,
                        total = -netAmount,
                        comment = "Returned: $remarks",
                        taxIncludedInPrice = -vatAmount,
                        netAmountNotIncludingTax = -vatableSales
                    )
                }

                transactionViewModel.updateTransactionSummary(updatedTransaction)
                transactionViewModel.updateTransactionRecords(returnedItems)
                updateInventory(returnedItems)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Return processed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    printReturnReceipt(updatedTransaction, returnedItems, remarks)
                }
            } catch (e: Exception) {
                Log.e("ReturnTransaction", "Error processing return: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error processing return: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    transactionViewModel.loadTransactions()
                }
            }}
        }*/
    private fun processReturnTransaction(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        remarks: String
    ) {
        lifecycleScope.launch {
            try {
                // Calculate correct amounts for returned items
                val returnedGrossAmount = items.sumByDouble { item ->
                    val effectivePrice =
                        if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                    effectivePrice * item.quantity
                }

                // Calculate all discount components separately
                val returnedDiscountAmount = items.sumByDouble { item ->
                    when (item.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> {
                            val itemTotal =
                                (if (item.priceOverride!! > 0.0) item.priceOverride else item.price) * item.quantity
                            itemTotal * item.discountRate
                        }

                        "FIXED" -> item.discountAmount * item.quantity
                        "FIXEDTOTAL" -> item.discountAmount
                        else -> 0.0
                    }
                }

                // Calculate net amount
                val returnedNetAmount = returnedGrossAmount - returnedDiscountAmount

                // Calculate VAT components correctly from net amount
                val returnedVatAmount = returnedNetAmount * 0.12 / 1.12
                val returnedVatableSales = returnedNetAmount / 1.12

                // Calculate the proportion of return amount to original transaction amount
                val returnProportion = returnedNetAmount / transaction.netAmount

                // Calculate partial payment adjustment proportionally
                val returnedPartialPayment = if (transaction.partialPayment > 0) {
                    transaction.partialPayment * returnProportion
                } else {
                    0.0
                }

                val updatedTransaction = transaction.copy(
                    netAmount = transaction.netAmount - returnedNetAmount,
                    costAmount = transaction.costAmount - items.sumByDouble {
                        it.costAmount ?: 0.0
                    },
                    grossAmount = transaction.grossAmount - returnedGrossAmount,
                    discountAmount = transaction.discountAmount - returnedDiscountAmount,
                    customerDiscountAmount = transaction.customerDiscountAmount - returnedDiscountAmount,
                    totalDiscountAmount = transaction.totalDiscountAmount - returnedDiscountAmount,
                    numberOfItems = transaction.numberOfItems - items.sumOf { it.quantity.toDouble() },
                    taxIncludedInPrice = transaction.taxIncludedInPrice - returnedVatAmount,
                    vatAmount = transaction.vatAmount - returnedVatAmount,
                    vatableSales = transaction.vatableSales - returnedVatableSales,
                    partialPayment = transaction.partialPayment - returnedPartialPayment,
                    comment = "${transaction.comment}\nReturn processed: $remarks",


                    )

                // Create returned items with correct amounts and preserving discount information
                val returnedItems = items.map { item ->
                    val effectivePrice =
                        if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                    val itemTotal = effectivePrice * item.quantity

                    // Preserve original discount information but make it negative
                    val discountAmount = when (item.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> itemTotal * item.discountRate
                        "FIXED" -> item.discountAmount * item.quantity
                        "FIXEDTOTAL" -> item.discountAmount
                        else -> 0.0
                    }

                    val netAmount = itemTotal - discountAmount
                    val vatAmount = netAmount * 0.12 / 1.12
                    val vatableSales = netAmount / 1.12

                    item.copy(
                        quantity = -item.quantity,
                        subtotal = -itemTotal,
                        vatAmount = -vatAmount,
                        discountAmount = -discountAmount,
                        discountRate = item.discountRate,  // Preserve original discount rate
                        lineDiscountAmount = -(item.lineDiscountAmount
                            ?: 0.0),  // Preserve line discount
                        total = -netAmount,
                        comment = "Returned: $remarks",
                        taxIncludedInPrice = -vatAmount,
                        netAmountNotIncludingTax = -vatableSales,
                        // Preserve all discount-related fields but make amounts negative
                        priceOverride = item.priceOverride,
                        discountType = item.discountType

                    )
                }

                transactionViewModel.updateTransactionSummary(updatedTransaction)
                transactionViewModel.updateTransactionRecords(returnedItems)
                updateInventory(returnedItems)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Return processed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    printReturnReceipt(updatedTransaction, returnedItems, remarks)
                }
            } catch (e: Exception) {
                Log.e("ReturnTransaction", "Error processing return: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error processing return: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    transactionViewModel.loadTransactions()
                }
            }
        }
    }

    private fun printReturnReceipt(
        transaction: TransactionSummary,
        returnedItems: List<TransactionRecord>,
        remarks: String
    ) {
        try {
            val printerMacAddress = "DC:0D:30:70:09:19"  // Replace with your printer's MAC address

            // Ensure you're passing the address to the connect method
            if (!bluetoothPrinterHelper.isConnected() && !bluetoothPrinterHelper.connect(
                    printerMacAddress
                )
            ) {
                Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
                return
            }

            // Print return receipt
            if (bluetoothPrinterHelper.printReceipt(
                    transaction,
                    returnedItems,
                    BluetoothPrinterHelper.ReceiptType.RETURN
                )
            ) {
                Toast.makeText(this, "Return receipt printed successfully", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Failed to print return receipt", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PrintReceipt", "Error printing return receipt: ${e.message}", e)
            Toast.makeText(this, "Error printing return receipt: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }


    private fun updateInventory(returnItems: List<TransactionRecord>) {
        // Implement your inventory update logic here
        // This might involve increasing the stock of returned items
    }

    private fun setupOverlay() {
        val toggleButton = findViewById<ImageButton>(R.id.toggleButton)
        val overlayLayout = findViewById<ConstraintLayout>(R.id.overlayLayout)
        val searchCardView = findViewById<CardView>(R.id.searchCardView)
        val insertButton = findViewById<Button>(R.id.insertButton)

        toggleButton.setOnClickListener {
            if (overlayLayout.visibility == View.GONE) {
                overlayLayout.visibility = View.VISIBLE
                searchCardView.visibility = View.GONE
                insertButton.visibility = View.GONE
            } else {
                overlayLayout.visibility = View.GONE
                searchCardView.visibility = View.VISIBLE
                insertButton.visibility = View.VISIBLE
            }
        }
    }


    private fun showClearCartConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear Cart")
            .setMessage("Are you sure you want to clear all items from the cart?")
            .setPositiveButton("Yes") { _, _ ->
                clearCart()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun clearCart() {
        lifecycleScope.launch {
            if (partialPaymentAmount == 0.0) {
                cartViewModel.deleteAll(windowId)
                transactionComment = "" // Clear the comment
                updateTotalAmountWithComment()

                Toast.makeText(this@Window1, "Cart cleared", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this@Window1,
                    "Cannot clear cart with partial payment",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    /*   private fun refreshUI() {
        productViewModel.refreshProducts()
        categoryViewModel.refreshCategories()
        productViewModel.loadAlignedProducts()
        productViewModel.selectCategory(null) // Reset category filter
    }*/

    private fun initializeAutoDatabaseTransferManager() {
        autoDatabaseTransferManager = AutoDatabaseTransferManager(this, lifecycleScope)
        autoDatabaseTransferManager.startMonitoringConnectivity()
    }


    override fun onDestroy() {
        super.onDestroy()
        if (::refreshJob.isInitialized) {
            refreshJob.cancel()
        }
        disconnectPrinter()
        returnDialog?.dismiss()
    }

    /*    private fun setupSearchView() {
        searchView = binding.searchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productViewModel.filterProducts(newText)
                return true
            }
        })
    }*/

    private fun setupSearchView() {
        // Initialize SearchView from the layout
        searchView = binding.searchView

        // Access the EditText inside the SearchView and set the background
        val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        editText.setBackgroundResource(android.R.color.transparent) // Set background to transparent

        // Set the text color for the query and hint
        editText.setTextColor(Color.BLACK) // Set text color to black
        editText.setHintTextColor(Color.GRAY) // Set hint text color to gray

        // Move the search icon to the right
        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)
            ?.let { searchIcon ->
                // Remove the search icon from its parent
                (searchIcon.parent as? ViewGroup)?.removeView(searchIcon)

                // Create a new LinearLayout to hold the search icon
                val linearLayout = LinearLayout(searchView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                }

                // Add the search icon to the right of the LinearLayout
                linearLayout.addView(searchIcon, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 0
                    marginEnd = 16 // Set right margin to 16dp
                })

                // Add the LinearLayout to the SearchView
                (searchView as ViewGroup).addView(linearLayout)
            }

        // Set the OnQueryTextListener to handle text changes and submissions
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus() // Hide the keyboard on submit
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productViewModel.filterProducts(newText)
                return true
            }
        })

        // Handle the focus change for the SearchView to hide the keyboard when it loses focus
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            }
        }
    }

    private fun filterProducts(query: String?) {
        productViewModel.filterProducts(query)
    }

    private fun showVoidPartialPaymentDialog() {
        if (partialPaymentApplied) {
            val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle("Void Partial Payment")
                .setMessage("Are you sure you want to void the partial payment?")
                .setPositiveButton("Yes") { _, _ ->
                    voidPartialPayment()
                }
                .setNegativeButton("No", null)
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.show()
        } else {
            Toast.makeText(this@Window1, "No partial payment applied", Toast.LENGTH_SHORT).show()
        }
    }

    private fun voidPartialPayment() {
        lifecycleScope.launch {
            try {
                val cartItems = cartViewModel.getAllCartItems(windowId).first()
                val removedAmount = cartItems.firstOrNull()?.partialPayment ?: 0.0

                cartViewModel.resetPartialPayment(windowId)
                partialPaymentAmount = 0.0
                partialPaymentApplied = false

                updateTotalAmount(cartItems)
                Toast.makeText(this@Window1, "Partial payment voided", Toast.LENGTH_SHORT).show()

                // Print the simplified void partial payment receipt
                printVoidPartialPaymentReceipt(removedAmount, cartItems)

                /* refreshUI()*/
            } catch (e: Exception) {
                Log.e(TAG, "Error voiding partial payment", e)
                Toast.makeText(this@Window1, "Error voiding partial payment", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun printVoidPartialPaymentReceipt(removedAmount: Double, cartItems: List<CartItem>) {
        val content = StringBuilder()

        content.append("==============================\n")
        content.append("    VOID PARTIAL PAYMENT      \n")
        content.append("==============================\n")
        content.append(
            "Date: ${
                SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(
                    Date()
                )
            }\n"
        )
        content.append("Window: $windowId\n")
        content.append("------------------------------\n")
        content.append("Total Removed Amount: P${String.format("%.2f", removedAmount)}\n")
        content.append("------------------------------\n")
        content.append("Products:\n")

        cartItems.forEach { item ->
            content.append("${item.productName}\n")
            content.append(
                "  ${item.quantity} x P${
                    String.format(
                        "%.2f",
                        item.price
                    )
                } = P${String.format("%.2f", item.quantity * item.price)}\n"
            )
        }

        content.append("------------------------------\n")
        content.append("Partial payment has been voided.\n")
        content.append("==============================\n")
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))

        // Print the receipt
        printReceiptWithBluetoothPrinter(content.toString())
    }

    private fun getWindowId() {
        windowId = intent.getIntExtra("WINDOW_ID", -1)
        Log.d(TAG, "Window ID: $windowId")
        if (windowId == -1) {
            Toast.makeText(this, "Error: No window ID provided", Toast.LENGTH_LONG).show()
            finish()
            throw IllegalStateException("No window ID provided")
        }
    }

    private fun initializeRepositories() {
        val cartDao = database.cartDao()
        val cartRepository = CartRepository(cartDao)
        transactionDao = database.transactionDao()
        val cashFundDao = database.cashFundDao()
        cashFundRepository = CashFundRepository(cashFundDao)
        numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())

    }

    private fun initializeViewModels() {
        productViewModel = ViewModelProvider(
            this,
            ProductViewModel.ProductViewModelFactory(application)
        ).get(ProductViewModel::class.java)

        val cartRepository = CartRepository(database.cartDao())
        val cartViewModelFactory = CartViewModelFactory(cartRepository)
        cartViewModel = ViewModelProvider(this, cartViewModelFactory)[CartViewModel::class.java]
        cartViewModel.setCurrentWindow(windowId)

        val categoryApi = RetrofitClient.categoryApi
        val categoryDao = database.categoryDao()
        val categoryRepository = CategoryRepository(categoryApi, categoryDao)
        categoryViewModel = ViewModelProvider(
            this,
            CategoryViewModelFactory(categoryRepository)
        ).get(CategoryViewModel::class.java)
        val discountApiService = RetrofitClient.discountApiService
        val discountDao =
            database.discountDao() // Assuming you have this method in your AppDatabase
        val discountRepository = DiscountRepository(discountApiService, discountDao)
        val discountViewModelFactory = DiscountViewModelFactory(discountRepository)
        discountViewModel =
            ViewModelProvider(this, discountViewModelFactory)[DiscountViewModel::class.java]
        windowViewModel = ViewModelProvider(this).get(WindowViewModel::class.java)

        productViewModel.loadAlignedProducts()

        val arApi = RetrofitClient.arApi
        val arDao = database.arDao() // Assuming you have this method in your AppDatabase
        val arRepository = ARRepository(arApi, arDao)
        val arViewModelFactory = ARViewModelFactory(arRepository)
        arViewModel = ViewModelProvider(this, arViewModelFactory)[ARViewModel::class.java]

        // Initialize CustomerViewModel
        val customerApi = RetrofitClient.customerApi
        val customerDao =
            database.customerDao() // Assuming you have this method in your AppDatabase
        val customerRepository = CustomerRepository(customerApi, customerDao)
        val customerViewModelFactory = CustomerViewModelFactory(customerRepository)
        customerViewModel =
            ViewModelProvider(this, customerViewModelFactory)[CustomerViewModel::class.java]

        val repository = TransactionRepository(transactionDao)
        val factory = TransactionViewModel.TransactionViewModelFactory(repository)
        transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]

        // Set up the observer
        transactionViewModel.syncStatus.observe(this@Window1) { result ->
            result.fold(
                onSuccess = { response ->
                    Log.d("Sync", "Successfully synced transaction: ${response.transactionId}")
                    Toast.makeText(
                        this@Window1,
                        "Transaction synced successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Log.e("Sync", "Failed to sync: ${error.message}")
                    Toast.makeText(
                        this@Window1,
                        "Failed to sync transaction: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
//    private fun setupMixMatchButton() {
//        binding.buttonMixMatch.setOnClickListener {
//            showMixMatchDialog()
//        }
//    }

    private fun initializeMixMatch() {
        val mixMatchDao = database.mixMatchDao()
        val mixMatchApi = RetrofitClient.mixMatchApi  // Use the API from RetrofitClient
        val mixMatchRepository = MixMatchRepository(mixMatchDao, mixMatchApi)
        val factory = MixMatchViewModelFactory(mixMatchRepository)
        mixMatchViewModel = ViewModelProvider(this, factory)[MixMatchViewModel::class.java]

        // Observe mix match data
        lifecycleScope.launch {
            mixMatchViewModel.mixMatches.collect { mixMatches ->
                // Update UI as needed
            }
        }

        // Trigger initial refresh
        mixMatchViewModel.refreshMixMatches()
    }


    private fun showMixMatchDialog() {
        try {
            val mixMatches = mixMatchViewModel.mixMatches.value
            Log.d("MixMatch", "Available mix matches: ${mixMatches.size}")
            mixMatches.forEach { mixMatch ->
                Log.d(
                    "MixMatch", """
                ID: ${mixMatch.mixMatch.id}
                Description: ${mixMatch.mixMatch.description}
                Discount Type: ${mixMatch.mixMatch.discountType}
                Discount Value: ${mixMatch.mixMatch.discountPctValue}
                Line Groups: ${mixMatch.lineGroups.size}
            """.trimIndent()
                )
            }
            if (mixMatches.isEmpty()) {
                Toast.makeText(this, "No mix & match offers available", Toast.LENGTH_SHORT).show()
                return
            }

            val adapter = MixMatchAdapter(mixMatches) { selectedMixMatch ->
                showMixMatchProductSelection(selectedMixMatch)
            }

            AlertDialog.Builder(this, R.style.CustomDialogStyle)
                .setTitle("Mix & Match Offers")
                .setAdapter(adapter) { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } catch (e: Exception) {
            Log.e("MixMatch", "Error showing dialog", e)
            Toast.makeText(this, "Unable to show mix & match offers", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMixMatchProductSelection(mixMatch: MixMatchWithDetails) {
        try {
            val dialogView = layoutInflater.inflate(R.layout.dialog_mix_match_product_selection, null)

            val dialog = AlertDialog.Builder(
                this,
                com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert
            )
                .setView(dialogView)
                .create()

            // Initialize views
            val titleView = dialogView.findViewById<TextView>(R.id.textViewMixMatchTitle)
            val descriptionView = dialogView.findViewById<TextView>(R.id.textViewMixMatchDescription)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewLineGroups)
            val applyButton = dialogView.findViewById<Button>(R.id.buttonApply)

            titleView?.text = mixMatch.mixMatch.description
            descriptionView?.text = getDiscountDescription(mixMatch.mixMatch)

            // Get ProductViewModel instance
            val productViewModel = ViewModelProvider(this, ProductViewModel.ProductViewModelFactory(application))
                .get(ProductViewModel::class.java)

            // Set up RecyclerView with ProductViewModel
            recyclerView?.layoutManager = LinearLayoutManager(this)
            val lineGroupAdapter = LineGroupAdapter(mixMatch.lineGroups, productViewModel)
            recyclerView?.adapter = lineGroupAdapter

            applyButton?.setOnClickListener {
                val selections = lineGroupAdapter.getSelections()
                if (validateSelections(mixMatch, selections)) {
                    applyMixMatchToCart(mixMatch, selections)
                    dialog.dismiss()
                } else {
                    Toast.makeText(
                        this,
                        "Please select required items for all groups",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog.show()
        } catch (e: Exception) {
            Log.e("MixMatch", "Error showing product selection dialog", e)
            Toast.makeText(this, "Unable to show product selection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDiscountDescription(mixMatch: MixMatch): String {
        return when (mixMatch.discountType) {
            0 -> "Deal Price: P${mixMatch.dealPriceValue}"
            1 -> "Discount: ${mixMatch.discountPctValue}%"
            2 -> "Discount: P${mixMatch.discountAmountValue}"
            else -> "Special Offer"
        }
    }

    private fun validateSelections(
        mixMatch: MixMatchWithDetails,
        selections: Map<Int, String>
    ): Boolean {
        return try {
            mixMatch.lineGroups.all { lineGroup ->
                val hasValidSelection = selections[lineGroup.lineGroup.id]?.let { itemIdentifier ->
                    // Match by either ID or name
                    lineGroup.discountLines.any { line ->
                        line.itemId.toString().trim() == itemIdentifier.trim() ||
                                line.itemId?.trim() == itemIdentifier.trim()
                    }
                } ?: false

                if (!hasValidSelection) {
                    Log.w(
                        "MixMatch",
                        "Invalid or missing selection for line group: ${lineGroup.lineGroup.id}"
                    )
                }

                hasValidSelection
            }
        } catch (e: Exception) {
            Log.e("MixMatch", "Error validating selections", e)
            false
        }
    }

    private fun applyMixMatchToCart(
        mixMatch: MixMatchWithDetails,
        selections: Map<Int, String>
    ) {
        val bundleId = System.currentTimeMillis().toInt()

        lifecycleScope.launch {
            try {
                var totalBundlePrice = 0.0
                val selectedItems = mutableListOf<Triple<Product, Int, LineGroupWithDiscounts>>()

                // First pass: collect all selected products
                selections.forEach { (lineGroupId, itemIdentifier) ->
                    val lineGroup = mixMatch.lineGroups.find { it.lineGroup.id == lineGroupId }
                    val discountLine = lineGroup?.discountLines?.find { line ->
                        line.itemId?.toString()?.trim() == itemIdentifier.trim()
                    }

                    if (discountLine == null) {
                        throw Exception("Discount line not found for identifier: $itemIdentifier")
                    }

                    // Simplified product lookup using only itemid
                    val product = productViewModel.findProduct(discountLine.itemId?.toString())
                        ?: throw Exception("Product not found with itemId: ${discountLine.itemId}")

                    selectedItems.add(Triple(product, discountLine.qty, lineGroup!!))
                    totalBundlePrice += product.price * discountLine.qty
                }

                // Calculate discounts based on mix match type
                val (discountPerItem, discountType) = when (mixMatch.mixMatch.discountType) {
                    0 -> { // Deal Price
                        val dealPrice = mixMatch.mixMatch.dealPriceValue
                        val totalDiscount = totalBundlePrice - dealPrice
                        val itemCount = selectedItems.sumOf { it.second }
                        Pair(totalDiscount / itemCount, "DEAL")
                    }
                    1 -> { // Percentage
                        val percentageDiscount = mixMatch.mixMatch.discountPctValue
                        Pair(percentageDiscount, "PERCENTAGE")
                    }
                    2 -> { // Fixed Total
                        val fixedDiscount = mixMatch.mixMatch.discountAmountValue
                        val itemCount = selectedItems.sumOf { it.second }
                        Pair(fixedDiscount / itemCount, "FIXEDTOTAL")
                    }
                    else -> Pair(0.0, "")
                }

                // Second pass: add items to cart with calculated discounts
                selectedItems.forEach { (product, qty, lineGroup) ->
                    val cartItem = CartItem(
                        productId = product.id,
                        productName = product.itemName,
                        price = product.price,
                        quantity = qty,
                        windowId = windowId,
                        bundleId = bundleId,
                        mixMatchId = mixMatch.mixMatch.id,
                        discountType = discountType,
                        discount = discountPerItem,
                        vatAmount = 0.0,
                        vatExemptAmount = 0.0,
                        bundleSelections = selections.toString(),
                        itemGroup = product.itemGroup,  // Added field
                        itemId = product.itemid
                    )
                    cartViewModel.insert(cartItem)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Mix & Match items added to cart",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("MixMatch", "Error applying mix match to cart", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error adding mix match items to cart: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun Any?.debugLog(tag: String, prefix: String = "") {
        Log.d(tag, "$prefix${this?.toString() ?: "null"}")
    }
    private fun setupRecyclerViews() {
        setupProductRecyclerView()
        setupCategoryRecyclerView()
        setupCartRecyclerView()
    }

    private fun setupProductRecyclerView() {
        productAdapter = ProductAdapter { product -> addToCart(product) }

        // Calculate number of columns based on screen width and density
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density

        // Adjust column width based on screen size
        val desiredColumnWidthDp = when {
            screenWidthDp >= 900 -> 200f // For larger tablets
            screenWidthDp >= 600 -> 180f // For smaller tablets
            else -> 160f // For phones
        }

        val columnCount = (screenWidthDp / desiredColumnWidthDp).toInt().coerceIn(2, 5)

        binding.recyclerview.apply {
            adapter = productAdapter
            layoutManager = GridLayoutManager(this@Window1, columnCount)

            // Add item decoration for consistent spacing
            val spacing = (8 * resources.displayMetrics.density).toInt()
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(spacing, spacing, spacing, spacing)
                }
            })
        }
    }
    private fun setupViewModel() {
        val transactionRepository = TransactionRepository(database.transactionDao())
        val factory = TransactionViewModel.TransactionViewModelFactory(transactionRepository)
        transactionViewModel =
            ViewModelProvider(this, factory).get(TransactionViewModel::class.java)

        // Fixed flow collection
        lifecycleScope.launch {
            combine(
                productViewModel.allProducts.asFlow(),
                categoryViewModel.categories,
                productViewModel.isLoading
            ) { products: List<Product>, categories: List<Category>, isLoading: Boolean ->
                Triple(products, categories, isLoading)
            }.collect { (products, categories, isLoading) ->
                if (!isLoading && products.isNotEmpty()) {
                    updateUI(products, categories)
                }
            }
        }
    }


    private fun updateUI(products: List<Product>, categories: List<Category>) {
        val allCategory = Category(-1, "All")
        val mixMatchCategory = Category(-2, "Mix & Match")
        val activeCategories = listOf(allCategory, mixMatchCategory) +
                categories.filter { category ->
                    products.any { product ->
                        product.itemGroup.equals(category.name, ignoreCase = true)
                    }
                }.sortedBy { it.name }

        runOnUiThread {
            categoryAdapter.setCategories(activeCategories)
            productAdapter.submitList(products)
        }
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.viewTransactionButton).setOnClickListener {
            showTransactionListDialog()
        }
    }

    private fun updateCategoriesAndRecreate() {
        lifecycleScope.launch {
            val products = productViewModel.allProducts.value ?: emptyList()
            val categories = categoryViewModel.categories.first()

            Log.d(TAG, "Total products: ${products.size}")
            Log.d(TAG, "Total categories: ${categories.size}")

            val allCategory = Category(-1, "All")
            val validCategories = categories.filter { category ->
                val productsInCategory = products.filter { it.itemGroup == category.name }
                Log.d(TAG, "Category ${category.name}: ${productsInCategory.size} products")
                category.name != "Uncategorized" && productsInCategory.isNotEmpty()
            }

            Log.d(TAG, "Valid categories: ${validCategories.size}")

            withContext(Dispatchers.Main) {
                val categoriesToDisplay = listOf(allCategory) + validCategories
                categoryAdapter.setCategories(categoriesToDisplay)

                // Don't select any category by default
                categoryAdapter.setSelectedCategory(null)
                productViewModel.selectCategory(null)

                Log.d(TAG, "Categories set in adapter: ${categoriesToDisplay.size}")
            }

            // Instead of recreating, just refresh the product list
            withContext(Dispatchers.Main) {
                productViewModel.loadAlignedProducts()
            }
        }
    }

    private fun displayAlignedProducts(alignedProducts: Map<Category, List<Product>>) {
        categoryAdapter.setCategories(alignedProducts.keys.toList())
        // Implement additional UI updates if needed
    }


    private fun setupCategoryRecyclerView() {
        categoryAdapter = CategoryAdapter { category ->
            when (category.name) {
                "All" -> {
                    productViewModel.selectCategory(category)
                    if (hasInternetConnection()) {
                        reloadCategoriesAndProducts()
                    }
                }

                "Mix & Match" -> {
                    showMixMatchDialog()

                }

                else -> {
                    productViewModel.selectCategory(category)
                }
            }
        }

        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@Window1, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }

        lifecycleScope.launch {
            combine(
                productViewModel.allProducts.asFlow(),
                categoryViewModel.categories,
                productViewModel.isLoading
            ) { products, categories, isLoading ->
                if (!isLoading) {
                    val allCategories = categories.toMutableList()

                    // Ensure "All" category is first
                    if (!allCategories.any { it.name == "All" }) {
                        allCategories.add(0, Category(name = "All"))
                    }

                    // Ensure "Mix & Match" category exists
                    if (!allCategories.any { it.name == "Mix & Match" }) {
                        allCategories.add(1, Category(name = "Mix & Match"))
                    }

                    // Filter categories that have products
                    allCategories.filter { category ->
                        category.name == "All" ||
                                category.name == "Mix & Match" ||
                                products.any { product ->
                                    product.itemGroup.equals(category.name, ignoreCase = true)
                                }
                    }.sortedBy {
                        when (it.name) {
                            "All" -> "0"
                            "Mix & Match" -> "1"
                            else -> it.name
                        }
                    }
                } else {
                    listOf(Category(name = "All"), Category(name = "Mix & Match"))
                }
            }.collect { validCategories ->
                categoryAdapter.setCategories(validCategories)
                if (productViewModel.selectedCategory.value == null) {
                    val allCategory = validCategories.first()
                    categoryAdapter.setSelectedCategory(allCategory)
                    productViewModel.selectCategory(allCategory)
                }
            }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }

    private fun reloadCategoriesAndProducts() {
        lifecycleScope.launch {
            try {
                binding.loadingProgressBar.visibility = View.VISIBLE
                productViewModel.refreshProductsAndCategories()
                categoryViewModel.refreshCategories()
            } catch (e: Exception) {
                Log.e(TAG, "Error reloading categories and products", e)
                Toast.makeText(
                    this@Window1,
                    "Error reloading data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                binding.loadingProgressBar.visibility = View.GONE
            }
        }
    }

    private fun observeCategoriesAndProducts() {
        lifecycleScope.launch {
            combine(
                productViewModel.allProducts.asFlow(),
                categoryViewModel.categories,
                productViewModel.isLoading
            ) { products, categories, isLoading ->
                Triple(products, categories, isLoading)
            }.collect { (products, categories, isLoading) ->
                if (!isLoading) {
                    updateUI(products, categories)
                }
            }
        }
    }


    private fun initializeZXReadButtons() {
        zReadButton = findViewById(R.id.zReadButton)
        xReadButton = findViewById(R.id.xReadButton)

        zReadButton.setOnClickListener { performZRead() }
        xReadButton.setOnClickListener { performXRead() }

        // Initialize lastZReadTime (you might want to store and retrieve this from SharedPreferences)
        lastZReadTime = getLastZReadTimeFromPreferences()
    }

    private fun getLastZReadTimeFromPreferences(): Long {
        val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getLong("lastZReadTime", System.currentTimeMillis())
    }

    private fun saveLastZReadTimeToPreferences(time: Long) {
        val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putLong("lastZReadTime", time)
            apply()
        }
    }

    private fun performXRead() {
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permission is required to print X-Read", Toast.LENGTH_LONG).show()
            return
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Confirm X-Read")
            .setMessage("Are you sure you want to perform an X-Read?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    val transactions = transactionDao.getAllTransactionsSince(lastZReadTime)
                    val currentTenderDeclaration = tenderDeclarationDao.getLatestTenderDeclaration()
                    printXReadWithBluetoothPrinter(transactions, currentTenderDeclaration)
                }
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun performZRead() {
        if (!isPulloutCashFundProcessed || !isTenderDeclarationProcessed) {
            Toast.makeText(
                this,
                "Please process pull-out cash fund and tender declaration first",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!checkBluetoothPermissions()) {
            Toast.makeText(
                this,
                "Bluetooth permission is required to print Z-Read",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                val currentDate = getCurrentDate()
                val existingZRead = zReadDao.getZReadByDate(currentDate)

                if (existingZRead != null) {
                    Toast.makeText(
                        this@Window1,
                        "Z-Read has already been performed today. Please wait until tomorrow.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val dialog = AlertDialog.Builder(this@Window1, R.style.CustomDialogStyle)
                    .setTitle("Confirm Z-Read")
                    .setMessage("Are you sure you want to perform a Z-Read? This will reset all transaction data.")
                    .setPositiveButton("Yes") { _, _ ->
                        processZRead()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()

                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()
            } catch (e: Exception) {
                Log.e("ZRead", "Error checking Z-Read date", e)
                Toast.makeText(
                    this@Window1,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun processZRead() {
        lifecycleScope.launch {
            try {
                val zReportId = generateZReportId()
                val transactions = transactionDao.getAllTransactionsSince(lastZReadTime)

                // Calculate total amount from transactions
                val totalAmount = transactions.sumOf { it.totalAmountPaid } // Assuming transaction has 'amount' property

                // Update transactions with Z-report ID
                transactions.forEach { transaction ->
                    transaction.zReportId = zReportId
                    transactionDao.updateTransactionSummary(transaction)
                }

                val currentTenderDeclaration = tenderDeclaration
                if (currentTenderDeclaration == null) {
                    Toast.makeText(
                        this@Window1,
                        "Tender declaration not found. Please process tender declaration first.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Create Z-Read record
                val zRead = ZRead(
                    zReportId = zReportId,
                    date = getCurrentDate(),
                    time = getCurrentTime(),
                    totalTransactions = transactions.size,
                    totalAmount = totalAmount
                )

                // Save Z-Read record
                zReadDao.insert(zRead)

                // Print Z-Read report
                val zReadContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = true,
                    zReportId = zReportId,
                    tenderDeclaration = currentTenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(zReadContent)) {
                    Log.d("PrintZRead", "Z-Read report content sent successfully")

                    // Add a small delay before sending the cut command
                    delay(1000)

                    // Send the cut command
                    val cutCommand = "\u001D\u0056\u0000"  // GS V 0 for full cut
                    if (bluetoothPrinterHelper.printGenericReceipt(cutCommand)) {
                        Log.d("PrintZRead", "Z-Read report printed and cut successfully")

                        // Reset all data after successful printing
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@Window1,
                                "Z-Read report printed successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            resetAfterZRead()
                        }
                    }
                } else {
                    Log.e("PrintZRead", "Failed to print Z-Read report")
                    Toast.makeText(
                        this@Window1,
                        "Failed to print Z-Read report",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ZRead", "Error performing Z-Read", e)
                Toast.makeText(
                    this@Window1,
                    "Error performing Z-Read: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun checkCashFundBeforeTransaction() {
        if (isZReadPerformed && currentCashFund <= 0) {
            showCashFundDialog()
        }
    }

    private fun resetAfterZRead() {
        lifecycleScope.launch {
            try {
                // Reset time tracking
                lastZReadTime = System.currentTimeMillis()
                saveLastZReadTimeToPreferences(lastZReadTime)

                // Clear tender declaration
                tenderDeclarationDao.deleteAll()
                tenderDeclaration = null

                // Clear cash fund
                cashFundRepository.deleteAll()
                currentCashFund = 0.0

                // Reset status flags
                resetCashManagementStatus()
                isCashFundEntered = false

                // Disable transactions
                withContext(Dispatchers.Main) {
                    disableTransactions()
                }

                // Set Z-Read performed flag
                isZReadPerformed = true

                withContext(Dispatchers.Main) {
                    // Show message to user
                    Toast.makeText(
                        this@Window1,
                        "All data has been reset. Please enter new cash fund to continue.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Show cash fund dialog to start new cycle
                    showCashFundDialog()
                }
            } catch (e: Exception) {
                Log.e("ZRead", "Error resetting after Z-Read", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error resetting data: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

}

/*  private fun resetCashManagementStatus() {
        isPulloutCashFundProcessed = false
        isTenderDeclarationProcessed = false
    }*/
    private fun printXReadWithBluetoothPrinter(
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?
    ) {
        try {
            val printerMacAddress = "DC:0D:30:70:09:19"  // Replace with your printer's MAC address

            if (!bluetoothPrinterHelper.isConnected()) {
                Log.d("PrintXRead", "Attempting to connect to printer")
                if (!bluetoothPrinterHelper.connect(printerMacAddress)) {
                    Log.e("PrintXRead", "Failed to connect to printer")
                    Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            Log.d("PrintXRead", "Attempting to print X-Read report")
            val xReadContent = bluetoothPrinterHelper.buildReadReport(
                transactions,
                isZRead = false,
                tenderDeclaration = tenderDeclaration
            )
            if (bluetoothPrinterHelper.printGenericReceipt(xReadContent)) {
                Log.d("PrintXRead", "X-Read report printed successfully")

                // Add a small delay before sending the cut command
                Thread.sleep(1000)

                // Send the cut command
                val cutCommand = "\u001D\u0056\u0000"  // GS V 0 for full cut
                if (bluetoothPrinterHelper.printGenericReceipt(cutCommand)) {
                    Log.d("PrintXRead", "Cut command sent successfully")
                    Toast.makeText(
                        this,
                        "X-Read report printed and cut successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.e("PrintXRead", "Failed to send cut command")
                    Toast.makeText(
                        this,
                        "X-Read report printed, but cutting failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.e("PrintXRead", "Failed to print X-Read report")
                Toast.makeText(this, "Failed to print X-Read report", Toast.LENGTH_SHORT).show()
            }
        } catch (se: SecurityException) {
            Log.e("PrintXRead", "SecurityException: ${se.message}")
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("PrintXRead", "Error printing X-Read report: ${e.message}")
            Toast.makeText(
                this,
                "Error printing X-Read report: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun resetCashManagementStatus() {
        isPulloutCashFundProcessed = false
        isTenderDeclarationProcessed = false
        isZReadPerformed = false  // Reset Z-Read flag after cash management steps
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun showCashFundDialog() {
        if (isCashFundEntered) {
            showCurrentCashFundStatus()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_cash_fund, null)
        val editTextCashFund = dialogView.findViewById<EditText>(R.id.editTextCashFund)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Enter Cash Fund")
            .setView(dialogView)
            .setPositiveButton("Submit") { _, _ ->
                val cashFund = editTextCashFund.text.toString().toDoubleOrNull()
                if (cashFund != null && cashFund > 0) {
                    currentCashFund = cashFund
                    saveCashFund(cashFund, "INITIAL")
                    if (bluetoothPrinterHelper.printCashFundReceipt(cashFund, "INITIAL")) {
                        Toast.makeText(
                            this,
                            "Cash Fund Receipt printed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to print Cash Fund Receipt",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    isCashFundEntered = true
                    enableTransactions()
                } else {
                    Toast.makeText(
                        this,
                        "Please enter a valid amount greater than zero",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showCurrentCashFundStatus() {
        AlertDialog.Builder(this)
            .setTitle("Cash Fund Status")
            .setMessage(
                "Current Cash Fund: ₱${
                    String.format(
                        "%.2f",
                        currentCashFund
                    )
                }\n\nYou can manage the cash fund through the Pull-out Cash Fund option."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun enableTransactions() {
        binding.payButton.isEnabled = true
        binding.clearCartButton.isEnabled = true
        binding.insertButton.isEnabled = true
        binding.priceOverrideButton.isEnabled = true
        binding.discountButton.isEnabled = true
        binding.partialPaymentButton.isEnabled = true
        binding.voidPartialPaymentButton.isEnabled = true
    }

    private fun disableTransactions() {
        binding.payButton.isEnabled = false
        binding.clearCartButton.isEnabled = false
        binding.insertButton.isEnabled = false
        binding.priceOverrideButton.isEnabled = false
        binding.discountButton.isEnabled = false
        binding.partialPaymentButton.isEnabled = false
        binding.voidPartialPaymentButton.isEnabled = false
    }

    private fun showPulloutCashFundDialog() {
        if (currentCashFund <= 0) {
            Toast.makeText(this, "No cash fund available to pull out", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Pull Out Cash Fund")
            .setMessage("Current Cash Fund: ₱${String.format("%.2f", currentCashFund)}")
            .setPositiveButton("Pull Out") { _, _ ->
                processPulloutCashFund(currentCashFund)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showTenderDeclarationDialog() {
        if (!isPulloutCashFundProcessed) {
            Toast.makeText(this, "Please process pull-out cash fund first", Toast.LENGTH_LONG)
                .show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_tender_declaration, null)

        // Find all EditText views for cash denominations
        val cashEditTexts = listOf(
            dialogView.findViewById<EditText>(R.id.editText1000),
            dialogView.findViewById<EditText>(R.id.editText500),
            dialogView.findViewById<EditText>(R.id.editText200),
            dialogView.findViewById<EditText>(R.id.editText100),
            dialogView.findViewById<EditText>(R.id.editText50),
            dialogView.findViewById<EditText>(R.id.editText20),
            dialogView.findViewById<EditText>(R.id.editText10),
            dialogView.findViewById<EditText>(R.id.editText5),
            dialogView.findViewById<EditText>(R.id.editText1)
        )

        val denominations = listOf(1000, 500, 200, 100, 50, 20, 10, 5, 1)

        val textViewTotalCash = dialogView.findViewById<TextView>(R.id.textViewTotalCash)
        val linearLayoutArTypes = dialogView.findViewById<LinearLayout>(R.id.linearLayoutArTypes)
        val textViewTotalAr = dialogView.findViewById<TextView>(R.id.textViewTotalAr)

        // Just set hints instead of default text
        cashEditTexts.forEach { editText ->
            editText.hint = "0"
        }

        // Simple text watcher that just calculates total
        cashEditTexts.forEach { editText ->
            editText.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    calculateTotalCash(cashEditTexts, denominations, textViewTotalCash)
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }

        val arEditTexts = mutableListOf<EditText>()

        lifecycleScope.launch {
            arViewModel.arTypes.collectLatest { arTypes ->
                linearLayoutArTypes.removeAllViews()
                arEditTexts.clear()

                arTypes.forEach { arType ->
                    if (arType.ar != "Cash") {
                        addArTypeToLayout(
                            arType.ar,
                            linearLayoutArTypes,
                            arEditTexts,
                            textViewTotalAr
                        )
                    }
                }
                calculateTotalAr(arEditTexts, textViewTotalAr)
            }
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Tender Declaration")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                val totalCash =
                    textViewTotalCash.text.toString().replace("Total Cash: ₱", "").toDoubleOrNull()
                        ?: 0.0
                val arAmounts = arEditTexts.associate { editText ->
                    val arType = editText.tag as? String ?: "Unknown"
                    val amount = editText.text.toString().toDoubleOrNull() ?: 0.0
                    arType to amount
                }
                processTenderDeclaration(totalCash, arAmounts)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun addArTypeToLayout(
        arType: String,
        layout: LinearLayout,
        editTexts: MutableList<EditText>,
        totalArTextView: TextView
    ) {
        val arTypeLayout = layoutInflater.inflate(R.layout.item_ar_type, null)
        arTypeLayout.setBackgroundColor(Color.parseColor("#FFE0E0E0"))

        val arTypeLabel = arTypeLayout.findViewById<TextView>(R.id.textViewArTypeLabel)
        val arTypeEditText = arTypeLayout.findViewById<EditText>(R.id.editTextArTypeAmount)

        arTypeLabel.text = arType
        arTypeEditText.tag = arType

        // Just set basic properties
        arTypeEditText.hint = "0.00"
        arTypeEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        arTypeEditText.setTextColor(Color.BLACK)
        arTypeEditText.setHintTextColor(Color.GRAY)

        editTexts.add(arTypeEditText)

        // Simple text watcher that just updates the total
        arTypeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalAr(editTexts, totalArTextView)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Keep the decimal filter
        arTypeEditText.filters = arrayOf(DecimalDigitsInputFilter(10, 2))

        layout.addView(arTypeLayout)
    }

    private fun calculateTotalAr(editTexts: List<EditText>, textViewTotalAr: TextView) {
        val total = editTexts.sumOf {
            it.text.toString().takeIf { it.isNotEmpty() }?.toDoubleOrNull() ?: 0.0
        }
        textViewTotalAr.text = String.format("Total AR: ₱%.2f", total)
    }

    private fun calculateTotalCash(
        editTexts: List<EditText>,
        denominations: List<Int>,
        textViewTotalCash: TextView
    ) {
        var total = 0.0
        editTexts.forEachIndexed { index, editText ->
            val count = editText.text.toString().takeIf { it.isNotEmpty() }?.toIntOrNull() ?: 0
            total += count * denominations[index]
        }
        textViewTotalCash.text = String.format("Total Cash: ₱%.2f", total)
    }

    private class DecimalDigitsInputFilter(
        private val maxDigits: Int,
        private val decimalDigits: Int
    ) : InputFilter {
        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val builder = StringBuilder(dest.toString())
            builder.replace(dstart, dend, source?.subSequence(start, end).toString())
            return if (!builder.toString()
                    .matches(("^\\d{0,$maxDigits}(\\.\\d{0,$decimalDigits})?$").toRegex())
            ) {
                if (source?.isEmpty() == true) dest?.subSequence(dstart, dend) else ""
            } else null
        }
    }

    private fun processTenderDeclaration(cashAmount: Double, arAmounts: Map<String, Double>) {
        if (cashAmount <= 0) {
            Toast.makeText(this, "Cash amount must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        if (arAmounts.isEmpty()) {
            Toast.makeText(this, "Please enter AR amounts", Toast.LENGTH_SHORT).show()
            return
        }

        val gson = Gson()
        val arAmountsJson = gson.toJson(arAmounts)

        tenderDeclaration = TenderDeclaration(
            cashAmount = cashAmount,
            arPayAmount = arAmounts.values.sum(),
            date = getCurrentDate(),
            time = getCurrentTime(),
            arAmounts = arAmountsJson
        )
        lifecycleScope.launch {
            tenderDeclarationDao.insert(tenderDeclaration!!)
        }
        isTenderDeclarationProcessed = true
        if (bluetoothPrinterHelper.printTenderDeclarationReceipt(cashAmount, arAmounts)) {
            Toast.makeText(
                this,
                "Tender Declaration Receipt printed successfully",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Failed to print Tender Declaration Receipt", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun saveCashFund(cashFund: Double, status: String) {
        lifecycleScope.launch {
            val cashFundEntity =
                Cashfund(cashFund = cashFund, status = status, date = getCurrentDate())
            cashFundRepository.insert(cashFundEntity)
        }
    }

    private fun processPulloutCashFund(pulloutAmount: Double) {
        currentCashFund = 0.0
        isPulloutCashFundProcessed = true
        if (bluetoothPrinterHelper.printPulloutCashFundReceipt(pulloutAmount)) {
            Toast.makeText(
                this,
                "Pull-out Cash Fund Receipt printed successfully",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(this, "Failed to print Pull-out Cash Fund Receipt", Toast.LENGTH_SHORT)
                .show()
        }
        // disableTransactions()
    }


    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }


    private fun checkForExistingCashFund() {
        lifecycleScope.launch {
            try {
                val currentDate = getCurrentDate()
                val existingCashFund = cashFundRepository.getCashFundByDate(currentDate)

                if (existingCashFund != null) {
                    currentCashFund = existingCashFund.cashFund
                } else {
                    showCashFundDialog()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for existing cash fund", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Window1, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateZReportId(): String {
        val sequentialNumber = getNextSequentialNumber()
        return String.format("%08d", sequentialNumber)
    }

    private fun getNextSequentialNumber(): Int {
        val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
        val currentNumber = sharedPreferences.getInt("lastSequentialNumber", 0)
        val nextNumber = currentNumber + 1

        with(sharedPreferences.edit()) {
            putInt("lastSequentialNumber", nextNumber)
            apply()
        }

        return nextNumber
    }

    private fun setupCashManagementButtons() {
        binding.cashFundButton.setOnClickListener {
            showCashFundDialog()
        }
        binding.pulloutCashFundButton.setOnClickListener {
            if (currentCashFund > 0) {
                showPulloutCashFundDialog()
            } else {
                Toast.makeText(this, "No cash fund available to pull out", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        binding.tenderDeclarationButton.setOnClickListener {
            showTenderDeclarationDialog()
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
                return false
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH),
                    BLUETOOTH_PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }


    private fun printReceiptWithBluetoothPrinter(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: BluetoothPrinterHelper.ReceiptType,
        isARReceipt: Boolean = false,
        copyType: String = ""

    ) {
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permission is required to print", Toast.LENGTH_LONG)
                .show()
            return
        }

        try {
            val printerMacAddress = "DC:0D:30:70:09:19"  // Replace with your printer's MAC address

            if (!bluetoothPrinterHelper.isConnected() && !bluetoothPrinterHelper.connect(
                    printerMacAddress
                )
            ) {
                Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
                return
            }

            val receiptContent = bluetoothPrinterHelper.generateReceiptContent(
                transaction,
                items,
                receiptType,
                isARReceipt,
                copyType
            )

            val printSuccess = bluetoothPrinterHelper.printGenericReceipt(receiptContent)

            if (printSuccess) {
                Toast.makeText(this, "Receipt printed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to print receipt", Toast.LENGTH_SHORT).show()
            }

            // Send cut command after printing
            bluetoothPrinterHelper.cutPaper()

        } catch (se: SecurityException) {
            Log.e("PrintReceipt", "SecurityException: ${se.message}")
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("PrintReceipt", "Error printing receipt: ${e.message}")
            Toast.makeText(this, "Error printing receipt: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Make sure this function is defined
    private fun sendCutCommand() {
        val cutCommand = "\u001D\u0056\u0000"  // GS V 0 for full cut
        bluetoothPrinterHelper.printGenericReceipt(cutCommand)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST_CODE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, you can print now
                    // You might want to call printReceiptWithBluetoothPrinter here if it was initiated by user action
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(
                        this,
                        "Bluetooth permission is required to print receipts",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
        }
    }

    private fun updateTotalAmountWithComment() {
        lifecycleScope.launch {
            val cartItems = cartViewModel.getAllCartItems(windowId).first()
            updateTotalAmount(cartItems)

        }
    }

    private fun setupPartialPaymentDisplay() {
        binding.partialPaymentTextView.visibility = View.GONE
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            productViewModel.alignedProducts.collect { alignedProducts ->
                displayAlignedProducts(alignedProducts)
            }
        }

        lifecycleScope.launch {
            productViewModel.filteredProducts.collectLatest { products ->
                productAdapter.submitList(products)
            }
        }
        lifecycleScope.launch {
            cartViewModel.currentWindowCartItems.collect { cartItems ->
                Log.d(TAG, "Received ${cartItems.size} cart items for window $windowId")
                (binding.recyclerviewcart.adapter as CartAdapter).submitList(cartItems)
                updateTotalAmount(cartItems)
                updateCartUI(cartItems)
            }
        }

        productViewModel.allProducts.observe(this) { products ->
            productAdapter.submitList(products)
        }
        lifecycleScope.launch {
            try {
                numberSequenceRepository.initializeSequence(
                    type = "TRANSACTION",
                    startValue = 1,
                    paddingLength = 9
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing number sequences", e)
            }
        }

        lifecycleScope.launch {
            categoryViewModel.categories.collect { categories ->
                categoryAdapter.setCategories(categories)
            }

            lifecycleScope.launch {
                categoryViewModel.categories.collectLatest { categories ->
                    productViewModel.allProducts.value?.let { products ->
                        val nonEmptyCategories = categories.filter { category ->
                            products.any { it.itemGroup == category.name }
                        }
                        val allCategory = Category(-1, "All")
                        categoryAdapter.setCategories(listOf(allCategory) + nonEmptyCategories)
                    }
                }
            }

            categoryViewModel.error.collect { errorMessage ->
                errorMessage?.let {
                    Log.e(TAG, "Category Error: $it")
                    Toast.makeText(this@Window1, it, Toast.LENGTH_LONG).show()
                }
            }

            lifecycleScope.launch {
                cartViewModel.getAllCartItems(windowId).collect { cartItems ->
                    Log.d(TAG, "Retrieved ${cartItems.size} cart items")
                    (binding.recyclerviewcart.adapter as CartAdapter).submitList(cartItems)
                    updateTotalAmount(cartItems)
                    updateCartUI(cartItems)
                }
            }
        }

        lifecycleScope.launch {
            cartViewModel.totalWithVat.collect { totalWithVat ->
                binding.totalAmountTextView.text =
                    String.format("Total (incl. VAT): P%.2f", totalWithVat)
            }
        }

        lifecycleScope.launch {
            partialPaymentAmount = cartViewModel.getTotalPartialPayment(windowId)

        }

        /*  productViewModel.filteredProducts.observe(this) { products ->
            productAdapter.submitList(products)
        }*/

        productViewModel.operationStatus.observe(this, Observer { result ->
            result.onSuccess { products ->
                Toast.makeText(
                    this,
                    "${products.size} products refreshed successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupButtonListeners() {
        binding.payButton.setOnClickListener {
            showPaymentDialog()
        }
        binding.clearCartButton.setOnClickListener {
            showClearCartConfirmationDialog()
        }
        /* binding.insertButton.setOnClickListener {
            productViewModel.insertAllProductsFromApi()
            recreate()
        }*/
        binding.priceOverrideButton.setOnClickListener {
            showPriceOverrideDialog()
        }
        // Add this new listener
        binding.discountButton.setOnClickListener {
            showDiscountDialog()
        }
        binding.partialPaymentButton.setOnClickListener {
            showPartialPaymentDialog()
        }

        binding.voidPartialPaymentButton.setOnClickListener {
            showVoidPartialPaymentDialog()
        }


        // Modify insert button click handler

        binding.insertButton.setOnClickListener {
            lifecycleScope.launch {
                try {
                    binding.loadingProgressBar.isVisible = true

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Fetching all data from API...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Create a coroutine scope for parallel execution
                    coroutineScope {
                        // Launch all data fetching operations in parallel
                        val productsJob = async { productViewModel.insertAllProductsFromApi() }
                        val discountsJob = async {
                            try {
                                discountViewModel.fetchDiscounts()
                            } catch (e: Exception) {
                                Log.e("Window1", "Error fetching discounts", e)
                            }
                        }
                        val arTypesJob = async {
                            try {
                                arViewModel.refreshARTypes()
                            } catch (e: Exception) {
                                Log.e("Window1", "Error fetching AR types", e)
                            }
                        }
                        val customersJob = async {
                            try {
                                customerViewModel.refreshCustomers()
                            } catch (e: Exception) {
                                Log.e("Window1", "Error fetching customers", e)
                            }
                        }
                        val mixMatchJob = async {
                            try {
                                mixMatchViewModel.refreshMixMatches()
                            } catch (e: Exception) {
                                Log.e("Window1", "Error fetching mix & match data", e)
                            }
                        }

                        // Wait for all operations to complete
                        val results = awaitAll(
                            productsJob,
                            discountsJob,
                            arTypesJob,
                            customersJob,
                            mixMatchJob
                        )

                        // Check results and prepare status message
                        val statusMessages = mutableListOf<String>()

                        // Products status
                        if (results[0] != null) {
                            statusMessages.add("Products updated")
                        }

                        // Discounts status
                        discountViewModel.discounts.value?.let {
                            statusMessages.add("Discounts updated (${it.size} items)")
                        }

                        // AR Types status
                        (arViewModel.arTypes.value as? List<*>)?.let {
                            statusMessages.add("AR Types updated (${it.size} items)")
                        }

                        // Customers status
                        (customerViewModel.customers.value as? List<*>)?.let {
                            statusMessages.add("Customers updated (${it.size} items)")
                        }

                        // Mix & Match status
                        mixMatchViewModel.mixMatches.value.let {
                            statusMessages.add("Mix & Match offers updated (${it.size} items)")
                        }

                        withContext(Dispatchers.Main) {
                            if (statusMessages.isNotEmpty()) {
                                Toast.makeText(
                                    this@Window1,
                                    statusMessages.joinToString("\n"),
                                    Toast.LENGTH_LONG
                                ).show()
                                updateCategoriesAndRecreate()
                            } else {
                                Toast.makeText(
                                    this@Window1,
                                    "No data was updated. Please check your connection.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Window1", "Error syncing data", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } finally {
                    binding.loadingProgressBar.isVisible = false
                }
            }
        }
    }


    private fun initializeProgressDialog() {
        val progressView = layoutInflater.inflate(R.layout.progress_dialog, null)
        progressDialog = AlertDialog.Builder(this)
            .setView(progressView)
            .setCancelable(false)
            .create()
    }


    private fun disconnectPrinter() {
        try {
            if (bluetoothPrinterHelper.isConnected()) {
                bluetoothPrinterHelper.disconnect()
                Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("DisconnectPrinter", "Error disconnecting printer: ${e.message}")
        }
    }

    private fun disableClearCartAndItemDeletion() {
        binding.clearCartButton.isEnabled = false
        (binding.recyclerviewcart.adapter as? CartAdapter)?.setDeletionEnabled(false)
    }

    private fun enableClearCartAndItemDeletion() {
        binding.clearCartButton.isEnabled = true
        (binding.recyclerviewcart.adapter as? CartAdapter)?.setDeletionEnabled(true)
    }


    private fun showKeyboardForEditText(editText: EditText) {
        editText.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)

        // If the above doesn't work, try this more aggressive approach
        editText.postDelayed({
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        }, 100)
    }


    private fun setupCommentButton() {
        binding.addCommentButton.setOnClickListener {
            showAddCommentDialog()
        }
    }

    private fun setupCommentHandling() {
        lifecycleScope.launch {
            cartViewModel.getCartComment(windowId)
        }

        lifecycleScope.launch {
            cartViewModel.cartComment.collect { comment ->
                transactionComment = comment ?: "" // Update local comment to empty if null
                updateTotalAmountWithComment()
            }
        }
    }

    private fun showAddCommentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_comment, null)
        val commentEditText = dialogView.findViewById<EditText>(R.id.commentEditText)

        // Pre-fill existing comment if any
        commentEditText.setText(transactionComment)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Add Transaction Comment")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, _ ->
                val comment = commentEditText.text.toString().trim()
                lifecycleScope.launch {
                    cartViewModel.updateCartComment(windowId, comment)
                    transactionComment = comment // Update the local variable
                    updateTotalAmountWithComment() // Refresh total amount
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun setupDeleteCommentButton() {
        binding.deleteCommentButton.setOnClickListener {
            showDeleteCommentConfirmation()
            binding.deleteCommentButton.isEnabled = false
            Handler(Looper.getMainLooper()).postDelayed({
                binding.deleteCommentButton.isEnabled = true
            }, 100)
        }
    }

    private fun showDeleteCommentConfirmation() {
        if (transactionComment.isEmpty()) {
            Toast.makeText(this, "No comment to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Delete Comment")
            .setMessage("Are you sure you want to delete the current comment?")
            .setPositiveButton("Yes") { _, _ ->
                deleteComment()  // Delete the comment and refresh UI
            }
            .setNegativeButton("No", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun deleteComment() {
        lifecycleScope.launch {
            cartViewModel.updateCartComment(windowId, null)
            transactionComment = ""
            updateTotalAmountWithComment()
            cartViewModel.getCartComment(windowId) // Refresh the comment from the ViewModel
            Toast.makeText(this@Window1, "Comment deleted", Toast.LENGTH_SHORT).show()
        }
    }


    private fun updateCartUI(cartItems: List<CartItem>) {
        // Update your RecyclerView or other UI components with the cart items
    }

    private fun connectToPrinter() {
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permission is required", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val printerMacAddress = "DC:0D:30:70:09:19"  // Your printer's MAC address

            if (!bluetoothPrinterHelper.isConnected()) {
                if (bluetoothPrinterHelper.connect(printerMacAddress)) {
                    Toast.makeText(this, "", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (se: SecurityException) {
            Log.e("ConnectPrinter", "SecurityException: ${se.message}")
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("ConnectPrinter", "Error connecting to printer: ${e.message}")
            Toast.makeText(this, "Error connecting to printer: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun printReceiptWithBluetoothPrinter(content: String) {
        if (!checkBluetoothPermissions()) {
            Toast.makeText(this, "Bluetooth permission is required to print", Toast.LENGTH_LONG)
                .show()
            return
        }

        try {
            if (!bluetoothPrinterHelper.isConnected()) {
                connectToPrinter()
                if (!bluetoothPrinterHelper.isConnected()) {
                    Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
                    return
                }
            }

            val cutCommand = "\u001D\u0056\u0000"  // GS V 0 for full cut
            val contentWithCut = "$content\n$cutCommand"

            val printSuccess = bluetoothPrinterHelper.printGenericReceipt(contentWithCut)

            if (printSuccess) {
                Toast.makeText(this, "Receipt printed and cut successfully", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(this, "Failed to print receipt", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PrintReceipt", "Error printing receipt: ${e.message}")
            Toast.makeText(this, "Error printing receipt: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPartialPaymentDialog() {
        lifecycleScope.launch {
            val cartItems = cartViewModel.getAllCartItems(windowId).first()
            if (cartItems.isEmpty()) {
                Toast.makeText(this@Window1, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_partial_payment, null)
            val amountPaidEditText = dialogView.findViewById<EditText>(R.id.amountPaidEditText)
            val paymentMethodSpinner = dialogView.findViewById<Spinner>(R.id.paymentMethodSpinner)
            val customerAutoComplete =
                dialogView.findViewById<AutoCompleteTextView>(R.id.customerAutoComplete)

            // Set up payment methods spinner with black text
            val paymentMethods = arrayOf("Cash", "Gcash", "Credit Card", "Debit Card")
            val adapter = ArrayAdapter(this@Window1, R.layout.spinner_item, paymentMethods)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            paymentMethodSpinner.adapter = adapter

            // Set listener to ensure text color is maintained after selection
            paymentMethodSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        (view as? TextView)?.setTextColor(Color.BLACK)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            // Rest of your existing partial payment dialog code...
            val customers = mutableListOf<Customer>()
            customers.add(Customer(accountNum = "000000", name = "Walk-in Customer"))
            val customerAdapter = ArrayAdapter(
                this@Window1,
                android.R.layout.simple_dropdown_item_1line,
                customers.map { it.name }
            )
            customerAutoComplete.setAdapter(customerAdapter)
            customerAutoComplete.setText("Walk-in Customer", false)
            var selectedCustomer = customers[0]

            customerAutoComplete.setOnItemClickListener { _, _, position, _ ->
                selectedCustomer = customers[position]
                dismissKeyboard(customerAutoComplete)
            }

            customerAutoComplete.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    dismissKeyboard(v)
                    true
                } else {
                    false
                }
            }

            val dialog = AlertDialog.Builder(this@Window1, R.style.CustomDialogStyle)
                .setTitle("Partial Payment")
                .setView(dialogView)
                .setPositiveButton("Pay") { dialog, _ ->
                    val amountPaid = amountPaidEditText.text.toString().toDoubleOrNull()
                    val paymentMethod = paymentMethodSpinner.selectedItem.toString()

                    if (amountPaid != null) {
                        processPartialPayment(amountPaid, paymentMethod, selectedCustomer)
                    } else {
                        Toast.makeText(
                            this@Window1,
                            "Please enter a valid amount",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.show()
        }
    }

    // Helper function to dismiss the keyboard
    private fun dismissKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun processPartialPayment(
        amountPaid: Double,
        paymentMethod: String,
        selectedCustomer: Customer
    ) {
        lifecycleScope.launch {
            val cartItems = cartViewModel.getAllCartItems(windowId).first()
            if (cartItems.isEmpty()) {
                Toast.makeText(this@Window1, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val vatRate = 1.12 // 12% VAT

            var gross = 0.0
            var pwdScDiscount = 0.0
            var otherDiscount = 0.0
            var vatAmount = 0.0
            var vatExemptAmount = 0.0

            // Check if PWD or SC discount is applied
            val pwdOrScItem = cartItems.find {
                it.discountType.equals(
                    "PWD",
                    ignoreCase = true
                ) || it.discountType.equals("SC", ignoreCase = true)
            }
            val isPWDorSC = pwdOrScItem != null

            // Calculate totals from cart items
            cartItems.forEach { cartItem ->
                val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
                val itemTotal = effectivePrice * cartItem.quantity
                gross += itemTotal

                if (isPWDorSC) {
                    val vatExclusiveAmount = itemTotal / vatRate
                    vatExemptAmount += vatExclusiveAmount
                    val discountPercentage = pwdOrScItem!!.discount / 100
                    pwdScDiscount += vatExclusiveAmount * discountPercentage
                    vatAmount = 0.0
                } else {
                    vatAmount += itemTotal - (itemTotal / vatRate)

                    when (cartItem.discountType.toUpperCase()) {
                        "PERCENTAGE" -> otherDiscount += itemTotal * (cartItem.discount / 100)
                        "FIXED" -> otherDiscount += cartItem.discount * cartItem.quantity
                        "FIXEDTOTAL" -> otherDiscount += cartItem.discount
                    }
                }
            }

            val totalDiscount = pwdScDiscount + otherDiscount
            val discountedSubtotal =
                if (isPWDorSC) vatExemptAmount - pwdScDiscount else gross - totalDiscount

            val currentPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
            val newPartialPayment = currentPartialPayment + amountPaid

            if (newPartialPayment > discountedSubtotal) {
                Toast.makeText(
                    this@Window1,
                    "Partial payment cannot exceed the total amount. Please enter a smaller amount.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            val remainingBalance = discountedSubtotal - newPartialPayment

            // Update cart items with new partial payment
            cartItems.forEach { cartItem ->
                val updatedCartItem = cartItem.copy(
                    partialPayment = newPartialPayment, // Update partial payment
                    amountPaid = amountPaid,
                    netAmount = discountedSubtotal,
                    grossAmount = gross,
                    discountAmount = totalDiscount,
                    numberOfItems = cartItems.sumOf { it.quantity }.toDouble(),
                    createdDate = Date(),
                    taxIncludedInPrice = vatAmount,
                    gCash = if (paymentMethod == "Gcash") amountPaid else cartItem.gCash,
                    cash = if (paymentMethod == "Cash") amountPaid else cartItem.cash,
                    card = if (paymentMethod == "Credit Card" || paymentMethod == "Debit Card") amountPaid else cartItem.card,
                    totalAmountPaid = newPartialPayment, // Update total paid
                    paymentMethod = paymentMethod,
                    customerName = selectedCustomer.name,
                    customerAccName = selectedCustomer.accountNum,
                    vatAmount = vatAmount,
                    vatExemptAmount = vatExemptAmount
                )
                cartViewModel.update(updatedCartItem)
            }

            // Print receipts for customer and staff copy
            printPartialPaymentReceipt(
                cartItems,
                amountPaid,
                paymentMethod,
                newPartialPayment,
                remainingBalance,
                "Customer Copy"
            )
            printPartialPaymentReceipt(
                cartItems,
                amountPaid,
                paymentMethod,
                newPartialPayment,
                remainingBalance,
                "Staff Copy"
            )
            printWindowNumberReceipt(windowId, newPartialPayment, remainingBalance)

            Toast.makeText(
                this@Window1,
                "Partial payment of P%.2f applied. Remaining: P%.2f".format(
                    amountPaid,
                    remainingBalance
                ),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun printPartialPaymentReceipt(
        cartItems: List<CartItem>,
        amountPaid: Double,
        paymentMethod: String,
        totalPartialPayment: Double,
        remainingBalance: Double,
        copyType: String
    ) {
        val sb = StringBuilder()

        // Store Header
        sb.appendLine("YOUR BUSINESS NAME")
        sb.appendLine("Address Line 1")
        sb.appendLine("Address Line 2")
        sb.appendLine("Tel: Your Phone Number")
        sb.appendLine("TIN: Your TIN Number")
        sb.appendLine("ACC: Your ACC Number")
        sb.appendLine(
            "Date Issued: ${
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                    Date()
                )
            }"
        )
        sb.appendLine("Contact #: Your Contact Number")
        sb.appendLine("-".repeat(32))

        // Receipt Type Header
        sb.appendLine("       PARTIAL PAYMENT        ")
        sb.appendLine("         $copyType          ")
        sb.appendLine("-".repeat(32))

        // Transaction Details
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("Date: ${dateFormat.format(Date())}")
        sb.appendLine("Cashier: ${cartItems.firstOrNull()?.staff ?: "N/A"}")
        sb.appendLine("-".repeat(32))

        // Items Section
        sb.appendLine("Item Name              Price    Qty    Total")

        var gross = 0.0
        var totalDiscount = 0.0
        var vatAmount = 0.0
        var costAmount = 0.0

        // Process items
        cartItems.forEach { item ->
            val effectivePrice = item.overriddenPrice ?: item.price
            val itemTotal = effectivePrice * item.quantity
            gross += itemTotal

            // Display item line
            sb.appendLine(
                "${item.productName.take(20).padEnd(20)} ${
                    String.format(
                        "%10.2f",
                        effectivePrice
                    )
                } ${String.format("%5d", item.quantity)} ${String.format("%8.2f", itemTotal)}"
            )

            // Show price override if applied
            if (item.overriddenPrice != null) {
                sb.appendLine("  Original Price: ${String.format("%.2f", item.price)}")
                sb.appendLine("  Price Override: ${String.format("%.2f", item.overriddenPrice)}")
            }

            // Handle discounts
            when (item.discountType.uppercase()) {
                "PERCENTAGE", "PWD", "SC" -> {
                    val discountAmount = itemTotal * (item.discount / 100)
                    if (discountAmount > 0) {
                        totalDiscount += discountAmount
                        sb.appendLine(
                            "  Discount (${item.discountType}): ${
                                String.format(
                                    "%.2f",
                                    discountAmount
                                )
                            }"
                        )
                        sb.appendLine("  Discount Rate: ${String.format("%.1f", item.discount)}%")
                    }
                }

                "FIXED" -> {
                    val perItemDiscount = item.discount
                    val totalItemDiscount = perItemDiscount * item.quantity
                    if (perItemDiscount > 0) {
                        totalDiscount += totalItemDiscount
                        sb.appendLine(
                            "  Discount Per Item: ${
                                String.format(
                                    "%.2f",
                                    perItemDiscount
                                )
                            }"
                        )
                    }
                }

                "FIXEDTOTAL" -> {
                    if (item.discount > 0) {
                        totalDiscount += item.discount
                        sb.appendLine(
                            "  Discount (Fixed Total): ${
                                String.format(
                                    "%.2f",
                                    item.discount
                                )
                            }"
                        )
                    }
                }
            }
        }

        // Calculate VAT (12%)
        costAmount = gross - (gross * 0.12)
        vatAmount = gross * 0.12

        sb.appendLine("-".repeat(32))

        // Totals Section
        sb.appendLine("Gross Amount:        ${String.format("%12.2f", gross)}")
        sb.appendLine("Total Discounts:     ${String.format("%12.2f", totalDiscount)}")

        val netAmount = gross - totalDiscount
        sb.appendLine("Net Amount:          ${String.format("%12.2f", netAmount)}")

        // Payment Method
        sb.appendLine("-".repeat(32))
        sb.appendLine("Payment Method: $paymentMethod")
        when (paymentMethod) {
            "Cash" -> sb.appendLine("Cash:               ${String.format("%12.2f", amountPaid)}")
            "Credit Card", "Debit Card" -> sb.appendLine(
                "Card:               ${
                    String.format(
                        "%12.2f",
                        amountPaid
                    )
                }"
            )

            "Gcash" -> sb.appendLine("GCash:              ${String.format("%12.2f", amountPaid)}")
            "PayMaya" -> sb.appendLine("PayMaya:            ${String.format("%12.2f", amountPaid)}")
        }

        // Partial Payment Summary
        sb.appendLine("-".repeat(32))
        sb.appendLine("Partial Payment Summary:")
        sb.appendLine("This Payment:        ${String.format("%12.2f", amountPaid)}")
        sb.appendLine("Total Paid:          ${String.format("%12.2f", totalPartialPayment)}")
        sb.appendLine("Remaining Balance:   ${String.format("%12.2f", remainingBalance)}")

        // VAT Information
        sb.appendLine("-".repeat(32))
        sb.appendLine("Vatable Sales:       ${String.format("%12.2f", costAmount)}")
        sb.appendLine("VAT Amount (12%):    ${String.format("%12.2f", vatAmount)}")
        sb.appendLine("Vat Exempt           ${String.format("%12.2f", 0.0)}")
        sb.appendLine("Zero Rated Sales:    ${String.format("%12.2f", 0.0)}")

        // Transaction Details
        sb.appendLine("-".repeat(32))
        sb.appendLine("Store: ${cartItems.firstOrNull()?.store ?: "N/A"}")
        sb.appendLine("Staff: ${cartItems.firstOrNull()?.staff ?: "N/A"}")
        sb.appendLine("Window Number: $windowId")

        // Footer
        sb.appendLine("-".repeat(32))
        sb.appendLine("This serves as your partial payment receipt")
        sb.appendLine("This invoice/receipt shall be valid for")
        sb.appendLine("five (5) years from the date of the")
        sb.appendLine("permit to use")
        sb.appendLine("-".repeat(32))
        sb.appendLine("POS Provider: Maximum Ideas")
        sb.appendLine("Business Solutions")
        sb.appendLine("Alabang Muntinlupa City, PHL")
        sb.appendLine("-".repeat(32))
        sb.appendLine("Thank you for your partial payment!")

        // Add extra spacing at the bottom
        repeat(6) {
            sb.appendLine(" ".repeat(32))
        }

        // Print the receipt
        printReceiptWithBluetoothPrinter(sb.toString())
    }


    private fun printWindowNumberReceipt(
        windowId: Int,
        totalPartialPayment: Double,
        remainingBalance: Double
    ) {
        val content = StringBuilder()
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append("====================\n")
        content.append("    WINDOW $windowId    \n")
        content.append("====================\n")
        content.append("Partial Payment:\n")
        content.append("P${String.format("%.2f", totalPartialPayment)}\n")
        content.append("Remaining:\n")
        content.append("P${String.format("%.2f", remainingBalance)}\n")
        content.append("====================\n")
        content.append("====================\n")
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))
        content.append(" ".repeat(46))


        // Print the small receipt
        printReceiptWithBluetoothPrinter(content.toString())
    }

    private fun setupCartRecyclerView() {
        val cartAdapter = CartAdapter(
            onItemClick = { cartItem -> /* Handle item click */ },
            onDeleteClick = { cartItem ->
                if (!partialPaymentApplied) {
                    cartViewModel.deleteCartItem(cartItem)
                } else {
                    Toast.makeText(
                        this@Window1,
                        "Cannot delete items when partial payment is applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onQuantityChange = { cartItem, newQuantity ->
                cartViewModel.update(cartItem.copy(quantity = newQuantity))
            },
            onDiscountLongPress = { cartItem ->
                showDiscountDialog()
            }
        )

        binding.recyclerviewcart.apply {
            adapter = cartAdapter
            layoutManager = LinearLayoutManager(this@Window1)
        }

        // Set up swipe-to-delete
        val itemTouchHelper = ItemTouchHelper(object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // We don't want drag & drop
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val cartItem = cartAdapter.currentList[position]
                if (!partialPaymentApplied) {
                    cartViewModel.deleteCartItem(cartItem)
                } else {
                    // If there's a partial payment, don't allow deletion and snap the item back
                    cartAdapter.notifyItemChanged(position)
                    Toast.makeText(
                        this@Window1,
                        "Cannot delete items when partial payment is applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return if (partialPaymentApplied) {
                    0 // Disable swiping when partial payment is applied
                } else {
                    super.getSwipeDirs(recyclerView, viewHolder)
                }
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.recyclerviewcart)

        // Observe partial payment changes
        lifecycleScope.launch {
            cartViewModel.getPartialPaymentForWindow(windowId).collect { partialPayment ->
                partialPaymentApplied = partialPayment > 0
                partialPaymentAmount = partialPayment
                cartAdapter.setPartialPaymentApplied(partialPaymentApplied)
                cartAdapter.setDeletionEnabled(!partialPaymentApplied)
            }
        }
    }

    private fun showDiscountDialog() {
        lifecycleScope.launch {
            val cartItems = cartViewModel.getAllCartItems(windowId).first()
            if (cartItems.isEmpty()) {
                Toast.makeText(
                    this@Window1,
                    "No items in cart. Please add items before applying discount.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_discount, null)
            val discountRecyclerView = dialogView.findViewById<RecyclerView>(R.id.discountRecyclerView)
            val cartItemsLayout = dialogView.findViewById<LinearLayout>(R.id.cartItemsLayout)
            val cartItemsTitle = dialogView.findViewById<TextView>(R.id.cartItemsTitle)
            val searchEditText = dialogView.findViewById<EditText>(R.id.searchDiscounts)
            val selectAllCheckbox = dialogView.findViewById<CheckBox>(R.id.selectAllCheckbox)

            var selectedDiscount: Discount? = null
            var adapter: DiscountAdapter? = null

            val dialog = AlertDialog.Builder(this@Window1, R.style.CustomDialogStyle)
                .setTitle("Apply Discount")
                .setView(dialogView)
                .setPositiveButton("Apply") { dialog, _ ->
                    applySelectedDiscount(dialogView, selectedDiscount)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

            // Set up search functionality
            searchEditText.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    adapter?.filter(s.toString())
                }
            })

            // Handle "Done" action on keyboard
            searchEditText.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hideKeyboard(v)
                    true
                } else {
                    false
                }
            }

            discountViewModel.fetchDiscounts()
            discountViewModel.discounts.observe(this@Window1) { discounts ->
                adapter = DiscountAdapter(discounts) { discount ->
                    selectedDiscount = discount
                    updateCartItemsForDiscount(cartItemsLayout, cartItemsTitle, selectAllCheckbox, discount)
                }
                discountRecyclerView.adapter = adapter

                val layoutManager = LinearLayoutManager(this@Window1, LinearLayoutManager.HORIZONTAL, false)
                discountRecyclerView.layoutManager = layoutManager
            }

            // Set up Select All checkbox listener
            selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
                for (i in 0 until cartItemsLayout.childCount) {
                    val view = cartItemsLayout.getChildAt(i)
                    if (view is CheckBox && view.isEnabled) {
                        view.isChecked = isChecked
                    }
                }
            }

            // Populate cart items
            cartItems.forEach { cartItem ->
                if (cartItem.discount == 0.0) {
                    val checkBox = CheckBox(this@Window1).apply {
                        text = "${cartItem.productName} (${cartItem.quantity} x ₱${cartItem.price})"
                        tag = cartItem.id
                        setTextColor(Color.BLACK)
                        buttonTintList = ColorStateList.valueOf(Color.BLACK)
                    }
                    cartItemsLayout.addView(checkBox)
                }
            }

            dialog.show()
        }
    }

    // Add this extension function to hide the keyboard
    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }


    private fun updateCartItemsForDiscount(
        cartItemsLayout: LinearLayout,
        cartItemsTitle: TextView,
        selectAllCheckbox: CheckBox,
        discount: Discount
    ) {
        cartItemsTitle.visibility = View.VISIBLE
        cartItemsLayout.visibility = View.VISIBLE

        val isSCOrPWD = discount.DISCOFFERNAME.contains("SENIOR", ignoreCase = true) ||
                discount.DISCOFFERNAME.contains("PWD", ignoreCase = true)

        // Show/hide select all checkbox based on discount type
        selectAllCheckbox.visibility = if (isSCOrPWD) View.GONE else View.VISIBLE

        // Reset select all checkbox state
        selectAllCheckbox.isChecked = false

        for (i in 0 until cartItemsLayout.childCount) {
            val view = cartItemsLayout.getChildAt(i)
            if (view is CheckBox) {
                view.isChecked = isSCOrPWD
                view.isEnabled = !isSCOrPWD
            }
        }
    }

    private fun applySelectedDiscount(dialogView: View, selectedDiscount: Discount?) {
        val cartItemsLayout = dialogView.findViewById<LinearLayout>(R.id.cartItemsLayout)
        val selectedCartItemIds = mutableListOf<Int>()

        for (i in 0 until cartItemsLayout.childCount) {
            val view = cartItemsLayout.getChildAt(i)
            if (view is CheckBox && view.isChecked) {
                view.tag?.let { tag ->
                    if (tag is Int) {
                        selectedCartItemIds.add(tag)
                    }
                }
            }
        }

        if (selectedCartItemIds.isNotEmpty() && selectedDiscount != null) {
            lifecycleScope.launch {
                val cartItems = cartViewModel.getAllCartItems(windowId).first()
                val selectedItems = cartItems.filter { it.id in selectedCartItemIds }

                // Check if items already have discounts
                val itemsWithExistingDiscounts = selectedItems.filter {
                    it.discountType.isNotBlank() && it.discountType != "FIXEDTOTAL"
                }

                if (selectedDiscount.DISCOUNTTYPE.equals("FIXEDTOTAL", ignoreCase = true) &&
                    itemsWithExistingDiscounts.isNotEmpty()
                ) {
                    // Show warning dialog for items with existing discounts
                    AlertDialog.Builder(this@Window1)
                        .setTitle("Warning")
                        .setMessage("Some selected items already have discounts. Fixed Total discount cannot be combined with other discounts. Remove existing discounts first?")
                        .setPositiveButton("Remove and Apply") { _, _ ->
                            // Remove existing discounts and apply FIXEDTOTAL
                            selectedItems.forEach { item ->
                                val updatedItem = item.copy(
                                    discount = 0.0,
                                    discountType = ""
                                )
                                cartViewModel.update(updatedItem)
                            }
                            applyFixedTotalDiscount(
                                selectedItems,
                                selectedDiscount.PARAMETER.toDouble()
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Apply the discount normally
                    when (selectedDiscount.DISCOUNTTYPE.toUpperCase()) {
                        "FIXEDTOTAL" -> applyFixedTotalDiscount(
                            selectedItems,
                            selectedDiscount.PARAMETER.toDouble()
                        )

                        "PERCENTAGE" -> {
                            val isVatExempt = selectedDiscount.DISCOFFERNAME.contains(
                                "SENIOR",
                                ignoreCase = true
                            ) ||
                                    selectedDiscount.DISCOFFERNAME.contains(
                                        "PWD",
                                        ignoreCase = true
                                    )
                            applyPercentageDiscount(
                                selectedItems,
                                selectedDiscount.PARAMETER.toDouble(),
                                isVatExempt
                            )
                        }

                        "FIXED" -> applyFixedDiscount(
                            selectedItems,
                            selectedDiscount.PARAMETER.toDouble()
                        )
                    }
                }

                Toast.makeText(
                    this@Window1,
                    "Discount applied to ${selectedItems.size} items",
                    Toast.LENGTH_SHORT
                ).show()
                updateTotalAmount(cartViewModel.getAllCartItems(windowId).first())
            }
        } else {
            Toast.makeText(
                this@Window1,
                "Please select both a discount and at least one item",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    // In your Window1.kt
    private fun applyFixedTotalDiscount(
        items: List<CartItem>,
        totalDiscountAmount: Double
    ) {
        // Calculate total price of all selected items
        val totalPrice = items.sumOf { it.price * it.quantity }

        // Calculate discount percentage based on total discount amount
        val discountPercentage = (totalDiscountAmount / totalPrice).roundToTwoDecimals()

        items.forEach { cartItem ->
            val itemTotal = cartItem.price * cartItem.quantity

            // Apply the same discount percentage to each item
            val itemDiscountAmount = (itemTotal * discountPercentage).roundToTwoDecimals()

            Log.d(
                "Discount", """
        Applying FIXED TOTAL discount:
        Item: ${cartItem.productName}
        Original Price: ${cartItem.price}
        Quantity: ${cartItem.quantity}
        Total: $itemTotal
        Discount Amount: $itemDiscountAmount
    """.trimIndent()
            )

            val updatedCartItem = cartItem.copy(
                discount = totalDiscountAmount,  // Store the total discount amount
                discountType = "FIXEDTOTAL",
                discountAmount = totalDiscountAmount,  // Store the total discount amount
            )
            cartViewModel.update(updatedCartItem)
        }
    }

    // Helper function for rounding


    private fun applyPercentageDiscount(
        items: List<CartItem>,
        discountPercentage: Double,
        isSpecialDiscount: Boolean
    ) {
        items.forEach { cartItem ->
            val updatedCartItem = cartItem.copy(
                discount = discountPercentage,
                discountType = if (isSpecialDiscount) {
                    if (discountPercentage == 5.0) "PWD" else "SC"
                } else "PERCENTAGE"
            )
            cartViewModel.update(updatedCartItem)
        }
    }

    private fun applyFixedDiscount(items: List<CartItem>, discountAmount: Double) {
        items.forEach { cartItem ->
            val updatedCartItem = cartItem.copy(
                discount = discountAmount,
                discountType = "FIXED"
            )
            cartViewModel.update(updatedCartItem)
        }
    }

    private fun showPriceOverrideDialog() {
        lifecycleScope.launch {
            val cartItems = cartViewModel.getAllCartItems(windowId).first()
            if (cartItems.isEmpty()) {
                Toast.makeText(this@Window1, "Cart is empty", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialogView = layoutInflater.inflate(R.layout.dialog_price_override_list, null)
            val recyclerView = dialogView.findViewById<RecyclerView>(R.id.priceOverrideRecyclerView)

            val dialog = AlertDialog.Builder(this@Window1, R.style.CustomDialogStyle)
                .setTitle("Override Prices")
                .setView(dialogView)
                .setPositiveButton("Close", null)
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

            val priceOverrideAdapter = PriceOverrideAdapter(
                onPriceOverride = { cartItem, newPrice ->
                    cartViewModel.updatePriceoverride(cartItem.id, newPrice)
                    dialog.dismiss()
                },
                onPriceReset = { cartItem ->
                    cartViewModel.resetPrice(cartItem.id)
                },
                onEditTextClicked = { editText ->
                    showKeyboardForEditText(editText)
                }
            )

            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@Window1)
                adapter = priceOverrideAdapter
            }

            priceOverrideAdapter.submitList(cartItems)

            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

            dialog.setOnShowListener {
                dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            }

            dialog.show()

            dialog.window?.setLayout(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun updateTotalAmount(cartItems: List<CartItem>) {
        var subtotal = 0.0
        var discount = 0.0
        var vatAmount = 0.0
        var partialPayment = 0.0
        var bundleDiscount = 0.0

        cartItems.forEach { cartItem ->
            val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
            val itemTotal = effectivePrice * cartItem.quantity
            subtotal += itemTotal
            partialPayment = cartItem.partialPayment

            // Only calculate discount if there's no partial payment or if this is the first calculation
            if (cartItem.partialPayment == 0.0) {
                if (cartItem.bundleId != null) {
                    bundleDiscount += cartItem.discount
                } else {
                    when (cartItem.discountType.uppercase()) {
                        "FIXEDTOTAL" -> discount += cartItem.discountAmount // Use the stored total discount amount
                        "PERCENTAGE", "PWD", "SC" -> discount += itemTotal * (cartItem.discount / 100)
                        "FIXED" -> discount += cartItem.discount * cartItem.quantity
                    }
                }
            }

            vatAmount += itemTotal * 0.12 / 1.12
        }

        // If there's a partial payment, get the discount from the first item
        // since it contains the correct total discount
        if (partialPayment > 0 && cartItems.isNotEmpty()) {
            val firstItem = cartItems[0]
            discount = firstItem.discountAmount // Use the stored discount amount
        }

        val totalDiscount = discount + bundleDiscount
        val discountedTotal = subtotal - totalDiscount
        val finalTotal = discountedTotal - partialPayment

        // Update UI
        binding.totalAmountTextView.text = String.format("P%.2f", subtotal)
        binding.discountAmountText.text = String.format("P%.2f", totalDiscount)
        binding.vatAmountText.text = String.format("P%.2f", vatAmount)

        if (partialPayment > 0) {
            binding.partialLabel.visibility = View.VISIBLE
            binding.partialPaymentTextView.visibility = View.VISIBLE
            binding.partialPaymentTextView.text = String.format("P%.2f", partialPayment)
            binding.finalTotalText.text = String.format("P%.2f", finalTotal)
            disableClearCartAndItemDeletion()
        } else {
            binding.partialLabel.visibility = View.GONE
            binding.partialPaymentTextView.visibility = View.GONE
            binding.finalTotalText.text = String.format("P%.2f", discountedTotal)
            enableClearCartAndItemDeletion()
        }

        if (transactionComment.isNotEmpty()) {
            binding.commentView.visibility = View.VISIBLE
            binding.commentView.text = "Note: $transactionComment"
        } else {
            binding.commentView.visibility = View.GONE
        }
    }

    private fun Double.roundToTwoDecimals(): Double {
        return (this * 100).roundToInt() / 100.0
    }

    private fun showPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val amountPaidEditText = dialogView.findViewById<EditText>(R.id.amountPaidEditText)
        val paymentMethodSpinner = dialogView.findViewById<Spinner>(R.id.paymentMethodSpinner)
        val customerAutoComplete =
            dialogView.findViewById<AutoCompleteTextView>(R.id.customerAutoComplete)
        val totalAmountTextView = dialogView.findViewById<TextView>(R.id.totalAmountTextView)

        val defaultPaymentMethods = listOf("Cash")
        val paymentMethods = mutableListOf<String>()

        var totalAmount = 0.0
        var discountType = "No Discount"
        var discountValue = 0.0
        var partialPayment = 0.0

        lifecycleScope.launch {
            cartViewModel.getAllCartItems(windowId).collect { cartItems ->
                var gross = 0.0
                var totalDiscount = 0.0
                var vatAmount = 0.0
                var priceOverrideTotal = 0.0

                cartItems.forEach { cartItem ->
                    val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
                    val itemTotal = effectivePrice * cartItem.quantity
                    gross += itemTotal
                    partialPayment = cartItem.partialPayment

                    // Calculate price override difference
                    if (cartItem.overriddenPrice != null) {
                        val originalTotal = cartItem.price * cartItem.quantity
                        priceOverrideTotal += itemTotal - originalTotal
                    }

                    // Calculate all discounts including PWD/SC as regular percentage discounts
                    when (cartItem.discountType.toUpperCase()) {
                        "PERCENTAGE", "PWD", "SC" -> {
                            totalDiscount += itemTotal * (cartItem.discount / 100)
                            discountType = cartItem.discountType
                            discountValue = cartItem.discount
                        }

                        "FIXED" -> {
                            totalDiscount += cartItem.discount * cartItem.quantity
                            discountType = "FIXED"
                            discountValue = cartItem.discount
                        }

                        "FIXEDTOTAL", "FIXED TOTAL" -> {
                            totalDiscount += cartItem.discount
                            discountType = "FIXEDTOTAL"
                            discountValue = cartItem.discount
                        }
                    }

                    vatAmount += itemTotal * 0.12 / 1.12
                }

                val discountedTotal = gross - totalDiscount
                totalAmount = discountedTotal - partialPayment

                val formattedText = StringBuilder().apply {
                    append(String.format("Gross Amount: ₱%.2f", gross))
                    if (priceOverrideTotal != 0.0) {
                        append(
                            String.format(
                                "\nPrice Override Adjustment: ₱%.2f",
                                priceOverrideTotal
                            )
                        )
                    }
                    append(String.format("\nVAT Amount: ₱%.2f", vatAmount))
                    if (totalDiscount > 0) {
                        append(String.format("\nTotal Discount: ₱%.2f", totalDiscount))
                    }
                    append(String.format("\nDiscounted Total: ₱%.2f", discountedTotal))
                    if (partialPayment > 0) {
                        append(String.format("\nPartial Payment: ₱%.2f", partialPayment))
                        append(String.format("\nRemaining Balance: ₱%.2f", totalAmount))
                    }
                    if (transactionComment.isNotEmpty()) {
                        append("\nComment: $transactionComment")
                    }
                }.toString()

                totalAmountTextView.text = formattedText
            }
        }


        // Setup for payment methods spinner
        lifecycleScope.launch {
            launch {
                arViewModel.arTypes.collectLatest { arTypes ->
                    paymentMethods.clear()
                    paymentMethods.addAll(defaultPaymentMethods)
                    if (arTypes.isNotEmpty()) {
                        paymentMethods.addAll(arTypes.map { it.ar })
                        Log.i(TAG, "Received ${arTypes.size} AR types")
                    } else {
                        Log.i(TAG, "No AR types received. Using default payment methods.")
                    }
                    updatePaymentMethodSpinner(paymentMethodSpinner, paymentMethods)

                    // Set Cash as default
                    val cashIndex = paymentMethods.indexOf("Cash")
                    if (cashIndex != -1) {
                        paymentMethodSpinner.setSelection(cashIndex)
                    }
                }
            }

            launch {
                arViewModel.error.collectLatest { errorMessage ->
                    if (errorMessage != null) {
                        Log.e(TAG, "Error in AR ViewModel: $errorMessage")
                        paymentMethods.clear()
                        paymentMethods.addAll(defaultPaymentMethods)
                        updatePaymentMethodSpinner(paymentMethodSpinner, paymentMethods)
                    }
                }
            }
        }

        arViewModel.refreshARTypes()

        // Setup for customer selection
        val customers = mutableListOf<Customer>()
        customers.add(Customer(accountNum = "000000", name = "Walk-in Customer"))
        val customerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            customers.map { it.name })
        customerAutoComplete.setAdapter(customerAdapter)
        customerAutoComplete.setText("Walk-in Customer", false)
        var selectedCustomer = customers[0]

        lifecycleScope.launch {
            try {
                customerViewModel.refreshCustomers()
                customerViewModel.customers.collectLatest { customerList ->
                    updateCustomerList(customers, customerList, customerAdapter)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching customers: ${e.message}")
                updateCustomerList(customers, emptyList(), customerAdapter)
            }
        }
        customerAutoComplete.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        customerAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedCustomer = customers[position]
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(customerAutoComplete.windowToken, 0)
        }

        customerAutoComplete.setOnItemClickListener { _, _, position, _ ->
            selectedCustomer = customers[position]
        }

        customerAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    try {
                        if (s?.length ?: 0 >= 1) {
                            customerViewModel.searchCustomers(s.toString())
                        } else {
                            updateCustomerList(
                                customers,
                                customerViewModel.customers.value,
                                customerAdapter
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error searching customers: ${e.message}")
                        updateCustomerList(customers, emptyList(), customerAdapter)
                    }
                }
            }
        })

        lifecycleScope.launch {
            customerViewModel.searchResults.collectLatest { searchResults ->
                updateCustomerList(customers, searchResults, customerAdapter)
            }
        }

        // Add listener to payment method spinner
        paymentMethodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedMethod = parent?.getItemAtPosition(position).toString()
                if (selectedMethod == "Cash") {
                    amountPaidEditText.isEnabled = true
                    amountPaidEditText.setText("") // Clear the text for new input
                } else {
                    amountPaidEditText.isEnabled = false
                    amountPaidEditText.setText(
                        String.format(
                            "%.2f",
                            totalAmount
                        )
                    ) // Display total amount
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Payment")
            .setView(dialogView)
            .setPositiveButton("Pay", null)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .create()

// Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        dialog.setOnShowListener {
            val payButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            payButton.setOnClickListener {
                val paymentMethod = paymentMethodSpinner.selectedItem.toString()
                val amountPaid = amountPaidEditText.text.toString().toDoubleOrNull() ?: 0.0

                val isAR = paymentMethod != "Cash"

                if (!isAR && amountPaid < totalAmount) {
                    Toast.makeText(this, "Insufficient amount", Toast.LENGTH_SHORT).show()
                } else {
                    val change = if (isAR) 0.0 else amountPaid - totalAmount
                    if (!isAR && change < 0) {
                        Toast.makeText(
                            this,
                            "Error: Negative change calculated",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        if (!isAR && change > 0) {
                            Toast.makeText(this, "Change: ₱%.2f".format(change), Toast.LENGTH_SHORT)
                                .show()
                        }

                        processPayment(
                            if (isAR) totalAmount else amountPaid,
                            paymentMethod,
                            1.12, // VAT rate (12%)
                            discountType,
                            discountValue,
                            selectedCustomer
                        )
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun updatePaymentMethodSpinner(spinner: Spinner, methods: List<String>) {
        val uniqueMethods = methods.distinct().sorted()

        // Create custom layout for selected item
        val adapter = ArrayAdapter(this, R.layout.spinner_item, uniqueMethods)
        // Create custom layout for dropdown items
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinner.adapter = adapter

        // Set listener to ensure text color is maintained after selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                (view as? TextView)?.setTextColor(Color.BLACK)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateCustomerList(
        customersList: MutableList<Customer>,
        newCustomers: List<Customer>,
        adapter: ArrayAdapter<String>
    ) {
        customersList.clear()
        customersList.add(Customer(accountNum = "WALK-IN", name = "Walk-in Customer"))
        customersList.addAll(newCustomers)
        adapter.clear()
        adapter.addAll(customersList.map { it.name })
        adapter.notifyDataSetChanged()
    }

    private fun addToCart(product: Product) {
        if (currentCashFund <= 0) {
            Toast.makeText(
                this,
                "Cannot perform transactions. Please set a cash fund.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        lifecycleScope.launch {
            val existingItems = cartViewModel.getAllCartItems(windowId).first()
            val partialPayment = existingItems.firstOrNull()?.partialPayment ?: 0.0

            val existingItem = existingItems.find { it.productId == product.id }
            if (existingItem != null) {
                cartViewModel.update(
                    existingItem.copy(
                        quantity = existingItem.quantity + 1,
                        partialPayment = partialPayment
                    )
                )
            } else {
                cartViewModel.insert(
                    CartItem(
                        productId = product.id,
                        quantity = 1,
                        windowId = windowId,
                        productName = product.itemName,
                        price = product.price,
                        partialPayment = partialPayment,
                        vatAmount = 0.0,
                        vatExemptAmount = 0.0,
                        itemGroup = product.itemGroup,  // Added field
                        itemId = product.itemid
                    )
                )
            }
            Log.d(TAG, "Added/Updated cart item for product ${product.id} in window $windowId")

            // Update total amount after adding/updating the item
            updateTotalAmount(cartViewModel.getAllCartItems(windowId).first())
        }
    }

    private fun getCurrentStore(): String {
        return SessionManager.getCurrentUser()?.storeid ?: "Unknown Store"
    }

    private fun getCurrentStaff(): String {
        return SessionManager.getCurrentUser()?.name ?: "Unknown Staff"
    }

    private fun initializeTransactionNumber() {
        loadLastTransactionNumber()
    }

    private suspend fun generateTransactionId(): String {
        return numberSequenceRepository.getNextNumber("TRANSACTION")
    }

    private fun loadLastTransactionNumber() {
        val sharedPref = getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)
        lastTransactionNumber = sharedPref.getInt("lastTransactionNumber", 0)
    }
    private fun checkCurrentSequence() {
        lifecycleScope.launch {
            val currentValue = numberSequenceRepository.getCurrentValue("TRANSACTION")
            Log.d(TAG, "Current transaction number: $currentValue")
            // Optionally update UI to show current value
            // binding.textViewSequence.text = "Current Sequence: $currentValue"
        }
    }
    // Add this function to save the last transaction number to SharedPreferences
    private fun saveLastTransactionNumber() {
        val sharedPref = getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("lastTransactionNumber", lastTransactionNumber)
            apply()
        }
    }
    private fun setupSequenceButton() {
        binding.btnSetSequence.setOnClickListener {
            showSequenceDialog()
        }
    }

    private fun showSequenceDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sequence_number, null)
        val sequenceInput = dialogView.findViewById<EditText>(R.id.editTextSequence)
        val currentSequence = dialogView.findViewById<TextView>(R.id.textViewCurrentSequence)

        // Load current sequence
        lifecycleScope.launch {
            val currentValue = numberSequenceRepository.getCurrentValue("TRANSACTION")
            currentSequence.text = "Current Sequence: ${currentValue ?: 0}"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Set Sequence Number")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()

        // Replace the default positive button click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newValue = sequenceInput.text.toString().toLongOrNull()
            if (newValue == null) {
                sequenceInput.error = "Please enter a valid number"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    numberSequenceRepository.resetSequence("TRANSACTION", newValue)
                    Toast.makeText(
                        this@Window1,
                        "Sequence number updated successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@Window1,
                        "Error updating sequence: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun processPayment(
        amountPaid: Double,
        paymentMethod: String,
        vatRate: Double,
        discountType: String,
        discountValue: Double,
        customer: Customer
    ) {

        lifecycleScope.launch {
            try {
                val cartItems = cartViewModel.getAllCartItems(windowId).first()
                val transactionComment = cartItems.firstOrNull()?.cartComment ?: ""

                if (cartItems.isEmpty()) {
                    Toast.makeText(
                        this@Window1,
                        "Cannot process payment. Cart is empty.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                var gross = 0.0
                var totalDiscount = 0.0
                var vatAmount = 0.0
                var vatableSales = 0.0
                var partialPayment = 0.0
                var priceOverrideTotal = 0.0
                var hasPriceOverride = false

                // Get existing discount from first cart item if partial payment exists
                val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
                val existingDiscount = if (existingPartialPayment > 0) {
                    cartItems.firstOrNull()?.discountAmount ?: 0.0
                } else null

                // Calculate totals from cart items
                cartItems.forEach { cartItem ->
                    val originalPrice = cartItem.price
                    val effectivePrice = cartItem.overriddenPrice ?: originalPrice
                    val itemTotal = effectivePrice * cartItem.quantity
                    gross += itemTotal
                    partialPayment = cartItem.partialPayment

                    // If we have existing discount from partial payment, use that instead of recalculating
                    if (existingDiscount != null) {
                        totalDiscount = existingDiscount
                    } else {
                        // Calculate discount only if there's no partial payment
                        when (cartItem.discountType.uppercase()) {
                            "FIXEDTOTAL" -> {
                                totalDiscount += cartItem.discountAmount
                            }

                            "PERCENTAGE", "PWD", "SC" -> {
                                totalDiscount += itemTotal * (cartItem.discount / 100)
                            }

                            "FIXED" -> {
                                totalDiscount += cartItem.discount * cartItem.quantity
                            }
                        }
                    }

                    vatableSales += itemTotal / vatRate
                    vatAmount += itemTotal * 0.12 / 1.12
                }

                val discountedSubtotal = gross - totalDiscount
                val netSales = discountedSubtotal - partialPayment
                val totalAmountDue = netSales.roundToTwoDecimals()
                val transactionId =
                    cartItems.firstOrNull()?.transactionId ?: generateTransactionId()

                // Determine if the payment method is AR
                val isAR = paymentMethod != "Cash"

                val change = if (isAR) 0.0 else (amountPaid - totalAmountDue).roundToTwoDecimals()
                val ar = if (isAR) totalAmountDue else 0.0

                // Prepare the transaction summary
                val transactionSummary = TransactionSummary(
                    transactionId = transactionId,
                    type = if (isAR) 3 else 1, // 3 for AR, 1 for Cash
                    receiptId = transactionId,
                    store = getCurrentStore(),
                    staff = getCurrentStaff(),
                    customerAccount = customer.accountNum,
                    netAmount = discountedSubtotal,
                    costAmount = gross - vatAmount,
                    grossAmount = gross,
                    partialPayment = partialPayment,
                    transactionStatus = 1,
                    discountAmount = totalDiscount,
                    customerDiscountAmount = totalDiscount,
                    totalDiscountAmount = totalDiscount,
                    numberOfItems = cartItems.sumOf { it.quantity }.toDouble(),
                    refundReceiptId = null,
                    currency = "PHP",
                    zReportId = null,
                    createdDate = Date(),
                    priceOverride = if (hasPriceOverride) priceOverrideTotal else 0.0,
                    comment = transactionComment,
                    receiptEmail = null,
                    markupAmount = 0.0,
                    markupDescription = null,
                    taxIncludedInPrice = vatAmount,
                    windowNumber = windowId,
                    gCash = if (paymentMethod == "Gcash") amountPaid else 0.0,
                    payMaya = if (paymentMethod == "PayMaya") amountPaid else 0.0,
                    cash = if (paymentMethod == "Cash") amountPaid else 0.0,
                    card = if (paymentMethod == "Credit Card" || paymentMethod == "Debit Card") amountPaid else 0.0,
                    loyaltyCard = 0.0,
                    changeGiven = change,
                    totalAmountPaid = if (isAR) 0.0 else amountPaid,
                    paymentMethod = paymentMethod,
                    customerName = customer.name,
                    vatAmount = (gross - vatAmount) * 0.12,
                    vatExemptAmount = 0.0,
                    vatableSales = gross - vatAmount,
                    discountType = discountType
                )

                // Insert the transaction summary into the database
                transactionDao.insertTransactionSummary(transactionSummary)

                // Update transaction records
                updateTransactionRecords(
                    transactionId,
                    cartItems,
                    paymentMethod,
                    ar,
                    vatRate,
                    totalDiscount,
                    netSales,
                    partialPayment,
                    discountType
                )

                // Get transaction records
                val transactionRecords = getTransactionRecords(
                    transactionId,
                    cartItems,
                    paymentMethod,
                    ar,
                    vatRate,
                    discountType
                )

                Log.d("Payment", "Transaction Records before printing: ${
                    transactionRecords.map {
                        "Item: ${it.name}, DiscountType: ${it.discountType}, Amount: ${it.discountAmount}"
                    }
                }")
                // Print receipts
                if (isAR) {
                    printReceiptWithBluetoothPrinter(
                        transactionSummary,
                        transactionRecords,
                        BluetoothPrinterHelper.ReceiptType.AR,
                        isARReceipt = true,
                        copyType = "Customer Copy"
                    )
                    printReceiptWithBluetoothPrinter(
                        transactionSummary,
                        transactionRecords,
                        BluetoothPrinterHelper.ReceiptType.AR,
                        isARReceipt = true,
                        copyType = "Staff Copy"
                    )
                } else {
                    printReceiptWithBluetoothPrinter(
                        transactionSummary,
                        transactionRecords,
                        BluetoothPrinterHelper.ReceiptType.ORIGINAL
                    )
                }

                // Show change and receipt dialog
                showChangeAndReceiptDialog(
                    change,
                    cartItems,
                    transactionId,
                    paymentMethod,
                    ar,
                    vatAmount,
                    totalDiscount,
                    netSales,
                    if (gross > 0) totalDiscount / gross else 0.0,
                    transactionComment,
                    partialPayment,
                    amountPaid
                )

                // Clear cart after payment
                transactionDao.updateSyncStatus(transactionId, false)

                transactionViewModel.syncTransaction(transactionId)

                // Observe the sync result
                transactionViewModel.syncStatus.observe(this@Window1) { result ->
                    result.fold(
                        onSuccess = { response ->
                            Log.d(
                                "Payment",
                                "Transaction synced successfully: ${response.transactionId}"
                            )
                            Toast.makeText(
                                this@Window1,
                                "Transaction synced successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { error ->
                            Log.e("Payment", "Sync failed: ${error.message}")
                            Toast.makeText(
                                this@Window1,
                                "Transaction saved locally but sync failed: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }

                // Clear cart and update UI
                cartViewModel.deleteAll(windowId)
                cartViewModel.clearCartComment(windowId)
                updateTotalAmount(emptyList())

            } catch (e: Exception) {
                Log.e("Payment", "Error during payment processing: ${e.message}")
                Toast.makeText(
                    this@Window1,
                    "Error processing payment: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private suspend fun updateTransactionRecords(
        transactionId: String,
        cartItems: List<CartItem>,
        paymentMethod: String,
        ar: Double,
        vatRate: Double,
        totalDiscount: Double,
        netSales: Double,
        partialPayment: Double,
        discountType: String
    ) {
        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
        val existingDiscount = if (existingPartialPayment > 0) {
            cartItems.firstOrNull()?.discountAmount ?: 0.0
        } else null

        cartItems.forEachIndexed { index, cartItem ->
            val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
            val itemTotal = effectivePrice * cartItem.quantity

            // Use existing discount if partial payment exists
            val itemDiscount = if (existingDiscount != null) {
                if (index == 0) existingDiscount else 0.0 // Apply discount only to first item
            } else {
                when (cartItem.discountType.uppercase()) {
                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)
                    "FIXED" -> cartItem.discount * cartItem.quantity
                    "FIXEDTOTAL" -> cartItem.discountAmount
                    else -> 0.0
                }
            }

            val itemVat = itemTotal * 0.12 / 1.12

            val transactionRecord = TransactionRecord(
                transactionId = transactionId,
                name = cartItem.productName,
                price = cartItem.price,
                quantity = cartItem.quantity,
                subtotal = itemTotal,
                vatRate = vatRate - 1,
                vatAmount = itemVat,
                discountRate = when (cartItem.discountType.uppercase()) {
                    "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
                    else -> if (itemTotal > 0) itemDiscount / itemTotal else 0.0
                },
                discountAmount = when (cartItem.discountType.uppercase()) {
                    "FIXED" -> cartItem.discount  // Store per-item discount
                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)  // Store total discount
                    "FIXEDTOTAL" -> cartItem.discountAmount  // Store fixed total discount
                    else -> 0.0
                },
                total = itemTotal - itemDiscount,
                receiptNumber = transactionId,
                timestamp = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                ar = if (ar > 0.0) ((cartItem.overriddenPrice
                    ?: cartItem.price) * cartItem.quantity) else 0.0,
                windowNumber = windowId,
                partialPaymentAmount = if (index == 0) partialPayment else 0.0,
                comment = cartItem.cartComment ?: "",
                lineNum = index + 1,
                receiptId = transactionId,
                itemId = cartItem.itemId.toString(),
                itemGroup = cartItem.itemGroup.toString(),
                netPrice = cartItem.price,
                costAmount = itemTotal / vatRate,
                netAmount = itemTotal - itemDiscount,
                grossAmount = itemTotal,
                customerAccount = null,
                store = getCurrentStore(),
                priceOverride = cartItem.overriddenPrice ?: 0.0,
                staff = getCurrentStaff(),
                discountOfferId = null,
                lineDiscountAmount = itemDiscount,
                lineDiscountPercentage = if (itemTotal > 0) (itemDiscount / itemTotal) * 100 else 0.0,
                customerDiscountAmount = itemDiscount,
                unit = "PCS",
                unitQuantity = cartItem.quantity.toDouble(),
                unitPrice = cartItem.price,
                taxAmount = itemVat,
                createdDate = Date(),
                remarks = null,
                inventoryBatchId = null,
                inventoryBatchExpiryDate = null,
                giftCard = null,
                returnTransactionId = null,
                returnQuantity = null,
                creditMemoNumber = null,
                taxIncludedInPrice = itemVat,
                description = null,
                returnLineId = null,
                priceUnit = 1.0,
                discountType = when (cartItem.discountType.toUpperCase()) {
                    "PWD" -> "PWD"
                    "SC" -> "SC"
                    "PERCENTAGE" -> "PERCENTAGE"
                    "FIXED" -> "FIXED"
                    "FIXEDTOTAL", "FIXED TOTAL" -> "FIXEDTOTAL"
                    else -> "No Discount"
                },
                netAmountNotIncludingTax = itemTotal - itemDiscount - itemVat,
                storeTaxGroup = null,
                currency = "PHP",
                taxExempt = 0.0
            )
            Log.d(
                "TransactionDebug", """
            Created TransactionRecord:
            Item: ${transactionRecord.name}
            DiscountType: ${transactionRecord.discountType}
            DiscountAmount: ${transactionRecord.discountAmount}
            Original CartItem DiscountAmount: ${cartItem.discountAmount}
        """.trimIndent()
            )

            transactionDao.insertOrUpdateTransactionRecord(transactionRecord)
        }
    }

    private fun getTransactionRecords(
        transactionId: String,
        cartItems: List<CartItem>,
        paymentMethod: String,
        ar: Double,
        vatRate: Double,
        discountType: String
    ): List<TransactionRecord> {
        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
        val existingDiscount = if (existingPartialPayment > 0) {
            cartItems.firstOrNull()?.discountAmount ?: 0.0
        } else null

        return cartItems.mapIndexed { index, cartItem ->
            val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
            val itemTotal = effectivePrice * cartItem.quantity

            // Use existing discount if partial payment exists
            val itemDiscount = if (existingDiscount != null) {
                if (index == 0) existingDiscount else 0.0 // Apply discount only to first item
            } else {
                when (cartItem.discountType.uppercase()) {
                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)
                    "FIXED" -> cartItem.discount * cartItem.quantity
                    "FIXEDTOTAL" -> cartItem.discountAmount
                    else -> 0.0
                }
            }

            val itemVat = itemTotal * 0.12 / 1.12

            TransactionRecord(
                transactionId = transactionId,
                name = cartItem.productName,
                price = cartItem.price,
                quantity = cartItem.quantity,
                subtotal = itemTotal,
                vatRate = vatRate - 1,
                vatAmount = itemVat,
                discountRate = when (cartItem.discountType.uppercase()) {
                    "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
                    else -> if (itemTotal > 0) itemDiscount / itemTotal else 0.0
                },
                discountAmount = when (cartItem.discountType.uppercase()) {
                    "FIXED" -> cartItem.discount  // Store per-item discount
                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)  // Store total discount
                    "FIXEDTOTAL" -> cartItem.discountAmount  // Store fixed total discount
                    else -> 0.0
                },
                total = itemTotal - itemDiscount,
                receiptNumber = transactionId,
                timestamp = System.currentTimeMillis(),
                paymentMethod = paymentMethod,
                ar = if (ar > 0.0) (effectivePrice * cartItem.quantity) else 0.0,
                windowNumber = windowId,
                partialPaymentAmount = if (index == 0) cartItem.partialPayment ?: 0.0 else 0.0,
                comment = cartItem.cartComment ?: "",
                lineNum = index + 1,
                receiptId = transactionId,
                itemId = cartItem.productId.toString(),
                itemGroup = cartItem.id.toString(),
                netPrice = cartItem.price,
                costAmount = itemTotal / vatRate,
                netAmount = itemTotal - itemDiscount,
                grossAmount = itemTotal,
                customerAccount = null,
                store = getCurrentStore(),
                priceOverride = cartItem.overriddenPrice ?: 0.0,
                staff = getCurrentStaff(),
                discountOfferId = null,
                lineDiscountAmount = itemDiscount,
                lineDiscountPercentage = if (itemTotal > 0) (itemDiscount / itemTotal) * 100 else 0.0,
                customerDiscountAmount = itemDiscount,
                unit = "PCS",
                unitQuantity = cartItem.quantity.toDouble(),
                unitPrice = cartItem.price,
                taxAmount = itemVat,
                createdDate = Date(),
                remarks = null,
                inventoryBatchId = null,
                inventoryBatchExpiryDate = null,
                giftCard = null,
                returnTransactionId = null,
                returnQuantity = null,
                creditMemoNumber = null,
                taxIncludedInPrice = itemVat,
                description = null,
                returnLineId = null,
                priceUnit = 1.0,
                discountType = when (cartItem.discountType.toUpperCase()) {
                    "PWD" -> "PWD"
                    "SC" -> "SC"
                    "PERCENTAGE" -> "PERCENTAGE"
                    "FIXED" -> "FIXED"
                    "FIXED TOTAL", "FIXEDTOTAL" -> "FIXEDTOTAL"
                    else -> "No Discount"
                },
                netAmountNotIncludingTax = itemTotal - itemDiscount - itemVat,
                storeTaxGroup = null,
                currency = "PHP",
                taxExempt = 0.0
            )
        }
    }

    private fun showChangeAndReceiptDialog(
        change: Double,
        cartItems: List<CartItem>,
        receiptNumber: String,
        paymentMethod: String,
        ar: Double,
        vatAmount: Double,
        totalDiscount: Double,
        netSales: Double,
        discountRate: Double,
        transactionComment: String,
        partialPaymentAmount: Double,
        finalPaymentAmount: Double
    ) {
        // Create a layout for the dialog with more padding for better spacing
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)  // Increased padding
            gravity = Gravity.CENTER
        }

        // Enhanced "Change" label TextView
        val labelTextView = TextView(this).apply {
            text = "Change Amount"  // More descriptive label
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)  // Larger text
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)  // More space below the label
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)  // Modern font
        }

        // Create a container for amount display
        val amountContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)

            // Optional: Add a background for the amount section
            background = ColorDrawable(ContextCompat.getColor(context, android.R.color.white))
        }

        // Enhanced peso sign TextView
        val pesoSignTextView = TextView(this).apply {
            text = "₱"
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)  // Larger peso sign
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(0, 0, 8, 0)  // Add some space between peso sign and amount
        }

        // Enhanced Change amount TextView
        val changeTextView = TextView(this).apply {
            text = "%.2f".format(change)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 72f)  // Larger amount
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        // Add views to the amount container
        amountContainer.addView(pesoSignTextView)
        amountContainer.addView(changeTextView)

        // Add a divider for visual separation
        val divider = View(this).apply {
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                2  // 2dp height
            ).apply {
                setMargins(32, 32, 32, 32)
            }
        }

        // Transaction details (optional)
        val detailsTextView = TextView(this).apply {
            text = "Receipt #$receiptNumber\n$paymentMethod"
            setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        // Add all views to main layout
        layout.apply {
            addView(labelTextView)
            addView(amountContainer)
            addView(divider)
            addView(detailsTextView)
        }

        // Enhanced CardView
        val cardView = CardView(this).apply {
            radius = 16f * resources.displayMetrics.density  // Larger corner radius
            cardElevation = 8f * resources.displayMetrics.density  // More pronounced shadow
            setContentPadding(24, 24, 24, 24)
            setCardBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(48, 48, 48, 48)
            }
            addView(layout)
        }

        // Create and show the enhanced dialog
        AlertDialog.Builder(this)
            .setView(cardView)
            .setPositiveButton("Done") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .apply {
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                show()
            }

        Log.d(TAG, "Displayed change dialog: $change")
    }
}