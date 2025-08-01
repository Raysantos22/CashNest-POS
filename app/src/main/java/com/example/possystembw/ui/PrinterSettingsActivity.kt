package com.example.possystembw.ui

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.DatePickerDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.R
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.DeviceUtils
import com.example.possystembw.MainActivity
import com.example.possystembw.adapter.StoreExpenseAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.StoreExpense
import com.example.possystembw.ui.ViewModel.StoreExpenseViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Calendar
import android.graphics.Rect
import android.widget.AdapterView
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.possystembw.RetrofitClient
import com.example.possystembw.adapter.TransactionResyncAdapter
import com.example.possystembw.adapter.toFormattedString
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.example.possystembw.ui.ViewModel.NumberSequenceAutoChecker
import com.example.possystembw.ui.ViewModel.setupNumberSequenceChecker
import java.time.LocalDate

class PrinterSettingsActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var printerListView: ListView
    private val savedPrinters = mutableListOf<PrinterInfo>()
    private var discoveryReceiver: BroadcastReceiver? = null
    private var scanningDialog: AlertDialog? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private var currentStatusTextView: TextView? = null
    private var connectionStatusTimer: Job? = null

    // Mobile layout support
    private var drawerLayout: DrawerLayout? = null
    private var navigationView: NavigationView? = null
    private var hamburgerButton: ImageButton? = null
    private var optionsButton: ImageButton? = null
    private var isMobileLayout = false
    // Tablet layout components
    private lateinit var sidebarLayout: ConstraintLayout
    private lateinit var toggleButton: ImageButton
    private lateinit var buttonContainer: LinearLayout
    private lateinit var ecposTitle: TextView
    private var isSidebarExpanded = true
    private var isAnimating = false

    private lateinit var btnStoreExpense: Button
    private lateinit var expenseViewModel: StoreExpenseViewModel
    private lateinit var expenseAdapter: StoreExpenseAdapter
    private lateinit var expenseRecyclerView: RecyclerView
    private lateinit var repository: TransactionRepository
    private lateinit var localDataManager: LocalDataManager
    private var serverUrl: String? = null
    private lateinit var sequenceChecker: NumberSequenceAutoChecker

    companion object {
        private const val REQUEST_ENABLE_BT = 100
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 101
        private const val TAG = "PrinterSettingsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_settings)

        // Set orientation based on device
        DeviceUtils.setOrientationBasedOnDevice(this)

        // Detect layout type first
        detectLayoutType()

        // Initialize layout-specific views IMMEDIATELY after detection
        initializeLayoutSpecificViews()


        // Initialize layout-specific views
        initializeLayoutSpecificViews()

        // Initialize Bluetooth
        BluetoothPrinterHelper.initialize(this)
        bluetoothPrinterHelper = BluetoothPrinterHelper(this)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device - continuing without Bluetooth features", Toast.LENGTH_LONG).show()
        } else {
            bluetoothAdapter = adapter
        }

        localDataManager = LocalDataManager(this)

        // Setup layout-specific features
        if (isMobileLayout) {
            setupMobileSpecificFeatures()
        } else {
            initializeSidebarComponents()
            setupSidebar()
        }

        setupViews()
        setupExpenseList()
        loadSavedPrinters()
        setupPrinterListView()
        updateConnectionStatus()
        startConnectionStatusCheck()
        sequenceChecker = setupNumberSequenceChecker(this)

        setupEmergencyResync()

        findViewById<Button>(R.id.btnViewLocalData).setOnClickListener {
            toggleLocalDataServer()
        }
        findViewById<Button>(R.id.btnEmergencyResync).setOnClickListener {
            showEmergencyResyncDialog()
        }
        findViewById<Button>(R.id.btnScanPrinters).setOnClickListener {
            startScanningProcess()
        }
        findViewById<Button>(R.id.btnCreateRequest).setOnClickListener {
            val intent = Intent(this, RequestActivity::class.java)
            startActivity(intent)
        }
    }

    private fun detectLayoutType() {
        val drawerLayoutView = findViewById<DrawerLayout>(R.id.drawer_layout)
        val sidebarLayoutView = findViewById<ConstraintLayout>(R.id.sidebarLayout)

        val isTabletDevice = DeviceUtils.isTablet(this)
        val hasDrawer = drawerLayoutView != null
        val hasSidebar = sidebarLayoutView != null

        Log.d(TAG, "=== LAYOUT DETECTION ===")
        Log.d(TAG, "Device type: ${if (isTabletDevice) "Tablet" else "Phone"}")
        Log.d(TAG, "Has DrawerLayout: $hasDrawer")
        Log.d(TAG, "Has SidebarLayout: $hasSidebar")

        isMobileLayout = hasDrawer && !hasSidebar

        Log.d(TAG, "Final decision: ${if (isMobileLayout) "Mobile" else "Tablet"} mode")
    }

    private fun initializeLayoutSpecificViews() {
        if (isMobileLayout) {
            drawerLayout = findViewById(R.id.drawer_layout)
            navigationView = findViewById(R.id.nav_view)
            hamburgerButton = findViewById(R.id.hamburgerButton)

            Log.d(TAG, "✅ Mobile views initialized")
            Log.d(TAG, "DrawerLayout: ${drawerLayout != null}")
            Log.d(TAG, "NavigationView: ${navigationView != null}")
            Log.d(TAG, "HamburgerButton: ${hamburgerButton != null}")
        } else {
            Log.d(TAG, "✅ Tablet layout detected")
        }
    }

    private fun setupMobileSpecificFeatures() {
        try {
            // Setup navigation drawer
            navigationView?.setNavigationItemSelectedListener(this)

            // Update store name in navigation header
            navigationView?.getHeaderView(0)?.let { headerView ->
                val navStoreName = headerView.findViewById<TextView>(R.id.nav_store_name)
                val currentStore = SessionManager.getCurrentUser()?.storeid ?: "Unknown Store"
                navStoreName?.text = "Store: $currentStore"
            }

            // Setup hamburger button - this is the key fix
            hamburgerButton?.setOnClickListener {
                Log.d(TAG, "Hamburger button clicked")
                drawerLayout?.let { drawer ->
                    if (drawer.isDrawerOpen(GravityCompat.START)) {
                        drawer.closeDrawer(GravityCompat.START)
                    } else {
                        drawer.openDrawer(GravityCompat.START)
                    }
                } ?: run {
                    Log.e(TAG, "DrawerLayout is null")
                }
            }

            // Setup options button
//            optionsButton?.setOnClickListener {
//                showMobileOptionsDialog()
//            }

            Log.d(TAG, "✅ Mobile features setup complete")
            Log.d(TAG, "DrawerLayout available: ${drawerLayout != null}")
            Log.d(TAG, "NavigationView available: ${navigationView != null}")
            Log.d(TAG, "HamburgerButton available: ${hamburgerButton != null}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Mobile features setup failed", e)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        Log.d(TAG, "Navigation item selected: ${item.itemId}")

        when (item.itemId) {
            R.id.nav_pos_system -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_reports -> {
                val intent = Intent(this, ReportsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_stock_counting -> {
                val intent = Intent(this, StockCountingActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_web_reports -> {
                navigateToMainWithUrl("https://eljin.org/reports", "REPORTS")
            }
            R.id.nav_customers -> {
                navigateToMainWithUrl("https://eljin.org/customers", "CUSTOMER")
            }
            R.id.nav_loyalty_card -> {
                navigateToMainWithUrl("https://eljin.org/loyalty-cards", "Loyalty Card")
            }
            R.id.nav_stock_transfer -> {
                navigateToMainWithUrl("https://eljin.org/StockTransfer", "Stock Transfer")
            }
            R.id.nav_attendance -> {
                val intent = Intent(this, AttendanceActivity::class.java)
                startActivity(intent)

            }
            R.id.nav_printer_settings -> {
                val intent = Intent(this, PrinterSettingsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_cash_drawer -> {
                val intent = Intent(this, ReportsActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }

        // Always close drawer after selection
        drawerLayout?.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        when {
            isMobileLayout && drawerLayout?.isDrawerOpen(GravityCompat.START) == true -> {
                Log.d(TAG, "Closing drawer via back button")
                drawerLayout?.closeDrawer(GravityCompat.START)
            }
            else -> {
                Log.d(TAG, "Calling super.onBackPressed()")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    onBackPressedDispatcher.onBackPressed()
                } else {
                    @Suppress("DEPRECATION")
                    super.onBackPressed()
                }
            }
        }
    }
    private fun debugDrawerState() {
        Log.d(TAG, "=== DRAWER DEBUG ===")
        Log.d(TAG, "isMobileLayout: $isMobileLayout")
        Log.d(TAG, "drawerLayout != null: ${drawerLayout != null}")
        Log.d(TAG, "navigationView != null: ${navigationView != null}")
        Log.d(TAG, "hamburgerButton != null: ${hamburgerButton != null}")

        drawerLayout?.let { drawer ->
            Log.d(TAG, "Drawer isDrawerOpen: ${drawer.isDrawerOpen(GravityCompat.START)}")
            Log.d(TAG, "Drawer visibility: ${drawer.visibility}")
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

    data class TransactionData(
        val summary: TransactionSummary,
        val records: List<TransactionRecord>,
        var isSelected: Boolean = false
    )

    private fun showEmergencyResyncDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_emergency_resync, null)
        var startDate: Calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var endDate: Calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        val startDateEdit = dialogView.findViewById<EditText>(R.id.startDateEdit)
        val endDateEdit = dialogView.findViewById<EditText>(R.id.endDateEdit)
        val summaryTextView = dialogView.findViewById<TextView>(R.id.summaryTextView)
        val transactionList = dialogView.findViewById<RecyclerView>(R.id.transactionList)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val selectAllCheckbox = dialogView.findViewById<CheckBox>(R.id.selectAllCheckbox)

        // Apply mobile-specific styling
        if (isMobileLayout) {
            startDateEdit.textSize = 12f
            endDateEdit.textSize = 12f
            summaryTextView.textSize = 11f
        }

        // Set initial dates
        startDateEdit.setText(startDate.toFormattedString())
        endDateEdit.setText(endDate.toFormattedString())

        // Setup RecyclerView
        val adapter = TransactionResyncAdapter()
        transactionList.adapter = adapter
        transactionList.layoutManager = LinearLayoutManager(this)

        // Setup select all checkbox
        adapter.setOnSelectAllChangeListener { isChecked ->
            selectAllCheckbox.isChecked = isChecked
        }

        selectAllCheckbox.setOnCheckedChangeListener { _, isChecked ->
            adapter.selectAll(isChecked)
        }

        // Setup date pickers
        startDateEdit.setOnClickListener {
            showDatePickerDialog(startDate) { date ->
                startDate = date.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                startDateEdit.setText(date.toFormattedString())
                lifecycleScope.launch {
                    updateTransactionData(startDate, endDate, summaryTextView, adapter, progressBar)
                }
            }
        }

        endDateEdit.setOnClickListener {
            showDatePickerDialog(endDate) { date ->
                endDate = date.apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                endDateEdit.setText(date.toFormattedString())
                lifecycleScope.launch {
                    updateTransactionData(startDate, endDate, summaryTextView, adapter, progressBar)
                }
            }
        }

        // Initial data load
        lifecycleScope.launch {
            updateTransactionData(startDate, endDate, summaryTextView, adapter, progressBar)
        }

        val titleView = TextView(this@PrinterSettingsActivity)
        titleView.text = "Emergency Resync"
        titleView.textSize = if (isMobileLayout) 16f else 18f
        titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
        titleView.setPadding(
            if (isMobileLayout) 50 else 24,
            if (isMobileLayout) 50 else 20,
            if (isMobileLayout) 16 else 24,
            if (isMobileLayout) 8 else 12
        )
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.setTypeface(null, Typeface.BOLD)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Resync Selected") { dialog, _ ->
                lifecycleScope.launch {
                    val selectedTransactions = adapter.currentList.filter { it.isSelected }
                    resyncTransactions(selectedTransactions)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        // Apply mobile styling after dialog is shown
        applyMobileDialogStyling(dialog)
    }

    private fun showStoreExpenseDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_store_expense, null)

        // Get references to views
        val nameEdit = dialogView.findViewById<EditText>(R.id.editName)
        val expenseTypeContainer = dialogView.findViewById<LinearLayout>(R.id.expenseTypeContainer)
        val btnAddExpenseType = dialogView.findViewById<Button>(R.id.btnAddExpenseType)
        val amountEdit = dialogView.findViewById<EditText>(R.id.editAmount)
        val receivedByEdit = dialogView.findViewById<EditText>(R.id.editReceivedBy)
        val approvedByEdit = dialogView.findViewById<EditText>(R.id.editApprovedBy)
        val effectDateEdit = dialogView.findViewById<EditText>(R.id.editEffectDate)

        // Apply mobile-specific styling to EditTexts
        if (isMobileLayout) {
            nameEdit.textSize = 12f
            amountEdit.textSize = 12f
            receivedByEdit.textSize = 12f
            approvedByEdit.textSize = 12f
            effectDateEdit.textSize = 12f
            btnAddExpenseType.textSize = 11f
        }

        // Predefined expense types
        val expenseTypes = arrayOf(
            "Taxes & Licenses", "Print & Office Supplies", "Product Supplies", "Maintenance Expense",
            "Transpo & Travel", "Other"
        )

        // Function to create a new expense type spinner with dynamic "Other" handling
        fun createExpenseTypeLayout(): LinearLayout {
            val containerLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            val cardView = CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8, 0, 8)
                }
                radius = resources.getDimension(R.dimen.card_corner_radius)
                cardElevation = resources.getDimension(R.dimen.card_elevation)
                setCardBackgroundColor(ContextCompat.getColor(context, R.color.white))
            }

            val spinner = Spinner(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }
            }

            val adapter = ArrayAdapter(this, R.layout.spinner_item_black, expenseTypes)
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item_black)
            spinner.adapter = adapter

            val otherEditText = EditText(this).apply {
                hint = "Specify Other Expense Type"
                visibility = View.GONE
                setTextColor(ContextCompat.getColor(context, R.color.black))
                setHintTextColor(ContextCompat.getColor(context, R.color.hint_gray))
                backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.black))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 16)
                }
                if (isMobileLayout) textSize = 11f
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(ContextCompat.getColor(spinner.context, R.color.black))
                    otherEditText.visibility = if (expenseTypes[position] == "Other") View.VISIBLE else View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            cardView.addView(spinner)
            containerLayout.addView(cardView)
            containerLayout.addView(otherEditText)
            return containerLayout
        }

        // Initial expense type spinner
        expenseTypeContainer.addView(createExpenseTypeLayout())

        // Add more expense type spinners
        btnAddExpenseType.setOnClickListener {
            expenseTypeContainer.addView(createExpenseTypeLayout())
        }

        // Setup date picker
        effectDateEdit.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                effectDateEdit.setText(selectedDate)
            }, year, month, day).show()
        }

        val titleView = TextView(this@PrinterSettingsActivity)
        titleView.text = "Store Expense"
        titleView.textSize = if (isMobileLayout) 16f else 18f
        titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
        titleView.setPadding(
            if (isMobileLayout) 50 else 24,
            if (isMobileLayout) 50 else 20,
            if (isMobileLayout) 16 else 24,
            if (isMobileLayout) 8 else 12
        )
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.setTypeface(null, Typeface.BOLD)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Send") { dialog, _ ->
                val name = nameEdit.text.toString()
                val expenseTypes = mutableListOf<String>()

                // Collect expense types from spinners
                for (i in 0 until expenseTypeContainer.childCount) {
                    val spinnerLayout = expenseTypeContainer.getChildAt(i) as LinearLayout
                    val cardView = spinnerLayout.getChildAt(0) as CardView
                    val spinner = cardView.getChildAt(0) as Spinner
                    val otherEditText = spinnerLayout.getChildAt(1) as EditText

                    val selectedType = spinner.selectedItem.toString()
                    if (selectedType == "Other" && otherEditText.text.isNotBlank()) {
                        expenseTypes.add("Other:${otherEditText.text}")
                    } else if (selectedType != "Other") {
                        expenseTypes.add(selectedType)
                    }
                }

                val expenseType = expenseTypes.joinToString(", ")
                val amount = amountEdit.text.toString()
                val receivedBy = receivedByEdit.text.toString()
                val approvedBy = approvedByEdit.text.toString()
                val effectDate = effectDateEdit.text.toString()

                if (validateExpenseForm(name, expenseType, amount, receivedBy, approvedBy, effectDate)) {
                    sendExpense(name, expenseType, amount, receivedBy, approvedBy, effectDate)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        // Apply mobile styling after dialog is shown
        applyMobileDialogStyling(dialog)
    }

    private fun showManualAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_printer, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextPrinterName)
        val addressInput = dialogView.findViewById<EditText>(R.id.editTextMacAddress)
        val defaultCheck = dialogView.findViewById<CheckBox>(R.id.checkBoxDefault)

        // Apply mobile-specific styling
        if (isMobileLayout) {
            nameInput.textSize = 12f
            addressInput.textSize = 12f
        }

        val titleView = TextView(this@PrinterSettingsActivity)
        titleView.text = "Add Printer"
        titleView.textSize = if (isMobileLayout) 16f else 18f
        titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
        titleView.setPadding(
            if (isMobileLayout) 50 else 24,
            if (isMobileLayout) 50 else 20,
            if (isMobileLayout) 16 else 24,
            if (isMobileLayout) 8 else 12
        )
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.setTypeface(null, Typeface.BOLD)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val name = nameInput.text.toString().trim()
                val address = addressInput.text.toString().trim()

                if (name.isNotEmpty() && address.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))) {
                    addPrinter(name, address, defaultCheck.isChecked)
                } else {
                    Toast.makeText(this, "Please enter valid printer details", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        // Apply mobile styling after dialog is shown
        applyMobileDialogStyling(dialog)
    }

    private fun applyMobileDialogStyling(dialog: AlertDialog) {
        if (!isMobileLayout) return

        try {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.let { button ->
                button.textSize = 12f
                button.setPadding(12, 8, 12, 8)
            }

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { button ->
                button.textSize = 12f
                button.setPadding(12, 8, 12, 8)
            }

            val titleView = dialog.findViewById<TextView>(android.R.id.title)
            titleView?.let { title ->
                title.textSize = 14f
                title.setPadding(16, 12, 16, 8)
            }

            dialog.window?.let { window ->
                val layoutParams = window.attributes
                val displayMetrics = resources.displayMetrics
                layoutParams.width = (displayMetrics.widthPixels * 0.85).toInt()
                window.attributes = layoutParams
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error applying mobile dialog styling", e)
        }
    }

    // Rest of your existing methods remain the same, just add mobile styling where needed...

    private fun toggleLocalDataServer() {
        if (localDataManager.isServerRunning()) {
            localDataManager.stopServer()
            showServerStatus("Server stopped")
        } else {
            serverUrl = localDataManager.startServer()
            if (serverUrl != null) {
                showServerStatus("Server running at: $serverUrl")
                copyToClipboard(serverUrl!!)
            } else {
                showServerStatus("Failed to start server")
            }
        }
    }

    private fun showServerStatus(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d("MainActivity", message)

        if (serverUrl != null && localDataManager.isServerRunning()) {
            val titleView = TextView(this@PrinterSettingsActivity)
            titleView.text = "Local Data Server"
            titleView.textSize = if (isMobileLayout) 16f else 18f
            titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
            titleView.setPadding(
                if (isMobileLayout) 50 else 24,
                if (isMobileLayout) 50 else 20,
                if (isMobileLayout) 16 else 24,
                if (isMobileLayout) 8 else 12
            )
            titleView.gravity = Gravity.CENTER_VERTICAL
            titleView.setTypeface(null, Typeface.BOLD)

            val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
                .setCustomTitle(titleView)
                .setMessage("Server is running at:\n$serverUrl\n\nYou can access this URL from any device on the same network to view your tablet data.\n\nEndpoints:\n• / - Web interface\n• /transactions - All transaction records\n• /transaction-summaries - Transaction summaries\n• /stats - Database statistics")
                .setPositiveButton("Copy URL") { _, _ ->
                    copyToClipboard(serverUrl!!)
                }
                .setNegativeButton("Stop Server") { _, _ ->
                    localDataManager.stopServer()
                }
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
            dialog.show()

            // Apply mobile styling after dialog is shown
            applyMobileDialogStyling(dialog)
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Server URL", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun setupEmergencyResync() {
        val database = AppDatabase.getDatabase(application)
        val transactionDao = database.transactionDao()

        val numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
            RetrofitClient.numberSequenceApi,
            database.numberSequenceRemoteDao(),
            transactionDao
        )

        repository = TransactionRepository(
            AppDatabase.getDatabase(application).transactionDao(),
            numberSequenceRemoteRepository
        )
    }

    private fun showDatePickerDialog(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun formatDateToString(date: Date): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.format(date)
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    private suspend fun updateTransactionData(
        startDate: Calendar,
        endDate: Calendar,
        summaryTextView: TextView,
        adapter: TransactionResyncAdapter,
        progressBar: ProgressBar
    ) {
        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
        }

        try {
            val transactions = withContext(Dispatchers.IO) {
                val summaries = repository.transactionDao.getTransactionsByDateRange(
                    formatDateToString(startDate.time),
                    formatDateToString(endDate.time)
                )

                summaries.map { summary ->
                    val records = repository.getTransactionRecords(summary.transactionId)
                    TransactionData(summary, records)
                }
            }

            var totalQty = 0
            var totalGrossSales = 0.0
            var totalTransactions = 0
            var totalItems = 0

            transactions.forEach { transactionData ->
                totalGrossSales += transactionData.summary.grossAmount
                totalTransactions++
                totalItems += transactionData.records.size
                totalQty += transactionData.records.sumOf { it.quantity }
            }

            withContext(Dispatchers.Main) {
                summaryTextView.text = """
                    Total Transactions: $totalTransactions
                    Total Line Items: $totalItems
                    Total Quantity: $totalQty
                    Total Gross Sales: ₱${String.format("%.2f", totalGrossSales)}
                """.trimIndent()

                adapter.submitList(transactions)
                progressBar.visibility = View.GONE
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrinterSettingsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                progressBar.visibility = View.GONE
            }
        }
    }

    private suspend fun resyncTransactions(transactions: List<TransactionData>) {
        try {
            withContext(Dispatchers.IO) {
                transactions.forEach { transactionData ->
                    try {
                        Log.d("EmergencyResync", "Starting resync for transaction ${transactionData.summary.transactionId}")

                        if (!transactionData.summary.syncStatus) {
                            Log.d("EmergencyResync", "Transaction ${transactionData.summary.transactionId} is already in resync process, skipping")
                            return@forEach
                        }

                        repository.transactionDao.updateSyncStatus(transactionData.summary.transactionId, false)
                        repository.transactionDao.updateTransactionRecordsSync(transactionData.summary.transactionId, false)

                        Log.d("EmergencyResync", "Marked transaction ${transactionData.summary.transactionId} for resync")

                    } catch (e: Exception) {
                        Log.e("EmergencyResync", "Error resyncing transaction ${transactionData.summary.transactionId}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EmergencyResync", "Error in resync process", e)
        } finally {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@PrinterSettingsActivity,
                    "Transactions marked for resync. They will be processed by the sync service.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setupExpenseList() {
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)
        expenseAdapter = StoreExpenseAdapter(
            onDeleteClick = { /* Do nothing */ },
            onEditClick = { /* Do nothing */ }
        )

        expenseRecyclerView.apply {
            layoutManager = GridLayoutManager(this@PrinterSettingsActivity, 2)
            adapter = expenseAdapter
        }

        val factory = StoreExpenseViewModel.Factory(
            AppDatabase.getDatabase(application).storeExpenseDao(),
            RetrofitClient.storeExpenseApi
        )
        expenseViewModel = ViewModelProvider(this, factory)[StoreExpenseViewModel::class.java]
        expenseViewModel.allExpenses.observe(this) { expenses ->
            expenseAdapter.submitList(expenses)
        }
    }

    private fun showDeleteConfirmation(expense: StoreExpense) {
        val titleView = TextView(this@PrinterSettingsActivity)
        titleView.text = "Delete Expense"
        titleView.textSize = if (isMobileLayout) 16f else 18f
        titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
        titleView.setPadding(
            if (isMobileLayout) 50 else 24,
            if (isMobileLayout) 50 else 20,
            if (isMobileLayout) 16 else 24,
            if (isMobileLayout) 8 else 12
        )
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.setTypeface(null, Typeface.BOLD)

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { dialog, _ ->
                expenseViewModel.deleteExpense(expense)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()

        // Apply mobile styling after dialog is shown
        applyMobileDialogStyling(dialog)
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()

        if (editText.text.isNotEmpty()) {
            try {
                val parts = editText.text.toString().split("-")
                if (parts.size == 3) {
                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            } catch (e: Exception) {
                Log.e("DatePicker", "Error parsing date", e)
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                editText.setText(selectedDate)
            },
            year,
            month,
            day
        ).show()
    }

    private fun validateExpenseForm(
        name: String,
        expenseType: String,
        amount: String,
        receivedBy: String,
        approvedBy: String,
        effectDate: String
    ): Boolean {
        when {
            name.isBlank() -> {
                showToast("Please enter name")
                return false
            }
            expenseType.isBlank() -> {
                showToast("Please select at least one expense type")
                return false
            }
            amount.isBlank() -> {
                showToast("Please enter amount")
                return false
            }
        }
        return true
    }

    private fun sendExpense(
        name: String,
        expenseType: String,
        amount: String,
        receivedBy: String,
        approvedBy: String,
        effectDate: String
    ) {
        try {
            val expenseAmount = amount.toDoubleOrNull()
            if (expenseAmount == null) {
                showToast("Invalid amount format")
                return
            }

            expenseViewModel.addExpense(
                name = name,
                expenseType = expenseType,
                amount = expenseAmount,
                receivedBy = receivedBy.ifEmpty { "N/A" },
                approvedBy = approvedBy.ifEmpty { "N/A" },
                effectDate = effectDate.ifEmpty { "N/A" }
            )
            showToast("Expense submitted successfully")
        } catch (e: Exception) {
            showToast("Error saving expense: ${e.message}")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun initializeSidebarComponents() {
        try {
            sidebarLayout = findViewById(R.id.sidebarLayout)
            toggleButton = findViewById(R.id.toggleButton)
            buttonContainer = findViewById(R.id.buttonContainer)
            ecposTitle = findViewById(R.id.ecposTitle)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing sidebar components", e)
        }
    }

    private fun setupSidebar() {
        try {
            toggleButton.setOnClickListener {
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

    private fun setupSidebarButtons() {
        findViewById<ImageButton>(R.id.button2).setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.button3).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/order")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.stockcounting).setOnClickListener {
            val intent = Intent(this, StockCountingActivity::class.java)
            startActivity(intent)
            showToast("Stock Counting")
        }

        findViewById<ImageButton>(R.id.button5).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/StockTransfer")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.button6).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/reports")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.waste).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/waste")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.partycakes).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/loyalty-cards")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.customer).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/customers")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.button7).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.attendanceButton).setOnClickListener {
            val intent = Intent(this, AttendanceActivity::class.java)
            startActivity(intent)
            showToast("ATTENDANCE")
        }

        findViewById<ImageButton>(R.id.printerSettingsButton).setOnClickListener {
            // Already in PrinterSettings, no need to navigate
        }

        findViewById<ImageButton>(R.id.button8).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("logout", true)
            startActivity(intent)
        }
    }

    private fun collapseSidebar() {
        if (!isSidebarExpanded || isAnimating) return
        isAnimating = true

        val animatorSet = AnimatorSet()

        val collapseWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(24)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                    width = value
                }
            }
        }

        val toggleButtonMargin = ValueAnimator.ofInt(dpToPx(90), dpToPx(8)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                toggleButton.layoutParams = (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                    marginStart = value
                }
            }
        }

        animatorSet.playTogether(
            collapseWidth,
            toggleButtonMargin,
            ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 0f, 180f).apply {
                duration = 300
            },
            ObjectAnimator.ofFloat(buttonContainer, View.ALPHA, 1f, 0f).apply {
                duration = 150
            }
        )

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                buttonContainer.visibility = View.GONE
                ecposTitle.visibility = View.GONE
                isSidebarExpanded = false
                isAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun expandSidebar() {
        if (isSidebarExpanded || isAnimating) return
        isAnimating = true

        val animatorSet = AnimatorSet()

        val expandWidth = ValueAnimator.ofInt(sidebarLayout.width, dpToPx(100)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                sidebarLayout.layoutParams = sidebarLayout.layoutParams.apply {
                    width = value
                }
            }
        }

        val toggleButtonMargin = ValueAnimator.ofInt(dpToPx(8), dpToPx(90)).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                toggleButton.layoutParams = (toggleButton.layoutParams as ConstraintLayout.LayoutParams).apply {
                    marginStart = value
                }
            }
        }

        animatorSet.playTogether(
            expandWidth,
            toggleButtonMargin,
            ObjectAnimator.ofFloat(toggleButton, View.ROTATION, 180f, 0f).apply {
                duration = 300
            }
        )

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                buttonContainer.visibility = View.VISIBLE
                ecposTitle.visibility = View.VISIBLE
                buttonContainer.alpha = 0f
            }

            override fun onAnimationEnd(animation: Animator) {
                buttonContainer.animate()
                    .alpha(1f)
                    .setDuration(150)
                    .start()
                isSidebarExpanded = true
                isAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun setupPrinterListView() {
        printerListView.setOnItemClickListener { _, _, position, _ ->
            val printer = savedPrinters[position]
            showPrinterOptionsDialog(printer)
        }
    }

    private fun startConnectionStatusCheck() {
        connectionStatusTimer = lifecycleScope.launch {
            while (isActive) {
                updateConnectionStatus()
                updatePrinterList()
                delay(2000)
            }
        }
    }

    private fun startScanningProcess() {
        try {
            if (!checkBluetoothPermissions()) {
                requestBluetoothPermissions()
                return
            }

            if (bluetoothAdapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                try {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Bluetooth permission denied", e)
                    Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show()
                }
                return
            }

            showScanningDialog()
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied in startScanningProcess", e)
            Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getBondedDevices(): List<BluetoothDevice> {
        return try {
            if (checkBluetoothPermissions()) {
                bluetoothAdapter.bondedDevices?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while getting bonded devices", e)
            emptyList()
        }
    }

    private fun showScanningDialog() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_scanning_devices, null)
        val deviceList = dialogView.findViewById<ListView>(R.id.deviceList)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.scanningProgress)
        val statusText = dialogView.findViewById<TextView>(R.id.statusText)
        val rescanButton = dialogView.findViewById<Button>(R.id.rescanButton)
        val emptyText = dialogView.findViewById<TextView>(R.id.emptyText)

        // Apply mobile-specific styling
        if (isMobileLayout) {
            statusText.textSize = 12f
            rescanButton.textSize = 11f
            emptyText.textSize = 11f
        }

        // Create adapter with black text
        val devicesAdapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                textView.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                if (isMobileLayout) {
                    textView.textSize = 12f
                }
                return view
            }
        }

        deviceList.adapter = devicesAdapter

        try {
            if (checkBluetoothPermissions()) {
                bluetoothAdapter.bondedDevices?.forEach { device ->
                    val deviceName = device.name ?: "Unknown Device"
                    val deviceInfo = "$deviceName (${device.address})"
                    if (!discoveredDevices.contains(device)) {
                        discoveredDevices.add(device)
                        devicesAdapter.add(deviceInfo)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error accessing bonded devices", e)
        }

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        try {
                            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                            device?.let {
                                if (checkBluetoothPermissions()) {
                                    val deviceName = it.name ?: "Unknown Device"
                                    val deviceInfo = "$deviceName (${it.address})"
                                    if (!discoveredDevices.contains(it)) {
                                        discoveredDevices.add(it)
                                        runOnUiThread {
                                            devicesAdapter.add(deviceInfo)
                                            deviceList.visibility = View.VISIBLE
                                            emptyText.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Permission denied while handling discovery", e)
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            if (devicesAdapter.count == 0) {
                                statusText.text = "No devices found"
                                emptyText.visibility = View.VISIBLE
                                deviceList.visibility = View.GONE
                            } else {
                                statusText.text = "Select a device to connect"
                                emptyText.visibility = View.GONE
                                deviceList.visibility = View.VISIBLE
                            }
                            rescanButton.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }

        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, intentFilter)

        deviceList.setOnItemClickListener { _, _, position, _ ->
            val deviceInfo = devicesAdapter.getItem(position) ?: return@setOnItemClickListener
            val address = deviceInfo.substringAfterLast("(").removeSuffix(")")
            val name = deviceInfo.substringBefore(" (")

            lifecycleScope.launch {
                try {
                    progressBar.visibility = View.VISIBLE
                    statusText.text = "Connecting to $name..."
                    deviceList.isEnabled = false
                    rescanButton.isEnabled = false

                    val connected = withContext(Dispatchers.IO) {
                        bluetoothPrinterHelper.connect(address)
                    }

                    if (connected) {
                        addPrinter(name, address)

                        val prefs = getSharedPreferences("BluetoothPrinter", Context.MODE_PRIVATE)
                        prefs.edit()
                            .putString("last_printer_address", address)
                            .putString("last_printer_name", name)
                            .apply()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PrinterSettingsActivity,
                                "Successfully connected to $name",
                                Toast.LENGTH_SHORT
                            ).show()
                            updateConnectionStatus()
                            scanningDialog?.dismiss()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@PrinterSettingsActivity,
                                "Failed to connect to $name",
                                Toast.LENGTH_SHORT
                            ).show()
                            progressBar.visibility = View.GONE
                            statusText.text = "Select a device to connect"
                            deviceList.isEnabled = true
                            rescanButton.isEnabled = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@PrinterSettingsActivity,
                            "Error connecting to printer: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        progressBar.visibility = View.GONE
                        statusText.text = "Select a device to connect"
                        deviceList.isEnabled = true
                        rescanButton.isEnabled = true
                    }
                }
            }
        }

        rescanButton.setOnClickListener {
            startScanning(
                statusText,
                progressBar,
                deviceList,
                emptyText,
                rescanButton,
                devicesAdapter
            )
        }

        val titleView = TextView(this@PrinterSettingsActivity)
        titleView.text = "Available Devices"
        titleView.textSize = if (isMobileLayout) 16f else 18f
        titleView.setTextColor(ContextCompat.getColor(this@PrinterSettingsActivity, android.R.color.black))
        titleView.setPadding(
            if (isMobileLayout) 50 else 24,
            if (isMobileLayout) 50 else 20,
            if (isMobileLayout) 16 else 24,
            if (isMobileLayout) 8 else 12
        )
        titleView.gravity = Gravity.CENTER_VERTICAL
        titleView.setTypeface(null, Typeface.BOLD)

        scanningDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setCustomTitle(titleView)
            .setView(dialogView)
            .setOnCancelListener {
                stopScanning()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                stopScanning()
                dialog.dismiss()
            }
            .create()

        scanningDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        scanningDialog?.show()

        // Apply mobile styling after dialog is shown
        scanningDialog?.let { applyMobileDialogStyling(it) }

        startScanning(statusText, progressBar, deviceList, emptyText, rescanButton, devicesAdapter)
    }

    private fun stopScanning() {
        try {
            if (checkBluetoothPermissions() && bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied while stopping discovery", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }

        discoveryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering receiver", e)
            }
        }
        discoveryReceiver = null
    }

    private fun startScanning(
        statusText: TextView,
        progressBar: ProgressBar,
        deviceList: ListView,
        emptyText: TextView,
        rescanButton: Button,
        devicesAdapter: ArrayAdapter<String>
    ) {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {
            statusText.text = "Scanning for devices..."
            statusText.visibility = View.VISIBLE
            progressBar.visibility = View.VISIBLE
            rescanButton.visibility = View.GONE

            discoveredDevices.clear()
            devicesAdapter.clear()

            bluetoothAdapter.bondedDevices?.forEach { device ->
                val deviceName = device.name ?: "Unknown Device"
                val deviceInfo = "$deviceName (${device.address})"
                discoveredDevices.add(device)
                devicesAdapter.add(deviceInfo)
            }

            if (discoveredDevices.isEmpty()) {
                deviceList.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = "Searching for devices..."
            } else {
                deviceList.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
            }

            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            bluetoothAdapter.startDiscovery()

        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission denied", e)
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error during scanning", e)
            Toast.makeText(this, "Error starting device scan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupViews() {
        currentStatusTextView = findViewById<TextView>(R.id.currentPrinterStatus).apply {
            setTextColor(Color.BLACK)
            textSize = if (isMobileLayout) 14f else 16f
            setPadding(16, 16, 16, 16)
        }

        findViewById<Button>(R.id.btnAddManual).setOnClickListener {
            showManualAddDialog()
        }

        findViewById<Button>(R.id.btnScanPrinters).setOnClickListener {
            openBluetoothSettings()
        }

        printerListView = findViewById(R.id.listViewPrinters)
        btnStoreExpense = findViewById(R.id.btnStoreExpense)
        btnStoreExpense.setOnClickListener {
            showStoreExpenseDialog()
        }
    }

    private fun updateConnectionStatus() {
        try {
            val bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()
            val isConnected = bluetoothPrinterHelper.isConnected()
            val connectedAddress = bluetoothPrinterHelper.getCurrentPrinterAddress()

            Log.d(TAG, "Checking printer connection - Connected: $isConnected, Address: $connectedAddress")

            runOnUiThread {
                if (isConnected && connectedAddress != null) {
                    val connectedPrinter = savedPrinters.find { it.address == connectedAddress }

                    if (connectedPrinter != null) {
                        currentStatusTextView?.text = "Connected to: ${connectedPrinter.name} (${connectedPrinter.address})"
                        Log.d(TAG, "Found saved printer: ${connectedPrinter.name}")
                    } else {
                        try {
                            if (checkBluetoothPermissions()) {
                                val device = bluetoothAdapter.getRemoteDevice(connectedAddress)
                                val printerName = try {
                                    device.name ?: "Unknown Printer"
                                } catch (e: SecurityException) {
                                    "Unknown Printer"
                                }
                                currentStatusTextView?.text = "Connected to: $printerName ($connectedAddress)"
                                Log.d(TAG, "Adding new printer: $printerName")
                                addPrinter(printerName, connectedAddress, false)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting device info", e)
                            currentStatusTextView?.text = "Connected to: Unknown Printer ($connectedAddress)"
                        }
                    }
                } else {
                    currentStatusTextView?.text = bluetoothPrinterHelper.getLastError()?.let {
                        "Connection error: $it"
                    } ?: "No printer connected"
                }
                updatePrinterList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating connection status", e)
            runOnUiThread {
                currentStatusTextView?.text = "Error checking printer status: ${e.message}"
            }
        }
    }

    private fun openBluetoothSettings() {
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open Bluetooth settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPrinterOptionsDialog(printer: PrinterInfo) {
        val isConnected = bluetoothPrinterHelper.isConnected() &&
                bluetoothPrinterHelper.getCurrentPrinterAddress() == printer.address

        val options = if (!isConnected) {
            arrayOf("Connect", "Test Connection", "Set as Default", "Remove", "Cancel")
        } else {
            arrayOf("Disconnect", "Test Connection", "Set as Default", "Remove", "Cancel")
        }

        AlertDialog.Builder(this, R.style.CustomDialogStyle3)
            .setTitle(printer.name)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "Connect" -> connectToPrinter(printer)
                    "Disconnect" -> disconnectPrinter()
                    "Test Connection" -> testPrinterConnection(printer)
                    "Set as Default" -> setDefaultPrinter(printer)
                    "Remove" -> removePrinter(printer)
                }
            }
            .create()
            .apply {
                window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                show()
                applyMobileDialogStyling(this)
            }
    }

    private fun connectToPrinter(printer: PrinterInfo) {
        lifecycleScope.launch {
            try {
                val connected = withContext(Dispatchers.IO) {
                    bluetoothPrinterHelper.connect(printer.address)
                }

                if (connected) {
                    val prefs = getSharedPreferences("BluetoothPrinter", Context.MODE_PRIVATE)
                    prefs.edit().putString("last_printer_address", printer.address).apply()
                }
                updateConnectionStatus()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PrinterSettingsActivity,
                    "Error connecting to printer: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun disconnectPrinter() {
        lifecycleScope.launch {
            try {
                BluetoothPrinterHelper.getInstance().disconnect()
                Toast.makeText(
                    this@PrinterSettingsActivity,
                    "Printer disconnected",
                    Toast.LENGTH_SHORT
                ).show()

                val prefs = getSharedPreferences("BluetoothPrinter", Context.MODE_PRIVATE)
                prefs.edit().remove("last_printer_address").apply()

                updateConnectionStatus()
            } catch (e: Exception) {
                Toast.makeText(
                    this@PrinterSettingsActivity,
                    "Error disconnecting printer: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun testPrinterConnection(printer: PrinterInfo) {
        lifecycleScope.launch {
            try {
                if (!bluetoothPrinterHelper.isConnected()) {
                    val connected = bluetoothPrinterHelper.connect(printer.address)
                    if (!connected) {
                        Toast.makeText(
                            this@PrinterSettingsActivity,
                            "Failed to connect to printer",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                }

                val testContent = """
                    ===========================
                          TEST PRINT
                    ===========================
                    Printer: ${printer.name}
                    Address: ${printer.address}
                    Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                    ===========================
                    If you can read this,
                    printer is working!
                    ===========================
                    
                    
                    
                """.trimIndent()

                val success = bluetoothPrinterHelper.printGenericReceipt(testContent)

                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(
                            this@PrinterSettingsActivity,
                            "Test print sent successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this@PrinterSettingsActivity,
                            "Failed to send test print",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@PrinterSettingsActivity,
                        "Error during test print: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun setDefaultPrinter(printer: PrinterInfo) {
        savedPrinters.forEach { it.isDefault = false }
        savedPrinters.find { it.address == printer.address }?.isDefault = true
        savePrinters()
        updatePrinterList()
    }

    private fun removePrinter(printer: PrinterInfo) {
        savedPrinters.removeAll { it.address == printer.address }
        savePrinters()
        updatePrinterList()
    }

    private fun savePrinters() {
        val prefs = getSharedPreferences("PrinterSettings", Context.MODE_PRIVATE)
        val printersJson = Gson().toJson(savedPrinters)
        prefs.edit().putString("saved_printers", printersJson).apply()
    }

    private fun addPrinter(name: String, address: String, isDefault: Boolean = false) {
        if (name.isNotEmpty() && address.matches(Regex("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))) {
            if (isDefault) {
                savedPrinters.forEach { it.isDefault = false }
            }

            val printer = PrinterInfo(name, address, isDefault)
            savedPrinters.add(printer)
            savePrinters()
            updatePrinterList()

            Toast.makeText(this, "Printer added successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Invalid printer details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePrinterList() {
        try {
            Log.d(TAG, "Updating printer list. Saved printers: ${savedPrinters.size}")

            val isAnyConnected = bluetoothPrinterHelper.isConnected()
            val connectedAddress = bluetoothPrinterHelper.getCurrentPrinterAddress()

            val adapter = object : ArrayAdapter<PrinterInfo>(
                this,
                android.R.layout.simple_list_item_2,
                android.R.id.text1,
                savedPrinters
            ) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text1 = view.findViewById<TextView>(android.R.id.text1)
                    val text2 = view.findViewById<TextView>(android.R.id.text2)

                    val printer = getItem(position)!!
                    val isPrinterConnected = isAnyConnected && printer.address == connectedAddress

                    text1.text = buildString {
                        append(printer.name)
                        if (isPrinterConnected) append(" (Connected)")
                        else if (printer.isDefault) append(" (Default)")
                    }

                    // Set text colors - ensure they're always black
                    text1.setTextColor(
                        if (isPrinterConnected) ContextCompat.getColor(context, android.R.color.holo_green_dark)
                        else ContextCompat.getColor(context, android.R.color.black)
                    )
                    text2.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                    text2.text = printer.address

                    // Apply mobile styling
                    if (isMobileLayout) {
                        text1.textSize = 12f
                        text2.textSize = 10f
                    }

                    return view
                }
            }

            printerListView.adapter = adapter
            printerListView.setOnItemClickListener { _, _, position, _ ->
                showPrinterOptionsDialog(savedPrinters[position])
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating printer list", e)
        }
    }


    private fun loadSavedPrinters() {
        val prefs = getSharedPreferences("PrinterSettings", Context.MODE_PRIVATE)
        val printersJson = prefs.getString("saved_printers", "[]")
        val type = object : TypeToken<List<PrinterInfo>>() {}.type

        try {
            val loadedPrinters = Gson().fromJson<List<PrinterInfo>>(printersJson, type)
            savedPrinters.clear()
            savedPrinters.addAll(loadedPrinters)

            try {
                if (checkBluetoothPermissions()) {
                    getBondedDevices().forEach { device ->
                        try {
                            val address = device.address
                            if (!savedPrinters.any { it.address == address }) {
                                val name = try {
                                    device.name ?: "Unknown Printer"
                                } catch (e: SecurityException) {
                                    "Unknown Printer"
                                }
                                addPrinter(name, address, false)
                            }
                        } catch (e: SecurityException) {
                            Log.e(TAG, "Permission denied while accessing device info", e)
                        }
                    }
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "Permission denied while loading bonded devices", e)
            }

            updatePrinterList()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading saved printers", e)
            Toast.makeText(this, "Error loading saved printers", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                    hasPermission(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            hasPermission(Manifest.permission.BLUETOOTH) &&
                    hasPermission(Manifest.permission.BLUETOOTH_ADMIN)
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                ),
                BLUETOOTH_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
        updatePrinterList()
    }

    override fun onDestroy() {
        super.onDestroy()
        connectionStatusTimer?.cancel()
        localDataManager.stopServer()
    }

    override fun onPause() {
        super.onPause()
        stopScanning()
        scanningDialog?.dismiss()
        scanningDialog = null
    }
}

data class PrinterInfo(
    val name: String,
    val address: String,
    var isDefault: Boolean = false
)