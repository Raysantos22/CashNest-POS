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
import android.content.pm.PackageManager import android.os.Build
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
import android.provider.Settings
import android.util.Log
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.MainActivity
import com.example.possystembw.adapter.StoreExpenseAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.StoreExpense
import com.example.possystembw.ui.ViewModel.StoreExpenseViewModel
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
import java.time.LocalDate


class PrinterSettingsActivity : AppCompatActivity() {
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var printerListView: ListView
    private val savedPrinters = mutableListOf<PrinterInfo>()
    private var discoveryReceiver: BroadcastReceiver? = null
    private var scanningDialog: AlertDialog? = null
    private val discoveredDevices = mutableSetOf<BluetoothDevice>()
    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private var currentStatusTextView: TextView? = null
    private var connectionStatusTimer: Job? = null

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


    companion object {
        private const val REQUEST_ENABLE_BT = 100
        private const val BLUETOOTH_PERMISSION_REQUEST_CODE = 101
        private const val TAG = "PrinterSettingsActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_printer_settings)

        // Initialize Bluetooth
        BluetoothPrinterHelper.initialize(this)
        bluetoothPrinterHelper = BluetoothPrinterHelper(this)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

        // Fix: Check if adapter is null before assignment
        val adapter = bluetoothManager.adapter
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth is not available on this device - continuing without Bluetooth features", Toast.LENGTH_LONG).show()
            // Create a dummy adapter or handle null case
            // bluetoothAdapter will remain uninitialized, so we need to handle this in other methods
        } else {
            bluetoothAdapter = adapter
        }

        localDataManager = LocalDataManager(this)

        initializeSidebarComponents()
        setupSidebar()
        setupViews()
        setupExpenseList() // Add this line
        loadSavedPrinters()
        setupPrinterListView()
        updateConnectionStatus()
        startConnectionStatusCheck()
        // Remove duplicate setupExpenseList() call

        setupEmergencyResync()

        findViewById<Button>(R.id.btnViewLocalData).setOnClickListener {
            toggleLocalDataServer()
        }
        findViewById<Button>(R.id.btnEmergencyResync).setOnClickListener {
            showEmergencyResyncDialog()
        }
        // Set up scan button to show scanning dialog
        findViewById<Button>(R.id.btnScanPrinters).setOnClickListener {
            startScanningProcess()
        }
        findViewById<Button>(R.id.btnCreateRequest).setOnClickListener {
            val intent = Intent(this, RequestActivity::class.java)
            startActivity(intent)
        }
    }
//    private fun setupExpenseList() {
//        expenseRecyclerView = findViewById(R.id.expenseRecyclerView)
//        expenseAdapter = StoreExpenseAdapter(
//            onDeleteClick = { expense -> showDeleteConfirmation(expense) },
//            onEditClick = { expense -> showEditExpenseDialog(expense) }
//        )
//
//        expenseRecyclerView.apply {
//            layoutManager = GridLayoutManager(this@PrinterSettingsActivity, 2)
//            adapter = expenseAdapter
//        }
//
//        val factory = StoreExpenseViewModel.Factory(
//            AppDatabase.getDatabase(application).storeExpenseDao(),
//            RetrofitClient.storeExpenseApi
//        )
//        expenseViewModel = ViewModelProvider(this, factory)[StoreExpenseViewModel::class.java]
//        expenseViewModel.allExpenses.observe(this) { expenses ->
//            expenseAdapter.submitList(expenses)
//        }
//    }
data class TransactionData(
    val summary: TransactionSummary,
    val records: List<TransactionRecord>,
    var isSelected: Boolean = false
)
    private fun toggleLocalDataServer() {
        if (localDataManager.isServerRunning()) {
            localDataManager.stopServer()
            showServerStatus("Server stopped")
        } else {
            serverUrl = localDataManager.startServer()
            if (serverUrl != null) {
                showServerStatus("Server running at: $serverUrl")
                // Optionally copy URL to clipboard
                copyToClipboard(serverUrl!!)
            } else {
                showServerStatus("Failed to start server")
            }
        }
    }

    private fun showServerStatus(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.d("MainActivity", message)

        // Show dialog with URL and instructions
        if (serverUrl != null && localDataManager.isServerRunning()) {
            AlertDialog.Builder(this)
                .setTitle("Local Data Server")
                .setMessage("Server is running at:\n$serverUrl\n\nYou can access this URL from any device on the same network to view your tablet data.\n\nEndpoints:\n• / - Web interface\n• /transactions - All transaction records\n• /transaction-summaries - Transaction summaries\n• /stats - Database statistics")
                .setPositiveButton("Copy URL") { _, _ ->
                    copyToClipboard(serverUrl!!)
                }
                .setNegativeButton("Stop Server") { _, _ ->
                    localDataManager.stopServer()
                }
                .show()
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Server URL", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "URL copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    private fun setupEmergencyResync() {
        // Initialize repository with proper dependencies
        val numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
            RetrofitClient.numberSequenceApi,
            AppDatabase.getDatabase(application).numberSequenceRemoteDao()
        )

        repository = TransactionRepository(
            AppDatabase.getDatabase(application).transactionDao(),
            numberSequenceRemoteRepository
        )
    }

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
//        val selectAllCheckbox = dialogView.findViewById<CheckBox>(R.id.selectAllCheckbox)

        // Set initial dates
        startDateEdit.setText(startDate.toFormattedString())
        endDateEdit.setText(endDate.toFormattedString())

        // Setup RecyclerView
        val adapter = TransactionResyncAdapter()
        transactionList.adapter = adapter
        transactionList.layoutManager = LinearLayoutManager(this)

        // Setup select all checkbox
        val selectAllCheckbox = dialogView.findViewById<CheckBox>(R.id.selectAllCheckbox)
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

        AlertDialog.Builder(this)
            .setTitle("Emergency Resync")
            .setView(dialogView)
            .setPositiveButton("Resync Selected") { dialog, _ ->
                lifecycleScope.launch {
                    val selectedTransactions = adapter.currentList.filter { it.isSelected }
                    resyncTransactions(selectedTransactions)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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
                    startDate.time,
                    endDate.time
                )

                // Fetch records for each summary
                summaries.map { summary ->
                    val records = repository.getTransactionRecords(summary.transactionId)
                    TransactionData(summary, records)
                }
            }

            // Calculate totals
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

                        // Check if transaction is already being processed
                        if (!transactionData.summary.syncStatus) {
                            Log.d("EmergencyResync", "Transaction ${transactionData.summary.transactionId} is already in resync process, skipping")
                            return@forEach
                        }

                        // Reset sync status for both summary and records first
                        repository.transactionDao.updateSyncStatus(transactionData.summary.transactionId, false)
                        repository.transactionDao.updateTransactionRecordsSync(transactionData.summary.transactionId, false)

                        // Let the periodic sync handle it from here
                        // Don't call syncTransaction directly to avoid double-sending
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

    // Adapter for transaction list


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
        AlertDialog.Builder(this)
            .setTitle("Delete Expense")
            .setMessage("Are you sure you want to delete this expense?")
            .setPositiveButton("Delete") { dialog, _ ->
                expenseViewModel.deleteExpense(expense)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()

        // If there's an existing date, parse it
        if (editText.text.isNotEmpty()) {
            try {
                val parts = editText.text.toString().split("-")
                if (parts.size == 3) {
                    calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                }
            } catch (e: Exception) {
                // If parsing fails, use current date
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

            // Create CardView for the spinner
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

            // Create adapter with custom layout for black text
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
            }

            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Ensure spinner text is black
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

        // Create dialog with custom style
        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Store Expense")
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

        // Set custom background
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
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
                receivedBy = receivedBy.ifEmpty { "N/A" }, // Provide a default if empty
                approvedBy = approvedBy.ifEmpty { "N/A" }, // Provide a default if empty
                effectDate = effectDate.ifEmpty { "N/A" } // Use current date if empty
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
        sidebarLayout = findViewById(R.id.sidebarLayout)
        toggleButton = findViewById(R.id.toggleButton)
        buttonContainer = findViewById(R.id.buttonContainer)
        ecposTitle = findViewById(R.id.ecposTitle)

    }

    private fun setupSidebar() {
        toggleButton.setOnClickListener {
            if (isSidebarExpanded) {
                collapseSidebar()
            } else {
                expandSidebar()
            }
        }
        setupSidebarButtons()
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
            val intent = Intent (this, StockCountingActivity::class.java)
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
                updatePrinterList() // Update the list to reflect current connection status
                delay(2000) // Check every 2 seconds
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

        val devicesAdapter = ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            mutableListOf()
        )
        deviceList.adapter = devicesAdapter

        // Add already paired devices first
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
                            val device =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

        // Register receivers
        val intentFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, intentFilter)

        // Setup device selection
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
                        // Save the printer
                        addPrinter(name, address)

                        // Save as last connected printer and name
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

        // Setup rescan button
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

        // Create and show dialog
        scanningDialog = AlertDialog.Builder(this)
            .setTitle("Available Devices")
            .setView(dialogView)
            .setOnCancelListener {
                stopScanning()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                stopScanning()
                dialog.dismiss()
            }
            .create()

        scanningDialog?.show()

        // Start initial scan
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

            // Clear previous results
            discoveredDevices.clear()
            devicesAdapter.clear()

            // Add paired devices first
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

            // Cancel any ongoing discovery
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }

            // Start new discovery
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
            textSize = 16f
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

            Log.d(
                TAG,
                "Checking printer connection - Connected: $isConnected, Address: $connectedAddress"
            )

            runOnUiThread {
                if (isConnected && connectedAddress != null) {
                    val connectedPrinter = savedPrinters.find { it.address == connectedAddress }

                    if (connectedPrinter != null) {
                        currentStatusTextView?.text =
                            "Connected to: ${connectedPrinter.name} (${connectedPrinter.address})"
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
                                currentStatusTextView?.text =
                                    "Connected to: $printerName ($connectedAddress)"
                                Log.d(TAG, "Adding new printer: $printerName")
                                addPrinter(printerName, connectedAddress, false)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting device info", e)
                            currentStatusTextView?.text =
                                "Connected to: Unknown Printer ($connectedAddress)"
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

    private fun showManualAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_printer, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.editTextPrinterName)
        val addressInput = dialogView.findViewById<EditText>(R.id.editTextMacAddress)
        val defaultCheck = dialogView.findViewById<CheckBox>(R.id.checkBoxDefault)

        AlertDialog.Builder(this)
            .setTitle("Add Printer")
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
            .show()
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

        AlertDialog.Builder(this)
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
            .show()
    }

    private fun connectToPrinter(printer: PrinterInfo) {
        lifecycleScope.launch {
            try {
                val connected = withContext(Dispatchers.IO) {
                    bluetoothPrinterHelper.connect(printer.address)
                }

                if (connected) {
                    // Save as last connected printer
                    val prefs = getSharedPreferences("BluetoothPrinter", Context.MODE_PRIVATE)
                    prefs.edit().putString("last_printer_address", printer.address).apply()

                    // Connection is maintained in the singleton
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
                // Use the singleton instance to disconnect
                BluetoothPrinterHelper.getInstance().disconnect()
                Toast.makeText(
                    this@PrinterSettingsActivity,
                    "Printer disconnected",
                    Toast.LENGTH_SHORT
                ).show()

                // Clear the last connected printer from shared preferences
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
                    Date: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                }
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
            // If this printer should be default, remove default from others
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

                    text1.setTextColor(
                        if (isPrinterConnected) Color.GREEN
                        else Color.BLACK
                    )
                    text2.text = printer.address

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

            // Add bonded devices
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

    private fun savePrinter(name: String, address: String, isDefault: Boolean) {
        // Implement your printer saving logic here
        Toast.makeText(this, "Printer added: $name", Toast.LENGTH_SHORT).show()
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
        // Check if there's already a connected printer from Window1
        updateConnectionStatus()
        updatePrinterList()
    }


    override fun onDestroy() {
        super.onDestroy()
        connectionStatusTimer?.cancel()
        localDataManager.stopServer()

        // Don't disconnect the printer when leaving the activity
        // This allows the connection to persist for Window1
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