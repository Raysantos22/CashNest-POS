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
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.http.SslError
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.AutoCompleteTextView
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListAdapter
import android.widget.RelativeLayout
import android.widget.Switch
import androidx.appcompat.widget.SearchView // Change this import
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
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
import com.example.possystembw.data.NumberSequenceRemoteRepository
//import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.NumberSequenceRepository
import com.example.possystembw.database.Customer
import com.example.possystembw.database.LineGroupWithDiscounts
import com.example.possystembw.database.MixMatch
import com.example.possystembw.database.MixMatchWithDetails
import com.example.possystembw.database.ProductBundle
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.Window
import com.example.possystembw.database.ZRead
import com.example.possystembw.ui.ViewModel.ARViewModel
import com.example.possystembw.ui.ViewModel.ARViewModelFactory
import com.example.possystembw.ui.ViewModel.CustomerViewModel
import com.example.possystembw.ui.ViewModel.CustomerViewModelFactory
import com.example.possystembw.ui.ViewModel.MixMatchViewModel
import com.example.possystembw.ui.ViewModel.MixMatchViewModelFactory
import com.example.possystembw.ui.ViewModel.TransactionSyncService
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import java.nio.charset.Charset
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.possystembw.DAO.NumberSequenceRemoteDao
import com.example.possystembw.DAO.TransactionRecordRequest
import com.example.possystembw.DAO.TransactionSummaryRequest
import com.example.possystembw.DAO.TransactionSyncRequest
import com.example.possystembw.MainActivity

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


    private lateinit var numberSequenceRemoteRepository: NumberSequenceRemoteRepository
    private lateinit var numberSequenceRemoteDao: NumberSequenceRemoteDao

    private lateinit var sidebarLayout: ConstraintLayout
    private lateinit var sidebarToggleButton: ImageButton
    private lateinit var buttonContainer: LinearLayout
    private lateinit var insertButton: Button
    private lateinit var toggleButton: ImageButton
    private lateinit var overlayLayout: ConstraintLayout
    private lateinit var searchCardView: CardView
    private var isSidebarExpanded = true
    private var transactionSyncService: TransactionSyncService? = null  // Make nullable
    private lateinit var loadingDialog: AlertDialog


    private lateinit var webView: WebView
    private lateinit var webViewLoadingOverlay: FrameLayout
    private lateinit var webViewLoadingText: TextView

    private lateinit var webViewContainer: RelativeLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityWindow1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            database = AppDatabase.getDatabase(this)

            val numberSequenceApi = RetrofitClient.numberSequenceApi
            val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
            numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
                numberSequenceApi,
                numberSequenceRemoteDao
            )


            getWindowId()
            initializeRepositories()
            initializeViewModels()
            setupRecyclerViews()
            observeViewModels()
            setupButtonListeners()
//            initializeAutoDatabaseTransferManager()
            checkForExistingCashFund()
            setupButtonListeners()
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

            initializeSidebarComponents()
            initializeOverlayComponents()
            setupSidebar()
            setupOverlay()
//            setupNumberSequenceViewModel()


//            setupMixMatchButton() // Add this line

            webView = findViewById(R.id.webView)

            initializeSidebarComponents()
            setupSidebar()



        } catch (e: Exception) {
            Log.e(TAG, "Error during onCreate", e)
            Toast.makeText(this, "Initialization Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }


    private fun initializeDatabase() {
        // Remove numberSequenceRemoteRepository initialization from here since it's done in onCreate
        tenderDeclarationDao = database.tenderDeclarationDao()

        val repository = TransactionRepository(
            database.transactionDao(),
            numberSequenceRemoteRepository
        )
        transactionSyncService = TransactionSyncService(repository)
        transactionSyncService?.startSyncService(lifecycleScope)
    }
    override fun onResume() {
        super.onResume()

        // Check session validity and refresh
        if (!SessionManager.isSessionValid()) {
            // Redirect to login if session is invalid
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
        // Refresh session timestamp on activity resume
        SessionManager.refreshSession()
    }
    private fun initializeSidebarComponents() {
        try {
            sidebarLayout = findViewById(R.id.sidebarLayout)
            sidebarToggleButton = findViewById(R.id.toggleButton1)
            buttonContainer = findViewById(R.id.buttonContainer)

            // Set initial state
            isSidebarExpanded = true
            buttonContainer.visibility = View.VISIBLE
            buttonContainer.alpha = 1f
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sidebar components", e)
        }
    }



    private fun initializeOverlayComponents() {
        try {
            toggleButton = findViewById(R.id.toggleButton)
            overlayLayout = findViewById(R.id.overlayLayout)
            searchCardView = findViewById(R.id.searchCardView)
            insertButton = findViewById(R.id.insertButton)

            // Set initial state
            overlayLayout.visibility = View.GONE
            searchCardView.visibility = View.VISIBLE
            insertButton.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing overlay components", e)
        }
    }

    private fun setupSidebar() {
        try {
            sidebarToggleButton.setOnClickListener {
                if (isSidebarExpanded) {
                    collapseSidebar()
                } else {
                    expandSidebar()
                }
            }
            setupSidebarButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up sidebar", e)
        }
    }

    private fun setupOverlay() {
        try {
            toggleButton.setOnClickListener {
                val isOverlayVisible = overlayLayout.visibility == View.VISIBLE

                // Toggle visibility with animation
                if (isOverlayVisible) {
                    overlayLayout.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction {
                            overlayLayout.visibility = View.GONE
                            searchCardView.visibility = View.VISIBLE
                            insertButton.visibility = View.VISIBLE
                        }
                } else {
                    overlayLayout.alpha = 0f
                    overlayLayout.visibility = View.VISIBLE
                    searchCardView.visibility = View.GONE
                    insertButton.visibility = View.GONE
                    overlayLayout.animate()
                        .alpha(1f)
                        .setDuration(200)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up overlay", e)
        }
    }
//    private fun setupSidebarButtons() {
//        findViewById<ImageButton>(R.id.button2).setOnClickListener {
//            showToast("Dashboard")
//        }
//        findViewById<ImageButton>(R.id.button3).setOnClickListener {
//            showToast("Shopping Cart")
//        }
//        findViewById<ImageButton>(R.id.button4).setOnClickListener {
//            showToast("Orders")
//        }
//        findViewById<ImageButton>(R.id.button5).setOnClickListener {
//            showToast("Delivery")
//        }
//        findViewById<ImageButton>(R.id.button6).setOnClickListener {
//            showToast("Analytics")
//        }
//        findViewById<ImageButton>(R.id.button7).setOnClickListener {
//            showToast("Cashier")
//        }
//        findViewById<ImageButton>(R.id.button8).setOnClickListener {
////            logout()
//            showToast("Cashier")
//
//        }
//    }
    fun setupSidebarButtons() {
        findViewById<ImageButton>(R.id.button2).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/dashboard", "DASHBOARD")
        }

        findViewById<ImageButton>(R.id.button3).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/order", "ORDERING")
        }

        findViewById<ImageButton>(R.id.button4).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/StockCounting", "STOCK COUNTING")
        }

        findViewById<ImageButton>(R.id.button5).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/Received", "RECEIVING")
        }

        findViewById<ImageButton>(R.id.button6).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/reports", "REPORTS")
        }

        findViewById<ImageButton>(R.id.waste).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/waste", "WASTE")
        }

        findViewById<ImageButton>(R.id.partycakes).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/partycakes", "PARTYCAKES")
        }

        findViewById<ImageButton>(R.id.customer).setOnClickListener {
            navigateToMainWithUrl("https://eljin.org/customers", "CUSTOMER")
        }

        findViewById<ImageButton>(R.id.button7).setOnClickListener {
            navigateToMainWithUrl(null, "POS SYSTEM")
        }


    findViewById<ImageButton>(R.id.button8).setOnClickListener {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}

    private fun navigateToMainWithUrl(url: String?, message: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            url?.let { putExtra("web_url", it) }
        }
        message?.let { showToast(it) }
        startActivity(intent)
    }


    //    private fun logout() {
//        // Clear the session
//        SessionManager.clearCurrentUser()
//
//        // Cancel any ongoing jobs
//        if (::refreshJob.isInitialized) {
//            refreshJob.cancel()
//        }
//
//        // Navigate back to login screen
//        val intent = Intent(this, LoginActivity::class.java)
//        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        startActivity(intent)
//        finish()
//    }
    private fun showToast(message: String) {
        Toast.makeText(this@Window1, message, Toast.LENGTH_SHORT).show()
    }
    private fun collapseSidebar() {
        try {
            val collapse = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(24))
            collapse.duration = 200
            collapse.interpolator = AccelerateDecelerateInterpolator()

            collapse.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.updateLayoutParams {
                    width = value
                }
                updateContentMargins(value)
            }

            collapse.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    buttonContainer.animate()
                        .alpha(0f)
                        .setDuration(100)
                        .start()
                    sidebarToggleButton.animate()
                        .rotation(180f)
                        .setDuration(200)
                        .start()
                    findViewById<TextView>(R.id.ecposTitle)?.animate()
                        ?.alpha(0f)
                        ?.setDuration(100)
                        ?.start()
                }

                override fun onAnimationEnd(animation: Animator) {
                    isSidebarExpanded = false
                    buttonContainer.visibility = View.GONE
                    sidebarToggleButton.layoutParams =
                        (sidebarToggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                            marginStart = dpToPx(8)
                        }
                }
            })
            collapse.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error collapsing sidebar", e)
        }
    }

    private fun expandSidebar() {
        try {
            val expand = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(56))
            expand.duration = 200
            expand.interpolator = AccelerateDecelerateInterpolator()

            expand.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.updateLayoutParams {
                    width = value
                }
                updateContentMargins(value)
            }

            expand.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    buttonContainer.visibility = View.VISIBLE
                    buttonContainer.alpha = 0f
                    sidebarToggleButton.animate()
                        .rotation(0f)
                        .setDuration(200)
                        .start()
                    findViewById<TextView>(R.id.ecposTitle)?.animate()
                        ?.alpha(1f)
                        ?.setDuration(200)
                        ?.start()
                }

                override fun onAnimationEnd(animation: Animator) {
                    buttonContainer.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .start()
                    isSidebarExpanded = true
                    sidebarToggleButton.layoutParams =
                        (sidebarToggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                            marginStart = dpToPx(40)
                        }
                }
            })
            expand.start()
        } catch (e: Exception) {
            Log.e(TAG, "Error expanding sidebar", e)
        }
    }

    private fun updateContentMargins(sidebarWidth: Int) {
        try {
            findViewById<TextView>(R.id.textView3)?.updateLayoutParams<ConstraintLayout.LayoutParams> {
                marginStart = sidebarWidth + dpToPx(10)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating content margins", e)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
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
                        // Check if the item is returned and apply strikethrough if needed
                        val itemName = if (item.isReturned) "~~${item.name}~~" else item.name
                        val itemQuantity = if (item.isReturned) item.quantity else item.quantity

                        append("$itemName\n")
                        append("   ${itemQuantity} x ₱${String.format("%.2f", item.price)}")
                        append(" = ₱${String.format("%.2f", item.netAmount)}\n")

                        // Optionally, add a note for returned items
                        if (item.isReturned) {
                            append("   (Returned)\n")
                        }
                    }
                    append("\n${"-".repeat(40)}\n\n")
                }
                val spannableString = SpannableString(itemsDetails)
                items.forEachIndexed { index, item ->
                    if (item.isReturned) {
                        // Apply strikethrough span to the specific item line
                        val start = itemsDetails.indexOf(item.name)
                        val end = start + item.name.length
                        spannableString.setSpan(
                            StrikethroughSpan(),
                            start,
                            end,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                    }
                }
                detailsTextView.text = spannableString
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

            // Log the reprint first
            val transactionLogger = TransactionLogger(this@Window1)
            transactionLogger.logReprint(
                transaction = transaction,
                items = items  // Make sure you're passing the items
            )

            // Try to print after logging
            try {
                val printerMacAddress = "DC:0D:30:70:09:19"

                if (!bluetoothPrinterHelper.isConnected() && !bluetoothPrinterHelper.connect(printerMacAddress)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Failed to connect to printer, but reprint was logged",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val receiptContent = bluetoothPrinterHelper.generateReceiptContent(
                    transaction,
                    items,
                    BluetoothPrinterHelper.ReceiptType.REPRINT
                )

                bluetoothPrinterHelper.outputStream?.write(receiptContent.toByteArray())
                bluetoothPrinterHelper.outputStream?.flush()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Receipt reprinted and logged successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error printing receipt but reprint was logged: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error printing receipt but reprint was logged: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during reprint process: ${e.message}")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@Window1,
                    "Error during reprint process: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
                val returnable = items.filter { item ->
                    item.quantity > 0 && !item.isReturned
                }
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
    private fun processReturnTransaction(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        remarks: String
    ) {
        lifecycleScope.launch {
            try {
                // Get current store from session
                val currentStore = SessionManager.getCurrentUser()?.storeid
                    ?: throw IllegalStateException("No store ID found in current session")

                // Generate return transaction ID using number sequence
                val returnTransactionId = numberSequenceRemoteRepository.getNextTransactionNumber(currentStore)

                // Get store key and generate store-specific sequence
                val storeKey = numberSequenceRemoteRepository.getCurrentStoreKey(currentStore)
                val storeSequence = "$currentStore-${returnTransactionId.split("-").last()}"

                // Get original transaction records to update
                val originalTransactionItems = transactionViewModel.getTransactionItems(transaction.transactionId)

                // Calculate return amounts
                val returnedItems = items.map { item ->
                    val effectivePrice = item.priceOverride ?: item.price
                    val returnQuantity = item.quantity

                    // Calculate return amounts
                    val returnGrossAmount = effectivePrice * returnQuantity
                    val returnDiscountAmount = when (item.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> effectivePrice * returnQuantity * item.discountRate
                        "FIXED" -> item.discountAmount * returnQuantity
                        "FIXEDTOTAL" -> item.discountAmount
                        else -> 0.0
                    }
                    val returnNetAmount = returnGrossAmount - returnDiscountAmount
                    val returnVatAmount = returnNetAmount * 0.12 / 1.12
                    val returnVatableSales = returnNetAmount / 1.12

                    // Create return transaction record with negative quantities
                    TransactionRecord(
                        transactionId = returnTransactionId,
                        name = item.name,
                        price = item.price,
                        quantity = -returnQuantity,
                        subtotal = -returnGrossAmount,
                        vatRate = item.vatRate,
                        vatAmount = -returnVatAmount,
                        discountRate = item.discountRate,
                        discountAmount = -returnDiscountAmount,
                        total = -returnNetAmount,
                        receiptNumber = returnTransactionId,
                        paymentMethod = transaction.paymentMethod,
                        ar = 0.0,
                        comment = "Return: $remarks",
                        lineNum = item.lineNum,
                        itemId = item.itemId,
                        itemGroup = item.itemGroup,
                        netPrice = item.netPrice,
                        costAmount = item.costAmount?.let { -it },
                        netAmount = item.netAmount?.let { -it },
                        grossAmount = item.grossAmount?.let { -it },
                        customerAccount = item.customerAccount,
                        store = item.store,
                        priceOverride = item.priceOverride,
                        staff = getCurrentStaff(),
                        discountOfferId = item.discountOfferId,
                        lineDiscountAmount = item.lineDiscountAmount?.let { -it },
                        lineDiscountPercentage = item.lineDiscountPercentage,
                        customerDiscountAmount = item.customerDiscountAmount?.let { -it },
                        taxAmount = item.taxAmount?.let { -it },
                        isReturned = true,
                        returnTransactionId = transaction.transactionId,
                        returnQuantity = returnQuantity.toDouble(),
                        returnLineId = item.lineNum?.toDouble() ?: 0.0,
                        storeKey = storeKey,
                        storeSequence = storeSequence,
                        unit = "PCS",
                        unitQuantity = item.unitQuantity,
                        unitPrice = item.price,
                        createdDate = item.createdDate,
                        taxExempt = 0.0,
                        currency = "PHP",
                        discountType = item.discountType,
                        netAmountNotIncludingTax = item.netAmountNotIncludingTax?.let { -it }

                    )
                }

                // Update original transaction items to mark them as returned
                val updatedOriginalItems = originalTransactionItems.map { originalItem ->
                    val matchingReturnItem = returnedItems.find {
                        it.itemId == originalItem.itemId &&
                                it.lineNum == originalItem.lineNum
                    }

                    if (matchingReturnItem != null) {
                        originalItem.copy(
                            isReturned = true,
                            returnTransactionId = returnTransactionId,
                            returnQuantity = matchingReturnItem.returnQuantity?.absoluteValue ?: 0.0
                        )
                    } else {
                        originalItem
                    }
                }

                // Create return transaction summary with all negative values
                val returnTransactionSummary = TransactionSummary(
                    transactionId = returnTransactionId,
                    type = 2, // Return transaction type
                    receiptId = returnTransactionId,
                    store = transaction.store,
                    staff = getCurrentStaff(),
                    storeKey = storeKey,
                    storeSequence = storeSequence,
                    customerAccount = transaction.customerAccount,
                    netAmount = -abs(returnedItems.sumOf { it.netAmount ?: 0.0 }),
                    costAmount = -abs(returnedItems.sumOf { it.costAmount ?: 0.0 }),
                    grossAmount = -abs(returnedItems.sumOf { it.grossAmount ?: 0.0 }),
                    partialPayment = 0.0,
                    transactionStatus = 1,
                    discountAmount = -abs(returnedItems.sumOf { it.discountAmount }),
                    customerDiscountAmount = -abs(returnedItems.sumOf { it.discountAmount }),
                    totalDiscountAmount = -abs(returnedItems.sumOf { it.discountAmount }),
                    numberOfItems = -abs(returnedItems.sumOf { it.quantity.toDouble() }),
                    refundReceiptId = transaction.transactionId,
                    currency = "PHP",
                    zReportId = null,
                    createdDate = Date(),
                    priceOverride = 0.0,
                    comment = "Return for transaction: ${transaction.transactionId} - $remarks",
                    receiptEmail = null,
                    markupAmount = 0.0,
                    markupDescription = null,
                    taxIncludedInPrice = -abs(returnedItems.sumOf { it.vatAmount }),
                    windowNumber = transaction.windowNumber,
                    paymentMethod = transaction.paymentMethod,
                    customerName = transaction.customerName,
                    vatAmount = -abs(returnedItems.sumOf { it.vatAmount }),
                    vatExemptAmount = 0.0,
                    vatableSales = -abs(returnedItems.sumOf { it.netAmount ?: 0.0 }),
                    discountType = transaction.discountType,
                    totalAmountPaid = -abs(transaction.totalAmountPaid),
                    changeGiven = -abs(transaction.changeGiven),
                    gCash = if (transaction.paymentMethod.uppercase() == "GCASH" || transaction.paymentMethod.uppercase() == "Gcash")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    payMaya = if (transaction.paymentMethod.uppercase() == "PAYMAYA" || transaction.paymentMethod.uppercase() == "Paymaya")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    cash = if (transaction.paymentMethod.uppercase() == "CASH" || transaction.paymentMethod.uppercase() == "Cash")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    card = if (transaction.paymentMethod.uppercase() == "CARD" || transaction.paymentMethod.uppercase() == "Card")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    loyaltyCard = if (transaction.paymentMethod.uppercase() == "LOYALTYCARD" || transaction.paymentMethod.uppercase() == "Loyaltycard")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    charge = if (transaction.paymentMethod.uppercase() == "CHARGE" || transaction.paymentMethod.uppercase() == "Charge")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    foodpanda = if (transaction.paymentMethod.uppercase() == "FOODPANDA" || transaction.paymentMethod.uppercase() == "Foodpanda")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    grabfood = if (transaction.paymentMethod.uppercase() == "GRABFOOD" || transaction.paymentMethod.uppercase() == "Grabfood")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    representation = if (transaction.paymentMethod.uppercase() == "REPRESENTATION" || transaction.paymentMethod.uppercase() == "Representation")
                        -abs(returnedItems.sumOf { it.netAmount ?: 0.0 })
                    else 0.0,
                    syncStatus = false
                )

                // Perform database operations
                transactionDao.apply {
                    // Update original transaction items to mark returned items
                    updateTransactionRecords(updatedOriginalItems)

                    // Insert return transaction summary
                    insertTransactionSummary(returnTransactionSummary)

                    // Insert return transaction records
                    insertAll(returnedItems)

                    // Update original transaction's refund receipt ID
                    updateRefundReceiptId(transaction.transactionId, returnTransactionId)
                }
                // Sync the return transaction
                transactionViewModel.syncTransaction(returnTransactionId)

                // Observe the sync result
                transactionViewModel.syncStatus.observe(this@Window1) { result ->
                    result.fold(
                        onSuccess = { response ->
                            Log.e(
                                "Return",
                                "Return transaction synced successfully: ${response}"
                            )
                            Toast.makeText(
                                this@Window1,
                                "Return transaction synced successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { error ->
                            Log.e("Return", "Sync failed: ${error.message}")
                            Toast.makeText(
                                this@Window1,
                                "Return transaction saved locally but sync failed: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    )
                }
                val transactionLogger = TransactionLogger(this@Window1)
                transactionLogger.logReturn(
                    returnTransaction = returnTransactionSummary,
                    originalTransaction = transaction,
                    returnedItems = returnedItems,
                    remarks = remarks
                )
                // Update inventory
                updateInventory(returnedItems)

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Return processed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    printReturnReceipt(returnTransactionSummary, returnedItems, remarks)
                }
            } catch (e: Exception) {
                Log.e("ReturnTransaction", "Error processing return: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error processing return: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

//
//    private fun processReturn(
//        returnDialog: AlertDialog,
//        transaction: TransactionSummary,
//        returnable: List<TransactionRecord>,
//        remarksEditText: TextInputEditText,
//        returnButton: Button
//    ) {
//        val selectedItems = getSelectedItems(returnable)
//        val remarks = remarksEditText.text.toString().trim()
//
//        when {
//            selectedItems.isEmpty() -> {
//                Toast.makeText(this, "Please select items to return", Toast.LENGTH_SHORT).show()
//            }
//
//            remarks.isEmpty() -> {
//                remarksEditText.error = "Remarks are required"
//            }
//
//            else -> {
//                lifecycleScope.launch {
//                    try {
//                        // Show loading dialog
//                        val loadingDialog = AlertDialog.Builder(this@Window1)
//                            .setMessage("Processing return...")
//                            .setCancelable(false)
//                            .create()
//                        loadingDialog.show()
//
//                        val updatedItems = selectedItems.map { it.copy(isReturned = true) }
//                        transactionViewModel.updateTransactionRecords(updatedItems)
//                        processReturnTransaction(transaction, selectedItems, remarks)
//                        loadingDialog.dismiss()
//                        returnDialog.dismiss()
////                        recreate()
//                        Toast.makeText(
//                            this@Window1,
//                            "Return processed successfully",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    } catch (e: Exception) {
//                        Log.e(TAG, "Error processing return", e)
//                        Toast.makeText(
//                            this@Window1,
//                            "Error processing return: ${e.message}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                }
//            }
//        }
//    }
//    private fun processReturn//    private fun processReturnTransaction(
////        transaction: TransactionSummary,
////        items: List<TransactionRecord>,
////        remarks: String
////    ) {
////        lifecycleScope.launch {
////            try {
////                // Calculate amounts directly from selected items without any doubling
////                val returnedGrossAmount = items.sumOf { item ->
////                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
////                    effectivePrice * item.quantity
////                }
////
////                // Calculate discount amount directly from the items
////                val returnedDiscountAmount = items.sumOf { item ->
////                    when (item.discountType.uppercase()) {
////                        "PERCENTAGE", "PWD", "SC" -> {
////                            val itemTotal = (if (item.priceOverride!! > 0.0) item.priceOverride else item.price) * item.quantity
////                            itemTotal * item.discountRate
////                        }
////                        "FIXED" -> item.discountAmount * item.quantity
////                        "FIXEDTOTAL" -> item.discountAmount
////                        else -> 0.0
////                    }
////                }
////
////                // Calculate net amount as gross minus discount
////                val returnedNetAmount = returnedGrossAmount - returnedDiscountAmount
////
////                // Calculate VAT components from net amount only once
////                val returnedVatAmount = returnedNetAmount * 0.12 / 1.12
////                val returnedVatableSales = returnedNetAmount / 1.12
////
////                // Calculate partial payment proportion if exists
////                val returnProportion = if (transaction.netAmount != 0.0) {
////                    returnedNetAmount / transaction.netAmount
////                } else 0.0
////
////                val returnedPartialPayment = transaction.partialPayment * returnProportion
////
////                // Update transaction with correct amounts
////                val updatedTransaction = transaction.copy(
////                    netAmount = transaction.netAmount - returnedNetAmount,
////                    costAmount = transaction.costAmount - items.sumOf { it.costAmount ?: 0.0 },
////                    grossAmount = transaction.grossAmount - returnedGrossAmount,
////                    discountAmount = transaction.discountAmount - returnedDiscountAmount,
////                    customerDiscountAmount = transaction.customerDiscountAmount - returnedDiscountAmount,
////                    totalDiscountAmount = transaction.totalDiscountAmount - returnedDiscountAmount,
////                    numberOfItems = transaction.numberOfItems - items.sumOf { it.quantity.toDouble() },
////                    taxIncludedInPrice = transaction.taxIncludedInPrice - returnedVatAmount,
////                    vatAmount = transaction.vatAmount - returnedVatAmount,
////                    vatableSales = transaction.vatableSales - returnedVatableSales,
////                    partialPayment = transaction.partialPayment - returnedPartialPayment,
////                    comment = "${transaction.comment}\nReturn processed: $remarks"
////                )
////
////                // Create return items with correct negative amounts
////                val returnedItems = items.map { item ->
////                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
////                    val itemTotal = effectivePrice * item.quantity
////                    val itemDiscount = when (item.discountType.uppercase()) {
////                        "PERCENTAGE", "PWD", "SC" -> itemTotal * item.discountRate
////                        "FIXED" -> item.discountAmount * item.quantity
////                        "FIXEDTOTAL" -> item.discountAmount
////                        else -> 0.0
////                    }
////                    val netAmount = itemTotal - itemDiscount
////                    val vatAmount = netAmount * 0.12 / 1.12
////
////                    item.copy(
////                        quantity = -item.quantity,
////                        subtotal = 0.0,
////                        grossAmount = 0.0,
////                        costAmount = 0.0,
////                        netAmount = 0.0,
////                        vatAmount = 0.0,
////                        taxAmount = 0.0,
////                        taxIncludedInPrice = 0.0,
////                        netAmountNotIncludingTax = 0.0,
////                        discountAmount = 0.0,
////                        lineDiscountAmount = 0.0,
////                        customerDiscountAmount = 0.0,
////                        returnTransactionId = transaction.transactionId,
////                        returnQuantity = -item.quantity.toDouble(),
////                        returnLineId = item.lineNum?.toDouble() ?: 0.0,
////                        comment = "Returned: $remarks",
////                        discountRate = item.discountRate,
////                        priceOverride = item.priceOverride,
////                        discountType = item.discountType,
////                        total = 0.0
////                    )
////                }
////
////                // Update local database
////                transactionViewModel.updateTransactionSummary(updatedTransaction)
////                transactionViewModel.updateTransactionRecords(returnedItems)
////                updateInventory(returnedItems)
////
////                // Process refund on server
////                val currentStore = SessionManager.getCurrentUser()?.storeid
////                    ?: throw IllegalStateException("No store ID found in current session")
////
////                val refundResult = transactionViewModel.repository.processRefundOnServer(
////                    currentStore,
////                    updatedTransaction,
////                    returnedItems,
////                    remarks
////                )
////
////                // Handle the refund result
////                refundResult.fold(
////                    onSuccess = { response ->
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(
////                                this@Window1,
////                                "Return processed and synced successfully",
////                                Toast.LENGTH_SHORT
////                            ).show()
////                            // Print return receipt only after successful server sync
////                            printReturnReceipt(updatedTransaction, returnedItems, remarks)
////                        }
////                    },
////                    onFailure = { error ->
////                        Log.e("ReturnTransaction", "Error syncing return with server: ${error.message}", error)
////                        withContext(Dispatchers.Main) {
////                            Toast.makeText(
////                                this@Window1,
////                                "Return processed locally but sync failed: ${error.message}",
////                                Toast.LENGTH_LONG
////                            ).show()
////                        }
////                        // Still print receipt even if sync fails, but mark it as "Not Synced"
////                        printReturnReceipt(updatedTransaction, returnedItems, "$remarks (Not Synced)")
////                    }
////                )
////
////                // Update sync status in local database
////                transactionViewModel.repository.transactionDao.updateSyncStatus(
////                    updatedTransaction.transactionId,
////                    refundResult.isSuccess
////                )
////
////            } catch (e: Exception) {
////                Log.e("ReturnTransaction", "Error processing return: ${e.message}", e)
////                withContext(Dispatchers.Main) {
////                    Toast.makeText(
////                        this@Window1,
////                        "Error processing return: ${e.message}",
////                        Toast.LENGTH_LONG
////                    ).show()
////                    transactionViewModel.loadTransactions()
////                }
////            }
////        }
////    }(
//        transaction: TransactionSummary,
//        items: List<TransactionRecord>,
//        remarks: String
//    ) {
//        lifecycleScope.launch {
//            try {
//                // Calculate amounts directly from selected items without any doubling
//                val returnedGrossAmount = items.sumOf { item ->
//                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
//                    effectivePrice * item.quantity
//                }
//
//                // Calculate discount amount directly from the items
//                val returnedDiscountAmount = items.sumOf { item ->
//                    when (item.discountType.uppercase()) {
//                        "PERCENTAGE", "PWD", "SC" -> {
//                            val itemTotal = (if (item.priceOverride!! > 0.0) item.priceOverride else item.price) * item.quantity
//                            itemTotal * item.discountRate
//                        }
//                        "FIXED" -> item.discountAmount * item.quantity
//                        "FIXEDTOTAL" -> item.discountAmount
//                        else -> 0.0
//                    }
//                }
//
//                // Calculate net amount as gross minus discount
//                val returnedNetAmount = returnedGrossAmount - returnedDiscountAmount
//
//                // Calculate VAT components from net amount only once
//                val returnedVatAmount = returnedNetAmount * 0.12 / 1.12
//                val returnedVatableSales = returnedNetAmount / 1.12
//
//                // Calculate partial payment proportion if exists
//                val returnProportion = if (transaction.netAmount != 0.0) {
//                    returnedNetAmount / transaction.netAmount
//                } else 0.0
//
//                val returnedPartialPayment = transaction.partialPayment * returnProportion
//
//                // Update transaction with correct amounts
//                val updatedTransaction = transaction.copy(
//                    netAmount = transaction.netAmount - returnedNetAmount,
//                    costAmount = transaction.costAmount - items.sumOf { it.costAmount ?: 0.0 },
//                    grossAmount = transaction.grossAmount - returnedGrossAmount,
//                    discountAmount = transaction.discountAmount - returnedDiscountAmount,
//                    customerDiscountAmount = transaction.customerDiscountAmount - returnedDiscountAmount,
//                    totalDiscountAmount = transaction.totalDiscountAmount - returnedDiscountAmount,
//                    numberOfItems = transaction.numberOfItems - items.sumOf { it.quantity.toDouble() },
//                    taxIncludedInPrice = transaction.taxIncludedInPrice - returnedVatAmount,
//                    vatAmount = transaction.vatAmount - returnedVatAmount,
//                    vatableSales = transaction.vatableSales - returnedVatableSales,
//                    partialPayment = transaction.partialPayment - returnedPartialPayment,
//                    comment = "${transaction.comment}\nReturn processed: $remarks"
//                )
//
//                // Create return items with correct negative amounts
//                val returnedItems = items.map { item ->
//                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
//                    val itemTotal = effectivePrice * item.quantity
//                    val itemDiscount = when (item.discountType.uppercase()) {
//                        "PERCENTAGE", "PWD", "SC" -> itemTotal * item.discountRate
//                        "FIXED" -> item.discountAmount * item.quantity
//                        "FIXEDTOTAL" -> item.discountAmount
//                        else -> 0.0
//                    }
//                    val netAmount = itemTotal - itemDiscount
//                    val vatAmount = netAmount * 0.12 / 1.12
//
//                    item.copy(
//                        quantity = -item.quantity,
//                        subtotal = 0.0,
//                        grossAmount = 0.0,
//                        costAmount = 0.0,
//                        netAmount = 0.0,
//                        vatAmount = 0.0,
//                        taxAmount = 0.0,
//                        taxIncludedInPrice = 0.0,
//                        netAmountNotIncludingTax = 0.0,
//                        discountAmount = 0.0,
//                        lineDiscountAmount = 0.0,
//                        customerDiscountAmount = 0.0,
//                        returnTransactionId = transaction.transactionId,
//                        returnQuantity = -item.quantity.toDouble(),
//                        returnLineId = item.lineNum?.toDouble() ?: 0.0,
//                        comment = "Returned: $remarks",
//                        discountRate = item.discountRate,
//                        priceOverride = item.priceOverride,
//                        discountType = item.discountType,
//                        total = 0.0
//                    )
//                }
//
//                // Update local database
//                transactionViewModel.updateTransactionSummary(updatedTransaction)
//                transactionViewModel.updateTransactionRecords(returnedItems)
//                updateInventory(returnedItems)
//
//                // Process refund on server
//                val currentStore = SessionManager.getCurrentUser()?.storeid
//                    ?: throw IllegalStateException("No store ID found in current session")
//
//                val refundResult = transactionViewModel.repository.processRefundOnServer(
//                    currentStore,
//                    updatedTransaction,
//                    returnedItems,
//                    remarks
//                )
//
//                // Handle the refund result
//                refundResult.fold(
//                    onSuccess = { response ->
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@Window1,
//                                "Return processed and synced successfully",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                            // Print return receipt only after successful server sync
//                            printReturnReceipt(updatedTransaction, returnedItems, remarks)
//                        }
//                    },
//                    onFailure = { error ->
//                        Log.e("ReturnTransaction", "Error syncing return with server: ${error.message}", error)
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@Window1,
//                                "Return processed locally but sync failed: ${error.message}",
//                                Toast.LENGTH_LONG
//                            ).show()
//                        }
//                        // Still print receipt even if sync fails, but mark it as "Not Synced"
//                        printReturnReceipt(updatedTransaction, returnedItems, "$remarks (Not Synced)")
//                    }
//                )
//
//                // Update sync status in local database
//                transactionViewModel.repository.transactionDao.updateSyncStatus(
//                    updatedTransaction.transactionId,
//                    refundResult.isSuccess
//                )
//
//            } catch (e: Exception) {
//                Log.e("ReturnTransaction", "Error processing return: ${e.message}", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Error processing return: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    transactionViewModel.loadTransactions()
//                }
//            }
//        }
//    }

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



    override fun onDestroy() {
        transactionSyncService?.stopSyncService()
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
        editText.setBackgroundResource(android.R.color.transparent)
        editText.setTextColor(Color.BLACK)
        editText.setHintTextColor(Color.GRAY)

        // Move the search icon to the right
        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)?.let { searchIcon ->
            (searchIcon.parent as? ViewGroup)?.removeView(searchIcon)
            val linearLayout = LinearLayout(searchView.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                orientation = LinearLayout.HORIZONTAL
            }
            linearLayout.addView(searchIcon, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 0
                marginEnd = 16
            })
            (searchView as ViewGroup).addView(linearLayout)
        }

        // Set up the window-specific search functionality
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProductsForCurrentWindow(newText)
                return true
            }
        })

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchView.windowToken, 0)
            }
        }
    }
    private fun filterProductsForCurrentWindow(query: String?) {
        lifecycleScope.launch {
            try {
                val window = windowViewModel.allWindows.first().find { it.id == windowId }
                if (window != null) {
                    val description = window.description.uppercase()
                    val allProducts = productViewModel.allProducts.value ?: emptyList()

                    // First filter by window type
                    val windowFilteredProducts = when {
                        description.contains("GRABFOOD") -> {
                            allProducts.filter { it.grabfood > 0 }
                                .map { it.copy(price = it.grabfood) }
                        }
                        description.contains("FOODPANDA") -> {
                            allProducts.filter { it.foodpanda > 0 }
                                .map { it.copy(price = it.foodpanda) }
                        }
                        description.contains("MANILARATE") -> {
                            allProducts.filter { it.manilaprice > 0 }
                                .map { it.copy(price = it.manilaprice) }
                        }
                        description.contains("PARTYCAKES") -> {
                            allProducts.filter {
                                it.itemGroup.equals("PARTY CAKES", ignoreCase = true)
                            }
                        }
                        description.contains("PURCHASE") -> {
                            allProducts.filter {
                                it.price > 0 && it.grabfood == 0.0 &&
                                        it.foodpanda == 0.0 && it.manilaprice == 0.0
                            }
                        }
                        else -> allProducts
                    }

                    // Then apply search filter
                    val searchFilteredProducts = if (query.isNullOrBlank()) {
                        windowFilteredProducts
                    } else {
                        windowFilteredProducts.filter { product ->
                            product.itemName.contains(query, ignoreCase = true) ||
                                    product.itemGroup.contains(query, ignoreCase = true)
                        }
                    }

                    withContext(Dispatchers.Main) {
                        productAdapter.submitList(searchFilteredProducts)
                        updateAvailableCategories(searchFilteredProducts)
                    }
                }
            } catch (e: Exception) {
                Log.e("Window1", "Error filtering products", e)
            }
        }
    }
    private fun updateAvailableCategories(products: List<Product>) {
        val availableCategories = products
            .map { it.itemGroup.uppercase() }
            .distinct()
            .mapNotNull { itemGroup ->
                categoryViewModel.categories.value?.find {
                    it.name.uppercase() == itemGroup
                }
            }
            .sortedBy { it.name }

        val displayCategories = listOf(
            Category(-1, "All"),
            Category(-2, "Mix & Match")
        ) + availableCategories

        categoryAdapter.setCategories(displayCategories)
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
//    private fun initializeRepositories() {
//        val cartDao = database.cartDao()
//        val cartRepository = CartRepository(cartDao)
//        transactionDao = database.transactionDao()
//        val cashFundDao = database.cashFundDao()
//        cashFundRepository = CashFundRepository(cashFundDao)
//        numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())
//
//        val numberSequenceApi = RetrofitClient.numberSequenceApi
//        val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
//        numberSequenceRemoteRepository = NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)
//    }

//    private fun initializeViewModels() {
//        productViewModel = ViewModelProvider(
//            this,
//            ProductViewModel.ProductViewModelFactory(application)
//        ).get(ProductViewModel::class.java)
//
//        val cartRepository = CartRepository(database.cartDao())
//        val cartViewModelFactory = CartViewModelFactory(cartRepository)
//        cartViewModel = ViewModelProvider(this, cartViewModelFactory)[CartViewModel::class.java]
//        cartViewModel.setCurrentWindow(windowId)
//
//        val categoryApi = RetrofitClient.categoryApi
//        val categoryDao = database.categoryDao()
//        val categoryRepository = CategoryRepository(categoryApi, categoryDao)
//        categoryViewModel = ViewModelProvider(
//            this,
//            CategoryViewModelFactory(categoryRepository)
//        ).get(CategoryViewModel::class.java)
//        val discountApiService = RetrofitClient.discountApiService
//        val discountDao =
//            database.discountDao() // Assuming you have this method in your AppDatabase
//        val discountRepository = DiscountRepository(discountApiService, discountDao)
//        val discountViewModelFactory = DiscountViewModelFactory(discountRepository)
//        discountViewModel =
//            ViewModelProvider(this, discountViewModelFactory)[DiscountViewModel::class.java]
//        windowViewModel = ViewModelProvider(this).get(WindowViewModel::class.java)
//
//        productViewModel.loadAlignedProducts()
//
//        val arApi = RetrofitClient.arApi
//        val arDao = database.arDao() // Assuming you have this method in your AppDatabase
//        val arRepository = ARRepository(arApi, arDao)
//        val arViewModelFactory = ARViewModelFactory(arRepository)
//        arViewModel = ViewModelProvider(this, arViewModelFactory)[ARViewModel::class.java]
//
//        // Initialize CustomerViewModel
//        val customerApi = RetrofitClient.customerApi
//        val customerDao =
//            database.customerDao() // Assuming you have this method in your AppDatabase
//        val customerRepository = CustomerRepository(customerApi, customerDao)
//        val customerViewModelFactory = CustomerViewModelFactory(customerRepository)
//        customerViewModel =
//            ViewModelProvider(this, customerViewModelFactory)[CustomerViewModel::class.java]
//
//        val numberSequenceApi = RetrofitClient.numberSequenceApi
//        val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
//        numberSequenceRemoteRepository = NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)
//
////        val repository = TransactionRepository(transactionDao)
//        val numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())
//        val transactionRepository = TransactionRepository(
//            transactionDao,
//            numberSequenceRepository
//        )
//
//        val factory = TransactionViewModel.TransactionViewModelFactory(
//            repository = transactionRepository,
//            numberSequenceRepository = numberSequenceRepository  // Add this parameter
//        )
//        transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]
//
//
//        // Set up the observer
//        transactionViewModel.syncStatus.observe(this@Window1) { result ->
//            result.fold(
//                onSuccess = { response ->
////                    Log.e("Sync", "Successfully synced transaction: ${response.transactionId}")
//                    Toast.makeText(
//                        this@Window1,
//                        "Transaction synced successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                },
//                onFailure = { error ->
//                    Log.e("Sync", "Failed to sync: ${error.message}")
//                    Toast.makeText(
//                        this@Window1,
//                        "Failed to sync transaction: ${error.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            )
//        }
//    }
//    private fun setupMixMatchButton() {
//        binding.buttonMixMatch.setOnClickListener {
//            showMixMatchDialog()
//        }
//    }

    private fun initializeRepositories() {
        val cartDao = database.cartDao()
        val cartRepository = CartRepository(cartDao)
        transactionDao = database.transactionDao()
        val cashFundDao = database.cashFundDao()
        cashFundRepository = CashFundRepository(cashFundDao)
        numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())

        val numberSequenceApi = RetrofitClient.numberSequenceApi
        val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
        numberSequenceRemoteRepository = NumberSequenceRemoteRepository(numberSequenceApi, numberSequenceRemoteDao)

    }
    private fun setupTransactionView() {
        val numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())
        val transactionRepository = TransactionRepository(
            database.transactionDao(),
            numberSequenceRemoteRepository
        )
        val factory = TransactionViewModel.TransactionViewModelFactory(
            repository = transactionRepository,
            numberSequenceRemoteRepository = numberSequenceRemoteRepository
        )
        transactionViewModel = ViewModelProvider(this, factory).get(TransactionViewModel::class.java)

        transactionAdapter = TransactionAdapter { transaction ->
            showTransactionDetailsDialog(transaction)
        }
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

//        val repository = TransactionRepository(transactionDao)
    val numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())

    val transactionRepository = TransactionRepository(
        transactionDao,
        numberSequenceRemoteRepository
    )

    val factory = TransactionViewModel.TransactionViewModelFactory(
        repository = transactionRepository,
        numberSequenceRemoteRepository = numberSequenceRemoteRepository
    )
    transactionViewModel = ViewModelProvider(this, factory)[TransactionViewModel::class.java]


    // Set up the observer
    transactionViewModel.syncStatus.observe(this@Window1) { result ->
        result.fold(
            onSuccess = { response ->
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

            // Add quantity input to dialog
            val quantityLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 16, 16, 16)
                }
            }

            val quantityLabel = TextView(this).apply {
                text = "Bundle Quantity: "
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val quantityInput = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText("1")
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            quantityLayout.addView(quantityLabel)
            quantityLayout.addView(quantityInput)

            // Add quantity layout to the top of the dialog
            val container = dialogView.findViewById<LinearLayout>(R.id.containerLayout)
            container.addView(quantityLayout, 0)

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
                val bundleQuantity = quantityInput.text.toString().toIntOrNull() ?: 1

                if (bundleQuantity <= 0) {
                    Toast.makeText(
                        this,
                        "Please enter a valid quantity",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (validateSelections(mixMatch, selections)) {
                    applyMixMatchToCart(mixMatch, selections, bundleQuantity)
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
        selections: Map<Int, String>,
        bundleQuantity: Int
    ) {
        lifecycleScope.launch {
            try {
                val bundleId = System.currentTimeMillis().toInt()
                var totalBundlePrice = 0.0
                val selectedItems = mutableListOf<Triple<Product, LineGroupWithDiscounts, Int>>()

                // First pass: collect selected products
                selections.forEach { (lineGroupId, itemIdentifier) ->
                    val lineGroup = mixMatch.lineGroups.find { it.lineGroup.id == lineGroupId }
                    val discountLine = lineGroup?.discountLines?.find { line ->
                        line.itemId?.toString()?.trim() == itemIdentifier.trim()
                    }

                    if (discountLine == null) {
                        throw Exception("Discount line not found for identifier: $itemIdentifier")
                    }

                    val product = productViewModel.findProduct(discountLine.itemId?.toString())
                        ?: throw Exception("Product not found with itemId: ${discountLine.itemId}")

                    // Store lineGroup for lineNum info
                    selectedItems.add(Triple(product, lineGroup!!, discountLine.qty))
                    totalBundlePrice += product.price * discountLine.qty
                }

                // Calculate discounts based on mix match type
                val (discountPerItem, discountType) = when (mixMatch.mixMatch.discountType) {
                    0 -> { // Deal Price
                        val dealPrice = mixMatch.mixMatch.dealPriceValue
                        val totalDiscount = totalBundlePrice - dealPrice
                        val itemCount = selectedItems.sumOf { it.third }
                        Pair(totalDiscount / itemCount, "DEAL")
                    }
                    1 -> { // Percentage
                        val percentageDiscount = mixMatch.mixMatch.discountPctValue
                        Pair(percentageDiscount, "PERCENTAGE")
                    }
                    2 -> { // Fixed Total
                        val fixedDiscount = mixMatch.mixMatch.discountAmountValue
                        val itemCount = selectedItems.sumOf { it.third }
                        Pair(fixedDiscount / itemCount, "FIXEDTOTAL")
                    }
                    else -> Pair(0.0, "")
                }

                // Second pass: add items to cart with lineNum
                selectedItems.forEachIndexed { index, (product, lineGroup, qty) ->
                    val effectivePrice = product.price
                    val itemTotal = effectivePrice * qty
                    val lineNum = lineGroup.lineGroup.noOfItemsNeeded

                    val cartItem = CartItem(
                        productId = product.id,
                        productName = product.itemName,
                        price = product.price,
                        quantity = qty,
                        windowId = windowId,
                        bundleId = bundleId,
                        mixMatchId = mixMatch.mixMatch.description,
                        discountType = discountType,
                        discount = discountPerItem,
                        discountAmount = when (discountType.uppercase()) {
                            "PERCENTAGE" -> itemTotal * (discountPerItem / 100)
                            "FIXED" -> discountPerItem * qty
                            "FIXEDTOTAL" -> discountPerItem
                            else -> 0.0
                        },
                        vatAmount = (itemTotal * 0.12 / 1.12),
                        vatExemptAmount = 0.0,
                        netAmount = itemTotal,
                        bundleSelections = selections.toString(),
                        itemGroup = product.itemGroup,
                        itemId = product.itemid,
                        lineNum = lineNum  // Set the lineNum
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
            screenWidthDp >= 600 -> 100f // For smaller tablets
            else -> 160f // For phones
        }

        val columnCount = (screenWidthDp / desiredColumnWidthDp).toInt().coerceIn(3, 4)

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
//        val transactionRepository = TransactionRepository(database.transactionDao())
//        val factory = TransactionViewModel.TransactionViewModelFactory(transactionRepository)
//        transactionViewModel =
//            ViewModelProvider(this, factory).get(TransactionViewModel::class.java)
        val numberSequenceRepository = NumberSequenceRepository(database.numberSequenceDao())
        val transactionRepository = TransactionRepository(
            database.transactionDao(),
            numberSequenceRemoteRepository
        )

        val factory = TransactionViewModel.TransactionViewModelFactory(
            repository = transactionRepository,
            numberSequenceRemoteRepository = numberSequenceRemoteRepository
        )
        transactionViewModel = ViewModelProvider(this, factory).get(TransactionViewModel::class.java)

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


//    private fun setupCategoryRecyclerView() {
//        categoryAdapter = CategoryAdapter { category ->
//            when (category.name) {
//                "All" -> {
//                    productViewModel.selectCategory(category)
//                    if (hasInternetConnection()) {
//                        reloadCategoriesAndProducts()
//                    }
//                }
//
//                "Mix & Match" -> {
//                    showMixMatchDialog()
//
//                }
//
//                else -> {
//                    productViewModel.selectCategory(category)
//                }
//            }
//        }
//
//        binding.categoryRecyclerView.apply {
//            layoutManager = LinearLayoutManager(this@Window1, LinearLayoutManager.HORIZONTAL, false)
//            adapter = categoryAdapter
//        }
//
//        lifecycleScope.launch {
//            combine(
//                productViewModel.allProducts.asFlow(),
//                categoryViewModel.categories,
//                productViewModel.isLoading
//            ) { products, categories, isLoading ->
//                if (!isLoading) {
//                    val allCategories = categories.toMutableList()
//
//                    // Ensure "All" category is first
//                    if (!allCategories.any { it.name == "All" }) {
//                        allCategories.add(0, Category(name = "All"))
//                    }
//
//                    // Ensure "Mix & Match" category exists
//                    if (!allCategories.any { it.name == "Mix & Match" }) {
//                        allCategories.add(1, Category(name = "Mix & Match"))
//                    }
//
//                    // Filter categories that have products
//                    allCategories.filter { category ->
//                        category.name == "All" ||
//                                category.name == "Mix & Match" ||
//                                products.any { product ->
//                                    product.itemGroup.equals(category.name, ignoreCase = true)
//                                }
//                    }.sortedBy {
//                        when (it.name) {
//                            "All" -> "0"
//                            "Mix & Match" -> "1"
//                            else -> it.name
//                        }
//                    }
//                } else {
//                    listOf(Category(name = "All"), Category(name = "Mix & Match"))
//                }
//            }.collect { validCategories ->
//                categoryAdapter.setCategories(validCategories)
//                if (productViewModel.selectedCategory.value == null) {
//                    val allCategory = validCategories.first()
//                    categoryAdapter.setSelectedCategory(allCategory)
//                    productViewModel.selectCategory(allCategory)
//                }
//            }
//        }
//    }
private fun setupCategoryRecyclerView() {
    categoryAdapter = CategoryAdapter { category ->
        when (category.name) {
            "All" -> {
                lifecycleScope.launch {
                    loadWindowSpecificProducts() // This will reload all products for current window
                }
            }
            "Mix & Match" -> {
                showMixMatchDialog()
            }
            else -> {
                filterProductsByWindowAndCategory(category)
            }
        }
    }

    binding.categoryRecyclerView.apply {
        layoutManager = LinearLayoutManager(this@Window1, LinearLayoutManager.HORIZONTAL, false)
        adapter = categoryAdapter
    }

    // Observe categories and products
    lifecycleScope.launch {
        combine(
            productViewModel.allProducts.asFlow(),
            categoryViewModel.categories,
            windowViewModel.allWindows
        ) { products, categories, windows ->
            Triple(products, categories, windows)
        }.collect { (products, categories, windows) ->
            val currentWindow = windows.find { it.id == windowId }
            if (currentWindow != null) {
                updateCategoriesForWindow(currentWindow, categories, products)
            }
        }
    }
}
    private fun filterProductsByWindowAndCategory(category: Category) {
        lifecycleScope.launch {
            try {
                val window = windowViewModel.allWindows.first().find { it.id == windowId }
                if (window != null) {
                    val description = window.description.uppercase()
                    val allProducts = productViewModel.allProducts.value ?: emptyList()

                    // First filter by window type
                    val windowFilteredProducts = when {
                        description.contains("GRABFOOD") -> {
                            allProducts.filter { it.grabfood > 0 }
                                .map { it.copy(price = it.grabfood) }
                        }
                        description.contains("FOODPANDA") -> {
                            allProducts.filter { it.foodpanda > 0 }
                                .map { it.copy(price = it.foodpanda) }
                        }
                        description.contains("MANILARATE") -> {
                            allProducts.filter { it.manilaprice > 0 }
                                .map { it.copy(price = it.manilaprice) }
                        }
                        description.contains("PARTYCAKES") -> {
                            allProducts.filter {
                                it.itemGroup.equals("PARTY CAKES", ignoreCase = true)
                            }
                        }
                        description.contains("PURCHASE") -> {
                            allProducts.filter {
                                it.grabfood == 0.0 && it.foodpanda == 0.0 &&
                                        it.manilaprice == 0.0 && it.price > 0
                            }
                        }
                        else -> allProducts
                    }

                    // Then filter by category
                    val categoryFilteredProducts = windowFilteredProducts.filter { product ->
                        product.itemGroup.equals(category.name, ignoreCase = true)
                    }

                    withContext(Dispatchers.Main) {
                        productAdapter.submitList(categoryFilteredProducts)
                        findViewById<TextView>(R.id.textView3)?.text =
                            "Products (${categoryFilteredProducts.size})"
                    }
                }
            } catch (e: Exception) {
                Log.e("Window1", "Error filtering products by category", e)
            }
        }
    }
    private fun updateCategoriesForWindow(
        currentWindow: Window,
        allCategories: List<Category>,
        allProducts: List<Product>
    ) {
        val description = currentWindow.description.uppercase()

        // Filter products based on window type
        val windowProducts = when {
            description.contains("GRABFOOD") -> {
                allProducts.filter { it.grabfood > 0 }
            }
            description.contains("FOODPANDA") -> {
                allProducts.filter { it.foodpanda > 0 }
            }
            description.contains("MANILARATE") -> {
                allProducts.filter { it.manilaprice > 0 }
            }
            description.contains("PARTYCAKES") -> {
                allProducts.filter {
                    it.itemGroup.equals("PARTY CAKES", ignoreCase = true)
                }
            }
            description.contains("PURCHASE") -> {
                allProducts.filter {
                    it.grabfood == 0.0 && it.foodpanda == 0.0 &&
                            it.manilaprice == 0.0 && it.price > 0
                }
            }
            else -> allProducts
        }

        // Get unique categories from filtered products
        val categoriesInWindow = windowProducts
            .map { it.itemGroup.uppercase() }
            .distinct()
            .mapNotNull { itemGroup ->
                allCategories.find { it.name.uppercase() == itemGroup }
            }
            .sortedBy { it.name }

        // Always include "All" and "Mix & Match" categories
        val displayCategories = listOf(
            Category(-1, "All"),
            Category(-2, "Mix & Match")
        ) + categoriesInWindow

        // Update category adapter
        categoryAdapter.setCategories(displayCategories)
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

//    private fun performXRead() {
//        if (!checkBluetoothPermissions()) {
//            Toast.makeText(this, "Bluetooth permission is required to print X-Read", Toast.LENGTH_LONG).show()
//            return
//        }
//
//
//        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
//            .setTitle("Confirm X-Read")
//            .setMessage("Are you sure you want to perform an X-Read?")
//            .setPositiveButton("Yes") { _, _ ->
//                lifecycleScope.launch {
//                    val transactions = transactionDao.getAllTransactionsSince(lastZReadTime)
//                    val currentTenderDeclaration = tenderDeclarationDao.getLatestTenderDeclaration()
//                    printXReadWithBluetoothPrinter(transactions, currentTenderDeclaration)
//                }
//            }
//            .setNegativeButton("No") { dialog, _ ->
//                dialog.dismiss()
//            }
//            .create()
//
//        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//        dialog.show()
//    }

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
                    try {
                        // Get unprocessed transactions
                        val transactions = transactionDao.getAllUnprocessedTransactions()
                        val currentTenderDeclaration = tenderDeclarationDao.getLatestTenderDeclaration()

                        // Always create transaction logger for BIR purposes
                        val transactionLogger = TransactionLogger(this@Window1)
                        transactionLogger.logXRead(
                            transactions = transactions,
                            tenderDeclaration = currentTenderDeclaration
                        )

                        // Print report if there are transactions
                        if (transactions.isNotEmpty()) {
                            printXReadWithBluetoothPrinter(transactions, currentTenderDeclaration)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@Window1,
                                    "X-Read completed successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@Window1,
                                    "X-Read logged. No transactions to report.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("XRead", "Error performing X-Read", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@Window1,
                                "Error performing X-Read: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
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
        // Check prerequisites
        if (!isPulloutCashFundProcessed || !isTenderDeclarationProcessed) {
            Toast.makeText(
                this,
                "Please complete the following steps first:\n1. Pull-out Cash Fund\n2. Tender Declaration",
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
                Log.e("ZRead", "Error showing Z-Read dialog", e)
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
                val transactions = transactionDao.getAllUnprocessedTransactions()
                val totalAmount = transactions.sumOf { it.totalAmountPaid }

                // Get tender declaration - required even with no transactions
                val currentTenderDeclaration = tenderDeclaration
                if (currentTenderDeclaration == null) {
                    Toast.makeText(
                        this@Window1,
                        "Tender declaration not found. Please process tender declaration first.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Mark any existing transactions with Z-report ID
                if (transactions.isNotEmpty()) {
                    transactionDao.markTransactionsAsProcessed(zReportId)
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

                // Always create transaction logger for BIR purposes
                val transactionLogger = TransactionLogger(this@Window1)
                transactionLogger.logZRead(
                    zReportId = zReportId,
                    transactions = transactions,
                    tenderDeclaration = currentTenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(zReadContent)) {
                    Log.d("PrintZRead", "Z-Read report content sent successfully")
                    delay(1000)

                    val cutCommand = byteArrayOf(0x1D, 0x56, 0x00).toString(Charset.defaultCharset())
                    if (bluetoothPrinterHelper.printGenericReceipt(cutCommand)) {
                        Log.d("PrintZRead", "Z-Read report printed and cut successfully")

                        withContext(Dispatchers.Main) {
                            if (transactions.isEmpty()) {
                                Toast.makeText(
                                    this@Window1,
                                    "Z-Read completed. No transactions to report.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    this@Window1,
                                    "Z-Read report printed successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
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
                // Reset time tracking for next Z-Read cycle
                lastZReadTime = System.currentTimeMillis()
                saveLastZReadTimeToPreferences(lastZReadTime)

                // Clear tender declaration
                tenderDeclarationDao.deleteAll()
                tenderDeclaration = null
                isTenderDeclarationProcessed = false

                // Clear cash fund
                cashFundRepository.deleteAll()
                currentCashFund = 0.0

                // Reset status flags
                resetCashManagementStatus()
                isCashFundEntered = false

                withContext(Dispatchers.Main) {
                    disableTransactions()
                    Toast.makeText(
                        this@Window1,
                        "All data has been reset. Please enter new cash fund to continue.",
                        Toast.LENGTH_LONG
                    ).show()
                    showCashFundDialog()
                }

                // Set Z-Read performed flag
                isZReadPerformed = true

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

    private fun checkTenderDeclarationReset() {
        lifecycleScope.launch {
            val currentTenderDeclaration = tenderDeclarationDao.getLatestTenderDeclaration()
            if (currentTenderDeclaration == null) {
                Log.d("TenderDeclaration", "Tender declaration has been reset successfully")
            } else {
                Log.e("TenderDeclaration", "Tender declaration was not reset properly")
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
            Log.d("PrintXRead", "X-Read report content sent successfully")

            // Add a small delay before sending the cut command
            lifecycleScope.launch(Dispatchers.IO) {
                delay(1000)

                // Send the cut command
                val cutCommand = byteArrayOf(0x1D, 0x56, 0x00).toString(Charset.defaultCharset())

                if (bluetoothPrinterHelper.printGenericReceipt(cutCommand)) {
                    Log.d("PrintXRead", "X-Read report printed and cut successfully")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "X-Read report printed successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e("PrintXRead", "Failed to send cut command")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "X-Read report printed, but cutting failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
//private fun printXReadWithBluetoothPrinter(
//    transactions: List<TransactionSummary>,
//    tenderDeclaration: TenderDeclaration?
//) {
//    try {
//        val printerMacAddress = "DC:0D:30:70:09:19"
//        if (!bluetoothPrinterHelper.isConnected()) {
//            Log.d("PrintXRead", "Attempting to connect to printer")
//            if (!bluetoothPrinterHelper.connect(printerMacAddress)) {
//                Log.e("PrintXRead", "Failed to connect to printer")
//                Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
//                return
//            }
//        }
//
//        Log.d("PrintXRead", "Attempting to print X-Read report")
//        val xReportId = "X-${System.currentTimeMillis()}"  // Generate X-Read ID
//        val xReadContent = bluetoothPrinterHelper.buildReadReport(
//            transactions,
//            isZRead = false,
//            tenderDeclaration = tenderDeclaration
//        )
//
//        if (bluetoothPrinterHelper.printGenericReceipt(xReadContent)) {
//            // Add TransactionLogger here
//            val transactionLogger = TransactionLogger(this)
//            transactionLogger.logXRead(
////                xReportId = xReportId,
//                transactions = transactions,
//                tenderDeclaration = tenderDeclaration ?: return,
////                created = lastZReadTime,
////                endTime = System.currentTimeMillis()
//            )
//
//            Log.d("PrintXRead", "X-Read report printed successfully")
//            Thread.sleep(1000)
//            val cutCommand = ""
//            if (bluetoothPrinterHelper.printGenericReceipt(cutCommand)) {
//                Log.d("PrintXRead", "Cut command sent successfully")
//                Toast.makeText(this, "X-Read report printed and cut successfully", Toast.LENGTH_SHORT).show()
//            } else {
//                Log.e("PrintXRead", "Failed to send cut command")
//                Toast.makeText(this, "X-Read report printed, but cutting failed", Toast.LENGTH_SHORT).show()
//            }
//        } else {
//            Log.e("PrintXRead", "Failed to print X-Read report")
//            Toast.makeText(this, "Failed to print X-Read report", Toast.LENGTH_SHORT).show()
//        }
//    } catch (e: Exception) {
//        Log.e("PrintXRead", "Error printing X-Read report: ${e.message}")
//        Toast.makeText(this, "Error printing X-Read report: ${e.message}", Toast.LENGTH_LONG).show()
//    }
//}
    private fun resetCashManagementStatus() {
        isPulloutCashFundProcessed = false
        isTenderDeclarationProcessed = false
        isZReadPerformed = false
    }

    private fun getCurrentTime(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Manila")
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
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
        val timeZone = TimeZone.getTimeZone("Asia/Manila")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
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
//            bluetoothPrinterHelper.cutPaper()

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
            windowViewModel.allWindows.collect { windows ->
                val currentWindow = windows.find { it.id == windowId }
                if (currentWindow != null) {
                    loadWindowSpecificProducts()
                }
            }
        }

        lifecycleScope.launch {
            productViewModel.filteredProducts.collectLatest { products ->
                productAdapter.submitList(products)
            }
        }
        lifecycleScope.launch {
            combine(
                windowViewModel.allWindows,
                productViewModel.allProducts.asFlow()
            ) { windows, products ->
                loadWindowSpecificProducts()
            }.collect { result ->
                // Add collector block
                // This will be called whenever loadWindowSpecificProducts() completes
                Log.d("Window1", "Window-based product filtering completed")
            }
        }
//        lifecycleScope.launch {
//            combine(
//                windowViewModel.allWindows,
//                productViewModel.allProducts.asFlow()
//            ) { windows, products ->
//                loadWindowSpecificProducts()
//            }.collectLatest { result ->
//                Log.d("Window1", "Window-based product filtering completed")
//            }
//        }
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

            val cutCommand = ""  // GS V 0 for full cut
//            V
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
            val defaultWalkInCustomer = Customer(accountNum = "WALK-IN", name = "Walk-in Customer")
            customers.add(defaultWalkInCustomer) // Add the Walk-in Customer as initial customer

            customers.add(defaultWalkInCustomer)
            val customerAdapter = ArrayAdapter(
                this@Window1,
                android.R.layout.simple_dropdown_item_1line,
                customers.map { it.name }
            )
            customerAutoComplete.setAdapter(customerAdapter)
//            customerAutoComplete.setText("Walk-in Customer", false)
            var selectedCustomer = customers[0]

// Set up the AutoCompleteTextView
            customerAutoComplete.apply {
                threshold = 1  // Start filtering after 1 character
                setAdapter(customerAdapter)

                // Only set initial text if no customer is selected
                if (text.isEmpty()) {
                    setText(selectedCustomer.name, false)
                }
            }

// Update the OnItemClickListener
            customerAutoComplete.setOnItemClickListener { _, _, position, _ ->
                selectedCustomer = customers[position]
                // Update the text to show the selected customer's name
                customerAutoComplete.setText(selectedCustomer.name, false)

                // Hide keyboard after selection
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(customerAutoComplete.windowToken, 0)

                // Debug log
                Log.d(TAG, "Selected customer: ${selectedCustomer.name}, Account: ${selectedCustomer.accountNum}")
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

//    private fun setupCartRecyclerView() {
//        val cartAdapter = CartAdapter(
//            onItemClick = { cartItem -> /* Handle item click */ },
//            onDeleteClick = { cartItem ->
//                if (!partialPaymentApplied) {
//                    cartViewModel.deleteCartItem(cartItem)
//                } else {
//                    Toast.makeText(
//                        this@Window1,
//                        "Cannot delete items when partial payment is applied",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            },
//            onQuantityChange = { cartItem, newQuantity ->
//                cartViewModel.update(cartItem.copy(quantity = newQuantity))
//            },
//            onDiscountLongPress = { cartItem ->
//                showDiscountDialog()
//            }
//        )
//
//        binding.recyclerviewcart.apply {
//            adapter = cartAdapter
//            layoutManager = LinearLayoutManager(this@Window1)
//        }
//
//        // Set up swipe-to-delete
//        val itemTouchHelper = ItemTouchHelper(object :
//            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
//            override fun onMove(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder,
//                target: RecyclerView.ViewHolder
//            ): Boolean {
//                return false // We don't want drag & drop
//            }
//
//            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
//                val position = viewHolder.adapterPosition
//                val cartItem = cartAdapter.currentList[position]
//                if (!partialPaymentApplied) {
//                    cartViewModel.deleteCartItem(cartItem)
//                } else {
//                    // If there's a partial payment, don't allow deletion and snap the item back
//                    cartAdapter.notifyItemChanged(position)
//                    Toast.makeText(
//                        this@Window1,
//                        "Cannot delete items when partial payment is applied",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//
//            override fun getSwipeDirs(
//                recyclerView: RecyclerView,
//                viewHolder: RecyclerView.ViewHolder
//            ): Int {
//                return if (partialPaymentApplied) {
//                    0 // Disable swiping when partial payment is applied
//                } else {
//                    super.getSwipeDirs(recyclerView, viewHolder)
//                }
//            }
//        })
//
//        itemTouchHelper.attachToRecyclerView(binding.recyclerviewcart)
//
//        // Observe partial payment changes
//        lifecycleScope.launch {
//            cartViewModel.getPartialPaymentForWindow(windowId).collect { partialPayment ->
//                partialPaymentApplied = partialPayment > 0
//                partialPaymentAmount = partialPayment
//                cartAdapter.setPartialPaymentApplied(partialPaymentApplied)
//                cartAdapter.setDeletionEnabled(!partialPaymentApplied)
//            }
//        }
//    }

private fun setupCartRecyclerView() {
    class CartDeleteHelper(private val adapter: CartAdapter) {
        fun deleteBundle(cartItem: CartItem) {
            val bundleItems = adapter.currentList.filter { item ->
                item.bundleId == cartItem.bundleId && item.mixMatchId == cartItem.mixMatchId
            }
            bundleItems.forEach { bundleItem ->
                cartViewModel.deleteCartItem(bundleItem)
            }
        }
    }

    lateinit var deleteHelper: CartDeleteHelper

    val adapter = CartAdapter(
        onItemClick = { cartItem -> /* Handle item click */ },
        onDeleteClick = { cartItem ->
            if (!partialPaymentApplied) {
                if (cartItem.bundleId != null) {
                    deleteHelper.deleteBundle(cartItem)
                } else {
                    cartViewModel.deleteCartItem(cartItem)
                }
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

    deleteHelper = CartDeleteHelper(adapter)

    binding.recyclerviewcart.apply {
        this.adapter = adapter
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
            if (position != RecyclerView.NO_POSITION) {
                val cartItem = adapter.currentList[position]

                if (!partialPaymentApplied) {
                    if (cartItem.bundleId != null) {
                        deleteHelper.deleteBundle(cartItem)
                        Toast.makeText(
                            this@Window1,
                            "Entire bundle has been removed",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        cartViewModel.deleteCartItem(cartItem)
                    }
                } else {
                    // If there's a partial payment, don't allow deletion and snap the item back
                    adapter.notifyItemChanged(position)
                    Toast.makeText(
                        this@Window1,
                        "Cannot delete items when partial payment is applied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
            adapter.setPartialPaymentApplied(partialPaymentApplied)
            adapter.setDeletionEnabled(!partialPaymentApplied)
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
    private fun setupCustomNumberKeyboard(
        dialogView: View,
        amountEditText: EditText,
        buttons: List<Button>,
        backspaceButton: Button
    ) {
        // Disable soft keyboard
        amountEditText.showSoftInputOnFocus = false
        amountEditText.isFocusable = true
        amountEditText.isFocusableInTouchMode = true

        // Prevent system keyboard from showing
        amountEditText.setOnClickListener {
            // Hide system keyboard
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(amountEditText.windowToken, 0)
        }

        // Set click listener for number buttons
        val numberButtonClickListener = View.OnClickListener { view ->
            val buttonText = (view as Button).text.toString()
            val currentText = amountEditText.text.toString()

            // Handle dot button with special logic to prevent multiple dots
            if (buttonText == ".") {
                if (!currentText.contains(".")) {
                    amountEditText.setText(currentText + buttonText)
                    amountEditText.setSelection(amountEditText.text.length)
                }
            } else {
                // Limit input to 2 decimal places
                val parts = currentText.split(".")
                if (parts.size == 2 && parts[1].length >= 2) {
                    return@OnClickListener
                }

                amountEditText.setText(currentText + buttonText)
                amountEditText.setSelection(amountEditText.text.length)
            }
        }

        // Apply number button click listener
        buttons.forEach { button ->
            button.setOnClickListener(numberButtonClickListener)
        }

        // Backspace button logic
        backspaceButton.setOnClickListener {
            val currentText = amountEditText.text.toString()
            if (currentText.isNotEmpty()) {
                amountEditText.setText(currentText.substring(0, currentText.length - 1))
                amountEditText.setSelection(amountEditText.text.length)
            }
        }

        // Long press on backspace to clear
        backspaceButton.setOnLongClickListener {
            amountEditText.setText("")
            true
        }
    }
    private fun isChargePayment(paymentMethod: String): Boolean {
        return paymentMethod.equals("CHARGE", ignoreCase = true)
    }


    private fun isValidCustomerForCharge(customer: Customer?): Boolean {
        return customer != null &&
                customer.accountNum != "WALK-IN" &&
                customer.name != "Walk-in Customer"
    }
    private var selectedCustomer = Customer(accountNum = "WALK-IN", name = "Walk-in Customer")

    private fun showPaymentDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment, null)
        val amountPaidEditText = dialogView.findViewById<EditText>(R.id.amountPaidEditText1)
        val paymentMethodSpinner = dialogView.findViewById<Spinner>(R.id.paymentMethodSpinner1)
        if (paymentMethodSpinner == null) {
            Log.e(TAG, "Payment method spinner is null! Check your layout XML.")
            Toast.makeText(this, "Error initializing payment dialog", Toast.LENGTH_SHORT).show()
            return
        }
        val paymentMethodSpinner2 = dialogView.findViewById<Spinner>(R.id.paymentMethodSpinner2)
        if (paymentMethodSpinner2 == null) {
            Log.e(TAG, "Second payment method spinner is null!")
        }

        val customerAutoComplete = dialogView.findViewById<AutoCompleteTextView>(R.id.customerAutoComplete)
        val totalAmountTextView = dialogView.findViewById<TextView>(R.id.totalAmountTextView)

        // Split payment components
        val splitPaymentSwitch = dialogView.findViewById<Switch>(R.id.splitPaymentSwitch)
        val secondPaymentLayout = dialogView.findViewById<LinearLayout>(R.id.secondPaymentLayout)
        val amountPaidEditText2 = dialogView.findViewById<EditText>(R.id.amountPaidEditText2)

        val defaultPaymentMethods = listOf("Cash")
        val paymentMethods = mutableListOf<String>()

        var totalAmount = 0.0
        var discountType = "No Discount"
        var discountValue = 0.0
        var partialPayment = 0.0

        // Set up number buttons and backspace
        val numberButtons = listOf(
            dialogView.findViewById<Button>(R.id.button0),
            dialogView.findViewById<Button>(R.id.button1),
            dialogView.findViewById<Button>(R.id.button2),
            dialogView.findViewById<Button>(R.id.button3),
            dialogView.findViewById<Button>(R.id.button4),
            dialogView.findViewById<Button>(R.id.button5),
            dialogView.findViewById<Button>(R.id.button6),
            dialogView.findViewById<Button>(R.id.button7),
            dialogView.findViewById<Button>(R.id.button8),
            dialogView.findViewById<Button>(R.id.button9),
            dialogView.findViewById<Button>(R.id.buttonDot)
        )

        val backspaceButton = dialogView.findViewById<Button>(R.id.buttonBackspace)
        var currentFocusedEditText: EditText = amountPaidEditText

        // Focus change listeners
        amountPaidEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentFocusedEditText = amountPaidEditText
            }
        }

        amountPaidEditText2.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                currentFocusedEditText = amountPaidEditText2
            }
        }

        // Text change listener for second payment amount
        amountPaidEditText2.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (splitPaymentSwitch.isChecked) {
                    val secondAmount = s?.toString()?.toDoubleOrNull() ?: 0.0
                    val remainingAmount = totalAmount - secondAmount
                    if (currentFocusedEditText == amountPaidEditText2) {
                        amountPaidEditText.setText(String.format("%.2f", remainingAmount.coerceAtLeast(0.0)))
                    }
                }
            }
        })

        // Disable system keyboard for both EditTexts
        fun setupEditText(editText: EditText) {
            editText.apply {
                showSoftInputOnFocus = false
                isFocusable = true
                isFocusableInTouchMode = true
                setOnClickListener {
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
                }
            }
        }

        setupEditText(amountPaidEditText)
        setupEditText(amountPaidEditText2)

        // Number button click listener
        val numberButtonClickListener = View.OnClickListener { view ->
            val buttonText = (view as Button).text.toString()
            val currentText = currentFocusedEditText.text.toString()

            when (buttonText) {
                "." -> {
                    if (!currentText.contains(".")) {
                        currentFocusedEditText.setText(currentText + buttonText)
                        currentFocusedEditText.setSelection(currentFocusedEditText.text.length)
                    }
                }
                else -> {
                    // Limit input to 2 decimal places
                    val parts = currentText.split(".")
                    if (parts.size == 2 && parts[1].length >= 2) {
                        return@OnClickListener
                    }

                    currentFocusedEditText.setText(currentText + buttonText)
                    currentFocusedEditText.setSelection(currentFocusedEditText.text.length)
                }
            }
        }

        // Apply click listener to all number buttons
        numberButtons.forEach { button ->
            button.setOnClickListener(numberButtonClickListener)
        }

        // Backspace button logic
        backspaceButton.setOnClickListener {
            val currentText = currentFocusedEditText.text.toString()
            if (currentText.isNotEmpty()) {
                currentFocusedEditText.setText(currentText.substring(0, currentText.length - 1))
                currentFocusedEditText.setSelection(currentFocusedEditText.text.length)
            }
        }

        backspaceButton.setOnLongClickListener {
            currentFocusedEditText.setText("")
            true
        }

        // Split payment toggle logic with automatic amount distribution
        splitPaymentSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if (totalAmount <= 0) {
                    buttonView.isChecked = false
                    Toast.makeText(this@Window1, "Split payment not available - no remaining balance", Toast.LENGTH_SHORT).show()
                    return@setOnCheckedChangeListener
                }

                secondPaymentLayout.visibility = View.VISIBLE

                // Set default split: first payment gets total amount, second payment starts at 0
                amountPaidEditText.setText(String.format("%.2f", totalAmount))
                amountPaidEditText2.setText("0.00")

                // Setup second amount edit text
                setupEditText(amountPaidEditText2)
                amountPaidEditText2.requestFocus()  // Automatically focus the second payment field
            } else {
                secondPaymentLayout.visibility = View.GONE
                amountPaidEditText2.setText("")
                amountPaidEditText.setText(String.format("%.2f", totalAmount))
            }
        }

        // Cart items collection and total calculation
        lifecycleScope.launch {
            cartViewModel.getAllCartItems(windowId).collect { cartItems ->
                var gross = 0.0
                var totalDiscount = 0.0
                var vatAmount = 0.0
                var priceOverrideTotal = 0.0
                var bundleDiscount = 0.0

                cartItems.forEach { cartItem ->
                    val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
                    val itemTotal = effectivePrice * cartItem.quantity
                    gross += itemTotal
                    partialPayment = cartItem.partialPayment

                    // Existing discount and calculation logic remains the same
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
                        "DEAL", "PERCENTAGE", "FIXEDTOTAL" -> {
                            // Handle bundle discounts
                            if (cartItem.bundleId != null) {
                                bundleDiscount += when (cartItem.discountType.toUpperCase()) {
                                    "DEAL" -> cartItem.discount
                                    "PERCENTAGE" -> itemTotal * (cartItem.discount / 100)
                                    "FIXEDTOTAL" -> cartItem.discount
                                    else -> 0.0
                                }
                            }
                        }
                    }

                    vatAmount += itemTotal * 0.12 / 1.12
                }

                // Add bundle discount to total discount
                totalDiscount += bundleDiscount

                val discountedTotal = gross - totalDiscount
                totalAmount = discountedTotal - partialPayment

                amountPaidEditText.setText(String.format("%.2f", totalAmount))

                // Update UI elements
                if (totalAmount <= 0) {
                    splitPaymentSwitch.isChecked = false
                    splitPaymentSwitch.isEnabled = false
                } else {
                    splitPaymentSwitch.isEnabled = true
                }

                // Update total amount display
                val formattedText = StringBuilder().apply {
                    append(String.format("Gross Amount: ₱%.2f", gross))
                    if (priceOverrideTotal != 0.0) {
                        append(String.format("\nPrice Override Adjustment: ₱%.2f", priceOverrideTotal))
                    }
                    append(String.format("\nVAT Amount: ₱%.2f", vatAmount))
                    if (totalDiscount > 0) {
                        append(String.format("\nTotal Discount: ₱%.2f", totalDiscount))
                        if (bundleDiscount > 0) {
                            append(String.format("\n  Bundle Discount: ₱%.2f", bundleDiscount))
                        }
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
            arViewModel.arTypes.collectLatest { arTypes ->
                // Use withContext(Dispatchers.Main) if UI updates are needed
                withContext(Dispatchers.Main) {
                    paymentMethods.clear()
                    paymentMethods.addAll(defaultPaymentMethods)
                    if (arTypes.isNotEmpty()) {
                        paymentMethods.addAll(arTypes.map { it.ar })
                    }

                    // Update spinners on main thread
                    updatePaymentMethodSpinner(paymentMethodSpinner, paymentMethods, customerAutoComplete)
                    updatePaymentMethodSpinner(paymentMethodSpinner2, paymentMethods, customerAutoComplete)
                }
            }


            launch {
                arViewModel.error.collectLatest { errorMessage ->
                    if (errorMessage != null) {
                        Log.e(TAG, "Error in AR ViewModel: $errorMessage")
                        paymentMethods.clear()
                        paymentMethods.addAll(defaultPaymentMethods)
                        updatePaymentMethodSpinner(paymentMethodSpinner, paymentMethods, customerAutoComplete)
                        updatePaymentMethodSpinner(paymentMethodSpinner2, paymentMethods, customerAutoComplete)
                    }
                }
            }
        }

        arViewModel.refreshARTypes()

        // Setup for customer selection (existing code remains the same)
        val customers = mutableListOf<Customer>()
        customers.add(selectedCustomer)

        val customerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            customers.map { it.name }
        )
        customerAutoComplete.setAdapter(customerAdapter)
        customerAutoComplete.setText("Walk-in Customer", false)

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

        // Handle customer selection and search (existing code remains the same)
//        setupCustomerSelection(
//            customerAutoComplete,
//            customers,
//            customerAdapter,
//            paymentMethodSpinner,
//            paymentMethodSpinner2,
//            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
//        ) { customer ->
//            selectedCustomer = customer
//        }


        // Create and show the dialog
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Payment")
            .setView(dialogView)
            .setPositiveButton("Pay", null)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
            .create()

        // Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)

        // Handle dialog show and payment processing
        dialog.setOnShowListener {
            val payButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            // Update spinner initialization to include the pay button
            updatePaymentMethodSpinner(paymentMethodSpinner, paymentMethods, customerAutoComplete, payButton)
            updatePaymentMethodSpinner(paymentMethodSpinner2, paymentMethods, customerAutoComplete, payButton)

            // Update customer selection setup
            setupCustomerSelection(
                customerAutoComplete,
                customers,
                customerAdapter,
                paymentMethodSpinner,
                paymentMethodSpinner2,
                payButton
            ) { customer ->
                selectedCustomer = customer
            }

            payButton.setOnClickListener {
                val isSplitPayment = splitPaymentSwitch.isChecked

                // Collect payment methods and amounts
                val paymentMethods = mutableListOf<String>()
                val paymentAmounts = mutableListOf<Double>()

                // First payment method
                val paymentMethod1 = paymentMethodSpinner?.selectedItem?.toString() ?: run {
                    Toast.makeText(this, "Payment method not selected", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Check if payment method is CHARGE and validate customer
                val isCharge = isChargePayment(paymentMethod1) ||
                        (isSplitPayment && isChargePayment(paymentMethodSpinner2?.selectedItem?.toString() ?: ""))

                if (isCharge && !isValidCustomerForCharge(selectedCustomer)) {
                    Toast.makeText(
                        this,
                        "Please select a valid customer for charge payment",
                        Toast.LENGTH_SHORT
                    ).show()
                    customerAutoComplete.error = "Customer required for charge"
                    return@setOnClickListener
                }

                val amountPaid1 = amountPaidEditText?.text.toString().toDoubleOrNull() ?: run {
                    Toast.makeText(this, "Invalid payment amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                paymentMethods.add(paymentMethod1)
                paymentAmounts.add(amountPaid1)

                // Second payment method if split payment
                if (isSplitPayment) {
                    val paymentMethod2 = paymentMethodSpinner2.selectedItem.toString()
                    val amountPaid2 = amountPaidEditText2.text.toString().toDoubleOrNull() ?: 0.0

                    if (amountPaid2 > 0) {
                        paymentMethods.add(paymentMethod2)
                        paymentAmounts.add(amountPaid2)
                    }
                }

                // Validate total payment
                val totalPaid = paymentAmounts.sum()

                // Check if total paid is sufficient
                if (totalPaid < totalAmount) {
                    Toast.makeText(this, "Insufficient payment amount", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val change = if (totalPaid > totalAmount) totalPaid - totalAmount else 0.0

                // Process the payment
                processPayment(
                    paymentAmounts[0],
                    paymentMethods[0],
                    1.12, // VAT rate
                    discountType,
                    discountValue,
                    selectedCustomer,
                    totalAmount,
                    // Pass other payment methods and amounts if split
                    otherPaymentMethods = if (paymentMethods.size > 1) paymentMethods.slice(1 until paymentMethods.size) else emptyList(),
                    otherPaymentAmounts = if (paymentAmounts.size > 1) paymentAmounts.slice(1 until paymentAmounts.size) else emptyList()
                )

                // Show change if applicable
                if (change > 0) {
                    Toast.makeText(this, "Change: ₱${String.format("%.2f", change)}", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun setupCustomerSelection(
        customerAutoComplete: AutoCompleteTextView,
        customers: MutableList<Customer>,
        adapter: ArrayAdapter<String>,
        paymentMethodSpinner: Spinner,
        paymentMethodSpinner2: Spinner?,
        payButton: Button,
        onCustomerSelected: (Customer) -> Unit
    ) {
        // Handle keyboard done action
        customerAutoComplete.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        // Handle customer selection
        customerAutoComplete.setOnItemClickListener { parent, view, position, id ->
            val selectedCustomerName = parent.getItemAtPosition(position).toString()
            val customer = customers.find { it.name == selectedCustomerName }
                ?: Customer(accountNum = "WALK-IN", name = "Walk-in Customer")

            // Check if CHARGE is selected in either spinner
            val isCharge1 = isChargePayment(paymentMethodSpinner.selectedItem?.toString() ?: "")
            val isCharge2 = paymentMethodSpinner2?.let {
                isChargePayment(it.selectedItem?.toString() ?: "")
            } ?: false

            if ((isCharge1 || isCharge2) && !isValidCustomerForCharge(customer)) {
                customerAutoComplete.error = "Please select a valid customer for charge payment"
                payButton.isEnabled = false
                // Clear the selection if it's invalid for charge
                customerAutoComplete.setText("")
                return@setOnItemClickListener
            }

            // Update UI and selected customer
            customerAutoComplete.setText(customer.name, false)
            customerAutoComplete.error = null
            payButton.isEnabled = true
            onCustomerSelected(customer)
        }

        // Handle customer search
        customerAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                lifecycleScope.launch {
                    try {
                        val searchText = s?.toString() ?: ""
                        if (searchText.length >= 1) {
                            customerViewModel.searchCustomers(searchText)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error searching customers: ${e.message}")
                    }
                }
            }
        })

        // Add text change listener for validation
        customerAutoComplete.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentText = s?.toString() ?: ""
                val isCharge1 = isChargePayment(paymentMethodSpinner.selectedItem?.toString() ?: "")
                val isCharge2 = paymentMethodSpinner2?.let {
                    isChargePayment(it.selectedItem?.toString() ?: "")
                } ?: false

                if ((isCharge1 || isCharge2) && (currentText.isEmpty() || currentText == "Walk-in Customer")) {
                    customerAutoComplete.error = "Please select a valid customer for charge payment"
                    payButton.isEnabled = false
                }
            }
        })
    }
    // Extension function to round to two decimals
    private fun updatePaymentMethodSpinner(
        spinner: Spinner?,
        methods: List<String>,
        customerAutoComplete: AutoCompleteTextView,
        payButton: Button? = null
    ) {
        if (spinner == null) {
            Log.e(TAG, "Attempted to update null spinner")
            return
        }

        val uniqueMethods = methods.distinct().sorted()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, uniqueMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        // Add selection listener
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMethod = parent?.getItemAtPosition(position).toString()

                if (isChargePayment(selectedMethod)) {
                    // Clear customer field and reset selected customer
                    customerAutoComplete.setText("")
                    selectedCustomer = Customer(accountNum = "WALK-IN", name = "Walk-in Customer")

                    // Update UI to show requirement
                    customerAutoComplete.hint = "Select Customer (Required for Charge)"
                    customerAutoComplete.error = "Customer required for charge"
                    payButton?.isEnabled = false
                } else {
                    // Reset to default state
                    customerAutoComplete.hint = "Select Customer"
                    customerAutoComplete.error = null
                    payButton?.isEnabled = true

                    // Reset to walk-in customer if no specific customer is selected
                    if (customerAutoComplete.text.isEmpty()) {
                        customerAutoComplete.setText("Walk-in Customer", false)
                        selectedCustomer = Customer(accountNum = "WALK-IN", name = "Walk-in Customer")
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                customerAutoComplete.hint = "Select Customer"
                customerAutoComplete.error = null
                payButton?.isEnabled = true
            }
        }

        // Select default method (Cash)
        val cashIndex = uniqueMethods.indexOf("Cash")
        if (cashIndex != -1) {
            spinner.setSelection(cashIndex)
        }
    }

    private fun updateCustomerList(
        customersList: MutableList<Customer>,
        newCustomers: List<Customer>,
        adapter: ArrayAdapter<String>
    ) {
        customersList.clear()
        // Update the Walk-in customer with a standard account number
        customersList.add(Customer(accountNum = "WALK-IN", name = "Walk-in Customer"))

        // Add all other customers, preserving their full details
        customersList.addAll(newCustomers)

        // Update adapter with customer names
        adapter.clear()
        adapter.addAll(customersList.map { it.name })
        adapter.notifyDataSetChanged()

        // For debugging - log the customer list
        customersList.forEach { customer ->
            Log.d(TAG, "Customer in list: ${customer.name}, Account: ${customer.accountNum}")
        }
    }

    private fun updatePricesForWindow(windowDescription: String) {
        lifecycleScope.launch {
            try {
                val currentProducts = productViewModel.allProducts.value ?: return@launch

                // Update prices based on window type
                val updatedProducts = currentProducts.map { product ->
                    when {
                        windowDescription.contains("GRABFOOD") && product.grabfood > 0 -> {
                            product.copy(price = product.grabfood)
                        }
                        windowDescription.contains("FOODPANDA") && product.foodpanda > 0 -> {
                            product.copy(price = product.foodpanda)
                        }
                        windowDescription.contains("MANILARATE") && product.manilaprice > 0 -> {
                            product.copy(price = product.manilaprice)
                        }
                        else -> product
                    }
                }

                // Update the product list with adjusted prices
                withContext(Dispatchers.Main) {
                    productAdapter.submitList(updatedProducts)
                }
            } catch (e: Exception) {
                Log.e("Window1", "Error updating prices for window", e)
            }
        }
    }
    private fun loadWindowSpecificProducts() {
        lifecycleScope.launch {
            try {
                val window = windowViewModel.allWindows.first().find { it.id == windowId }

                if (window != null) {
                    val description = window.description.uppercase()
                    Log.d("Window1", "Window description: $description")

                    val allProducts = productViewModel.allProducts.value ?: emptyList()

                    // Filter products based on window description
                    val filteredProducts = when {
                        description.contains("GRABFOOD") -> {
                            allProducts.filter { product ->
                                product.grabfood > 0
                            }.map { product ->
                                product.copy(price = product.grabfood)
                            }
                        }
                        description.contains("FOODPANDA") -> {
                            allProducts.filter { product ->
                                product.foodpanda > 0
                            }.map { product ->
                                product.copy(price = product.foodpanda)
                            }
                        }
                        description.contains("MANILARATE") -> {
                            allProducts.filter { product ->
                                product.manilaprice > 0
                            }.map { product ->
                                product.copy(price = product.manilaprice)
                            }
                        }
                        description.contains("PARTYCAKES") -> {
                            allProducts.filter { product ->
                                product.itemName.equals("PARTY CAKES", ignoreCase = true)
                            }
                        }
                        description.contains("PURCHASE") -> {
                            // For PURCHASE windows, show items that only have regular price and no other prices
                            allProducts.filter { product ->
                                product.price > 0 && product.grabfood == 0.0 &&
                                        product.foodpanda == 0.0 && product.manilaprice == 0.0
                            }
                        }
                        else -> allProducts
                    }

                    withContext(Dispatchers.Main) {
                        productAdapter.submitList(filteredProducts)
                        updateAvailableCategories(filteredProducts)
                        findViewById<TextView>(R.id.textView3)?.text = "Products (${filteredProducts.size})"
                    }
                }
            } catch (e: Exception) {
                Log.e("Window1", "Error loading window-specific products", e)
            }
        }
    }
//    private fun loadWindowSpecificProducts() {
//        lifecycleScope.launch {
//            try {
//                Log.d("Window1", "Starting loadWindowSpecificProducts for windowId: $windowId")
//
//                // Get current window description using windowViewModel
//                val window = windowViewModel.allWindows.first().find { it.id == windowId }
//
//                if (window != null) {
//                    val description = window.description.uppercase()
//                    Log.d("Window1", "Window description: $description")
//
//                    val allProducts = productViewModel.allProducts.value ?: emptyList()
//                    Log.d("Window1", "Total products before filtering: ${allProducts.size}")
//
//                    // Filter products based on window description
//                    val filteredProducts = when {
//                        description.contains("GRABFOOD") -> {
//                            Log.d("Window1", "Applying GRABFOOD filter")
//                            allProducts.filter { product ->
//                                val hasGrabPrice = product.grabfood > 0
//                                if (hasGrabPrice) {
//                                    Log.d("Window1", "Product ${product.itemName} has GrabFood price: ${product.grabfood}")
//                                }
//                                hasGrabPrice
//                            }.map { product ->
//                                product.copy(price = product.grabfood)
//                            }
//                        }
//                        description.contains("FOODPANDA") -> {
//                            Log.d("Window1", "Applying FOODPANDA filter")
//                            allProducts.filter { product ->
//                                val hasFoodPandaPrice = product.foodpanda > 0
//                                if (hasFoodPandaPrice) {
//                                    Log.d("Window1", "Product ${product.itemName} has FoodPanda price: ${product.foodpanda}")
//                                }
//                                hasFoodPandaPrice
//                            }.map { product ->
//                                product.copy(price = product.foodpanda)
//                            }
//                        }
//                        description.contains("MANILARATE") -> {
//                            Log.d("Window1", "Applying MANILARATE filter")
//                            allProducts.filter { product ->
//                                val hasManilaPrice = product.manilaprice > 0
//                                if (hasManilaPrice) {
//                                    Log.d("Window1", "Product ${product.itemName} has Manila price: ${product.manilaprice}")
//                                }
//                                hasManilaPrice
//                            }.map { product ->
//                                product.copy(price = product.manilaprice)
//                            }
//                        }
//                        description.contains("PARTYCAKES") -> {
//                            Log.d("Window1", "Applying PARTYCAKES filter")
//                            allProducts.filter { product ->
//                                val isPartyCake = product.itemName.equals("PARTY CAKES", ignoreCase = true)
//                                if (isPartyCake) {
//                                    Log.d("Window1", "Product ${product.itemName} is a party cake")
//                                }
//                                isPartyCake
//                            }
//                        }
//                        description.contains("PURCHASE") -> {
//                            Log.d("Window1", "Applying PURCHASE filter")
//                            allProducts.filter { product ->
//                                val isRegularPrice = product.grabfood == 0.0 &&
//                                        product.foodpanda == 0.0 &&
//                                        product.manilaprice == 0.0 &&
//                                        product.price > 0
//                                if (isRegularPrice) {
//                                    Log.d("Window1", "Product ${product.itemName} has regular price: ${product.price}")
//                                }
//                                isRegularPrice
//                            }
//                        }
//                        else -> {
//                            Log.d("Window1", "No specific filter applied")
//                            allProducts
//                        }
//                    }
//
//                    Log.d("Window1", "Filtered products count: ${filteredProducts.size}")
//
//                    // Update the product display
//                    withContext(Dispatchers.Main) {
//                        productAdapter.submitList(filteredProducts)
//                        findViewById<TextView>(R.id.textView3)?.text = "Products (${filteredProducts.size})"
//                    }
//                } else {
//                    Log.e("Window1", "Window not found for id: $windowId")
//                }
//
//            } catch (e: Exception) {
//                Log.e("Window1", "Error loading window-specific products: ${e.message}", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Error loading products: ${e.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        }
//    }

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

            // Find matching items, separating discounted and non-discounted
            val existingNonDiscountedItem = existingItems.find { item ->
                item.productId == product.id &&
                        item.discount == 0.0 &&
                        item.discountType.isEmpty() &&
                        item.bundleId == null
            }

            val existingDiscountedItem = existingItems.find { item ->
                item.productId == product.id &&
                        (item.discount > 0.0 ||
                                item.discountType.isNotEmpty() ||
                                item.bundleId != null)
            }

            when {
                // If clicking a non-discounted item
                existingNonDiscountedItem != null -> {
                    // Update quantity of existing non-discounted item
                    cartViewModel.update(
                        existingNonDiscountedItem.copy(
                            quantity = existingNonDiscountedItem.quantity + 1,
                            partialPayment = partialPayment
                        )
                    )
                }
                // If the product exists but only as a discounted item, create new non-discounted entry
                existingDiscountedItem != null -> {
                    // Create new cart item without discount
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
                            itemGroup = product.itemGroup,
                            itemId = product.itemid,
                            discount = 0.0,
                            discountType = ""
                        )
                    )
                }
                // If the product doesn't exist in cart at all
                else -> {
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
                            itemGroup = product.itemGroup,
                            itemId = product.itemid,
                            discount = 0.0,
                            discountType = ""
                        )
                    )
                }
            }

            Log.d(TAG, "Added/Updated cart item for product ${product.id} in window $windowId")
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

    // In your ViewModel or Repository
    private suspend fun generateTransactionId(): String {
        val currentStore = SessionManager.getCurrentUser()?.storeid
            ?: throw IllegalStateException("No store ID found in current session")
        return numberSequenceRepository.getNextNumber("TRANSACTION", currentStore)
    }


    private fun loadLastTransactionNumber() {
        val sharedPref = getSharedPreferences("TransactionPrefs", Context.MODE_PRIVATE)
        lastTransactionNumber = sharedPref.getInt("lastTransactionNumber", 0)
    }

private fun initializeSequences() {
    lifecycleScope.launch {
        try {
            val currentStore = SessionManager.getCurrentUser()?.storeid
            if (currentStore != null) {
                // Initialize sequence for the current store
                numberSequenceRepository.initializeSequence(
                    type = "TRANSACTION",
                    storeId = currentStore,
                    startValue = 1,
                    paddingLength = 9
                )

                // Check current sequence value
                val currentValue = numberSequenceRepository.getCurrentValue("TRANSACTION", currentStore)
                Log.d(TAG, "Current sequence for store $currentStore: $currentValue")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing number sequences", e)
        }
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

        // Load current sequence for the current store
        lifecycleScope.launch {
            val currentStore = SessionManager.getCurrentUser()?.storeid
            if (currentStore != null) {
                val currentValue = numberSequenceRepository.getCurrentValue("TRANSACTION", currentStore)
                currentSequence.text = "Current Sequence: ${currentValue ?: 0}"
            }
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
                    numberSequenceRepository.resetSequence("TRANSACTION", newValue.toString())
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
//    suspend fun someMethodThatAccessesDatabase() {
//        withContext(Dispatchers.IO) {
//            // Database operations here
//            numberSequenceRemoteRepository.getNextTransactionNumber(storeId)
//        }
//    }
//private fun processPayment(
//    amountPaid: Double,
//    paymentMethod: String,
//    vatRate: Double,
//    discountType: String,
//    discountValue: Double,
//    selectedCustomer: Customer,
//    totalAmountDue: Double,
//    otherPaymentMethods: List<String> = emptyList(),
//    otherPaymentAmounts: List<Double> = emptyList()
//) {
//    lifecycleScope.launch(Dispatchers.IO) {
//        try {
//            Log.d("Payment", "Starting payment process...")
//            val cartItems = cartViewModel.getAllCartItems(windowId).first()
//            val transactionComment = cartItems.firstOrNull()?.cartComment ?: ""
//
//            if (cartItems.isEmpty()) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Cannot process payment. Cart is empty.",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                return@launch
//            }
//
//            val currentStore = SessionManager.getCurrentUser()?.storeid
//            Log.d("Payment", "Current store ID: $currentStore")
//
//            if (currentStore == null) {
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "No store ID found in session",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//                return@launch
//            }
//
//            try {
//                Log.d("Payment", "Fetching number sequence for store: $currentStore")
//                val numberSequenceResult = numberSequenceRemoteRepository.fetchAndUpdateNumberSequence(currentStore)
//
//                numberSequenceResult.onSuccess { numberSequence ->
//                    Log.d("Payment", "Successfully fetched number sequence: $numberSequence")
//
//                    // All database operations within withContext(Dispatchers.IO)
//                    withContext(Dispatchers.IO) {
//                        val transactionId = numberSequenceRemoteRepository.getNextTransactionNumber(currentStore)
//                        Log.d("Payment", "Generated transaction ID: $transactionId")
//
//                        val storeKey = numberSequenceRemoteRepository.getCurrentStoreKey(currentStore)
//
//                        Log.d("Payment", "Generated store key: $storeKey")
//
//
//                        var gross = 0.0
//            var totalDiscount = 0.0
//            var bundleDiscount = 0.0
//            var vatAmount = 0.0
//            var vatableSales = 0.0
//            var partialPayment = 0.0
//            var priceOverrideTotal = 0.0
//            var hasPriceOverride = false
//
//            // Get existing discount from first cart item if partial payment exists
//            val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
//            val existingDiscount = if (existingPartialPayment > 0) {
//                cartItems.firstOrNull()?.discountAmount ?: 0.0
//            } else null
//
//            // Group items by bundleId for bundle discount calculation
//            val bundledItems = cartItems.groupBy { it.bundleId }
//
//            // Calculate totals from cart items
//            cartItems.forEach { cartItem ->
//                val originalPrice = cartItem.price
//                val effectivePrice = cartItem.overriddenPrice ?: originalPrice
//                val itemTotal = effectivePrice * cartItem.quantity
//                gross += itemTotal
//                partialPayment = cartItem.partialPayment
//
//                // If we have existing discount from partial payment, use that instead of recalculating
//                if (existingDiscount != null) {
//                    totalDiscount = existingDiscount
//                } else {
//                    // Calculate regular discounts
//                    when (cartItem.discountType.uppercase()) {
//                        "FIXEDTOTAL" -> {
//                            if (cartItem.bundleId == null) {
//                                totalDiscount += cartItem.discountAmount
//                            }
//                        }
//                        "PERCENTAGE", "PWD", "SC" -> {
//                            if (cartItem.bundleId == null) {
//                                totalDiscount += itemTotal * (cartItem.discount / 100)
//                            }
//                        }
//                        "FIXED" -> {
//                            if (cartItem.bundleId == null) {
//                                totalDiscount += cartItem.discount * cartItem.quantity
//                            }
//                        }
//                    }
//
//                    // Calculate bundle discounts
//                    if (cartItem.bundleId != null) {
//                        val bundleItems = bundledItems[cartItem.bundleId]
//                        if (bundleItems != null) {
//                            when (cartItem.discountType.uppercase()) {
//                                "DEAL" -> bundleDiscount += cartItem.discount
//                                "PERCENTAGE" -> bundleDiscount += itemTotal * (cartItem.discount / 100)
//                                "FIXEDTOTAL" -> bundleDiscount += cartItem.discount
//                            }
//                        }
//                    }
//                }
//
//                vatableSales += itemTotal / vatRate
//                vatAmount += itemTotal * 0.12 / 1.12
//            }
//
//            // Add bundle discount to total discount
//            totalDiscount += bundleDiscount
//
//            val discountedSubtotal = gross - totalDiscount
//            val netSales = discountedSubtotal - partialPayment
//            val totalAmountDue = netSales.roundToTwoDecimals()
////                val transactionId = cartItems.firstOrNull()?.transactionId ?: generateTransactionId()
//
//
////            val transactionId = numberSequenceRepository.getNextNumber("TRANSACTION", currentStore)
////
////
////            val storeKey = numberSequenceRepository.getCurrentStoreKey("TRANSACTION", currentStore)
//
//            // Create store-specific sequence
//            val storeSequence = "$currentStore-${transactionId.split("-").last()}"
//            // Determine if the payment method is AR
//            val isAR = paymentMethod != "Cash"
//
//            val change = if (isAR) 0.0 else (amountPaid - totalAmountDue).roundToTwoDecimals()
//            val ar = if (isAR) totalAmountDue else 0.0
//
//            val paymentMethodAmounts = mutableMapOf(
//                "GCASH" to 0.0,
//                "PAYMAYA" to 0.0,
//                "CASH" to 0.0,
//                "CARD" to 0.0,
//                "LOYALTYCARD" to 0.0,
//                "CHARGE" to 0.0,
//                "FOODPANDA" to 0.0,
//                "GRABFOOD" to 0.0,
//                "REPRESENTATION" to 0.0
//            )
//
//            // Set the primary payment method amount
//            val primaryPaymentKey = paymentMethod.uppercase()
//            paymentMethodAmounts[primaryPaymentKey] = amountPaid
//
//            // Distribute remaining payment methods if provided
//            otherPaymentMethods.forEachIndexed { index, method ->
//                if (index < otherPaymentAmounts.size) {
//                    paymentMethodAmounts[method.uppercase()] = otherPaymentAmounts[index]
//                }
//            }
//
//            // Prepare the transaction summary
//            val transactionSummary = TransactionSummary(
//                transactionId = transactionId,
//                type = if (isAR) 3 else 1, // 3 for AR, 1 for Cash
//                receiptId = transactionId,
//                store = getCurrentStore(),
//                staff = getCurrentStaff(),
//                storeKey = storeKey,
//                storeSequence = storeSequence,
//                customerAccount = selectedCustomer.name,
//                netAmount = discountedSubtotal,
//                costAmount = gross - vatAmount,
//                grossAmount = gross,
//                partialPayment = partialPayment,
//                transactionStatus = 1,
//                discountAmount = totalDiscount,
//                customerDiscountAmount = totalDiscount,
//                totalDiscountAmount = totalDiscount,
//                numberOfItems = cartItems.sumOf { it.quantity }.toDouble(),
//                refundReceiptId = null,
//                currency = "PHP",
//                zReportId = null,
//                createdDate = Date(),
//                priceOverride = if (hasPriceOverride) priceOverrideTotal else 0.0,
//                comment = transactionComment,
//                receiptEmail = null,
//                markupAmount = 0.0,
//                markupDescription = null,
//                taxIncludedInPrice = vatAmount,
//                windowNumber = windowId,
//                // Update these payment fields
//                gCash = paymentMethodAmounts["GCASH"] ?: 0.0,
//                payMaya = paymentMethodAmounts["PAYMAYA"] ?: 0.0,
//                cash = paymentMethodAmounts["CASH"] ?: 0.0,
//                card = paymentMethodAmounts["CARD"] ?: 0.0,
//                loyaltyCard = paymentMethodAmounts["LOYALTYCARD"] ?: 0.0,
//                charge = paymentMethodAmounts["CHARGE"] ?: 0.0,
//                foodpanda = paymentMethodAmounts["FOODPANDA"] ?: 0.0,
//                grabfood = paymentMethodAmounts["GRABFOOD"] ?: 0.0,
//                representation = paymentMethodAmounts["REPRESENTATION"] ?: 0.0,
//                changeGiven = change,
//                totalAmountPaid = if (isAR) 0.0 else amountPaid,  // Make sure this is set correctly
//                paymentMethod = paymentMethod,
//                customerName = selectedCustomer.name,
//                vatAmount =  (discountedSubtotal / 1.12) * 0.12,
//                vatExemptAmount = 0.0,
//                vatableSales = discountedSubtotal / 1.12,
//                discountType = discountType,
//                syncStatus = false
//            )
//
//            // Insert the transaction summary into the database
//                        transactionSummary.syncStatus = false
//                        transactionDao.insertTransactionSummary(transactionSummary)
//                        transactionDao.insertAll(transactionRecords)
//
//
//                        updateTransactionRecords(
//                            transactionId,
//                            cartItems,
//                            paymentMethod,
//                            ar,
//                            vatRate,
//                            totalDiscount,
//                            netSales,
//                            partialPayment,
//                            discountType
//                        )
//
//                        val transactionRecords = getTransactionRecords(
//                            transactionId,
//                            cartItems,
//                            paymentMethod,
//                            ar,
//                            vatRate,
//                            discountType
//
//                        )
//                        transactionRecords.forEach { record ->
//                            record.syncStatusRecord = false
//                        }
//
//                        // Insert the records
//                        // UI operations moved to Main dispatcher
//                        withContext(Dispatchers.Main) {
//                            // Your existing receipt printing and UI update code
//                            if (isAR) {
//                                printReceiptWithBluetoothPrinter(
//                                    transactionSummary,
//                                    transactionRecords,
//                                    BluetoothPrinterHelper.ReceiptType.AR,
//                                    isARReceipt = true,
//                                    copyType = "Customer Copy"
//                                )
//                printReceiptWithBluetoothPrinter(
//                    transactionSummary,
//                    transactionRecords,
//                    BluetoothPrinterHelper.ReceiptType.AR,
//                    isARReceipt = true,
//                    copyType = "Staff Copy"
//                )
//            } else {
//                printReceiptWithBluetoothPrinter(
//                    transactionSummary,
//                    transactionRecords,
//                    BluetoothPrinterHelper.ReceiptType.ORIGINAL
//                )
//            }
//
//                            showChangeAndReceiptDialog(
//                                change,
//                                cartItems,
//                                transactionId,
//                                paymentMethod,
//                                ar,
//                                vatAmount,
//                                totalDiscount,
//                                netSales,
//                                if (gross > 0) totalDiscount / gross else 0.0,
//                                transactionComment,
//                                partialPayment,
//                                amountPaid
//                            )
//                        }
//
//                        // Back to IO dispatcher for cleanup
//                        transactionDao.updateSyncStatus(transactionId, false)
//                        cartViewModel.deleteAll(windowId)
//                        cartViewModel.clearCartComment(windowId)
//
//                        withContext(Dispatchers.Main) {
//                            updateTotalAmount(emptyList())
//                            SessionManager.setCurrentNumberSequence(numberSequence)
//
//                            val transactionLogger = TransactionLogger(this@Window1)
//                            transactionLogger.logPayment(
//                                transactionSummary,
//                                paymentMethod,
//                                totalAmountDue
//                            )
//                        }
//                    }
//                }.onFailure { error ->
//                    Log.e("Payment", "Failed to fetch number sequence: ${error.message}")
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@Window1,
//                            "Failed to generate transaction number: ${error.message}",
//                            Toast.LENGTH_LONG
//                        ).show()
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("Payment", "Error in number sequence processing", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Error in number sequence: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        } catch (e: Exception) {
//            Log.e("Payment", "Error during payment processing: ${e.message}")
//            withContext(Dispatchers.Main) {
//                Toast.makeText(
//                    this@Window1,
//                    "Error processing payment: ${e.message}",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//        }
//    }
//}

        private fun processPayment(
        amountPaid: Double,
        paymentMethod: String,
        vatRate: Double,
        discountType: String,
        discountValue: Double,
        selectedCustomer: Customer,
        totalAmountDue: Double,
        otherPaymentMethods: List<String> = emptyList(),
        otherPaymentAmounts: List<Double> = emptyList()
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d("Payment", "Starting payment process...")
                val cartItems = cartViewModel.getAllCartItems(windowId).first()
                val transactionComment = cartItems.firstOrNull()?.cartComment ?: ""

                if (cartItems.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Cannot process payment. Cart is empty.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val currentStore = SessionManager.getCurrentUser()?.storeid
                Log.d("Payment", "Current store ID: $currentStore")

                if (currentStore == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "No store ID found in session",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                try {
                    Log.d("Payment", "Fetching number sequence for store: $currentStore")
                    val numberSequenceResult = numberSequenceRemoteRepository.fetchAndUpdateNumberSequence(currentStore)

                    numberSequenceResult.onSuccess { numberSequence ->
                        Log.d("Payment", "Successfully fetched number sequence: $numberSequence")

                        // All database operations within withContext(Dispatchers.IO)
                        withContext(Dispatchers.IO) {
                            val transactionId = numberSequenceRemoteRepository.getNextTransactionNumber(currentStore)
                            Log.d("Payment", "Generated transaction ID: $transactionId")

                            val storeKey = numberSequenceRemoteRepository.getCurrentStoreKey(currentStore)

                            Log.d("Payment", "Generated store key: $storeKey")


                            var gross = 0.0
                            var totalDiscount = 0.0
                            var bundleDiscount = 0.0
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

                            // Group items by bundleId for bundle discount calculation
                            val bundledItems = cartItems.groupBy { it.bundleId }

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
                                    // Calculate regular discounts
                                    when (cartItem.discountType.uppercase()) {
                                        "FIXEDTOTAL" -> {
                                            if (cartItem.bundleId == null) {
                                                totalDiscount += cartItem.discountAmount
                                            }
                                        }
                                        "PERCENTAGE", "PWD", "SC" -> {
                                            if (cartItem.bundleId == null) {
                                                totalDiscount += itemTotal * (cartItem.discount / 100)
                                            }
                                        }
                                        "FIXED" -> {
                                            if (cartItem.bundleId == null) {
                                                totalDiscount += cartItem.discount * cartItem.quantity
                                            }
                                        }
                                    }

                                    // Calculate bundle discounts
                                    if (cartItem.bundleId != null) {
                                        val bundleItems = bundledItems[cartItem.bundleId]
                                        if (bundleItems != null) {
                                            when (cartItem.discountType.uppercase()) {
                                                "DEAL" -> bundleDiscount += cartItem.discount
                                                "PERCENTAGE" -> bundleDiscount += itemTotal * (cartItem.discount / 100)
                                                "FIXEDTOTAL" -> bundleDiscount += cartItem.discount
                                            }
                                        }
                                    }
                                }

                                vatableSales += itemTotal / vatRate
                                vatAmount += itemTotal * 0.12 / 1.12
                            }

                            // Add bundle discount to total discount
                            totalDiscount += bundleDiscount

                            val discountedSubtotal = gross - totalDiscount
                            val netSales = discountedSubtotal - partialPayment
                            val totalAmountDue = netSales.roundToTwoDecimals()
//                val transactionId = cartItems.firstOrNull()?.transactionId ?: generateTransactionId()


//            val transactionId = numberSequenceRepository.getNextNumber("TRANSACTION", currentStore)
//
//
//            val storeKey = numberSequenceRepository.getCurrentStoreKey("TRANSACTION", currentStore)

                            // Create store-specific sequence
                            val storeSequence = "$currentStore-${transactionId.split("-").last()}"
                            // Determine if the payment method is AR
                            val isAR = paymentMethod != "Cash"

                            val change = if (isAR) 0.0 else (amountPaid - totalAmountDue).roundToTwoDecimals()
                            val ar = if (isAR) totalAmountDue else 0.0

                            val paymentMethodAmounts = mutableMapOf(
                                "GCASH" to 0.0,
                                "PAYMAYA" to 0.0,
                                "CASH" to 0.0,
                                "CARD" to 0.0,
                                "LOYALTYCARD" to 0.0,
                                "CHARGE" to 0.0,
                                "FOODPANDA" to 0.0,
                                "GRABFOOD" to 0.0,
                                "REPRESENTATION" to 0.0
                            )

                            // Set the primary payment method amount
                            val primaryPaymentKey = paymentMethod.uppercase()
                            paymentMethodAmounts[primaryPaymentKey] = amountPaid

                            // Distribute remaining payment methods if provided
                            otherPaymentMethods.forEachIndexed { index, method ->
                                if (index < otherPaymentAmounts.size) {
                                    paymentMethodAmounts[method.uppercase()] = otherPaymentAmounts[index]
                                }
                            }

                            // Prepare the transaction summary
                            val transactionSummary = TransactionSummary(
                                transactionId = transactionId,
                                type = if (isAR) 3 else 1, // 3 for AR, 1 for Cash
                                receiptId = transactionId,
                                store = getCurrentStore(),
                                staff = getCurrentStaff(),
                                storeKey = storeKey,
                                storeSequence = storeSequence,
                                customerAccount = selectedCustomer.name,
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
                                // Update these payment fields
                                gCash = paymentMethodAmounts["GCASH"] ?: 0.0,
                                payMaya = paymentMethodAmounts["PAYMAYA"] ?: 0.0,
                                cash = paymentMethodAmounts["CASH"] ?: 0.0,
                                card = paymentMethodAmounts["CARD"] ?: 0.0,
                                loyaltyCard = paymentMethodAmounts["LOYALTYCARD"] ?: 0.0,
                                charge = paymentMethodAmounts["CHARGE"] ?: 0.0,
                                foodpanda = paymentMethodAmounts["FOODPANDA"] ?: 0.0,
                                grabfood = paymentMethodAmounts["GRABFOOD"] ?: 0.0,
                                representation = paymentMethodAmounts["REPRESENTATION"] ?: 0.0,
                                changeGiven = change,
                                totalAmountPaid = if (isAR) 0.0 else amountPaid,  // Make sure this is set correctly
                                paymentMethod = paymentMethod,
                                customerName = selectedCustomer.name,
                                vatAmount =  (discountedSubtotal / 1.12) * 0.12,
                                vatExemptAmount = 0.0,
                                vatableSales = discountedSubtotal / 1.12,
                                discountType = discountType,
                                syncStatus = false
                            )

                            // Insert the transaction summary into the database
                            transactionDao.insertTransactionSummary(transactionSummary)

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

                            val transactionRecords = getTransactionRecords(
                                transactionId,
                                cartItems,
                                paymentMethod,
                                ar,
                                vatRate,
                                discountType
                            )
                            transactionRecords.forEach { record ->
                            record.syncStatusRecord = false
                        }

                            // UI operations moved to Main dispatcher
                            withContext(Dispatchers.Main) {
                                // Your existing receipt printing and UI update code
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
                            }

                            // Back to IO dispatcher for cleanup
                            transactionDao.updateSyncStatus(transactionId, false)
                            cartViewModel.deleteAll(windowId)
                            cartViewModel.clearCartComment(windowId)

                            withContext(Dispatchers.Main) {
                                updateTotalAmount(emptyList())
                                SessionManager.setCurrentNumberSequence(numberSequence)

                                val items = withContext(Dispatchers.IO) {
                                    transactionDao.getTransactionItems(transactionId)
                                }

                                val transactionLogger = TransactionLogger(this@Window1)
                                transactionLogger.logPayment(
                                    transactionSummary = transactionSummary,
                                    paymentMethod = paymentMethod,
                                    items = items
                                )

                            }
                        }
                    }.onFailure { error ->
                        Log.e("Payment", "Failed to fetch number sequence: ${error.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@Window1,
                                "Failed to generate transaction number: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Payment", "Error in number sequence processing", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@Window1,
                            "Error in number sequence: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Payment", "Error during payment processing: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Window1,
                        "Error processing payment: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
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
        discountType: String,
        otherPaymentMethods: List<String> = emptyList(),
        otherPaymentAmounts: List<Double> = emptyList()
    ) {
        val currentStore = SessionManager.getCurrentUser()?.storeid
            ?: throw IllegalStateException("No store ID found in current session")

        val storeKey = numberSequenceRemoteRepository.getCurrentStoreKey(currentStore)
        val storeSequence = "$currentStore-${transactionId.split("-").last()}"

        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
        val existingDiscount = if (existingPartialPayment > 0) {
            cartItems.firstOrNull()?.discountAmount ?: 0.0
        } else null

        // Combine all payment methods and amounts
        val allPaymentMethods = listOf(paymentMethod) + otherPaymentMethods
        val allPaymentAmounts = listOf(netSales) + otherPaymentAmounts

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

            // Create a list to track split payments for this item
            val splitPayments = allPaymentMethods.mapIndexed { paymentIndex, method ->
                val paymentProportion = allPaymentAmounts[paymentIndex] / netSales
                val splitItemTotal = itemTotal * paymentProportion
                val splitItemDiscount = itemDiscount * paymentProportion

                TransactionRecord(
                    transactionId = transactionId,
                    name = cartItem.productName,
                    price = cartItem.price,
                    quantity = cartItem.quantity,
                    subtotal = splitItemTotal,
                    vatRate = vatRate - 1,
                    vatAmount = itemVat * paymentProportion,
                    discountRate = when (cartItem.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
                        else -> if (splitItemTotal > 0) splitItemDiscount / splitItemTotal else 0.0
                    },
                    discountAmount = splitItemDiscount,
                    total = splitItemTotal - splitItemDiscount,
                    receiptNumber = transactionId,
                    timestamp = System.currentTimeMillis(),
                    paymentMethod = method.uppercase(),
                    ar = if (ar > 0.0) ((effectivePrice * cartItem.quantity) * paymentProportion) else 0.0,
                    windowNumber = windowId,
                    partialPaymentAmount = if (index == 0) partialPayment * paymentProportion else 0.0,
                    comment = cartItem.cartComment ?: "",
                    lineNum = index + 1,
                    receiptId = transactionId,
                    itemId = cartItem.itemId.toString(),
                    itemGroup = cartItem.itemGroup.toString(),
                    netPrice = cartItem.price,
                    costAmount = splitItemTotal / vatRate,
                    netAmount = splitItemTotal - splitItemDiscount,
                    grossAmount = splitItemTotal,
                    customerAccount = null,
                    store = getCurrentStore(),
                    priceOverride = cartItem.overriddenPrice ?: 0.0,
                    staff = getCurrentStaff(),
                    discountOfferId = cartItem.mixMatchId ?: "",
                    lineDiscountAmount = splitItemDiscount,
                    lineDiscountPercentage = if (splitItemTotal > 0) (splitItemDiscount / splitItemTotal) * 100 else 0.0,
                    customerDiscountAmount = splitItemDiscount,
                    unit = "PCS",
                    unitQuantity = cartItem.quantity.toDouble(),
                    unitPrice = cartItem.price,
                    taxAmount = ((splitItemTotal - splitItemDiscount) / 1.12) * 0.12,
                    createdDate = Date(),
                    discountType = when (cartItem.discountType.toUpperCase()) {
                        "PWD" -> "PWD"
                        "SC" -> "SC"
                        "PERCENTAGE" -> "PERCENTAGE"
                        "FIXED" -> "FIXED"
                        "FIXEDTOTAL", "FIXED TOTAL" -> "FIXEDTOTAL"
                        else -> "No Discount"
                    },
                    netAmountNotIncludingTax = (splitItemTotal - splitItemDiscount) / 1.12,
                    currency = "PHP",
                    storeKey = storeKey,
                    storeSequence = storeSequence
                )
            }

            // Insert each split payment transaction record
            splitPayments.forEach { transactionRecord ->
                transactionDao.insertOrUpdateTransactionRecord(transactionRecord)
            }
        }
    }

    private suspend fun getTransactionRecords(
        transactionId: String,
        cartItems: List<CartItem>,
        paymentMethod: String,
        ar: Double,
        vatRate: Double,
        discountType: String,
        otherPaymentMethods: List<String> = emptyList(),
        otherPaymentAmounts: List<Double> = emptyList()
    ): List<TransactionRecord> {
        val currentStore = SessionManager.getCurrentUser()?.storeid
            ?: throw IllegalStateException("No store ID found in current session")

        val storeKey = numberSequenceRemoteRepository.getCurrentStoreKey(currentStore)
        val storeSequence = "$currentStore-${transactionId.split("-").last()}"

        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
        val existingDiscount = if (existingPartialPayment > 0) {
            cartItems.firstOrNull()?.discountAmount ?: 0.0
        } else null

        // Calculate total amount to split
        val totalAmount = cartItems.sumOf {
            (it.overriddenPrice ?: it.price) * it.quantity
        } - (existingDiscount ?: 0.0)

        // Combine all payment methods and amounts
        val allPaymentMethods = listOf(paymentMethod) + otherPaymentMethods
        val allPaymentAmounts = listOf(totalAmount) + otherPaymentAmounts

        val transactionRecords = mutableListOf<TransactionRecord>()

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
            val netAmountNotIncludingTax = (itemTotal - itemDiscount) / 1.12

            // Create split transaction records for each payment method
            val splitRecords = allPaymentMethods.mapIndexed { paymentIndex, method ->
                val paymentProportion = allPaymentAmounts[paymentIndex] / totalAmount
                val splitItemTotal = itemTotal * paymentProportion
                val splitItemDiscount = itemDiscount * paymentProportion

                TransactionRecord(
                    transactionId = transactionId,
                    name = cartItem.productName,
                    price = cartItem.price,
                    quantity = cartItem.quantity,
                    subtotal = splitItemTotal,
                    vatRate = vatRate - 1,
                    vatAmount = itemVat * paymentProportion,
                    discountRate = when (cartItem.discountType.uppercase()) {
                        "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
                        else -> if (splitItemTotal > 0) splitItemDiscount / splitItemTotal else 0.0
                    },
                    discountAmount = splitItemDiscount,
                    total = splitItemTotal - splitItemDiscount,
                    receiptNumber = transactionId,
                    timestamp = System.currentTimeMillis(),
                    paymentMethod = method.uppercase(),
                    ar = if (ar > 0.0) ((effectivePrice * cartItem.quantity) * paymentProportion) else 0.0,
                    windowNumber = windowId,
                    partialPaymentAmount = if (index == 0) cartItem.partialPayment * paymentProportion else 0.0,
                    comment = cartItem.cartComment ?: "",
                    lineNum = index + 1,
                    receiptId = transactionId,
                    itemId = cartItem.productId.toString(),
                    itemGroup = cartItem.id.toString(),
                    netPrice = cartItem.price,
                    costAmount = splitItemTotal / vatRate,
                    netAmount = splitItemTotal - splitItemDiscount,
                    grossAmount = splitItemTotal,
                    customerAccount = null,
                    store = getCurrentStore(),
                    priceOverride = cartItem.overriddenPrice ?: 0.0,
                    staff = getCurrentStaff(),
                    discountOfferId = cartItem.mixMatchId ?: "",
                    lineDiscountAmount = splitItemDiscount,
                    lineDiscountPercentage = if (splitItemTotal > 0) (splitItemDiscount / splitItemTotal) * 100 else 0.0,
                    customerDiscountAmount = splitItemDiscount,
                    unit = "PCS",
                    unitQuantity = cartItem.quantity.toDouble(),
                    unitPrice = cartItem.price,
                    taxAmount = itemVat * paymentProportion,
                    createdDate = Date(),
                    discountType = when (cartItem.discountType.toUpperCase()) {
                        "PWD" -> "PWD"
                        "SC" -> "SC"
                        "PERCENTAGE" -> "PERCENTAGE"
                        "FIXED" -> "FIXED"
                        "FIXED TOTAL", "FIXEDTOTAL" -> "FIXEDTOTAL"
                        else -> "No Discount"
                    },
                    netAmountNotIncludingTax = netAmountNotIncludingTax * paymentProportion,
                    storeTaxGroup = null,
                    currency = "PHP",
                    taxExempt = 0.0,
                    storeKey = storeKey,
                    storeSequence = storeSequence
                )
            }

            transactionRecords.addAll(splitRecords)
        }

        return transactionRecords
    }


//    private suspend fun updateTransactionRecords(
//        transactionId: String,
//        cartItems: List<CartItem>,
//        paymentMethod: String,
//        ar: Double,
//        vatRate: Double,
//        totalDiscount: Double,
//        netSales: Double,
//        partialPayment: Double,
//        discountType: String,
//        otherPaymentMethods: List<String> = emptyList(),
//        otherPaymentAmounts: List<Double> = emptyList()
//    ) {
//        val currentStore = SessionManager.getCurrentUser()?.storeid
//            ?: throw IllegalStateException("No store ID found in current session")
//
//        val storeKey = numberSequenceRepository.getCurrentStoreKey("TRANSACTION", currentStore)
//        val storeSequence = "$currentStore-${transactionId.split("-").last()}"
//
//        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
//        val existingDiscount = if (existingPartialPayment > 0) {
//            cartItems.firstOrNull()?.discountAmount ?: 0.0
//        } else null
//
//        cartItems.forEachIndexed { index, cartItem ->
//            val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
//            val itemTotal = effectivePrice * cartItem.quantity
//
//            // Use existing discount if partial payment exists
//            val itemDiscount = if (existingDiscount != null) {
//                if (index == 0) existingDiscount else 0.0 // Apply discount only to first item
//            } else {
//                when (cartItem.discountType.uppercase()) {
//                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)
//                    "FIXED" -> cartItem.discount * cartItem.quantity
//                    "FIXEDTOTAL" -> cartItem.discountAmount
//                    else -> 0.0
//                }
//            }
//
//            val itemVat = itemTotal * 0.12 / 1.12
//
//            val transactionRecord = TransactionRecord(
//                transactionId = transactionId,
//                name = cartItem.productName,
//                price = cartItem.price,
//                quantity = cartItem.quantity,
//                subtotal = itemTotal,
//                vatRate = vatRate - 1,
//                vatAmount = itemVat,
//                discountRate = when (cartItem.discountType.uppercase()) {
//                    "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
//                    else -> if (itemTotal > 0) itemDiscount / itemTotal else 0.0
//                },
//                discountAmount = when (cartItem.discountType.uppercase()) {
//                    "FIXED" -> cartItem.discount  // Store per-item discount
//                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)  // Store total discount
//                    "FIXEDTOTAL" -> cartItem.discountAmount  // Store fixed total discount
//                    else -> 0.0
//                },
//                total = itemTotal - itemDiscount,
//                receiptNumber = transactionId,
//                timestamp = System.currentTimeMillis(),
//                paymentMethod = paymentMethod,
//                ar = if (ar > 0.0) ((cartItem.overriddenPrice
//                    ?: cartItem.price) * cartItem.quantity) else 0.0,
//                windowNumber = windowId,
//                partialPaymentAmount = if (index == 0) partialPayment else 0.0,
//                comment = cartItem.cartComment ?: "",
//                lineNum = index + 1,
//                receiptId = transactionId,
//                itemId = cartItem.itemId.toString(),
//                itemGroup = cartItem.itemGroup.toString(),
//                netPrice = cartItem.price,
//                costAmount = itemTotal / vatRate,
//                netAmount = itemTotal - itemDiscount,
//                grossAmount = itemTotal,
//                customerAccount = null,
//                store = getCurrentStore(),
//                priceOverride = cartItem.overriddenPrice ?: 0.0,
//                staff = getCurrentStaff(),
//                discountOfferId = cartItem.mixMatchId ?: "",
//                lineDiscountAmount = itemDiscount,
//                lineDiscountPercentage = if (itemTotal > 0) (itemDiscount / itemTotal) * 100 else 0.0,
//                customerDiscountAmount = itemDiscount,
//                unit = "PCS",
//                unitQuantity = cartItem.quantity.toDouble(),
//                unitPrice = cartItem.price,
//                taxAmount =((itemTotal - itemDiscount) / 1.12) * 0.12,
//                createdDate = Date(),
//                remarks = null,
//                inventoryBatchId = null,
//                inventoryBatchExpiryDate = null,
//                giftCard = null,
//                returnTransactionId = null,
//                returnQuantity = null,
//                creditMemoNumber = null,
//                taxIncludedInPrice = ((itemTotal - itemDiscount) / 1.12) * 0.12,
//                description = null,
//                returnLineId = null,
//                priceUnit = 1.0,
//                discountType = when (cartItem.discountType.toUpperCase()) {
//                    "PWD" -> "PWD"
//                    "SC" -> "SC"
//                    "PERCENTAGE" -> "PERCENTAGE"
//                    "FIXED" -> "FIXED"
//                    "FIXEDTOTAL", "FIXED TOTAL" -> "FIXEDTOTAL"
//                    else -> "No Discount"
//                },
//                netAmountNotIncludingTax = (itemTotal - itemDiscount) / 1.12,
//                storeTaxGroup = null,
//                currency = "PHP",
//                taxExempt = 0.0,
//                storeKey = storeKey,
//                storeSequence = storeSequence,
//
//
//
//            )
//            Log.d("TransactionSync", "MixMatchId (DiscountOfferId): ${cartItem.mixMatchId}")
//
//
//            Log.d("TransactionDebug", """
//            Cart Item Details:
//            ProductName: ${cartItem.productName}
//            MixMatchId: ${cartItem.mixMatchId}
//            DiscountType: ${cartItem.discountType}
//            Is MixMatchId null: ${cartItem.mixMatchId == null}
//        """.trimIndent())
//            Log.d(
//                "TransactionDebug", """
//            Created TransactionRecord:
//            Item: ${transactionRecord.name}
//            DiscountType: ${transactionRecord.discountType}
//            DiscountAmount: ${transactionRecord.discountAmount}
//            Original CartItem DiscountAmount: ${cartItem.discountAmount}
//        """.trimIndent()
//
//            )
//
//            transactionDao.insertOrUpdateTransactionRecord(transactionRecord)
//        }
//    }
//
//    private suspend fun getTransactionRecords(
//        transactionId: String,
//        cartItems: List<CartItem>,
//        paymentMethod: String,
//        ar: Double,
//        vatRate: Double,
//        discountType: String
//    ): List<TransactionRecord> {
//        val currentStore = SessionManager.getCurrentUser()?.storeid
//            ?: throw IllegalStateException("No store ID found in current session")
//
//        val storeKey = numberSequenceRepository.getCurrentStoreKey("TRANSACTION", currentStore)
//        val storeSequence = "$currentStore-${transactionId.split("-").last()}"
//
//        val existingPartialPayment = cartItems.firstOrNull()?.partialPayment ?: 0.0
//        val existingDiscount = if (existingPartialPayment > 0) {
//            cartItems.firstOrNull()?.discountAmount ?: 0.0
//        } else null
//
//        return cartItems.mapIndexed { index, cartItem ->
//            val effectivePrice = cartItem.overriddenPrice ?: cartItem.price
//            val itemTotal = effectivePrice * cartItem.quantity
//
//            // Use existing discount if partial payment exists
//            val itemDiscount = if (existingDiscount != null) {
//                if (index == 0) existingDiscount else 0.0 // Apply discount only to first item
//            } else {
//                when (cartItem.discountType.uppercase()) {
//                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)
//                    "FIXED" -> cartItem.discount * cartItem.quantity
//                    "FIXEDTOTAL" -> cartItem.discountAmount
//                    else -> 0.0
//                }
//            }
//
//            val itemVat = itemTotal * 0.12 / 1.12
//            val netAmountNotIncludingTax = (itemTotal - itemDiscount) / 1.12
//
//            TransactionRecord(
//                transactionId = transactionId,
//                name = cartItem.productName,
//                price = cartItem.price,
//                quantity = cartItem.quantity,
//                subtotal = itemTotal,
//                vatRate = vatRate - 1,
//                vatAmount = itemVat,
//                discountRate = when (cartItem.discountType.uppercase()) {
//                    "PERCENTAGE", "PWD", "SC" -> cartItem.discount / 100
//                    else -> if (itemTotal > 0) itemDiscount / itemTotal else 0.0
//                },
//                discountAmount = when (cartItem.discountType.uppercase()) {
//                    "FIXED" -> cartItem.discount  // Store per-item discount
//                    "PERCENTAGE", "PWD", "SC" -> itemTotal * (cartItem.discount / 100)  // Store total discount
//                    "FIXEDTOTAL" -> cartItem.discountAmount  // Store fixed total discount
//                    else -> 0.0
//                },
//                total = itemTotal - itemDiscount,
//                receiptNumber = transactionId,
//                timestamp = System.currentTimeMillis(),
//                paymentMethod = paymentMethod,
//                ar = if (ar > 0.0) (effectivePrice * cartItem.quantity) else 0.0,
//                windowNumber = windowId,
//                partialPaymentAmount = if (index == 0) cartItem.partialPayment ?: 0.0 else 0.0,
//                comment = cartItem.cartComment ?: "",
//                lineNum = index + 1,
//                receiptId = transactionId,
//                itemId = cartItem.productId.toString(),
//                itemGroup = cartItem.id.toString(),
//                netPrice = cartItem.price,
//                costAmount = itemTotal / vatRate,
//                netAmount = itemTotal - itemDiscount,
//                grossAmount = itemTotal,
//                customerAccount = null,
//                store = getCurrentStore(),
//                priceOverride = cartItem.overriddenPrice ?: 0.0,
//                staff = getCurrentStaff(),
//                discountOfferId = cartItem.mixMatchId ?: "",
//                lineDiscountAmount = itemDiscount,
//                lineDiscountPercentage = if (itemTotal > 0) (itemDiscount / itemTotal) * 100 else 0.0,
//                customerDiscountAmount = itemDiscount,
//                unit = "PCS",
//                unitQuantity = cartItem.quantity.toDouble(),
//                unitPrice = cartItem.price,
//                taxAmount = itemVat,
//                createdDate = Date(),
//                remarks = null,
//                inventoryBatchId = null,
//                inventoryBatchExpiryDate = null,
//                giftCard = null,
//                returnTransactionId = null,
//                returnQuantity = null,
//                creditMemoNumber = null,
//                taxIncludedInPrice = itemVat,
//                description = null,
//                returnLineId = null,
//                priceUnit = 1.0,
//                discountType = when (cartItem.discountType.toUpperCase()) {
//                    "PWD" -> "PWD"
//                    "SC" -> "SC"
//                    "PERCENTAGE" -> "PERCENTAGE"
//                    "FIXED" -> "FIXED"
//                    "FIXED TOTAL", "FIXEDTOTAL" -> "FIXEDTOTAL"
//                    else -> "No Discount"
//                },
//                netAmountNotIncludingTax = netAmountNotIncludingTax,
//                storeTaxGroup = null,
//                currency = "PHP",
//                taxExempt = 0.0,
//                storeKey = storeKey,
//                storeSequence = storeSequence
//            )
//        }
//    }

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
//    private fun printReceiptWithItems(transaction: TransactionSummary) {
//        lifecycleScope.launch {
//            try {
//                val items = withContext(Dispatchers.IO) {
//                    transactionViewModel.getTransactionItems(transaction.transactionId)
//                }
//
//                if (items.isEmpty()) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@Window1,
//                            "No items found for this transaction",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    return@launch
//                }
//
//                val printerMacAddress =
//                    "DC:0D:30:70:09:19"  // Replace with your printer's MAC address
//
//                if (!bluetoothPrinterHelper.isConnected() && !bluetoothPrinterHelper.connect(
//                        printerMacAddress
//                    )
//                ) {
//                    withContext(Dispatchers.Main) {
//                        Toast.makeText(
//                            this@Window1,
//                            "Failed to connect to printer",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                    return@launch
//                }
//
//                // Generate the receipt content using the new format
//                val receiptContent = bluetoothPrinterHelper.generateReceiptContent(
//                    transaction,
//                    items,
//                    BluetoothPrinterHelper.ReceiptType.REPRINT // or REPRINT
//                )
//                // Write the content to the printer
//                bluetoothPrinterHelper.outputStream?.write(receiptContent.toByteArray())
//                bluetoothPrinterHelper.outputStream?.flush()
//
//                val transactionLogger = TransactionLogger(this@Window1)
//                transactionLogger.logReprint(
//                    transaction = transaction,
//                    items = items
//                )
//                // Cut the paper
////                bluetoothPrinterHelper.cutPaper()
//
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Receipt reprinted successfully",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error reprinting receipt", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@Window1,
//                        "Error reprinting receipt: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }