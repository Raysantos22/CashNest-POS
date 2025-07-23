package com.example.possystembw.ui

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.DAO.*
import com.example.possystembw.R
import com.example.possystembw.RetrofitClient
import com.example.possystembw.adapter.ItemSalesAdapter

import com.example.possystembw.data.ARRepository
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.CashFundRepository
import com.example.possystembw.data.NumberSequenceRemoteRepository
import com.example.possystembw.data.TransactionRepository
import com.example.possystembw.database.*
import com.example.possystembw.databinding.ActivityReportsBinding
import com.example.possystembw.ui.ViewModel.ARViewModel
import com.example.possystembw.ui.ViewModel.ARViewModelFactory
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.ui.ViewModel.NumberSequenceAutoChecker
import com.example.possystembw.ui.ViewModel.setupNumberSequenceChecker


class ReportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportsBinding
    private lateinit var transactionDao: TransactionDao
    private lateinit var zReadDao: ZReadDao
    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private lateinit var tenderDeclarationDao: TenderDeclarationDao
    private lateinit var cashFundRepository: CashFundRepository
    private lateinit var arViewModel: ARViewModel

    private var startDate: Date = Date()
    private var endDate: Date = Date()
    private var currentCashFund: Double = 0.0
    private var tenderDeclaration: TenderDeclaration? = null
    private var isCashFundEntered: Boolean = false
    private var isPulloutCashFundProcessed: Boolean = false
    private var isTenderDeclarationProcessed: Boolean = false

    private lateinit var autoZReadReceiver: BroadcastReceiver
    private lateinit var alarmManager: AlarmManager
    private lateinit var transactionRepository: TransactionRepository

    private var autoSyncJob: Job? = null
    private var lastSyncTimestamp: Long = 0L
    private var isAutoSyncEnabled: Boolean = true
    private val autoSyncIntervalMs: Long = 30000L
    private var lastKnownTransactionCount: Int = 0  // <- This fixes your error

    private lateinit var sequenceChecker: NumberSequenceAutoChecker


    companion object {
        // FIXED: Use simple formatters like in showTransactionListDialog
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        private val DISPLAY_DATE_FORMAT = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())


        // FIXED: Use the same date formatting as showTransactionListDialog
//        private fun formatDateToString(date: Date): String {
//            return try {
//                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
//                format.timeZone = TimeZone.getTimeZone("UTC")
//                val result = format.format(date)
//                Log.d("ReportsActivity", "Reports formatDateToString: $date -> '$result'")
//                result
//            } catch (e: Exception) {
//                Log.e("ReportsActivity", "Error formatting date to string: ${e.message}")
//                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
//                fallbackFormat.timeZone = TimeZone.getTimeZone("UTC")
//                fallbackFormat.format(Date())
//            }
//        }
        private fun formatDateToString(date: Date): String {
            return try {
                // FIXED: Use your exact format 2025-07-17 09:03:35
                val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                val result = format.format(date)
                Log.d("ReportsActivity", "Reports formatDateToString: $date -> '$result'")
                result
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error formatting date to string: ${e.message}")
                val fallbackFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                fallbackFormat.format(Date())
            }
        }
        fun formatDateForDisplay(date: Date): String {
            return DISPLAY_DATE_FORMAT.format(date)
        }

        private fun isSameDay(date1: Date, date2: Date): Boolean {
            val cal1 = Calendar.getInstance().apply { time = date1 }
            val cal2 = Calendar.getInstance().apply { time = date2 }
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }
        fun getStartOfDay(date: Date): Date {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar.time
        }

        fun getEndOfDay(date: Date): Date {
            val calendar = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            return calendar.time
        }

        fun getCurrentPhilippineTime(): Date {
            return Date() // Use system current time
        }
    }

    // Keep your existing data classes...
    data class PaymentDistribution(
        val cash: Double = 0.0,
        val card: Double = 0.0,
        val gCash: Double = 0.0,
        val payMaya: Double = 0.0,
        val loyaltyCard: Double = 0.0,
        val charge: Double = 0.0,
        val foodpanda: Double = 0.0,
        val grabfood: Double = 0.0,
        val representation: Double = 0.0,
        val ar: Double = 0.0
    )

    data class ItemSalesSummary(
        val name: String,
        val quantity: Int,
        val totalAmount: Double,
        val itemGroup: String = "Unknown"
    )

    data class SalesReport(
        val totalGross: Double = 0.0,
        val totalNetSales: Double = 0.0,
        val totalDiscount: Double = 0.0,
        val totalQuantity: Int = 0,
        val totalTransactions: Int = 0,
        val totalReturns: Int = 0,
        val totalReturnAmount: Double = 0.0,
        val totalReturnDiscount: Double = 0.0,
        val paymentDistribution: PaymentDistribution = PaymentDistribution(),
        val itemSales: List<ItemSalesSummary> = emptyList(),
        val vatAmount: Double = 0.0,
        val vatableSales: Double = 0.0,
        val startingOR: String = "N/A",
        val endingOR: String = "N/A"
    )

    data class ItemGroupSummary(
        val groupName: String,
        val items: List<ItemSalesSummary>,
        val totalQuantity: Int,
        val totalAmount: Double
    )

    data class ItemCalculationResult(
        val totalGross: Double,
        val totalQuantity: Int,
        val itemSales: List<ItemSalesSummary>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        simpleTest()

        initializeComponents()
        setupDatePickers()
        setupButtons()
        loadTodaysReport()
        checkForExistingCashFund()
        setupAutomaticZRead()
        testShowTransactionListDialogMethod()
        testDateFormats()
        debugWhatDialogWouldShow()

        // Test the date format fix
        testDateFormats()
        checkYesterdayTransactionsOnStartup()

        setupSimpleAutoSync()
        initializeWithTodaysDate()
        sequenceChecker = setupNumberSequenceChecker(this)
        sequenceChecker.checkAndUpdateSequence()

    }
    private fun setupSimpleAutoSync() {
        lifecycleScope.launch {
            delay(3000) // Wait for everything to load
            startSimpleMonitoring()
        }
    }

    private fun startSimpleMonitoring() {
        autoSyncJob = lifecycleScope.launch {
            try {
                Log.d("AutoSync", "Starting simple transaction monitoring with date conversion...")

                // Initialize the count
                val currentStoreId = SessionManager.getCurrentUser()?.storeid
                if (currentStoreId != null) {
                    lastKnownTransactionCount = withContext(Dispatchers.IO) {
                        transactionDao.getTransactionsByStore(currentStoreId).size
                    }
                }

                // Run initial date conversion
                convertAllTransactionDates()

                while (isAutoSyncEnabled) {
                    try {
                        val hasChanges = checkForAPIChanges()
                        if (hasChanges) {
                            Log.d("AutoSync", "Changes detected, refreshing UI with date conversion...")
                            refreshReportsFromAPI()
                        }
                        delay(autoSyncIntervalMs)
                    } catch (e: Exception) {
                        Log.e("AutoSync", "Error in monitoring loop", e)
                        delay(autoSyncIntervalMs * 2)
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoSync", "Error in simple monitoring", e)
            }
        }
    }

    // ADD: Method to convert transaction records too (if needed)
    private fun convertAllTransactionRecordDates() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    Log.d("DateConverter", "Converting transaction record dates...")

                    val allRecords = transactionDao.getAllTransactionRecords()
                    val convertedRecords = mutableListOf<TransactionRecord>()

                    allRecords.forEach { record ->
                        val originalDate = record.createdDate

                        if (originalDate?.contains("T") == true && originalDate.contains("Z")) {
                            try {
                                val convertedDate = originalDate.convertApiDateToSimple()

                                if (convertedDate != originalDate) {
                                    val updatedRecord = record.copy(
                                        createdDate = convertedDate
                                    )
                                    convertedRecords.add(updatedRecord)
                                }
                            } catch (e: Exception) {
                                Log.e("DateConverter", "Error converting record date: ${e.message}")
                            }
                        }
                    }

                    if (convertedRecords.isNotEmpty()) {
                        transactionDao.insertAll(convertedRecords)
                        Log.d("DateConverter", "Updated ${convertedRecords.size} transaction records with converted dates")
                    }

                } catch (e: Exception) {
                    Log.e("DateConverter", "Error converting transaction record dates: ${e.message}")
                }
            }
        }
    }

    // ADD: Convert both summaries and records

//    private fun convertAllTransactionData() {
//        lifecycleScope.launch {
//            try {
//                showSyncIndicator(true)
//
//                convertAllTransactionDates()
//
//                delay(1000)
//
//                Toast.makeText(this@ReportsActivity, "All transaction dates converted to simple format", Toast.LENGTH_SHORT).show()
//
//            } catch (e: Exception) {
//                Toast.makeText(this@ReportsActivity, "Error converting dates: ${e.message}", Toast.LENGTH_SHORT).show()
//            } finally {
//                showSyncIndicator(false)
//            }
//        }
//    }

    // ADD: Button to manually trigger conversion (add this to your setupButtons method)
    private fun addDateConverterButton() {
        // Add long press to any existing button for manual conversion
        binding.reprintZreadButton.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Convert Date Formats")
                .setMessage("Convert all transaction dates from API format to simple format?\n\nFrom: 2025-07-01T01:27:32.000000Z\nTo: 2025-07-01 09:27:32")
                .setPositiveButton("Convert All") { _, _ ->
                    convertAllTransactionData()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }
    }
    fun manualRefreshOnly() {
        lifecycleScope.launch {
            try {
                Log.d("AutoSync", "Manual refresh triggered")

                showSyncIndicator(true)

                // Just reload the UI with current local data
                loadReportForDateRange()

                Toast.makeText(this@ReportsActivity, "Reports refreshed", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Refresh failed: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showSyncIndicator(false)
            }
        }
    }

    // New method to check if current date has Z-Read
    private suspend fun hasZReadForCurrentDate(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // FIXED: Use proper string date instead of Date object
                val currentDateString = getCurrentDate() // This returns String, not Date
                val existingZRead = zReadDao.getZReadByDate(currentDateString)
                Log.d("ReportsActivity", "Checking Z-Read for date: $currentDateString, found: ${existingZRead != null}")
                existingZRead != null
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking Z-Read for current date: ${e.message}", e)
                false
            }
        }
    }
    // X-Read functionality
    private fun showXReadPreviewDialog(
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?,
        hasZRead: Boolean = false
    ) {
        lifecycleScope.launch {
            try {
                // FIXED: Use the same buildReadReport method for both X-Read and Z-Read
                val xReadContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = false, // Only difference is this flag
                    tenderDeclaration = tenderDeclaration
                )

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_report_preview, null)
                    val reportTextView = dialogView.findViewById<TextView>(R.id.reportContentTextView)
                    val printButton = dialogView.findViewById<Button>(R.id.printButton)
                    val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

                    reportTextView.typeface = Typeface.MONOSPACE
                    reportTextView.text = xReadContent

                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("X-Read Preview")
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

                    printButton.setOnClickListener {
                        if (bluetoothPrinterHelper.printGenericReceipt(xReadContent)) {
                            val message = when {
                                transactions.isEmpty() -> "X-Read completed. No transactions for today."
                                hasZRead -> "X-Read completed. Showing today's actual sales data (Z-Read already performed)."
                                else -> "X-Read completed successfully"
                            }
                            Toast.makeText(this@ReportsActivity, message, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ReportsActivity, "Failed to print X-Read report", Toast.LENGTH_SHORT).show()
                        }
                        dialog.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.window?.apply {
                        setBackgroundDrawableResource(R.drawable.dialog_background)
                        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }

                    dialog.show()
                }

            } catch (e: Exception) {
                Log.e("XRead", "Error generating X-Read preview", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error generating X-Read preview: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun showZReadPreviewDialog(
        transactions: List<TransactionSummary>,
        zReportId: String,
        tenderDeclaration: TenderDeclaration?
    ) {
        lifecycleScope.launch {
            try {
                // Generate the Z-Read report content first
                val zReadContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = true,
                    zReportId = zReportId,
                    tenderDeclaration = tenderDeclaration
                )

                withContext(Dispatchers.Main) {
                    // Create a custom dialog with scrollable content
                    val dialogView = layoutInflater.inflate(R.layout.dialog_report_preview, null)
                    val reportTextView = dialogView.findViewById<TextView>(R.id.reportContentTextView)
                    val printButton = dialogView.findViewById<Button>(R.id.printButton)
                    val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

                    // Set monospace font for better alignment
                    reportTextView.typeface = Typeface.MONOSPACE
                    reportTextView.text = zReadContent

                    // FIXED: Better button text for reprint
                    printButton.text = "🖨️ Reprint Z-Read"

                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("Z-Read Preview - Report #$zReportId")
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

                    printButton.setOnClickListener {
                        if (bluetoothPrinterHelper.printGenericReceipt(zReadContent)) {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Z-Read #$zReportId reprinted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ReportsActivity,
                                "Failed to reprint Z-Read #$zReportId",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        dialog.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.window?.apply {
                        setBackgroundDrawableResource(R.drawable.dialog_background)
                        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }

                    dialog.show()
                }

            } catch (e: Exception) {
                Log.e("ZRead", "Error generating Z-Read preview", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error generating Z-Read preview: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Modified performXRead method
//    private fun performXRead() {
//        lifecycleScope.launch {
//            try {
//                val currentDate = Date()
//                val selectedDate = endDate
//
//                val isToday = isSameDay(currentDate, selectedDate)
//
//                if (!isToday) {
//                    // FIXED: For past dates, check if Z-Read exists and show reprint option
//                    val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
//                    val existingZRead = withContext(Dispatchers.IO) {
//                        zReadDao.getZReadByDate(selectedDateString)
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        if (existingZRead != null) {
//                            // Show Z-Read reprint dialog instead of "X-Read not available"
//                            AlertDialog.Builder(this@ReportsActivity)
//                                .setTitle("CHECK ZREAD")
//                                .setMessage(
//                                    "X-Read is only available for today's transactions.\n\n" +
//                                            "However, Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}.\n\n" +
//                                            "Would you like to view and reprint the Z-Read instead?"
//
//                                )
//
//                                .setPositiveButton("View Z-Read") { _, _ ->
//                                    // Show the existing Z-Read
//                                    lifecycleScope.launch {
//                                        try {
//                                            val transactions = withContext(Dispatchers.IO) {
//                                                transactionDao.getTransactionsByDateRange(
//                                                    formatDateToString(startDate),
//                                                    formatDateToString(endDate)
//                                                )
//                                            }
//                                            val tenderDeclaration = withContext(Dispatchers.IO) {
//                                                tenderDeclarationDao.getLatestTenderDeclaration()
//                                            }
//
//                                            showZReadPreviewDialog(transactions, existingZRead.zReportId, tenderDeclaration)
//                                        } catch (e: Exception) {
//                                            Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
//                                        }
//                                    }
//                                }
//                                .setNegativeButton("Cancel", null)
//                                .create()
//                                .show()
//                        } else {
//                            // No Z-Read exists for this date
//                            AlertDialog.Builder(this@ReportsActivity)
//                                .setTitle("X-Read Not Available")
//                                .setMessage(
//                                    "X-Read is only available for today's transactions.\n\n" +
//                                            "No Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}."
//                                )
//                                .setPositiveButton("OK") { dialog, _ ->
//                                    dialog.dismiss()
//                                }
//
//                                .create()
//                                .show()
//                        }
//                    }
//                    return@launch
//                }
//
//                // For today's X-Read, get today's transactions only
//                val todayStart = Calendar.getInstance().apply {
//                    set(Calendar.HOUR_OF_DAY, 0)
//                    set(Calendar.MINUTE, 0)
//                    set(Calendar.SECOND, 0)
//                    set(Calendar.MILLISECOND, 0)
//                }
//                val todayEnd = Calendar.getInstance().apply {
//                    time = todayStart.time
//                    set(Calendar.HOUR_OF_DAY, 23)
//                    set(Calendar.MINUTE, 59)
//                    set(Calendar.SECOND, 59)
//                    set(Calendar.MILLISECOND, 999)
//                }
//
//                val transactions = withContext(Dispatchers.IO) {
//                    transactionDao.getTransactionsByDateRange(
//                        formatDateToString(todayStart.time),
//                        formatDateToString(todayEnd.time)
//                    ).filter { it.transactionStatus == 1 } // Only completed transactions
//                }
//
//                withContext(Dispatchers.Main) {
//                    // Check if there are actually transactions before showing preview
//                    if (transactions.isEmpty()) {
//                        AlertDialog.Builder(this@ReportsActivity)
//                            .setTitle("No Transactions")
//                            .setMessage("No transactions found for today. X-Read cannot be generated.")
//                            .setPositiveButton("OK") { dialog, _ ->
//                                dialog.dismiss()
//                            }
//                            .create()
//                            .show()
//                        return@withContext
//                    }
//
//                    val currentTenderDeclaration = withContext(Dispatchers.IO) {
//                        tenderDeclarationDao.getLatestTenderDeclaration()
//                    }
//                    val hasZRead = hasZReadForCurrentDate()
//
//                    showXReadPreviewDialog(transactions, currentTenderDeclaration, hasZRead)
//                }
//
//            } catch (e: Exception) {
//                Log.e("XRead", "Error performing X-Read", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@ReportsActivity,
//                        "Error performing X-Read: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }

//    private fun performXRead() {
//        lifecycleScope.launch {
//            try {
//                val currentDate = Date()
//                val selectedDate = endDate
//
//                val isToday = isSameDay(currentDate, selectedDate)
//
//                if (!isToday) {
//                    // For past dates, check for existing Z-Read and transaction states
//                    val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
//                    val existingZRead = withContext(Dispatchers.IO) {
//                        zReadDao.getZReadByDate(selectedDateString)
//                    }
//
//                    // Get transactions for the selected date
//                    val transactions = withContext(Dispatchers.IO) {
//                        transactionDao.getTransactionsByDateRange(
//                            formatDateToString(startDate),
//                            formatDateToString(endDate)
//                        ).filter { it.transactionStatus == 1 }
//                    }
//
//                    withContext(Dispatchers.Main) {
//                        if (existingZRead != null) {
//                            // Show Z-Read reprint dialog with consistent styling
//                            val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                                .setTitle("CHECK ZREAD")
//                                .setMessage(
//                                    "X-Read is only available for today's transactions.\n\n" +
//                                            "However, Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}:\n\n" +
//                                            "Z-Report ID: ${existingZRead.zReportId}\n" +
//                                            "Date: ${existingZRead.date}\n" +
//                                            "Time: ${existingZRead.time}\n" +
//                                            "Transactions: ${existingZRead.totalTransactions}\n" +
//                                            "Amount: ₱${String.format("%.2f", existingZRead.totalAmount)}\n\n" +
//                                            "Would you like to view and reprint the Z-Read instead?"
//                                )
//                                .setPositiveButton("View Z-Read") { _, _ ->
//                                    // Show the existing Z-Read
//                                    lifecycleScope.launch {
//                                        try {
//                                            val tenderDeclaration = withContext(Dispatchers.IO) {
//                                                tenderDeclarationDao.getLatestTenderDeclaration()
//                                            }
//                                            showZReadPreviewDialog(transactions, existingZRead.zReportId, tenderDeclaration)
//                                        } catch (e: Exception) {
//                                            Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
//                                        }
//                                    }
//                                }
//                                .setNegativeButton("Cancel", null)
//                                .create()
//
//                            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                            dialog.show()
//
//                        } else if (transactions.isNotEmpty()) {
//                            // Check if transactions have Z-Report IDs (same logic as checkForExistingZRead)
//                            val transactionsWithZRead = transactions.filter {
//                                !it.zReportId.isNullOrEmpty()
//                            }
//
//                            if (transactionsWithZRead.isNotEmpty()) {
//                                val firstZReportId = transactionsWithZRead.first().zReportId
//                                val allSameZReportId = transactionsWithZRead.all {
//                                    it.zReportId == firstZReportId
//                                }
//
//                                if (allSameZReportId && transactionsWithZRead.size == transactions.size) {
//                                    // All transactions have the same Z-Report ID
//                                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                                        .setTitle("CHECK ZREAD")
//                                        .setMessage(
//                                            "X-Read is only available for today's transactions.\n\n" +
//                                                    "However, Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}:\n\n" +
//                                                    "Z-Report ID: $firstZReportId\n" +
//                                                    "Transactions: ${transactions.size}\n" +
//                                                    "Total Amount: ₱${String.format("%.2f", transactions.sumOf { it.netAmount })}\n\n" +
//                                                    "Would you like to view and reprint the Z-Read instead?"
//                                        )
//                                        .setPositiveButton("View Z-Read") { _, _ ->
//                                            lifecycleScope.launch {
//                                                try {
//                                                    val tenderDeclaration = withContext(Dispatchers.IO) {
//                                                        tenderDeclarationDao.getLatestTenderDeclaration()
//                                                    }
//                                                    showZReadPreviewDialog(transactions, firstZReportId!!, tenderDeclaration)
//                                                } catch (e: Exception) {
//                                                    Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
//                                                }
//                                            }
//                                        }
//                                        .setNegativeButton("Cancel", null)
//                                        .create()
//
//                                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                                    dialog.show()
//
//                                } else {
//                                    // Mixed state - some transactions have Z-Report IDs, some don't
//                                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                                        .setTitle("CHECK ZREAD")
//                                        .setMessage(
//                                            "X-Read is only available for today's transactions.\n\n" +
//                                                    "Transactions for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)} have mixed Z-Read states:\n\n" +
//                                                    "Total Transactions: ${transactions.size}\n" +
//                                                    "With Z-Read: ${transactionsWithZRead.size}\n" +
//                                                    "Without Z-Read: ${transactions.size - transactionsWithZRead.size}\n\n" +
//                                                    "Would you like to view the existing Z-Read data?"
//                                        )
//                                        .setPositiveButton("View Z-Read") { _, _ ->
//                                            showMixedZReadStateDialog(transactions, transactionsWithZRead)
//                                        }
//                                        .setNegativeButton("Cancel", null)
//                                        .create()
//
//                                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                                    dialog.show()
//                                }
//                            } else {
//                                // No Z-Read exists for this date and no transactions have Z-Report IDs
//                                val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                                    .setTitle("X-Read Not Available")
//                                    .setMessage(
//                                        "X-Read is only available for today's transactions.\n\n" +
//                                                "No Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}.\n\n" +
//                                                "Transactions: ${transactions.size}\n" +
//                                                "Total Amount: ₱${String.format("%.2f", transactions.sumOf { it.netAmount })}"
//                                    )
//                                    .setPositiveButton("OK") { dialog, _ ->
//                                        dialog.dismiss()
//                                    }
//                                    .create()
//
//                                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                                dialog.show()
//                            }
//                        } else {
//                            // No transactions found for this date
//                            val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                                .setTitle("X-Read Not Available")
//                                .setMessage(
//                                    "X-Read is only available for today's transactions.\n\n" +
//                                            "No transactions found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}."
//                                )
//                                .setPositiveButton("OK") { dialog, _ ->
//                                    dialog.dismiss()
//                                }
//                                .create()
//
//                            dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                            dialog.show()
//                        }
//                    }
//                    return@launch
//                }
//
//                // For today's X-Read, get today's transactions only
//                val todayStart = Calendar.getInstance().apply {
//                    set(Calendar.HOUR_OF_DAY, 0)
//                    set(Calendar.MINUTE, 0)
//                    set(Calendar.SECOND, 0)
//                    set(Calendar.MILLISECOND, 0)
//                }
//                val todayEnd = Calendar.getInstance().apply {
//                    time = todayStart.time
//                    set(Calendar.HOUR_OF_DAY, 23)
//                    set(Calendar.MINUTE, 59)
//                    set(Calendar.SECOND, 59)
//                    set(Calendar.MILLISECOND, 999)
//                }
//
//                val transactions = withContext(Dispatchers.IO) {
//                    transactionDao.getTransactionsByDateRange(
//                        formatDateToString(todayStart.time),
//                        formatDateToString(todayEnd.time)
//                    ).filter { it.transactionStatus == 1 } // Only completed transactions
//                }
//
//                withContext(Dispatchers.Main) {
//                    // Check if there are actually transactions before showing preview
//                    if (transactions.isEmpty()) {
//                        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//                            .setTitle("No Transactions")
//                            .setMessage("No transactions found for today. X-Read cannot be generated.")
//                            .setPositiveButton("OK") { dialog, _ ->
//                                dialog.dismiss()
//                            }
//                            .create()
//
//                        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//                        dialog.show()
//                        return@withContext
//                    }
//
//                    val currentTenderDeclaration = withContext(Dispatchers.IO) {
//                        tenderDeclarationDao.getLatestTenderDeclaration()
//                    }
//                    val hasZRead = hasZReadForCurrentDate()
//
//                    showXReadPreviewDialog(transactions, currentTenderDeclaration, hasZRead)
//                }
//
//            } catch (e: Exception) {
//                Log.e("XRead", "Error performing X-Read", e)
//                withContext(Dispatchers.Main) {
//                    Toast.makeText(
//                        this@ReportsActivity,
//                        "Error performing X-Read: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//            }
//        }
//    }
private fun performXRead() {
    lifecycleScope.launch {
        try {
            val currentDate = Date()
            val selectedDate = endDate

            val isToday = isSameDay(currentDate, selectedDate)

            if (!isToday) {
                // For past dates, check for existing Z-Read
                handlePastDateXRead(selectedDate)
                return@launch
            }

            // FIXED: For today's X-Read, use consistent date range
            val (todayTransactions, hasZRead) = getTodaysTransactionData()

            withContext(Dispatchers.Main) {
                if (todayTransactions.isEmpty()) {
                    showNoTransactionsDialog("X-Read")
                    return@withContext
                }

                val currentTenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                showXReadPreviewDialog(todayTransactions, currentTenderDeclaration, hasZRead)
            }

        } catch (e: Exception) {
            Log.e("XRead", "Error performing X-Read", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ReportsActivity,
                    "Error performing X-Read: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

    private suspend fun getTodaysTransactionData(): Pair<List<TransactionSummary>, Boolean> {
        return withContext(Dispatchers.IO) {
            // Always use current date for "today" operations
            val today = Date()
            val todayStart = Calendar.getInstance().apply {
                time = today
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val todayEnd = Calendar.getInstance().apply {
                time = today
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            Log.d("TodayTransactions", "Getting today's transactions:")
            Log.d("TodayTransactions", "Start: ${formatDateToString(todayStart)}")
            Log.d("TodayTransactions", "End: ${formatDateToString(todayEnd)}")

            // Get today's transactions using consistent date formatting
            val transactions = transactionDao.getTransactionsByDateRange(
                formatDateToString(todayStart),
                formatDateToString(todayEnd)
            ).filter { it.transactionStatus == 1 } // Only completed transactions

            Log.d("TodayTransactions", "Found ${transactions.size} completed transactions for today")

            // Check if Z-Read exists for today
            val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(today)
            val hasZRead = zReadDao.getZReadByDate(todayDateString) != null

            Log.d("TodayTransactions", "Has Z-Read for today: $hasZRead")

            Pair(transactions, hasZRead)
        }
    }


    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }

        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
    private fun printXReadReport(
        transactions: List<TransactionSummary>,
        tenderDeclaration: TenderDeclaration?,
        hasZReadToday: Boolean = false
    ) {
        lifecycleScope.launch {
            try {
                // Always build report with actual transaction data
                val xReadContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = false,
                    tenderDeclaration = tenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(xReadContent)) {
                    val message = when {
                        transactions.isEmpty() -> "X-Read completed. No transactions for today."
                        hasZReadToday -> "X-Read completed. Showing today's actual sales data (Z-Read already performed)."
                        else -> "X-Read completed successfully"
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ReportsActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ReportsActivity, "Failed to print X-Read report", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                Log.e("XRead", "Error printing X-Read", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error printing X-Read: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    // Add these helper methods to your ReportsActivity class

    private fun isAfterMidnight(): Boolean {
        val currentTime = Calendar.getInstance()
        val hour = currentTime.get(Calendar.HOUR_OF_DAY)
        return hour >= 0 && hour < 6 // Assuming "after midnight" means between 12:00 AM and 6:00 AM
    }

    private fun resetCashManagementStatus() {
        isPulloutCashFundProcessed = false
        isTenderDeclarationProcessed = false
        // Add isZReadPerformed if you have this variable
        // isZReadPerformed = false
    }


    private fun saveCashFund(cashFund: Double, status: String) {
        lifecycleScope.launch {
            try {
                val currentDateString = getCurrentDate() // Returns string
                val cashFundEntity = Cashfund(
                    cashFund = cashFund,
                    status = status,
                    date = currentDateString // Use string date
                )
                cashFundRepository.insert(cashFundEntity)
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error saving cash fund: ${e.message}", e)
            }
        }
    }
    private fun getCurrentDate(): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            format.format(Date())
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error getting current date: ${e.message}")
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
    }

    private fun checkForExistingCashFund() {
        lifecycleScope.launch {
            try {
                val currentDateString = getCurrentDate() // Returns string
                val existingCashFund = cashFundRepository.getCashFundByDate(currentDateString)

                if (existingCashFund != null) {
                    currentCashFund = existingCashFund.cashFund
                    isCashFundEntered = true
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for existing cash fund: ${e.message}", e)
            }
        }
    }
//
//    private fun getCurrentDate(): String {
//        return formatDateAsString(getCurrentPhilippineTime())
//    }
//
//
//    private fun getCurrentTime(): String {
//        return TIME_FORMAT.format(getCurrentPhilippineTime())
//    }
    // Rest of the existing methods...
private fun showReprintZReadSelection() {
    lifecycleScope.launch {
        try {
            val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate)
            val existingZRead = withContext(Dispatchers.IO) {
                zReadDao.getZReadByDate(selectedDateString)
            }

            if (existingZRead != null) {
                // Show reprint Z-Read dialog with consistent styling
                val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                    .setTitle("Reprint Z-Read")
                    .setMessage(
                        "Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}:\n\n" +
                                "Z-Report ID: ${existingZRead.zReportId}\n" +
                                "Date: ${existingZRead.date}\n" +
                                "Time: ${existingZRead.time}\n" +
                                "Transactions: ${existingZRead.totalTransactions}\n" +
                                "Amount: ₱${String.format("%.2f", existingZRead.totalAmount)}\n\n" +
                                "Would you like to view and reprint it?"
                    )
                    .setPositiveButton("View & Reprint") { _, _ ->
                        // Show the Z-Read preview dialog
                        lifecycleScope.launch {
                            try {
                                val transactions = withContext(Dispatchers.IO) {
                                    transactionDao.getTransactionsByDateRange(
                                        formatDateToString(startDate),
                                        formatDateToString(endDate)
                                    )
                                }
                                val tenderDeclaration = withContext(Dispatchers.IO) {
                                    tenderDeclarationDao.getLatestTenderDeclaration()
                                }

                                showZReadPreviewDialog(transactions, existingZRead.zReportId, tenderDeclaration)
                            } catch (e: Exception) {
                                Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .create()

                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()
                return@launch
            }

            // If no Z-Read in database, check transactions for Z-Report IDs
            val transactions = withContext(Dispatchers.IO) {
                transactionDao.getTransactionsByDateRange(
                    formatDateToString(startDate),
                    formatDateToString(endDate)
                ).filter { it.transactionStatus == 1 }
            }

            withContext(Dispatchers.Main) {
                if (transactions.isEmpty()) {
                    // No transactions found for selected date
                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("No Transactions Found")
                        .setMessage(
                            "No completed transactions found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}.\n\n" +
                                    "Please select a date that has transactions with a completed Z-Read."
                        )
                        .setPositiveButton("OK", null)
                        .create()

                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.show()
                    return@withContext
                }

                val transactionsWithZRead = transactions.filter {
                    !it.zReportId.isNullOrEmpty()
                }

                if (transactionsWithZRead.isNotEmpty()) {
                    val firstZReportId = transactionsWithZRead.first().zReportId

                    val allSameZReportId = transactionsWithZRead.all {
                        it.zReportId == firstZReportId
                    }

                    if (allSameZReportId && transactionsWithZRead.size == transactions.size) {
                        // All transactions have the same Z-Report ID - show existing Z-Read dialog
                        showExistingZReadDialog(firstZReportId!!, transactions)
                    } else {
                        // Mixed state - some transactions have Z-Report IDs, some don't
                        showMixedZReadStateDialog(transactions, transactionsWithZRead)
                    }
                } else {
                    // No Z-Read found anywhere for selected date
                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("No Z-Read Found")
                        .setMessage(
                            "No Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}.\n\n" +
                                    "The transactions for this date do not have Z-Read data:\n\n" +
                                    "Transactions: ${transactions.size}\n" +
                                    "Total Amount: ₱${String.format("%.2f", transactions.sumOf { it.netAmount })}\n\n" +
                                    "Would you like to generate a Z-Read first?"
                        )
                        .setPositiveButton("Generate Z-Read") { _, _ ->
                            // Offer to generate Z-Read for these transactions
                            showZReadConfirmationDialog(transactions.size)
                        }
                        .setNegativeButton("Cancel", null)
                        .create()

                    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                    dialog.show()
                }
            }

        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error checking for Z-Read to reprint", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ReportsActivity,
                    "Error checking Z-Read: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

    private fun initializeComponents() {
        val database = AppDatabase.getDatabase(this)
        transactionDao = database.transactionDao()
        zReadDao = database.zReadDao()
        tenderDeclarationDao = database.tenderDeclarationDao()
        bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()
        cashFundRepository = CashFundRepository(database.cashFundDao())

        val numberSequenceApi = RetrofitClient.numberSequenceApi
        val numberSequenceRemoteDao = database.numberSequenceRemoteDao()
        val numberSequenceRemoteRepository = NumberSequenceRemoteRepository(
            numberSequenceApi = numberSequenceApi,
            numberSequenceRemoteDao = numberSequenceRemoteDao,
            transactionDao = transactionDao  // ADD THIS PARAMETER
        )

        transactionRepository = TransactionRepository(
            transactionDao = transactionDao,
            numberSequenceRemoteRepository = numberSequenceRemoteRepository
        )

        val arApi = RetrofitClient.arApi
        val arDao = database.arDao()
        val arRepository = ARRepository(arApi, arDao)
        val arViewModelFactory = ARViewModelFactory(arRepository)
        arViewModel = ViewModelProvider(this, arViewModelFactory)[ARViewModel::class.java]

        bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()
        bluetoothPrinterHelper.setTransactionDao(transactionDao)

        // Set current date as default
        val currentDate = Calendar.getInstance()
        binding.startDatePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(currentDate.time)
        binding.endDatePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(currentDate.time)

        // Initialize date range
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayEnd = Calendar.getInstance().apply {
            time = todayStart.time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        startDate = todayStart.time
        endDate = todayEnd.time

        Log.d("ReportsActivity", "=== INITIALIZATION DEBUG ===")
        Log.d("ReportsActivity", "Initialized with today's range:")
        Log.d("ReportsActivity", "Start: ${formatDateToString(startDate)}")
        Log.d("ReportsActivity", "End: ${formatDateToString(endDate)}")

        // Run debugging functions
        testDateQueries()
    }

    // FIXED: Add method to check what works in your showTransactionListDialog
    private fun testShowTransactionListDialogMethod() {
        lifecycleScope.launch {
            try {
                val currentStoreId = SessionManager.getCurrentUser()?.storeid
                if (currentStoreId != null) {
                    // EXACT copy of the date logic from showTransactionListDialog
                    val todayStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val todayEnd = Calendar.getInstance().apply {
                        time = todayStart.time
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    // Use the EXACT same formatDateToString as in your working code
                    val transactions = withContext(Dispatchers.IO) {
                        transactionDao.getTransactionsByDateRange(
                            formatDateToString(todayStart.time),
                            formatDateToString(todayEnd.time)
                        )
                    }

                    Log.d("ReportsActivity", "=== DIALOG METHOD TEST ===")
                    Log.d("ReportsActivity", "Using exact dialog logic: ${transactions.size} transactions found")

                    if (transactions.isNotEmpty()) {
                        Log.d("ReportsActivity", "SUCCESS! Found transactions using dialog method")
                        transactions.take(3).forEach { transaction ->
                            Log.d("ReportsActivity", "  Transaction: ${transaction.transactionId} - ${transaction.createdDate}")
                        }
                    } else {
                        Log.d("ReportsActivity", "Still no transactions found with dialog method")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error testing dialog method", e)
            }
        }
    }
    private fun getTransactionsLikeDialog(): List<TransactionSummary> {
        return runBlocking {
            withContext(Dispatchers.IO) {
                val currentStoreId = SessionManager.getCurrentUser()?.storeid

                // Create date range EXACTLY like showTransactionListDialog
                val todayStart = Calendar.getInstance().apply {
                    time = startDate  // Use the selected date instead of "today"
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayEnd = Calendar.getInstance().apply {
                    time = todayStart.time
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }

                // Use the EXACT same formatDateToString as showTransactionListDialog
                fun formatDateToString(date: Date): String {
                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    return formatter.format(date)
                }

                val startStr = formatDateToString(todayStart.time)
                val endStr = formatDateToString(todayEnd.time)

                Log.d("ReportsActivity", "Dialog-exact query: '$startStr' to '$endStr'")

                try {
                    // Try the exact same query as dialog first
                    val exactDialogResults = transactionDao.getTransactionsByDateRange(startStr, endStr)
                    Log.d("ReportsActivity", "Exact dialog query: ${exactDialogResults.size} results")

                    if (exactDialogResults.isNotEmpty()) {
                        return@withContext exactDialogResults
                    }
                } catch (e: Exception) {
                    Log.e("ReportsActivity", "Exact dialog query failed: ${e.message}")
                }

                // Fallback: Use our working date-based approach but filter to current transactions only
                val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startDate)
                val allForDate = transactionDao.getTransactionsByDate(dateString)

                // Filter to only recent/relevant transactions (not old test data)
                val recentTransactions = allForDate.filter { transaction ->
                    // Only include transactions from this year/month to avoid old test data
                    transaction.createdDate.startsWith("2025-07-") &&
                            transaction.transactionStatus == 1 &&
                            (currentStoreId == null || transaction.store == currentStoreId)
                }

                Log.d("ReportsActivity", "Filtered to recent: ${recentTransactions.size} from ${allForDate.size} total")

                recentTransactions.sortedByDescending { it.createdDate }
            }
        }
    }

    // UPDATE your loadReportForDateRange to use this exact method:
    private fun loadReportForDateRangeExact() {
        lifecycleScope.launch {
            try {
                Log.d("ReportsActivity", "=== EXACT DIALOG REPLICATION ===")

                val transactions = getTransactionsLikeDialog()

                Log.d("ReportsActivity", "Final exact result: ${transactions.size} transactions")

                // Show what we got vs what dialog shows
                transactions.take(3).forEach { transaction ->
                    Log.d("ReportsActivity", "Reports exact: ${transaction.transactionId} - ${transaction.createdDate}")
                }

                val report = calculateSalesReport(transactions)
                updateUI(report)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error in exact replication", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // REPLACE the call in your date pickers - change loadReportForDateRange() to loadReportForDateRangeExact()
//    private fun setupDatePickers() {
//        binding.startDatePickerButton.setOnClickListener {
//            val currentDate = Calendar.getInstance()
//            val datePickerDialog = DatePickerDialog(
//                this,
//                { _, year, month, day ->
//                    Log.d("ReportsActivity", "=== REPORTS DATE PICKER ===")
//                    Log.d("ReportsActivity", "Reports selected: year=$year, month=$month, day=$day")
//
//                    // FIXED: Create a single date for both start and end (not a range)
//                    val selectedDate = Calendar.getInstance().apply {
//                        set(Calendar.YEAR, year)
//                        set(Calendar.MONTH, month)
//                        set(Calendar.DAY_OF_MONTH, day)
//                        set(Calendar.HOUR_OF_DAY, 0)
//                        set(Calendar.MINUTE, 0)
//                        set(Calendar.SECOND, 0)
//                        set(Calendar.MILLISECOND, 0)
//                    }
//                    val endDate = Calendar.getInstance().apply {
//                        time = selectedDate.time
//                        set(Calendar.HOUR_OF_DAY, 23)
//                        set(Calendar.MINUTE, 59)
//                        set(Calendar.SECOND, 59)
//                        set(Calendar.MILLISECOND, 999)
//                    }
//
//                    Log.d("ReportsActivity", "Reports selectedDate: ${selectedDate.time}")
//                    Log.d("ReportsActivity", "Reports endDate: ${endDate.time}")
//
//                    // FIXED: Update BOTH button texts to the same date
//                    val dateDisplayText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate.time)
//                    binding.startDatePickerButton.text = dateDisplayText
//                    binding.endDatePickerButton.text = dateDisplayText
//
//                    // Update the actual date variables
//                    startDate = selectedDate.time
//                    this@ReportsActivity.endDate = endDate.time
//
//                    // Load transactions for the selected date
//                    loadTransactionsForSelectedDate()
//                },
//                currentDate.get(Calendar.YEAR),
//                currentDate.get(Calendar.MONTH),
//                currentDate.get(Calendar.DAY_OF_MONTH)
//            )
//            datePickerDialog.show()
//        }
//
//        binding.endDatePickerButton.setOnClickListener {
//            // FIXED: Same date picker for end date to maintain consistency
//            val currentDate = Calendar.getInstance()
//            val datePickerDialog = DatePickerDialog(
//                this,
//                { _, year, month, day ->
//                    Log.d("ReportsActivity", "=== REPORTS DATE PICKER ===")
//                    Log.d("ReportsActivity", "Reports selected: year=$year, month=$month, day=$day")
//
//                    // FIXED: Create a single date for both start and end (not a range)
//                    val selectedDate = Calendar.getInstance().apply {
//                        set(Calendar.YEAR, year)
//                        set(Calendar.MONTH, month)
//                        set(Calendar.DAY_OF_MONTH, day)
//                        set(Calendar.HOUR_OF_DAY, 0)
//                        set(Calendar.MINUTE, 0)
//                        set(Calendar.SECOND, 0)
//                        set(Calendar.MILLISECOND, 0)
//                    }
//                    val endDateCalendar = Calendar.getInstance().apply {
//                        time = selectedDate.time
//                        set(Calendar.HOUR_OF_DAY, 23)
//                        set(Calendar.MINUTE, 59)
//                        set(Calendar.SECOND, 59)
//                        set(Calendar.MILLISECOND, 999)
//                    }
//
//                    Log.d("ReportsActivity", "Reports selectedDate: ${selectedDate.time}")
//                    Log.d("ReportsActivity", "Reports endDate: ${endDateCalendar.time}")
//
//                    // FIXED: Update BOTH button texts to the same date
//                    val dateDisplayText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate.time)
//                    binding.startDatePickerButton.text = dateDisplayText
//                    binding.endDatePickerButton.text = dateDisplayText
//
//                    // Update the actual date variables
//                    startDate = selectedDate.time
//                    endDate = endDateCalendar.time
//
//                    // Load transactions for the selected date
//                    loadTransactionsForSelectedDate()
//                },
//                currentDate.get(Calendar.YEAR),
//                currentDate.get(Calendar.MONTH),
//                currentDate.get(Calendar.DAY_OF_MONTH)
//            )
//            datePickerDialog.show()
//        }
//    }
    private fun setupDatePickers() {
        // Start Date Picker
        binding.startDatePickerButton.setOnClickListener {
            val currentDate = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    // Create start date at beginning of day
                    val selectedStartDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    // Update start date
                    startDate = selectedStartDate.time

                    // Update button text
                    binding.startDatePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedStartDate.time)

                    // If end date is before start date, update end date to match start date
                    if (endDate.before(startDate)) {
                        val selectedEndDate = Calendar.getInstance().apply {
                            time = selectedStartDate.time
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }
                        endDate = selectedEndDate.time
                        binding.endDatePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedStartDate.time)
                    }

                    // Load transactions for the selected range
                    loadTransactionsForSelectedDateRange()
                },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        // End Date Picker
        binding.endDatePickerButton.setOnClickListener {
            val currentDate = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, day ->
                    // Create end date at end of day
                    val selectedEndDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    // Check if selected end date is before start date
                    val startDateOnly = Calendar.getInstance().apply {
                        time = startDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    if (selectedEndDate.before(startDateOnly)) {
                        Toast.makeText(this@ReportsActivity, "End date cannot be before start date", Toast.LENGTH_SHORT).show()
                        return@DatePickerDialog
                    }

                    // Update end date
                    endDate = selectedEndDate.time

                    // Update button text
                    binding.endDatePickerButton.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedEndDate.time)

                    // Load transactions for the selected range
                    loadTransactionsForSelectedDateRange()
                },
                currentDate.get(Calendar.YEAR),
                currentDate.get(Calendar.MONTH),
                currentDate.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }
    }
    private fun loadTransactionsForSelectedDateRange() {
        lifecycleScope.launch {
            try {
                Log.d("ReportsActivity", "=== LOADING DATE RANGE TRANSACTIONS ===")
                Log.d("ReportsActivity", "Start date: ${formatDateToString(startDate)}")
                Log.d("ReportsActivity", "End date: ${formatDateToString(endDate)}")

                val transactions = withContext(Dispatchers.IO) {
                    // For date range queries, get all transactions between start and end dates
                    if (isSameDay(startDate, endDate)) {
                        // Single day - use existing logic
                        val result = transactionDao.getTransactionsByDateRange(
                            formatDateToString(startDate),
                            formatDateToString(endDate)
                        )
                        Log.d("ReportsActivity", "Single day: Found ${result.size} transactions")
                        result
                    } else {
                        // Multiple days - query date range
                        val result = transactionDao.getTransactionsByDateRange(
                            formatDateToString(startDate),
                            formatDateToString(endDate)
                        )
                        Log.d("ReportsActivity", "Date range: Found ${result.size} transactions")
                        result
                    }
                }

                val report = calculateSalesReport(transactions)
                updateUI(report)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error loading transactions for date range", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadTransactionsForSelectedDate() {
        lifecycleScope.launch {
            try {
                Log.d("ReportsActivity", "=== LOADING SELECTED DATE TRANSACTIONS ===")
                Log.d("ReportsActivity", "Start date: ${formatDateToString(startDate)}")
                Log.d("ReportsActivity", "End date: ${formatDateToString(endDate)}")

                val transactions = withContext(Dispatchers.IO) {
                    val result = transactionDao.getTransactionsByDateRange(
                        formatDateToString(startDate),
                        formatDateToString(endDate)
                    )
                    Log.d("ReportsActivity", "Found ${result.size} transactions for selected date")
                    result
                }

                val report = calculateSalesReport(transactions)
                updateUI(report)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error loading transactions for selected date", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
//    private fun initializeWithTodaysDate() {
//        val today = Calendar.getInstance()
//
//        // FIXED: Set both start and end to today
//        val todayStart = Calendar.getInstance().apply {
//            time = today.time
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }
//        val todayEnd = Calendar.getInstance().apply {
//            time = today.time
//            set(Calendar.HOUR_OF_DAY, 23)
//            set(Calendar.MINUTE, 59)
//            set(Calendar.SECOND, 59)
//            set(Calendar.MILLISECOND, 999)
//        }
//
//        startDate = todayStart.time
//        endDate = todayEnd.time
//
//        // FIXED: Set both button texts to the same date (today)
//        val todayText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(today.time)
//        binding.startDatePickerButton.text = todayText
//        binding.endDatePickerButton.text = todayText
//
//        Log.d("ReportsActivity", "Initialized with today's date:")
//        Log.d("ReportsActivity", "Start: ${formatDateToString(startDate)}")
//        Log.d("ReportsActivity", "End: ${formatDateToString(endDate)}")
//    }
private fun initializeWithTodaysDate() {
    val today = Calendar.getInstance()

    // Set start date to today at beginning of day
    val todayStart = Calendar.getInstance().apply {
        time = today.time
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    // Set end date to today at end of day
    val todayEnd = Calendar.getInstance().apply {
        time = today.time
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }

    startDate = todayStart.time
    endDate = todayEnd.time

    // Set button texts to today's date
    val todayText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(today.time)
    binding.startDatePickerButton.text = todayText
    binding.endDatePickerButton.text = todayText

    Log.d("ReportsActivity", "Initialized with today's date:")
    Log.d("ReportsActivity", "Start: ${formatDateToString(startDate)}")
    Log.d("ReportsActivity", "End: ${formatDateToString(endDate)}")
}
    private fun loadTransactionsExactlyLikeWindow1() {
        lifecycleScope.launch {
            try {
                val currentStoreId = SessionManager.getCurrentUser()?.storeid
                if (currentStoreId != null) {
                    Log.d("ReportsActivity", "=== EXACT WINDOW1 REPLICATION ===")
                    Log.d("ReportsActivity", "Reports currentStoreId: $currentStoreId")

                    val transactions = withContext(Dispatchers.IO) {
                        // Use the EXACT formatDateToString from Window1
                        val startDateStr = formatDateToString(startDate)
                        val endDateStr = formatDateToString(endDate)

                        Log.d("ReportsActivity", "=== REPORTS QUERY (Window1 exact) ===")
                        Log.d("ReportsActivity", "Reports querying transactions from $startDateStr to $endDateStr")

                        // Use the EXACT same DAO method as Window1
                        val result = transactionDao.getTransactionsByDateRange(startDateStr, endDateStr)

                        Log.d("ReportsActivity", "Reports raw result: ${result.size} transactions")

                        // Log the first few like in Window1
                        result.take(3).forEach { transaction ->
                            Log.d("ReportsActivity", "Reports transaction: ${transaction.transactionId} - ${transaction.createdDate}")
                        }

                        result
                    }

                    Log.d("ReportsActivity", "Reports found ${transactions.size} transactions for selected date")

                    // Apply the SAME sorting as Window1 TransactionAdapter
                    val sortedTransactions = transactions.sortedWith { t1, t2 ->
                        Log.d("ReportsActivity", "Reports sorting: ${t1.transactionId} vs ${t2.transactionId}")
                        t2.createdDate.compareTo(t1.createdDate)
                    }

                    Log.d("ReportsActivity", "Reports after sorting: ${sortedTransactions.size} transactions")
                    sortedTransactions.take(3).forEach { transaction ->
                        Log.d("ReportsActivity", "Reports sorted: ${transaction.transactionId} - ${transaction.createdDate}")
                    }

                    val report = calculateSalesReport(sortedTransactions)
                    updateUI(report)

                } else {
                    Log.e("ReportsActivity", "Current store ID is null!")
                    Toast.makeText(this@ReportsActivity, "Store ID not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error loading transactions exactly like Window1", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // STEP 4: Copy the EXACT sorting extension from TransactionAdapter
    private fun String.toTimestampForSorting(): Long {
        return try {
            if (this.isEmpty()) return 0L
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val date = format.parse(this)
            date?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    // STEP 5: For debugging - let's see what your showTransactionListDialog would actually return
    private fun debugWhatDialogWouldShow() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("ReportsActivity", "=== DEBUG: WHAT DIALOG WOULD SHOW ===")

                    // Get current store ID like dialog does
                    val currentStoreId = SessionManager.getCurrentUser()?.storeid
                    Log.d("ReportsActivity", "Store ID: $currentStoreId")

                    if (currentStoreId != null) {
                        // Create the EXACT same date range as your dialog for July 16
                        val july16Start = Calendar.getInstance().apply {
                            set(2025, Calendar.JULY, 16, 0, 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val july16End = Calendar.getInstance().apply {
                            time = july16Start.time
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }

                        val startDateStr = formatDateToString(july16Start.time)
                        val endDateStr = formatDateToString(july16End.time)

                        Log.d("ReportsActivity", "Dialog would query: '$startDateStr' to '$endDateStr'")

                        val dialogResults = transactionDao.getTransactionsByDateRange(startDateStr, endDateStr)
                        Log.d("ReportsActivity", "Dialog would get: ${dialogResults.size} transactions")

                        // Show what dialog would display
                        dialogResults.take(5).forEach { transaction ->
                            Log.d("ReportsActivity", "Dialog would show: ${transaction.transactionId} - ${transaction.createdDate} - ${transaction.receiptId}")
                        }

                        // Check if dialog does any additional filtering
                        val filteredForStore = dialogResults.filter { it.store == currentStoreId }
                        Log.d("ReportsActivity", "After store filter: ${filteredForStore.size} transactions")

                        val filteredForStatus = filteredForStore.filter { it.transactionStatus == 1 }
                        Log.d("ReportsActivity", "After status filter: ${filteredForStatus.size} transactions")

                    } else {
                        Log.e("ReportsActivity", "Store ID is null - dialog wouldn't work either!")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error debugging dialog", e)
            }
        }
    }

    // Main auto-sync monitoring function
    private fun startAutoSyncMonitoring() {
        autoSyncJob = lifecycleScope.launch {
            try {
                Log.d("ReportsActivity", "Starting auto-sync monitoring...")

                createAutoSyncFlow().collectLatest { hasChanges ->
                    if (hasChanges && isAutoSyncEnabled) {
                        Log.d("ReportsActivity", "Changes detected, refreshing reports...")
                        withContext(Dispatchers.Main) {
                            refreshReportsFromAPI()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error in auto-sync monitoring", e)
            }
        }
    }

    // Create a flow that periodically checks for API changes
    private fun createAutoSyncFlow(): Flow<Boolean> = flow {
        while (isAutoSyncEnabled) {
            try {
                val hasChanges = checkForAPIChanges()
                emit(hasChanges)
                delay(autoSyncIntervalMs)
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for API changes", e)
                emit(false)
                delay(autoSyncIntervalMs * 2) // Wait longer on error
            }
        }
    }

    // Check if there are changes in the API data
    private suspend fun checkForAPIChanges(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return@withContext false

                // Get current local transaction count
                val currentLocalCount = transactionDao.getTransactionsByStore(currentStoreId).size

                // Check if count changed from last time we checked
                val hasChanges = currentLocalCount != lastKnownTransactionCount

                if (hasChanges) {
                    Log.d("AutoSync", "Transaction count changed: $lastKnownTransactionCount -> $currentLocalCount")
                    lastKnownTransactionCount = currentLocalCount
                }

                return@withContext hasChanges

            } catch (e: Exception) {
                Log.e("AutoSync", "Error checking for changes: ${e.message}")
                false
            }
        }
    }
    // Get transaction count from API using your existing TransactionSyncApi
    private suspend fun getTransactionCountFromAPI(storeId: String): Int {
        return try {
            val response = RetrofitClient.transactionSyncApi.getTransactionSummaries(storeId)
            if (response.isSuccessful) {
                response.body()?.size ?: 0
            } else {
                Log.e("AutoSync", "API response failed: ${response.code()}")
                0
            }
        } catch (e: Exception) {
            Log.e("AutoSync", "Error getting transaction count from API", e)
            0
        }
    }

    // Get transaction sum from API for data integrity
    private suspend fun getTransactionSumFromAPI(storeId: String): Double {
        return try {
            val response = RetrofitClient.transactionSyncApi.getTransactionSummaries(storeId)
            if (response.isSuccessful) {
                response.body()?.sumOf { it.netAmount ?: 0.0 } ?: 0.0
            } else {
                0.0
            }
        } catch (e: Exception) {
            Log.e("AutoSync", "Error getting transaction sum from API", e)
            0.0
        }
    }

    // Get local transaction count
    private suspend fun getLocalTransactionCount(): Int {
        return try {
            val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return 0
            transactionDao.getTransactionsByStore(currentStoreId).size
        } catch (e: Exception) {
            Log.e("AutoSync", "Error getting local transaction count", e)
            0
        }
    }

    // Get local transaction sum
    private suspend fun getLocalTransactionSum(): Double {
        return try {
            val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return 0.0
            transactionDao.getTransactionsByStore(currentStoreId).sumOf { it.netAmount }
        } catch (e: Exception) {
            Log.e("AutoSync", "Error getting local transaction sum", e)
            0.0
        }
    }
    fun String.convertApiDateToSimple(): String {
        return try {
            val apiFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)
            apiFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = apiFormat.parse(this)

            val simpleFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            simpleFormat.timeZone = TimeZone.getTimeZone("Asia/Manila")
            simpleFormat.format(date ?: Date())
        } catch (e: Exception) {
            Log.w("DateConverter", "Could not convert date: $this")
            this
        }
    }


    // Refresh reports from API and update UI
    private fun convertAllTransactionDates() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    Log.d("DateConverter", "Starting comprehensive date conversion...")

                    val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return@withContext

                    // Step 1: Convert Transaction Summaries
                    val allTransactions = transactionDao.getTransactionsByStore(currentStoreId)
                    val convertedTransactions = mutableListOf<TransactionSummary>()

                    Log.d("DateConverter", "Converting ${allTransactions.size} transaction summaries...")

                    allTransactions.forEach { transaction ->
                        val originalDate = transaction.createdDate

                        // Check if date needs conversion (contains 'T' and 'Z')
                        if (originalDate.contains("T") && originalDate.contains("Z")) {
                            try {
                                val convertedDate = originalDate.convertApiDateToSimple()

                                if (convertedDate != originalDate) {
                                    val updatedTransaction = transaction.copy(
                                        createdDate = convertedDate,
                                        // FIXED: Handle potential null fields during copy
                                        paymentMethod = transaction.paymentMethod ?: "",
                                        customerAccount = transaction.customerAccount ?: "",
                                        staff = transaction.staff ?: "",
                                        comment = transaction.comment ?: "",
                                        currency = transaction.currency ?: "PHP",
                                        receiptEmail = transaction.receiptEmail ?: "",
                                        markupDescription = transaction.markupDescription ?: "",
                                        discountType = transaction.discountType ?: "",
                                        customerName = transaction.customerName ?: "",
                                        refundReceiptId = transaction.refundReceiptId ?: "",
                                        zReportId = transaction.zReportId ?: "",
                                        storeKey = transaction.storeKey ?: "",
                                        storeSequence = transaction.storeSequence ?: ""
                                    )
                                    convertedTransactions.add(updatedTransaction)
                                    Log.d("DateConverter", "Summary converted: $originalDate -> $convertedDate")
                                }
                            } catch (e: Exception) {
                                Log.e("DateConverter", "Error converting summary date for transaction ${transaction.transactionId}: ${e.message}")
                            }
                        }
                    }

                    if (convertedTransactions.isNotEmpty()) {
                        transactionDao.insertTransactionSummaries(convertedTransactions)
                        Log.d("DateConverter", "Updated ${convertedTransactions.size} transaction summaries with converted dates")
                    }

                    // Step 2: Convert Transaction Records
                    Log.d("DateConverter", "Converting transaction records...")

                    val allRecords = transactionDao.getAllTransactionRecords()
                    val convertedRecords = mutableListOf<TransactionRecord>()

                    Log.d("DateConverter", "Converting ${allRecords.size} transaction records...")

                    allRecords.forEach { record ->
                        val originalDate = record.createdDate

                        if (originalDate?.contains("T") == true && originalDate.contains("Z")) {
                            try {
                                val convertedDate = originalDate.convertApiDateToSimple()

                                if (convertedDate != originalDate) {
                                    val updatedRecord = record.copy(
                                        createdDate = convertedDate,
                                        // FIXED: Handle potential null fields during copy
                                        name = record.name ?: "",
                                        receiptNumber = record.receiptNumber ?: "",
                                        paymentMethod = record.paymentMethod ?: "",
                                        comment = record.comment ?: "",
                                        receiptId = record.receiptId ?: "",
                                        itemId = record.itemId ?: "",
                                        itemGroup = record.itemGroup ?: "",
                                        customerAccount = record.customerAccount ?: "",
                                        store = record.store ?: "",
                                        staff = record.staff ?: "",
                                        discountOfferId = record.discountOfferId ?: "",
                                        unit = record.unit ?: "PCS",
                                        remarks = record.remarks ?: "",
                                        inventoryBatchId = record.inventoryBatchId ?: "",
                                        inventoryBatchExpiryDate = record.inventoryBatchExpiryDate ?: "",
                                        giftCard = record.giftCard ?: "",
                                        returnTransactionId = record.returnTransactionId ?: "",
                                        creditMemoNumber = record.creditMemoNumber ?: "",
                                        description = record.description ?: "",
                                        storeTaxGroup = record.storeTaxGroup ?: "",
                                        currency = record.currency ?: "PHP",
                                        storeKey = record.storeKey ?: "",
                                        storeSequence = record.storeSequence ?: ""
                                    )
                                    convertedRecords.add(updatedRecord)
                                    Log.d("DateConverter", "Record converted: $originalDate -> $convertedDate")
                                }
                            } catch (e: Exception) {
                                Log.e("DateConverter", "Error converting record date for record ${record.id}: ${e.message}")
                            }
                        }
                    }

                    if (convertedRecords.isNotEmpty()) {
                        transactionDao.insertAll(convertedRecords)
                        Log.d("DateConverter", "Updated ${convertedRecords.size} transaction records with converted dates")
                    }

                    // Summary report
                    val totalConverted = convertedTransactions.size + convertedRecords.size
                    Log.d("DateConverter", "Date conversion complete:")
                    Log.d("DateConverter", "  Transaction summaries: ${convertedTransactions.size}")
                    Log.d("DateConverter", "  Transaction records: ${convertedRecords.size}")
                    Log.d("DateConverter", "  Total converted: $totalConverted")

                } catch (e: Exception) {
                    Log.e("DateConverter", "Error in comprehensive date conversion: ${e.message}", e)
                }
            }
        }
    }
    // UPDATE your refreshReportsFromAPI method to include date conversion
    private fun refreshReportsFromAPI() {
        lifecycleScope.launch {
            try {
                Log.d("AutoSync", "Refreshing reports and converting dates...")

                // First convert all transaction dates
                convertAllTransactionDates()

                // Wait a moment for conversion to complete
                delay(500)

                // Then refresh the UI with converted dates
                loadReportForDateRange()

                Log.d("AutoSync", "Reports refreshed with converted dates")
            } catch (e: Exception) {
                Log.e("AutoSync", "Error refreshing UI: ${e.message}")
            }
        }
    }

    // ADD: Manual date conversion method (you can call this anytime)
    fun convertAllDatesManually() {
        lifecycleScope.launch {
            try {
                showSyncIndicator(true)

                convertAllTransactionDates()

                // Wait for conversion to complete
                delay(1000)

                // Refresh UI to show converted dates
                loadReportForDateRange()

                Toast.makeText(this@ReportsActivity, "All transaction dates converted to simple format", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Error converting dates: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showSyncIndicator(false)
            }
        }
    }

    // Sync transactions from API using your existing infrastructure
    private suspend fun syncTransactionsFromAPI(storeId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("APISync", "Starting fresh API sync for store: $storeId")

                // STEP 1: Clear existing data to prevent duplicates
                Log.d("APISync", "Clearing existing transaction data to prevent duplicates...")
                transactionDao.deleteAllTransactionSummaries()
                transactionDao.deleteAllTransactionRecords()

                // STEP 2: Get fresh data from API
                val summariesResponse = RetrofitClient.transactionSyncApi.getTransactionSummaries(storeId)
                val detailsResponse = RetrofitClient.transactionSyncApi.getTransactionDetails(storeId)

                var syncedCount = 0

                if (summariesResponse.isSuccessful) {
                    val apiSummaries = summariesResponse.body() ?: emptyList()
                    Log.d("APISync", "Received ${apiSummaries.size} summaries from API")

                    // FIXED: Convert dates and handle ALL null fields during sync
                    val convertedSummaries = apiSummaries.map { summary ->
                        summary.copy(
                            createdDate = summary.createdDate?.convertApiDateToSimple() ?: getCurrentDateString(),
                            syncStatus = true,
                            // FIXED: Handle ALL nullable String fields
                            paymentMethod = summary.paymentMethod ?: "",
                            customerAccount = summary.customerAccount ?: "",
                            staff = summary.staff ?: "",
                            comment = summary.comment ?: "",
                            currency = summary.currency ?: "PHP",
                            receiptEmail = summary.receiptEmail ?: "",
                            markupDescription = summary.markupDescription ?: "",
                            discountType = summary.discountType ?: "",
                            customerName = summary.customerName ?: "",
                            refundReceiptId = summary.refundReceiptId ?: "",
                            zReportId = summary.zReportId ?: "",
                            storeKey = summary.storeKey ?: "",
                            storeSequence = summary.storeSequence ?: ""
                        )
                    }

                    transactionDao.insertTransactionSummaries(convertedSummaries)
                    syncedCount += convertedSummaries.size

                    Log.d("APISync", "Synced ${convertedSummaries.size} summaries with converted dates")
                }

                if (detailsResponse.isSuccessful) {
                    val apiDetails = detailsResponse.body() ?: emptyList()
                    Log.d("APISync", "Received ${apiDetails.size} details from API")

                    // FIXED: Convert dates and handle ALL null fields during sync
                    val convertedDetails = apiDetails.map { record ->
                        record.copy(
                            createdDate = record.createdDate?.convertApiDateToSimple() ?: getCurrentDateString(),
                            syncStatusRecord = true,
                            // FIXED: Handle ALL nullable String fields
                            name = record.name ?: "",
                            receiptNumber = record.receiptNumber ?: "",
                            paymentMethod = record.paymentMethod ?: "",
                            comment = record.comment ?: "",
                            receiptId = record.receiptId ?: "",
                            itemId = record.itemId ?: "",
                            itemGroup = record.itemGroup ?: "",
                            customerAccount = record.customerAccount ?: "",
                            store = record.store ?: "",
                            staff = record.staff ?: "",
                            discountOfferId = record.discountOfferId ?: "",
                            unit = record.unit ?: "PCS",
                            remarks = record.remarks ?: "",
                            inventoryBatchId = record.inventoryBatchId ?: "",
                            inventoryBatchExpiryDate = record.inventoryBatchExpiryDate ?: "",
                            giftCard = record.giftCard ?: "",
                            returnTransactionId = record.returnTransactionId ?: "",
                            creditMemoNumber = record.creditMemoNumber ?: "",
                            description = record.description ?: "",
                            storeTaxGroup = record.storeTaxGroup ?: "",
                            currency = record.currency ?: "PHP",
                            storeKey = record.storeKey ?: "",
                            storeSequence = record.storeSequence ?: ""
                        )
                    }

                    transactionDao.insertAll(convertedDetails)
                    syncedCount += convertedDetails.size

                    Log.d("APISync", "Synced ${convertedDetails.size} records with converted dates")
                }

                Log.d("APISync", "Successfully synced $syncedCount total items from API")
                return@withContext true

            } catch (e: Exception) {
                Log.e("APISync", "Error syncing from API: ${e.message}", e)
                return@withContext false
            }
        }
    }

    // Helper function to create a safe TransactionSummary with null handling
    private fun createSafeTransactionSummary(apiSummary: TransactionSummary): TransactionSummary {
        return TransactionSummary(
            transactionId = apiSummary.transactionId,
            type = apiSummary.type,
            receiptId = apiSummary.receiptId,
            store = apiSummary.store,
            staff = apiSummary.staff,
            customerAccount = apiSummary.customerAccount,
            netAmount = apiSummary.netAmount,
            costAmount = apiSummary.costAmount,
            grossAmount = apiSummary.grossAmount,
            partialPayment = apiSummary.partialPayment,
            transactionStatus = apiSummary.transactionStatus,
            discountAmount = apiSummary.discountAmount,
            customerDiscountAmount = apiSummary.customerDiscountAmount,
            totalDiscountAmount = apiSummary.totalDiscountAmount,
            numberOfItems = apiSummary.numberOfItems,
            refundReceiptId = apiSummary.refundReceiptId ?: "",
            currency = apiSummary.currency ?: "PHP",
            zReportId = apiSummary.zReportId ?: "",
            createdDate = apiSummary.createdDate,
            priceOverride = apiSummary.priceOverride,
            comment = apiSummary.comment ?: "",
            receiptEmail = apiSummary.receiptEmail ?: "",
            markupAmount = apiSummary.markupAmount,
            markupDescription = apiSummary.markupDescription ?: "",
            taxIncludedInPrice = apiSummary.taxIncludedInPrice,
            windowNumber = apiSummary.windowNumber,
            totalAmountPaid = apiSummary.totalAmountPaid,
            changeGiven = apiSummary.changeGiven,
            paymentMethod = apiSummary.paymentMethod ?: "",
            customerName = apiSummary.customerName ?: "",
            vatAmount = apiSummary.vatAmount,
            vatExemptAmount = apiSummary.vatExemptAmount,
            vatableSales = apiSummary.vatableSales,
            discountType = apiSummary.discountType ?: "",
            gCash = apiSummary.gCash,
            payMaya = apiSummary.payMaya,
            cash = apiSummary.cash,
            card = apiSummary.card,
            loyaltyCard = apiSummary.loyaltyCard,
            charge = apiSummary.charge,
            foodpanda = apiSummary.foodpanda,
            grabfood = apiSummary.grabfood,
            representation = apiSummary.representation,
            storeKey = apiSummary.storeKey ?: "",
            storeSequence = apiSummary.storeSequence ?: "",
            syncStatus = true // Mark as synced
        )
    }

    // Helper function to create a safe TransactionRecord with null handling
    private fun createSafeTransactionRecord(apiDetail: TransactionRecord): TransactionRecord {
        return TransactionRecord(
            id = apiDetail.id,
            transactionId = apiDetail.transactionId ?: "",
            name = apiDetail.name ?: "",
            price = apiDetail.price,
            quantity = apiDetail.quantity,
            subtotal = apiDetail.subtotal,
            vatRate = apiDetail.vatRate,
            vatAmount = apiDetail.vatAmount,
            discountRate = apiDetail.discountRate,
            discountAmount = apiDetail.discountAmount,
            total = apiDetail.total,
            receiptNumber = apiDetail.receiptNumber ?: "",
            timestamp = apiDetail.timestamp,
            paymentMethod = apiDetail.paymentMethod ?: "",
            ar = apiDetail.ar,
            windowNumber = apiDetail.windowNumber,
            partialPaymentAmount = apiDetail.partialPaymentAmount,
            comment = apiDetail.comment ?: "",
            lineNum = apiDetail.lineNum,
            receiptId = apiDetail.receiptId,
            itemId = apiDetail.itemId,
            itemGroup = apiDetail.itemGroup,
            netPrice = apiDetail.netPrice,
            costAmount = apiDetail.costAmount,
            netAmount = apiDetail.netAmount,
            grossAmount = apiDetail.grossAmount,
            customerAccount = apiDetail.customerAccount,
            store = apiDetail.store,
            priceOverride = apiDetail.priceOverride,
            staff = apiDetail.staff,
            discountOfferId = apiDetail.discountOfferId,
            lineDiscountAmount = apiDetail.lineDiscountAmount,
            lineDiscountPercentage = apiDetail.lineDiscountPercentage,
            customerDiscountAmount = apiDetail.customerDiscountAmount,
            unit = apiDetail.unit,
            unitQuantity = apiDetail.unitQuantity,
            unitPrice = apiDetail.unitPrice,
            taxAmount = apiDetail.taxAmount,
            createdDate = apiDetail.createdDate ?: getCurrentDateString(),
            remarks = apiDetail.remarks,
            inventoryBatchId = apiDetail.inventoryBatchId,
            inventoryBatchExpiryDate = apiDetail.inventoryBatchExpiryDate,
            giftCard = apiDetail.giftCard,
            returnTransactionId = apiDetail.returnTransactionId,
            returnQuantity = apiDetail.returnQuantity,
            creditMemoNumber = apiDetail.creditMemoNumber,
            taxIncludedInPrice = apiDetail.taxIncludedInPrice,
            description = apiDetail.description,
            returnLineId = apiDetail.returnLineId,
            priceUnit = apiDetail.priceUnit,
            netAmountNotIncludingTax = apiDetail.netAmountNotIncludingTax,
            storeTaxGroup = apiDetail.storeTaxGroup,
            currency = apiDetail.currency,
            taxExempt = apiDetail.taxExempt,
            storeKey = apiDetail.storeKey ?: "",
            storeSequence = apiDetail.storeSequence ?: "",
            syncStatusRecord = true // Mark as synced
        )
    }
    private fun getCurrentDateString(): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            format.timeZone = TimeZone.getTimeZone("Asia/Manila")
            format.format(Date())
        } catch (e: Exception) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        }
    }

    // SIMPLIFIED VERSION: If you just want to check for changes without full sync
    private suspend fun syncTransactionsFromAPISimplified(storeId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Just refresh the current report data without complex conversion
                // This approach reloads data that's already in your local database
                Log.d("AutoSync", "Refreshing local data for store: $storeId")

                // You can add a simple API call to verify server connection
                val response = RetrofitClient.transactionSyncApi.getTransactionSummaries(storeId)

                if (response.isSuccessful) {
                    Log.d("AutoSync", "API connection verified, data is current")
                    return@withContext true
                } else {
                    Log.w("AutoSync", "API response not successful: ${response.code()}")
                    return@withContext false
                }

            } catch (e: Exception) {
                Log.e("AutoSync", "Error in simplified sync: ${e.message}", e)
                false
            }
        }
    }
    // Show/hide sync indicator in UI
    private fun showSyncIndicator(show: Boolean) {
        // You can add a small sync indicator to your UI
        // For example, change the title or show a small progress indicator
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                if (show) {
                    // Optional: Add a sync indicator to your UI
                    // binding.syncIndicator?.visibility = View.VISIBLE
                    Log.d("AutoSync", "🔄 Syncing data...")
                } else {
                    // binding.syncIndicator?.visibility = View.GONE
                }
            }
        }
    }

    // Show brief success message
    private fun showAutoSyncSuccess() {
        // Optional: Show a brief, non-intrusive success message
        // Toast.makeText(this, "📊 Reports updated", Toast.LENGTH_SHORT).show()
    }

    // Manual refresh method (you can add a button for this)
    fun manualRefresh() {
        lifecycleScope.launch {
            try {
                Log.d("ManualRefresh", "Starting manual refresh with API sync and date conversion")
                showSyncIndicator(true)

                val currentStoreId = SessionManager.getCurrentUser()?.storeid
                if (currentStoreId != null) {
                    // Step 1: Clear old data (optional - remove if you want to keep old data)
                     withContext(Dispatchers.IO) {
                         transactionDao.deleteAllTransactions()
                         transactionDao.deleteAllTransactionSummaries()
                     }

                    // Step 2: Fetch fresh data from API
                    val success = syncTransactionsFromAPI(currentStoreId)

                    if (success) {
                        // Step 3: Convert all dates
                        convertAllTransactionDates()

                        // Step 4: Wait for conversion to complete
                        delay(1000)

                        // Step 5: Refresh UI
                        loadReportForDateRange()

                        Toast.makeText(this@ReportsActivity, "Data refreshed and dates converted", Toast.LENGTH_SHORT).show()
                    } else {
                        // If API sync fails, just convert existing data and refresh UI
                        convertAllTransactionDates()
                        delay(500)
                        loadReportForDateRange()
                        Toast.makeText(this@ReportsActivity, "Local data refreshed and dates converted", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReportsActivity, "No store ID found", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@ReportsActivity, "Error refreshing: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                showSyncIndicator(false)
            }
        }
    }

    // Control auto-sync (you can add toggle buttons for this)
    fun toggleAutoSync(enabled: Boolean) {
        isAutoSyncEnabled = enabled

        if (enabled) {
            startAutoSyncMonitoring()
            Toast.makeText(this, "Auto-sync enabled", Toast.LENGTH_SHORT).show()
        } else {
            stopAutoSync()
            Toast.makeText(this, "Auto-sync disabled", Toast.LENGTH_SHORT).show()
        }
    }

    // Stop auto-sync
    private fun stopAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    // STEP 5: Modify your existing setupButtons method to include auto-sync controls
    private fun setupButtons() {
        binding.xreadButton.setOnClickListener {
            performXRead()
        }

        binding.cashfundButton.setOnClickListener {
            showCashFundDialog()
        }

        binding.pulloutButton.setOnClickListener {
            showPulloutCashFundDialog()
        }

        binding.tenderButton.setOnClickListener {
            showTenderDeclarationDialog()
        }

        binding.zreadButton.setOnClickListener {
            checkForExistingZRead()
        }

        binding.itemsalesButton.setOnClickListener {
            val selectedDateRange = if (isSameDay(startDate, endDate)) {
                formatDateForDisplay(endDate)
            } else {
                "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${formatDateForDisplay(endDate)}"
            }
            showItemSalesDialog(selectedDateRange)
        }

        binding.reprintZreadButton.setOnClickListener {
            showReprintZReadSelection()
        }

        // Keep long press for manual refresh
        binding.reprintZreadButton.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("Manual Refresh")
                .setMessage("Refresh reports with latest local data?")
                .setPositiveButton("Refresh") { _, _ ->
                    manualRefreshOnly()
                }
                .setNegativeButton("Cancel", null)
                .show()
            true
        }

        // ADD THIS - Long press on title for manual refresh and auto-sync toggle
        binding.root.findViewById<TextView>(R.id.reportsTitle)?.setOnLongClickListener {
            showAutoSyncMenu()
            true
        } ?: run {
            // If you don't have a title TextView, add long press to any suitable button
            binding.xreadButton.setOnLongClickListener {
                showAutoSyncMenu()
                true
            }
        }
    }
    private fun debugDataDuplication() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val currentStoreId = SessionManager.getCurrentUser()?.storeid ?: return@withContext
                    val transactions = transactionDao.getTransactionsByStore(currentStoreId)

                    Log.d("DEBUG_DUPLICATION", "=== CHECKING FOR DUPLICATES ===")
                    Log.d("DEBUG_DUPLICATION", "Total transactions: ${transactions.size}")

                    // Group by transaction ID to find duplicates
                    val groupedById = transactions.groupBy { it.transactionId }
                    val duplicates = groupedById.filter { it.value.size > 1 }

                    if (duplicates.isNotEmpty()) {
                        Log.w("DEBUG_DUPLICATION", "Found ${duplicates.size} duplicate transaction IDs:")
                        duplicates.forEach { (id, transactions) ->
                            Log.w("DEBUG_DUPLICATION", "  ID: $id appears ${transactions.size} times")
                        }
                    } else {
                        Log.d("DEBUG_DUPLICATION", "No duplicate transaction IDs found")
                    }

                    // Check if there are recent duplicates in database
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val todayTransactions = transactionDao.getTransactionsByDate(today)
                    Log.d("DEBUG_DUPLICATION", "Today's transactions: ${todayTransactions.size}")

                } catch (e: Exception) {
                    Log.e("DEBUG_DUPLICATION", "Error checking duplicates", e)
                }
            }
        }
    }


    // STEP 6: Add this method to show auto-sync controls
    private fun showAutoSyncMenu() {
        val options = arrayOf(
            "Manual Refresh Now",
            "Fresh API Sync", // NEW: Full API sync
            if (isAutoSyncEnabled) "Disable Auto-Sync" else "Enable Auto-Sync",
            "Convert Date Formats",
            "Cancel"
        )
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)

//        AlertDialog.Builder(this)
            .setTitle("Sync Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> manualRefresh()
                    1 -> freshApiSync() // NEW: Fresh API sync
                    2 -> toggleAutoSync(!isAutoSyncEnabled)
                    3 -> showDateConverterDialog()
                    4 -> { /* Cancel */ }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
//    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
//
//    dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//    dialog.show()
    private fun freshApiSync() {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Fresh API Sync")
            .setMessage("This will:\n• Clear existing data\n• Fetch latest data from API\n• Convert all dates\n\nThis may take a moment.")
            .setPositiveButton("Sync Now") { _, _ ->
                lifecycleScope.launch {
                    try {
                        showSyncIndicator(true)

                        val currentStoreId = SessionManager.getCurrentUser()?.storeid
                        if (currentStoreId != null) {
                            withContext(Dispatchers.Main) {
                                showLoading("Clearing old data...")
                            }

                            delay(500)

                            withContext(Dispatchers.Main) {
                                showLoading("Fetching fresh data from API...")
                            }

                            val success = syncTransactionsFromAPI(currentStoreId)

                            if (success) {
                                withContext(Dispatchers.Main) {
                                    showLoading("Converting date formats...")
                                }

                                delay(1000)

                                withContext(Dispatchers.Main) {
                                    showLoading("Refreshing reports...")
                                }

                                loadReportForDateRange()

                                Toast.makeText(this@ReportsActivity, "Fresh data synced successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@ReportsActivity, "API sync failed", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@ReportsActivity, "No store ID found", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("FreshSync", "Error in fresh sync: ${e.message}", e)
                        Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        showSyncIndicator(false)
                        hideLoading()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    // ADD: Helper method to show loading overlay
    private fun showLoading(message: String) {
        // If you have a loading overlay, update it here
        // For now, just log the progress
        Log.d("FreshSync", "Progress: $message")
    }

    private fun hideLoading() {
        // Hide loading overlay if you have one
        Log.d("FreshSync", "Loading complete")
    }

    private fun showDateConverterDialog() {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Convert Date Formats")
            .setMessage("Convert all transaction dates from API format to simple format?\n\n" +
                    "From: 2025-07-01T01:27:32.000000Z\n" +
                    "To: 2025-07-01 09:27:32\n\n" +
                    "This will update BOTH transaction summaries AND transaction records in your local database.")
            .setPositiveButton("Convert All Dates") { _, _ ->
                convertAllTransactionDataComprehensive()
            }
            .setNeutralButton("Convert & Refresh") { _, _ ->
                lifecycleScope.launch {
                    try {
                        showSyncIndicator(true)

                        // Convert dates for both tables
                        convertAllTransactionDates()

                        // Wait for conversion
                        delay(1500)

                        // Refresh reports
                        loadReportForDateRange()

                        Toast.makeText(
                            this@ReportsActivity,
                            "All transaction dates converted and reports refreshed",
                            Toast.LENGTH_LONG
                        ).show()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        showSyncIndicator(false)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    // ADD: Comprehensive conversion method
    private fun convertAllTransactionDataComprehensive() {
        lifecycleScope.launch {
            try {
                showSyncIndicator(true)

                convertAllTransactionDates()

                // Wait a bit longer for both tables to be processed
                delay(2000)

                Toast.makeText(
                    this@ReportsActivity,
                    "All transaction summaries and records converted to simple format",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@ReportsActivity,
                    "Error converting dates: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showSyncIndicator(false)
            }
        }
    }

    // REPLACE your existing convertAllTransactionData method
    private fun convertAllTransactionData() {
        convertAllTransactionDataComprehensive()
    }

    // ADD the conversion methods to your ReportsActivity:

    private suspend fun handlePastDateXRead(selectedDate: Date) {
        val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(selectedDate)
        val existingZRead = withContext(Dispatchers.IO) {
            zReadDao.getZReadByDate(selectedDateString)
        }

        // Get transactions for the selected date using consistent method
        val transactions = withContext(Dispatchers.IO) {
            getTransactionsForDate(selectedDate)
        }

        withContext(Dispatchers.Main) {
            if (existingZRead != null) {
                // Show existing Z-Read reprint option
                showExistingZReadOption(existingZRead, transactions, selectedDate)
            } else if (transactions.isNotEmpty()) {
                // Check if transactions have Z-Report IDs from automatic Z-Read
                val transactionsWithZRead = transactions.filter { !it.zReportId.isNullOrEmpty() }

                if (transactionsWithZRead.isNotEmpty()) {
                    showAutomaticZReadOption(transactionsWithZRead, selectedDate)
                } else {
                    showNoZReadAvailable(transactions, selectedDate)
                }
            } else {
                showNoTransactionsDialog("X-Read", selectedDate)
            }
        }
    }
    private fun showAutomaticZReadOption(
        transactionsWithZRead: List<TransactionSummary>,
        selectedDate: Date
    ) {
        val firstZReportId = transactionsWithZRead.first().zReportId

        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("CHECK ZREAD")
            .setMessage(
                "X-Read is only available for today's transactions.\n\n" +
                        "However, automatic Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}:\n\n" +
                        "Z-Report ID: $firstZReportId\n" +
                        "Transactions: ${transactionsWithZRead.size}\n" +
                        "Total Amount: ₱${String.format("%.2f", transactionsWithZRead.sumOf { it.netAmount })}\n\n" +
                        "Would you like to view and reprint the Z-Read instead?"
            )
            .setPositiveButton("View Z-Read") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val tenderDeclaration = withContext(Dispatchers.IO) {
                            tenderDeclarationDao.getLatestTenderDeclaration()
                        }
                        showZReadPreviewDialog(transactionsWithZRead, firstZReportId!!, tenderDeclaration)
                    } catch (e: Exception) {
                        Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    private fun showNoZReadAvailable(
        transactions: List<TransactionSummary>,
        selectedDate: Date
    ) {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("X-Read Not Available")
            .setMessage(
                "X-Read is only available for today's transactions.\n\n" +
                        "No Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}.\n\n" +
                        "Transactions: ${transactions.size}\n" +
                        "Total Amount: ₱${String.format("%.2f", transactions.sumOf { it.netAmount })}"
            )
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    // Add these missing methods to your ReportsActivity class

    // FIXED: Consistent method to get today's transaction data


    // FIXED: Consistent method to get transactions for any date
    private suspend fun getTransactionsForDate(date: Date): List<TransactionSummary> {
        return withContext(Dispatchers.IO) {
            val dateStart = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            val dateEnd = Calendar.getInstance().apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.time

            transactionDao.getTransactionsByDateRange(
                formatDateToString(dateStart),
                formatDateToString(dateEnd)
            ).filter { it.transactionStatus == 1 }
        }
    }

    // FIXED: Handle past date X-Read requests


    // FIXED: Helper methods for consistent dialog handling
    private fun showNoTransactionsDialog(reportType: String, date: Date? = null) {
        val dateStr = if (date != null) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
        } else {
            "today"
        }

        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("No Transactions")
            .setMessage("No transactions found for $dateStr. $reportType cannot be generated.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showExistingZReadOption(
        existingZRead: ZRead,
        transactions: List<TransactionSummary>,
        selectedDate: Date
    ) {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("CHECK ZREAD")
            .setMessage(
                "X-Read is only available for today's transactions.\n\n" +
                        "However, Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}:\n\n" +
                        "Z-Report ID: ${existingZRead.zReportId}\n" +
                        "Date: ${existingZRead.date}\n" +
                        "Time: ${existingZRead.time}\n" +
                        "Transactions: ${existingZRead.totalTransactions}\n" +
                        "Amount: ₱${String.format("%.2f", existingZRead.totalAmount)}\n\n" +
                        "Would you like to view and reprint the Z-Read instead?"
            )
            .setPositiveButton("View Z-Read") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val tenderDeclaration = withContext(Dispatchers.IO) {
                            tenderDeclarationDao.getLatestTenderDeclaration()
                        }
                        showZReadPreviewDialog(transactions, existingZRead.zReportId, tenderDeclaration)
                    } catch (e: Exception) {
                        Toast.makeText(this@ReportsActivity, "Error loading Z-Read: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }



    // FIXED: Consistent method to get transactions for any date

    // FIXED: Updated Z-Read check method
    private fun checkForExistingZRead() {
        lifecycleScope.launch {
            try {
                // FIXED: For Z-Read, always use the selected date (not current date)
                val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate)
                val existingZRead = withContext(Dispatchers.IO) {
                    zReadDao.getZReadByDate(selectedDateString)
                }

                if (existingZRead != null) {
                    // Show reprint option for existing Z-Read
                    showReprintZReadDialog(existingZRead)
                    return@launch
                }

                // Get transactions for the selected date using consistent method
                val transactions = withContext(Dispatchers.IO) {
                    getTransactionsForDate(endDate)
                }

                if (transactions.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "No completed transactions found for Z-Read generation",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Check for existing Z-Report IDs (from automatic Z-Read)
                val transactionsWithZRead = transactions.filter { !it.zReportId.isNullOrEmpty() }

                if (transactionsWithZRead.isNotEmpty()) {
                    val firstZReportId = transactionsWithZRead.first().zReportId
                    val allSameZReportId = transactionsWithZRead.all { it.zReportId == firstZReportId }

                    if (allSameZReportId && transactionsWithZRead.size == transactions.size) {
                        // All transactions already have the same Z-Report ID
                        showExistingZReadDialog(firstZReportId!!, transactions)
                    } else {
                        // Mixed state - some transactions have Z-Report IDs
                        showMixedZReadStateDialog(transactions, transactionsWithZRead)
                    }
                } else {
                    // No Z-Report IDs found - allow new Z-Read generation
                    showZReadConfirmationDialog(transactions.size)
                }

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for existing Z-Read", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error checking Z-Read status: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
    private fun showExistingZReadDialog(zReportId: String, transactions: List<TransactionSummary>) {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Z-Read Already Exists")
            .setMessage("Z-Read for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)} already exists.\n\n" +
                    "Z-Report ID: $zReportId\n" +
                    "Transactions: ${transactions.size}\n" +
                    "Total Amount: ₱${String.format("%.2f", transactions.sumOf { it.netAmount })}\n\n" +
                    "Would you like to view and reprint it?")
            .setPositiveButton("View & Reprint") { _, _ ->
                // FIXED: Show preview instead of direct print
                lifecycleScope.launch {
                    try {
                        val tenderDeclaration = withContext(Dispatchers.IO) {
                            tenderDeclarationDao.getLatestTenderDeclaration()
                        }

                        showZReadPreviewDialog(transactions, zReportId, tenderDeclaration)

                    } catch (e: Exception) {
                        Log.e("ReportsActivity", "Error showing Z-Read preview", e)
                        Toast.makeText(
                            this@ReportsActivity,
                            "Error showing Z-Read preview: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showMixedZReadStateDialog(
        allTransactions: List<TransactionSummary>,
        transactionsWithZRead: List<TransactionSummary>
    ) {
        val transactionsWithoutZRead = allTransactions.filter {
            it.zReportId.isNullOrEmpty()
        }

        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Mixed Z-Read State")
            .setMessage("Some transactions for this date already have Z-Read assigned:\n\n" +
                    "Total transactions: ${allTransactions.size}\n" +
                    "Already processed: ${transactionsWithZRead.size}\n" +
                    "Not processed: ${transactionsWithoutZRead.size}\n\n" +
                    "What would you like to do?")
            .setPositiveButton("Process Remaining") { _, _ ->
                if (transactionsWithoutZRead.isNotEmpty()) {
                    showZReadConfirmationDialog(transactionsWithoutZRead.size, transactionsWithoutZRead)
                } else {
                    Toast.makeText(this@ReportsActivity, "No remaining transactions to process", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("View Existing") { _, _ ->
                val firstZReportId = transactionsWithZRead.first().zReportId
                if (firstZReportId != null) {
                    reprintExistingZRead(firstZReportId, transactionsWithZRead)
                }
            }
            .setNeutralButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    private fun setupAutomaticZRead() {
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setupAutoZReadReceiver()

        // Check for exact alarm permission before scheduling
        if (canScheduleExactAlarms()) {
            scheduleAutomaticZRead()
        } else {
            // Request permission or use alternative scheduling
            requestExactAlarmPermission()
        }
    }
    private fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Permission not required for older versions
        }
    }

    // 5. Add this method to request exact alarm permission
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)

                // Show explanation to user
                Toast.makeText(
                    this,
                    "Please allow exact alarms for automatic Z-Read functionality",
                    Toast.LENGTH_LONG
                ).show()
            } catch (e: Exception) {
                Log.e("AutoZRead", "Error requesting exact alarm permission", e)
                // Fallback to inexact alarms
                scheduleInexactAutomaticZRead()
            }
        }
    }

    private fun setupAutoZReadReceiver() {
        autoZReadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "AUTO_ZREAD_CHECK") {
                    performAutomaticZReadCheck()
                }
            }
        }

        val filter = IntentFilter("AUTO_ZREAD_CHECK")
        registerReceiver(autoZReadReceiver, filter)
    }

    private fun scheduleAutomaticZRead() {
        val intent = Intent("AUTO_ZREAD_CHECK")
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set to Philippines timezone
        val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            // Set to next day at midnight Philippines time
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            alarmManager.cancel(pendingIntent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // Use inexact alarm if exact alarms are not allowed
                scheduleInexactAutomaticZRead()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }

            Log.d("AutoZRead", "Scheduled exact automatic Z-Read for midnight Philippines time")

        } catch (e: SecurityException) {
            Log.e("AutoZRead", "SecurityException scheduling exact alarm, falling back to inexact", e)
            scheduleInexactAutomaticZRead()
        } catch (e: Exception) {
            Log.e("AutoZRead", "Error scheduling automatic Z-Read", e)
            scheduleInexactAutomaticZRead()
        }
    }

    // 7. Add this fallback method for inexact alarms
    private fun scheduleInexactAutomaticZRead() {
        val intent = Intent("AUTO_ZREAD_CHECK")
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            1002, // Different request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Set to Philippines timezone
        val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
        val calendar = Calendar.getInstance(philippinesTimeZone).apply {
            // Set to next day at midnight Philippines time
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            alarmManager.cancel(pendingIntent)

            // Use inexact repeating alarm (less precise but doesn't require special permission)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d("AutoZRead", "Scheduled inexact automatic Z-Read for approximately midnight Philippines time")

        } catch (e: Exception) {
            Log.e("AutoZRead", "Error scheduling inexact automatic Z-Read", e)
        }
    }

    private fun performAutomaticZReadCheck() {
        lifecycleScope.launch {
            try {
                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                // FIXED: Use proper date formatting
                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday)

                val existingZRead = withContext(Dispatchers.IO) {
                    zReadDao.getZReadByDate(yesterdayString)
                }

                if (existingZRead != null) {
                    Log.d("AutoZRead", "Z-Read already exists for $yesterdayString")
                    return@launch
                }

                val yesterdayStart = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val yesterdayEnd = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time

                val allTransactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(
                        formatDateToString(yesterdayStart),
                        formatDateToString(yesterdayEnd)
                    ).filter { it.transactionStatus == 1 }
                }

                if (allTransactions.isEmpty()) {
                    Log.d("AutoZRead", "No transactions found for $yesterdayString")
                    return@launch
                }

                val transactionsWithoutZRead = allTransactions.filter {
                    it.zReportId.isNullOrEmpty() || it.zReportId!!.isBlank()
                }

                if (transactionsWithoutZRead.isEmpty()) {
                    Log.d("AutoZRead", "All transactions already have Z-Read for $yesterdayString")
                    return@launch
                }

                Log.d("AutoZRead", "Found ${transactionsWithoutZRead.size} transactions without Z-Read for $yesterdayString")
                generateAutomaticZReadSilent(transactionsWithoutZRead, yesterdayString)

            } catch (e: Exception) {
                Log.e("AutoZRead", "Error in automatic Z-Read check: ${e.message}", e)
            }
        }
    }
    // FIXED: Check yesterday's transactions on startup (improved safety)
    private fun checkYesterdayTransactionsOnStartup() {
        lifecycleScope.launch {
            try {
                Log.d("StartupZRead", "=== CHECKING YESTERDAY'S TRANSACTIONS ===")

                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday)
                Log.d("StartupZRead", "Yesterday date: $yesterdayString")

                // Check if Z-Read already exists for yesterday
                val existingZRead = withContext(Dispatchers.IO) {
                    zReadDao.getZReadByDate(yesterdayString)
                }

                if (existingZRead != null) {
                    Log.d("StartupZRead", "Z-Read already exists for $yesterdayString: ${existingZRead.zReportId}")
                    return@launch
                }

                // Get yesterday's transactions using consistent method
                val yesterdayTransactions = withContext(Dispatchers.IO) {
                    getTransactionsForDate(yesterday)
                }

                Log.d("StartupZRead", "Found ${yesterdayTransactions.size} transactions for $yesterdayString")

                if (yesterdayTransactions.isEmpty()) {
                    Log.d("StartupZRead", "No transactions found for $yesterdayString")
                    return@launch
                }

                // Find transactions without Z-Read ID
                val transactionsWithoutZRead = yesterdayTransactions.filter {
                    it.zReportId.isNullOrEmpty() || it.zReportId!!.isBlank()
                }

                Log.d("StartupZRead", "Transactions without Z-Read: ${transactionsWithoutZRead.size}")

                if (transactionsWithoutZRead.isEmpty()) {
                    Log.d("StartupZRead", "All yesterday's transactions already have Z-Read")
                    return@launch
                }

                Log.i("StartupZRead", "Auto-generating Z-Read for ${transactionsWithoutZRead.size} transactions from $yesterdayString")

                // Generate automatic Z-Read for yesterday's transactions
                generateAutomaticZReadSilent(transactionsWithoutZRead, yesterdayString)

            } catch (e: Exception) {
                Log.e("StartupZRead", "Error checking yesterday's transactions: ${e.message}", e)
            }
        }
    }


    private suspend fun generateAutomaticZReadSilent(
        transactions: List<TransactionSummary>,
        dateString: String
    ) {
        try {
            Log.d("AutoZRead", "=== AUTOMATIC Z-READ FOR $dateString ===")
            Log.d("AutoZRead", "Processing ${transactions.size} transactions")

            // SAFETY CHECK: Ensure we're only processing yesterday's transactions
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -1)
            }.time
            val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday)

            if (dateString != yesterdayString) {
                Log.e("AutoZRead", "SAFETY VIOLATION: Attempting to auto Z-Read for $dateString, but only $yesterdayString is allowed")
                return
            }

            // SAFETY CHECK: Ensure all transactions are from the correct date
            val validTransactions = transactions.filter { transaction ->
                val transactionDate = transaction.createdDate.substring(0, 10)
                val isCorrectDate = transactionDate == dateString

                if (!isCorrectDate) {
                    Log.w("AutoZRead", "Filtering out transaction ${transaction.transactionId} with date $transactionDate")
                }

                isCorrectDate && transaction.transactionStatus == 1
            }

            if (validTransactions.size != transactions.size) {
                Log.w("AutoZRead", "Filtered ${transactions.size - validTransactions.size} invalid transactions")
            }

            if (validTransactions.isEmpty()) {
                Log.d("AutoZRead", "No valid transactions to process for automatic Z-Read")
                return
            }

            // Generate Z-Report ID
            val zReportId = generateZReportId()
            Log.d("AutoZRead", "Generated Z-Report ID: $zReportId for $dateString")

            // Update ONLY the specific valid transactions with Z-Report ID
            withContext(Dispatchers.IO) {
                validTransactions.forEach { transaction ->
                    try {
                        val updatedTransaction = transaction.copy(zReportId = zReportId)
                        transactionDao.updateTransactionSummary(updatedTransaction)
                        Log.d("AutoZRead", "Updated transaction ${transaction.transactionId} with Z-Report ID: $zReportId")
                    } catch (e: Exception) {
                        Log.e("AutoZRead", "Failed to update transaction ${transaction.transactionId}", e)
                    }
                }
            }

            // Save Z-Read record
            withContext(Dispatchers.IO) {
                try {
                    val currentTimeString = getCurrentTime()

                    val zReadRecord = ZRead(
                        zReportId = zReportId,
                        date = dateString,
                        time = currentTimeString,
                        totalTransactions = validTransactions.size,
                        totalAmount = validTransactions.sumOf { it.netAmount }
                    )
                    zReadDao.insert(zReadRecord)
                    Log.d("AutoZRead", "Saved Z-Read record for $dateString")
                } catch (e: Exception) {
                    Log.e("AutoZRead", "Error saving automatic Z-Read record: ${e.message}", e)
                }
            }

            Log.i("AutoZRead", "Successfully completed automatic Z-Read #$zReportId for $dateString")
            Log.d("AutoZRead", "=== END AUTOMATIC Z-READ ===")

        } catch (e: Exception) {
            Log.e("AutoZRead", "Error in automatic Z-Read for $dateString", e)
        }
    }

    private fun manualCheckYesterdayTransactions() {
        lifecycleScope.launch {
            try {
                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                // FIXED: Use proper date formatting
                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yesterday)

                val yesterdayStart = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val yesterdayEnd = Calendar.getInstance(philippinesTimeZone).apply {
                    time = yesterday
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time

                val yesterdayTransactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(
                        formatDateToString(yesterdayStart),
                        formatDateToString(yesterdayEnd)
                    ).filter { it.transactionStatus == 1 }
                }

                val transactionsWithoutZRead = yesterdayTransactions.filter {
                    it.zReportId.isNullOrEmpty() || it.zReportId!!.isBlank()
                }

                withContext(Dispatchers.Main) {
                    if (transactionsWithoutZRead.isEmpty()) {
                        Toast.makeText(this@ReportsActivity, "No pending Z-Read for yesterday", Toast.LENGTH_SHORT).show()
                    } else {
                        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                            .setTitle("Yesterday's Transactions Found")
                            .setMessage("Found ${transactionsWithoutZRead.size} transactions from $yesterdayString without Z-Read.\n\nGenerate automatic Z-Read?")
                            .setPositiveButton("Generate") { _, _ ->
                                lifecycleScope.launch {
                                    generateAutomaticZReadSilent(transactionsWithoutZRead, yesterdayString)
                                    Toast.makeText(this@ReportsActivity, "Automatic Z-Read generated", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .create()

                        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                        dialog.show()
                    }
                }

            } catch (e: Exception) {
                Log.e("ManualZRead", "Error checking yesterday's transactions: ${e.message}", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun updateSpecificTransactionsByIds(
        transactionIds: List<String>,
        zReportId: String
    ): Result<ZReportUpdateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Update only the specific transactions in local database
                transactionIds.forEach { transactionId ->
                    transactionDao.updateTransactionZReportId(transactionId, zReportId)
                }
                Log.d("ZRead", "Updated ${transactionIds.size} specific transactions locally with Z-Report ID: $zReportId")

                // Try to sync with server - you may need to create a new API endpoint for this
                // For now, we'll skip server sync for automatic Z-Read to avoid issues
                Log.d("ZRead", "Skipping server sync for automatic Z-Read to prevent conflicts")

                Result.success(ZReportUpdateResponse(
                    success = true,
                    message = "Local update successful",
                    data = null
                ))

            } catch (e: Exception) {
                Log.e("ZRead", "Error updating specific transactions with Z-Report ID", e)
                Result.failure(e)
            }
        }
    }

    private fun reprintExistingZRead(zReportId: String, transactions: List<TransactionSummary>) {
        lifecycleScope.launch {
            try {
                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                val reportContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = true,
                    zReportId = zReportId,
                    tenderDeclaration = tenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(reportContent)) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Z-Read #$zReportId reprinted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Failed to reprint Z-Read #$zReportId",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error reprinting Z-Read", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error reprinting Z-Read: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun showReprintZReadDialog(zRead: ZRead) {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Z-Read Already Exists")
            .setMessage("Z-Read for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)} already exists.\n\nZ-Report ID: ${zRead.zReportId}\nTime: ${zRead.time}\n\nWould you like to view and reprint it?")
            .setPositiveButton("View & Reprint") { _, _ ->
                // FIXED: Show preview instead of direct print
                lifecycleScope.launch {
                    try {
                        val transactions = withContext(Dispatchers.IO) {
                            transactionDao.getTransactionsByDateRange(
                                formatDateToString(startDate),
                                formatDateToString(endDate)
                            )
                        }

                        val tenderDeclaration = withContext(Dispatchers.IO) {
                            tenderDeclarationDao.getLatestTenderDeclaration()
                        }

                        // Show Z-Read preview dialog
                        showZReadPreviewDialog(transactions, zRead.zReportId, tenderDeclaration)

                    } catch (e: Exception) {
                        Log.e("ReportsActivity", "Error showing Z-Read preview", e)
                        Toast.makeText(
                            this@ReportsActivity,
                            "Error showing Z-Read preview: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun reprintZRead(zRead: ZRead) {
        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(
                        formatDateToString(startDate),
                        formatDateToString(endDate)
                    )
                }

                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                // Show Z-Read preview instead of directly printing
                showZReadPreviewDialog(transactions, zRead.zReportId, tenderDeclaration)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error showing Z-Read preview", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error showing Z-Read preview: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showZReadConfirmationDialog(
        transactionCount: Int,
        specificTransactions: List<TransactionSummary>? = null
    ) {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Z-Read Confirmation")
            .setMessage("Z-Read will generate a final sales report for the selected date range.\n\n" +
                    "Transactions to include: $transactionCount\n" +
                    "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}\n\n" +
                    "Continue?")
            .setPositiveButton("Generate Z-Read") { _, _ ->
                if (specificTransactions != null) {
                    generateZRead(specificTransactions)
                } else {
                    generateZRead()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private suspend fun getCurrentZReadSequence(): Int {
        return withContext(Dispatchers.IO) {
            try {
                val dbMaxId = transactionDao.getMaxZReadId() ?: 0
                val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                val sharedPrefId = sharedPreferences.getInt("lastSequentialNumber", 0)

                maxOf(dbMaxId, sharedPrefId)
            } catch (e: Exception) {
                Log.e("ZRead", "Error getting current Z-Read sequence", e)
                val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                sharedPreferences.getInt("lastSequentialNumber", 0)
            }
        }
    }


    // Add this method to reset Z-Read sequence (for testing or year-end reset)
    private suspend fun resetZReadSequence() {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Reset Z-Read Sequence")
            .setMessage("Are you sure you want to reset the Z-Read sequence number to 0?\n\nCurrent sequence: ${getCurrentZReadSequence()}")
            .setPositiveButton("Reset") { _, _ ->
                val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putInt("lastSequentialNumber", 0)
                    apply()
                }
                Toast.makeText(this@ReportsActivity, "Z-Read sequence reset to 0", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun generateZRead(specificTransactions: List<TransactionSummary>? = null) {
        lifecycleScope.launch {
            try {
                val transactions = if (specificTransactions != null) {
                    specificTransactions
                } else {
                    withContext(Dispatchers.IO) {
                        // Use the selected date for Z-Read generation
                        getTransactionsForDate(endDate)
                    }
                }

                if (transactions.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "No completed transactions found for the selected date",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                val zReportId = generateZReportId()
                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                // Show Z-Read preview with confirmation
                showZReadGenerationPreview(transactions, zReportId, tenderDeclaration)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error generating Z-Read", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error generating Z-Read: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showZReadGenerationPreview(
        transactions: List<TransactionSummary>,
        zReportId: String,
        tenderDeclaration: TenderDeclaration?
    ) {
        lifecycleScope.launch {
            try {
                val zReadContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = true,
                    zReportId = zReportId,
                    tenderDeclaration = tenderDeclaration
                )

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_report_preview, null)
                    val reportTextView = dialogView.findViewById<TextView>(R.id.reportContentTextView)
                    val printButton = dialogView.findViewById<Button>(R.id.printButton)
                    val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)

                    reportTextView.typeface = Typeface.MONOSPACE
                    reportTextView.text = zReadContent

                    // Change button text for generation
                    printButton.text = "Generate & Print Z-Read"

                    val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("Z-Read Generation Preview - #$zReportId")
                        .setView(dialogView)
                        .setCancelable(true)
                        .create()

                    printButton.setOnClickListener {
                        // Actually generate the Z-Read when confirmed
                        lifecycleScope.launch {
                            try {
                                val storeId = SessionManager.getCurrentUser()?.storeid
                                if (storeId != null) {
                                    val updateResult = updateSpecificTransactionsWithZReportId(storeId, zReportId, transactions)
                                    if (updateResult.isSuccess) {
                                        Log.d("ZRead", "Successfully updated transactions with Z-Report ID: $zReportId")
                                    } else {
                                        Log.w("ZRead", "Failed to sync Z-Report ID to server, but continuing with local update")
                                    }
                                }

                                if (bluetoothPrinterHelper.printGenericReceipt(zReadContent)) {
                                    saveZReadRecord(zReportId, transactions)
                                    Toast.makeText(
                                        this@ReportsActivity,
                                        "Z-Read #$zReportId completed successfully",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    loadReportForDateRange()
                                } else {
                                    Toast.makeText(
                                        this@ReportsActivity,
                                        "Failed to print Z-Read #$zReportId",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this@ReportsActivity,
                                    "Error generating Z-Read: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        dialog.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        dialog.dismiss()
                    }

                    dialog.window?.apply {
                        setBackgroundDrawableResource(R.drawable.dialog_background)
                        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    }

                    dialog.show()
                }

            } catch (e: Exception) {
                Log.e("ZRead", "Error generating Z-Read preview", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error generating Z-Read preview: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private suspend fun updateSpecificTransactionsWithZReportId(
        storeId: String,
        zReportId: String,
        transactions: List<TransactionSummary>
    ): Result<ZReportUpdateResponse> {
        return withContext(Dispatchers.IO) {
            try {
                // Update only the specific transactions in local database
                transactions.forEach { transaction ->
                    val updatedTransaction = transaction.copy(zReportId = zReportId)
                    transactionDao.updateTransactionSummary(updatedTransaction)
                }
                Log.d("ZRead", "Updated ${transactions.size} specific transactions locally with Z-Report ID: $zReportId")

                // For manual Z-Read, sync with server
                // For automatic Z-Read, we can skip server sync to avoid conflicts
                val serverUpdateResult = try {
                    transactionRepository.updateTransactionsZReport(storeId, zReportId)
                } catch (e: Exception) {
                    Log.w("ZRead", "Server sync failed, continuing with local update: ${e.message}")
                    Result.success(ZReportUpdateResponse(
                        success = true,
                        message = "Local update successful, server sync skipped",
                        data = null
                    ))
                }

                serverUpdateResult.onSuccess { response ->
                    Log.d("ZRead", "Successfully synced Z-Report ID to server: ${response.message}")
                }.onFailure { error ->
                    Log.e("ZRead", "Failed to sync Z-Report ID to server: ${error.message}")
                }

                // Return success since local update worked
                Result.success(ZReportUpdateResponse(
                    success = true,
                    message = "Transactions updated successfully",
                    data = null
                ))

            } catch (e: Exception) {
                Log.e("ZRead", "Error updating transactions with Z-Report ID", e)
                Result.failure(e)
            }
        }
    }
    private suspend fun updateTransactionsWithZReportId(
        storeId: String,
        zReportId: String,
        transactions: List<TransactionSummary>
    ) {
        withContext(Dispatchers.IO) {
            try {
                // Update transactions in local database
                transactionDao.updateTransactionsZReportId(storeId, zReportId)

                // Now this should work since transactionRepository is properly initialized
                val result = transactionRepository.updateTransactionsZReport(storeId, zReportId)
                result.onSuccess { response ->
                    Log.d("ZRead", "Successfully updated ${response.data?.updatedTransactions ?: 0} transactions with Z-Report ID: $zReportId")
                }.onFailure { error ->
                    Log.e("ZRead", "Failed to update transactions on server: ${error.message}")
                    // Continue with local update even if server update fails
                }

                Log.d("ZRead", "Updated ${transactions.size} transactions with Z-Report ID: $zReportId")
            } catch (e: Exception) {
                Log.e("ZRead", "Error updating transactions with Z-Report ID", e)
                throw e
            }
        }
    }

    private fun saveZReadRecord(zReportId: String, transactions: List<TransactionSummary>) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // FIXED: Use proper string formatting for dates
                    val currentDateString = getCurrentDate() // Returns string
                    val currentTimeString = getCurrentTime() // Returns string

                    val zReadRecord = ZRead(
                        zReportId = zReportId,
                        date = currentDateString,
                        time = currentTimeString,
                        totalTransactions = transactions.filter { it.transactionStatus == 1 }.size,
                        totalAmount = transactions.filter { it.transactionStatus == 1 }.sumOf { it.netAmount }
                    )
                    zReadDao.insert(zReadRecord)
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error saving Z-Read record: ${e.message}", e)
            }
        }
    }


    private suspend fun generateZReportId(): String {
        return withContext(Dispatchers.IO) {
            try {
                // Get the maximum Z-Read ID from the database
                val maxZReadId = transactionDao.getMaxZReadId() ?: 0

                // Also check SharedPreferences for consistency
                val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                val sharedPrefNumber = sharedPreferences.getInt("lastSequentialNumber", 0)

                // Use the higher number to ensure we don't have conflicts
                val nextNumber = maxOf(maxZReadId, sharedPrefNumber) + 1

                // Update SharedPreferences to keep it in sync
                with(sharedPreferences.edit()) {
                    putInt("lastSequentialNumber", nextNumber)
                    apply()
                }

                Log.d("ZRead", "Generated Z-Report ID: $nextNumber (DB Max: $maxZReadId, SharedPref: $sharedPrefNumber)")

                String.format("%09d", nextNumber)
            } catch (e: Exception) {
                Log.e("ZRead", "Error generating Z-Report ID", e)
                // Fallback to SharedPreferences only
                val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
                val currentNumber = sharedPreferences.getInt("lastSequentialNumber", 0)
                val nextNumber = currentNumber + 1

                with(sharedPreferences.edit()) {
                    putInt("lastSequentialNumber", nextNumber)
                    apply()
                }

                String.format("%09d", nextNumber)
            }
        }
    }

private fun simpleTest() {
    lifecycleScope.launch {
        try {
            Log.d("ReportsActivity", "=== SIMPLE TEST ===")

            withContext(Dispatchers.IO) {
                // Test 1: Check if we can get any transactions
                val recent = transactionDao.getRecentTransactions()
                Log.d("ReportsActivity", "Can we get recent transactions? ${recent.size}")

                if (recent.isNotEmpty()) {
                    val first = recent[0]
                    Log.d("ReportsActivity", "First transaction: ${first.transactionId}")
                    Log.d("ReportsActivity", "First transaction date: '${first.createdDate}'")
                    Log.d("ReportsActivity", "First transaction store: '${first.store}'")

                    // Test 2: Try querying for this specific transaction's date
                    val testDate = first.createdDate
                    Log.d("ReportsActivity", "Testing query with date: '$testDate'")

                    // Try different formats
                    val testQueries = listOf(
                        testDate,  // Exact match
                        testDate.substring(0, 19),  // Remove microseconds if any
                        testDate.substring(0, 10) + " 00:00:00" to testDate.substring(0, 10) + " 23:59:59"  // Date range
                    )

                    // Test exact match first
                    try {
                        val exactResult = transactionDao.getTransactionsByDateRange(testDate, testDate)
                        Log.d("ReportsActivity", "Exact date query result: ${exactResult.size}")
                    } catch (e: Exception) {
                        Log.e("ReportsActivity", "Exact date query failed: ${e.message}")
                    }

                } else {
                    Log.d("ReportsActivity", "NO TRANSACTIONS IN DATABASE!")
                }
            }
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Simple test failed", e)
        }
    }
}
    // FIXED: Let's test different date query approaches
    private fun testDateQueries() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val testDate = "2025-07-16"

                    Log.d("ReportsActivity", "=== TESTING DATE QUERIES ===")

                    // Test 1: Query with different formats
                    val formats = listOf(
                        "$testDate 00:00:00" to "$testDate 23:59:59",
                        "$testDate%" to "$testDate%", // Like pattern
                        testDate to testDate
                    )

                    formats.forEachIndexed { index, (start, end) ->
                        try {
                            val results = transactionDao.getTransactionsByDateRange(start, end)
                            Log.d("ReportsActivity", "Test $index - Query: '$start' to '$end' = ${results.size} results")
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "Test $index failed: ${e.message}")
                        }
                    }

                    // Test 2: Try to find transactions from today
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayResults = transactionDao.getTransactionsByDateRange(
                        "$today 00:00:00",
                        "$today 23:59:59"
                    )
                    Log.d("ReportsActivity", "Today ($today) query results: ${todayResults.size}")

                    // Test 3: Check what date format works with your showTransactionListDialog
                    val currentCalendar = Calendar.getInstance()
                    val dialogStart = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dialogEnd = Calendar.getInstance().apply {
                        time = dialogStart.time
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }

                    val dialogStartStr = formatDateToString(dialogStart.time)
                    val dialogEndStr = formatDateToString(dialogEnd.time)

                    val dialogResults = transactionDao.getTransactionsByDateRange(dialogStartStr, dialogEndStr)
                    Log.d("ReportsActivity", "Dialog-style query: '$dialogStartStr' to '$dialogEndStr' = ${dialogResults.size} results")
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error testing date queries", e)
            }
        }
    }
    private fun loadReportForDateRange() {
        lifecycleScope.launch {
            try {
                Log.d("ReportsActivity", "=== FILTERED DATE QUERY ===")

                val transactions = withContext(Dispatchers.IO) {
                    val currentStoreId = SessionManager.getCurrentUser()?.storeid
                    Log.d("ReportsActivity", "Current store ID: $currentStoreId")

                    if (isSameDay(startDate, endDate)) {
                        // For same day, use simple date string
                        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startDate)
                        Log.d("ReportsActivity", "Querying single date: '$dateString'")

                        // Get transactions for the date
                        val dateTransactions = try {
                            transactionDao.getTransactionsByDate(dateString)
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "getTransactionsByDate failed, trying pattern", e)
                            transactionDao.getTransactionsByDatePattern("$dateString%")
                        }

                        Log.d("ReportsActivity", "Raw date query result: ${dateTransactions.size} transactions")

                        // FILTER to match showTransactionListDialog criteria:
                        val filteredTransactions = dateTransactions.filter { transaction ->
                            // Apply the same filters as showTransactionListDialog
                            val isCurrentStore = currentStoreId == null || transaction.store == currentStoreId
                            val isValidTransaction = transaction.transactionStatus == 1 // Only completed transactions
                            val isRecentTransaction = transaction.createdDate.startsWith("2025-07-") // Only current year/month transactions

                            val shouldInclude = isCurrentStore && isValidTransaction && isRecentTransaction

                            if (!shouldInclude) {
                                Log.d("ReportsActivity", "Filtering out: ${transaction.transactionId} - Store: ${transaction.store}, Status: ${transaction.transactionStatus}, Date: ${transaction.createdDate}")
                            }

                            shouldInclude
                        }

                        Log.d("ReportsActivity", "After filtering: ${filteredTransactions.size} transactions")

                        // Sort by date descending (newest first) - same as showTransactionListDialog
                        filteredTransactions.sortedByDescending { it.createdDate }

                    } else {
                        // For date range, get all and filter
                        val startDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(startDate)
                        val endDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(endDate)

                        Log.d("ReportsActivity", "Querying date range: '$startDateStr' to '$endDateStr'")

                        // Get all transactions in the range
                        val allTransactions = transactionDao.getAllTransactionSummaries()
                        val filtered = allTransactions.filter { transaction ->
                            val transactionDate = transaction.createdDate.substring(0, 10) // Get YYYY-MM-DD part
                            val isInRange = transactionDate >= startDateStr && transactionDate <= endDateStr
                            val isCurrentStore = currentStoreId == null || transaction.store == currentStoreId
                            val isValidTransaction = transaction.transactionStatus == 1
                            val isRecentTransaction = transaction.createdDate.startsWith("2025-07-") // Only current transactions

                            isInRange && isCurrentStore && isValidTransaction && isRecentTransaction
                        }.sortedByDescending { it.createdDate }

                        Log.d("ReportsActivity", "Range filtered: ${filtered.size} transactions")
                        filtered
                    }
                }

                Log.d("ReportsActivity", "Final result: ${transactions.size} transactions")

                // Log some sample results to compare with showTransactionListDialog
                Log.d("ReportsActivity", "=== COMPARING WITH DIALOG ===")
                transactions.take(5).forEach { transaction ->
                    Log.d("ReportsActivity", "Reports: ${transaction.transactionId} - ${transaction.createdDate} - Status: ${transaction.transactionStatus}")
                }

                val report = calculateSalesReport(transactions)
                updateUI(report)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error in loadReportForDateRange", e)
                Toast.makeText(this@ReportsActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // STEP 8: Update your existing onDestroy method
    // Keep your existing onDestroy, just make sure it includes this
    override fun onDestroy() {
        super.onDestroy()

        // Stop the simple monitoring
        stopAutoSync()

        // Your existing onDestroy code...
        try {
            if (::autoZReadReceiver.isInitialized) {
                unregisterReceiver(autoZReadReceiver)
            }
        } catch (e: Exception) {
            Log.e("AutoZRead", "Error unregistering receiver", e)
        }
    }

    // Helper method to stop monitoring
    // STEP 9: Update your existing onResume method
    override fun onResume() {
        super.onResume()

        // Your existing onResume code
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                scheduleAutomaticZRead()
            }
        }

        // Add auto-sync resume
        if (isAutoSyncEnabled && autoSyncJob?.isActive != true) {
            setupSimpleAutoSync()
        }
    }

    // STEP 10: Add pause method for better resource management
    override fun onPause() {
        super.onPause()
        // Optionally pause auto-sync when activity is not visible
        // stopAutoSync()
    }

    private fun updateSyncStatus(status: String) {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                // You can update your title or add a status indicator
                supportActionBar?.subtitle = status

                // Or update any existing TextView you have
                // binding.statusTextView?.text = status
            }
        }
    }

private fun testDateFormats() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    Log.d("ReportsActivity", "=== TESTING DATE FORMATS ===")

                    // Get a sample transaction to work with
                    val sample = transactionDao.getRecentTransactions().firstOrNull()
                    if (sample != null) {
                        Log.d("ReportsActivity", "Sample transaction date: '${sample.createdDate}'")

                        // Extract just the date part
                        val dateOnly = sample.createdDate.substring(0, 10) // "2025-07-01"
                        Log.d("ReportsActivity", "Date only: '$dateOnly'")

                        // Test different query approaches
                        try {
                            val exactMatch = transactionDao.getTransactionsByDate(dateOnly)
                            Log.d("ReportsActivity", "DATE() function result: ${exactMatch.size}")
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "DATE() function failed: ${e.message}")
                        }

                        try {
                            val likeMatch = transactionDao.getTransactionsByDatePattern("$dateOnly%")
                            Log.d("ReportsActivity", "LIKE pattern result: ${likeMatch.size}")
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "LIKE pattern failed: ${e.message}")
                        }

                        // Test in-memory filtering
                        val allTransactions = transactionDao.getAllTransactionSummaries()
                        val memoryFiltered = allTransactions.filter { it.createdDate.startsWith(dateOnly) }
                        Log.d("ReportsActivity", "Memory filter result: ${memoryFiltered.size} from ${allTransactions.size} total")
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error testing date formats", e)
            }
        }
    }



    // FIXED: Load today's report using same date handling
    private fun loadTodaysReport() {
        loadTransactionsForSelectedDate()
    }
    // Updated showCashFundDialog function - REPLACE your existing one
    private fun showCashFundDialog() {
        lifecycleScope.launch {
            try {
                val hasZRead = hasZReadForCurrentDate()

                // Check if transactions are disabled after Z-Read
                if (hasZRead && isAfterMidnight()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Cash fund operations are disabled. Z-Read completed for today. Please wait until next day.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                if (hasZRead) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Cash fund operations are disabled. Z-Read completed for today.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                if (isCashFundEntered) {
                    showCurrentCashFundStatus()
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    val dialogView = layoutInflater.inflate(R.layout.dialog_cash_fund, null)
                    val editTextCashFund = dialogView.findViewById<EditText>(R.id.editTextCashFund)

                    val dialog = android.app.AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                        .setTitle("Enter Cash Fund")
                        .setView(dialogView)
                        .setPositiveButton("Submit") { _, _ ->
                            val cashFund = editTextCashFund.text.toString().toDoubleOrNull()
                            if (cashFund != null && cashFund > 0) {
                                currentCashFund = cashFund
                                saveCashFund(cashFund, "INITIAL")
                                if (bluetoothPrinterHelper.printCashFundReceipt(cashFund, "INITIAL")) {
                                    Toast.makeText(
                                        this@ReportsActivity,
                                        "Cash Fund Receipt printed successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        this@ReportsActivity,
                                        "Failed to print Cash Fund Receipt",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                isCashFundEntered = true
                                // No need to enable transactions here since this is ReportsActivity
                            } else {
                                Toast.makeText(
                                    this@ReportsActivity,
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
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error showing cash fund dialog", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error showing cash fund dialog: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Keep your existing showCurrentCashFundStatus method as is
    private fun showCurrentCashFundStatus() {
        val dialog = AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
            .setTitle("Cash Fund Status")
            .setMessage(
                "Current Cash Fund: ₱${String.format("%.2f", currentCashFund)}\n\nYou can manage the cash fund through the Pull-out Cash Fund option."
            )
            .setPositiveButton("OK") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    // Pullout Cash Fund functionality
    private fun showPulloutCashFundDialog() {
        lifecycleScope.launch {
            val hasZRead = hasZReadForCurrentDate()

            if (hasZRead) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Pull-out operations are disabled. Z-Read completed for today.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            if (currentCashFund <= 0) {
                Toast.makeText(this@ReportsActivity, "No cash fund available to pull out", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val dialog = android.app.AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                .setTitle("Pull Out Cash Fund")
                .setMessage("Current Cash Fund: ₱${String.format("%.2f", currentCashFund)}")
                .setPositiveButton("Pull Out") { _, _ ->
                    processPulloutCashFund(currentCashFund)
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()

            withContext(Dispatchers.Main) {
                dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
                dialog.show()
            }
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
    }
    private suspend fun hasZReadForToday(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // FIXED: Use string date directly
                val currentDateString = getCurrentDate()
                val existingZRead = zReadDao.getZReadByDate(currentDateString)
                existingZRead != null
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking Z-Read for today: ${e.message}", e)
                false
            }
        }
    }
    // Tender Declaration functionality
    private fun showTenderDeclarationDialog() {
        lifecycleScope.launch {
            try {
                val hasZRead = hasZReadForCurrentDate()

                if (hasZRead) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Tender declaration operations are disabled. Z-Read completed for today.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                if (!isPulloutCashFundProcessed) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ReportsActivity,
                            "Please process pull-out cash fund first",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
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

                    // Set hints for cash edit texts
                    cashEditTexts.forEach { editText ->
                        editText.hint = "0"
                        editText.addTextChangedListener(object : TextWatcher {
                            override fun afterTextChanged(s: Editable?) {
                                calculateTotalCash(cashEditTexts, denominations, textViewTotalCash)
                            }
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        })
                    }

                    val arEditTexts = mutableListOf<EditText>()

                    // Setup AR types - collect only once
                    lifecycleScope.launch {
                        try {
                            arViewModel.arTypes.collect { arTypes ->
                                val nonCashArTypes = arTypes.filter { it.ar != "Cash" }.map { it.ar }

                                // Use the new 3-column layout function
                                addArTypesToLayout(nonCashArTypes, linearLayoutArTypes, arEditTexts, textViewTotalAr)
                                calculateTotalAr(arEditTexts, textViewTotalAr)

                                // Show dialog after AR types are loaded
                                val dialog = android.app.AlertDialog.Builder(this@ReportsActivity, R.style.CustomDialogStyle)
                                    .setTitle("Tender Declaration")
                                    .setView(dialogView)
                                    .setPositiveButton("Confirm") { _, _ ->
                                        val totalCash = textViewTotalCash.text.toString().replace("Total Cash: ₱", "").toDoubleOrNull() ?: 0.0
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

                                // Break out of collect after first emission
                                return@collect
                            }
                        } catch (e: Exception) {
                            Log.e("ReportsActivity", "Error loading AR types", e)
                            Toast.makeText(
                                this@ReportsActivity,
                                "Error loading AR types: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error showing tender declaration dialog", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error showing tender declaration dialog: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Helper extension function for dp to px conversion
    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun addArTypesToLayout(
        arTypes: List<String>, // Assuming you have a list of AR type names
        layout: LinearLayout,
        editTexts: MutableList<EditText>,
        totalArTextView: TextView
    ) {
        layout.removeAllViews()
        editTexts.clear()

        // Group AR types into rows of 3
        val chunkedArTypes = arTypes.chunked(3)

        chunkedArTypes.forEach { rowArTypes ->
            val rowLayout = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 1.dpToPx()
                }
                orientation = LinearLayout.HORIZONTAL
            }

            rowArTypes.forEach { arType ->
                val arTypeView = createArTypeView(arType, editTexts, totalArTextView)
                rowLayout.addView(arTypeView)
            }

            // Add empty views to fill remaining columns if needed
            val remainingColumns = 3 - rowArTypes.size
            repeat(remainingColumns) {
                val emptyView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                }
                rowLayout.addView(emptyView)
            }

            layout.addView(rowLayout)
        }
    }
    private fun createArTypeView(
        arType: String,
        editTexts: MutableList<EditText>,
        totalArTextView: TextView
    ): LinearLayout {
        val arTypeLayout = layoutInflater.inflate(R.layout.item_ar_type, null) as LinearLayout

        // Set layout parameters for 3-column layout
        arTypeLayout.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val arTypeLabel = arTypeLayout.findViewById<TextView>(R.id.textViewArTypeLabel)
        val arTypeEditText = arTypeLayout.findViewById<EditText>(R.id.editTextArTypeAmount)

        // Truncate long AR type names to fit in the layout
        val displayName = if (arType.length > 8) "${arType.take(20)}" else arType
        arTypeLabel.text = "$displayName:"

        arTypeEditText.tag = arType
        arTypeEditText.hint = "0"
        arTypeEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        editTexts.add(arTypeEditText)

        arTypeEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                calculateTotalAr(editTexts, totalArTextView)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        return arTypeLayout
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

    private fun processTenderDeclaration(cashAmount: Double, arAmounts: Map<String, Double>) {
        if (cashAmount <= 0) {
            Toast.makeText(this, "Cash amount must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val gson = Gson()
            val arAmountsJson = gson.toJson(arAmounts)

            tenderDeclaration = TenderDeclaration(
                cashAmount = cashAmount,
                arPayAmount = arAmounts.values.sum(),
                date = getCurrentDate(), // Returns string
                time = getCurrentTime(), // Returns string
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
                Toast.makeText(this, "Failed to print Tender Declaration Receipt", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error processing tender declaration: ${e.message}", e)
            Toast.makeText(this, "Error processing tender declaration: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentTime(): String {
        return try {
            val format = SimpleDateFormat("HH:mm:ss", Locale.US)
            format.format(Date())
        } catch (e: Exception) {
            Log.e("ReportsActivity", "Error getting current time: ${e.message}")
            SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        }
    }
    private fun updatePaymentDistributionAndItemSales(
        paymentDistribution: PaymentDistribution,
        itemSales: List<ItemSalesSummary>
    ) {
        // Clear existing payment distribution views
        binding.paymentDistributionContainer.removeAllViews()

        // Create payment method views - only show methods with positive net amounts
        val paymentMethods = listOf(
            "Cash" to paymentDistribution.cash,
            "Card" to paymentDistribution.card,
            "GCash" to paymentDistribution.gCash,
            "PayMaya" to paymentDistribution.payMaya,
            "Loyalty Card" to paymentDistribution.loyaltyCard,
            "Charge" to paymentDistribution.charge,
            "Foodpanda" to paymentDistribution.foodpanda,
            "GrabFood" to paymentDistribution.grabfood,
            "Representation" to paymentDistribution.representation,
//            "AR" to paymentDistribution.ar
        ).filter { it.second != 0.0 } // Show all non-zero amounts (positive or negative)

        // Group payment methods into rows of 3
        val paymentMethodChunks = paymentMethods.chunked(3)

        paymentMethodChunks.forEach { rowMethods ->
            // Create a horizontal LinearLayout for each row
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dpToPx()
                }
            }

            rowMethods.forEachIndexed { index, (method, amount) ->
                val cardView = createPaymentMethodView(method, amount)

                // Set layout params with weight for equal distribution
                cardView.layoutParams = LinearLayout.LayoutParams(
                    0,
                    120.dpToPx(),
                    1f
                ).apply {
                    when (index) {
                        0 -> marginEnd = 6.dpToPx()
                        1 -> {
                            marginStart = 6.dpToPx()
                            marginEnd = 6.dpToPx()
                        }
                        2 -> marginStart = 6.dpToPx()
                    }
                }

                rowLayout.addView(cardView)
            }

            // If row has less than 3 items, add empty views to maintain grid
            val emptyViewsNeeded = 3 - rowMethods.size
            repeat(emptyViewsNeeded) { index ->
                val emptyView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        120.dpToPx(),
                        1f
                    ).apply {
                        when (rowMethods.size + index) {
                            1 -> {
                                marginStart = 6.dpToPx()
                                marginEnd = 6.dpToPx()
                            }
                            2 -> marginStart = 6.dpToPx()
                        }
                    }
                }
                rowLayout.addView(emptyView)
            }

            binding.paymentDistributionContainer.addView(rowLayout)
        }

        // Calculate total payment amount (net of returns)
        val totalPayments = paymentMethods.sumOf { it.second }

        if (totalPayments != 0.0) {
            // Add a divider before total
            val dividerView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dpToPx()
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
            binding.paymentDistributionContainer.addView(dividerView)

            // Create total payment summary
            val totalPaymentCard = androidx.cardview.widget.CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                radius = 12f
                cardElevation = 4f
                setCardBackgroundColor(
                    if (totalPayments >= 0) {
                        resources.getColor(android.R.color.holo_green_dark, null)
                    } else {
                        resources.getColor(android.R.color.holo_red_dark, null)
                    }
                )
            }

            val totalPaymentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }

            val totalPaymentText = TextView(this).apply {
                text = "NET TOTAL PAYMENTS"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val totalPaymentAmountText = TextView(this).apply {
                text = "₱${String.format("%.2f", totalPayments)}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 20f
                gravity = Gravity.END
                setTextColor(resources.getColor(android.R.color.white, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
//
//            totalPaymentLayout.addView(totalPaymentText)
//            totalPaymentLayout.addView(totalPaymentAmountText)
//            totalPaymentCard.addView(totalPaymentLayout)
            binding.paymentDistributionContainer.addView(totalPaymentCard)
        }

        // Update item sales with grouping
//        createAllItemSales(itemSales)
    }
    private fun createPaymentDistributionTwoColumns(paymentDistribution: PaymentDistribution) {
        val paymentMethods = mapOf(
            "Cash" to paymentDistribution.cash,
            "Card" to paymentDistribution.card,
            "GCash" to paymentDistribution.gCash,
            "PayMaya" to paymentDistribution.payMaya,
            "Loyalty Card" to paymentDistribution.loyaltyCard,
            "Charge" to paymentDistribution.charge,
            "Foodpanda" to paymentDistribution.foodpanda,
            "GrabFood" to paymentDistribution.grabfood,
            "Representation" to paymentDistribution.representation,
//            "AR" to paymentDistribution.ar
        ).filter { it.value > 0 }

        if (paymentMethods.isEmpty()) {
            val noPaymentsText = TextView(this).apply {
                text = "No payments recorded for this date range"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            binding.paymentDistributionContainer.addView(noPaymentsText)
            return
        }

        val methods = paymentMethods.toList()
        for (i in methods.indices step 2) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 4, 0, 4)
                }
            }

            val firstMethod = methods[i]
            val firstPaymentView = createPaymentMethodView(firstMethod.first, firstMethod.second)
            firstPaymentView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            rowLayout.addView(firstPaymentView)

            if (i + 1 < methods.size) {
                val secondMethod = methods[i + 1]
                val secondPaymentView = createPaymentMethodView(secondMethod.first, secondMethod.second)
                secondPaymentView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8
                }
                rowLayout.addView(secondPaymentView)
            } else {
                val emptyView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                rowLayout.addView(emptyView)
            }

            binding.paymentDistributionContainer.addView(rowLayout)
        }
    }
    // Replace your existing createAllItemSales method with this one
    private fun createAllItemSales() {
        binding.itemSalesContainer.removeAllViews()
        showItemSalesLoading()

        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(
                        formatDateToString(startDate),
                        formatDateToString(endDate)
                    )
                }

                val itemSales = mutableMapOf<String, ItemSalesSummary>()
                var totalSales = 0.0
                var totalQuantity = 0

                withContext(Dispatchers.IO) {
                    transactions.forEach { transaction ->
                        val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                        items.forEach { item ->
                            val key = item.name
                            val itemGroup = item.itemGroup ?: "Unknown"

                            val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                                name = item.name,
                                quantity = 0,
                                totalAmount = 0.0,
                                itemGroup = itemGroup
                            ))

                            val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                            val itemTotal = effectivePrice * item.quantity

                            totalQuantity += item.quantity

                            itemSales[key] = currentSummary.copy(
                                quantity = currentSummary.quantity + item.quantity,
                                totalAmount = currentSummary.totalAmount + itemTotal
                            )

                            totalSales += itemTotal
                        }
                    }
                }

                val groupedItems = itemSales.values
                    .groupBy { it.itemGroup }
                    .map { (groupName, items) ->
                        ItemGroupSummary(
                            groupName = groupName,
                            items = items.sortedByDescending { it.totalAmount },
                            totalQuantity = items.sumOf { it.quantity },
                            totalAmount = items.sumOf { it.totalAmount }
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                withContext(Dispatchers.Main) {
                    hideItemSalesLoading()

                    if (itemSales.isEmpty()) {
                        binding.noItemSalesText.visibility = android.view.View.VISIBLE
                        binding.viewAllItemsButton.visibility = android.view.View.GONE
                        return@withContext
                    }

                    binding.noItemSalesText.visibility = android.view.View.GONE
                    displayAllItemSalesFromDatabase(groupedItems, totalQuantity, totalSales, transactions.size)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideItemSalesLoading()
                    showItemSalesError(e.message ?: "Error loading items")
                    Log.e("ReportsActivity", "Error loading all item sales", e)
                }
            }
        }
    }
    private suspend fun displayAllItemSalesFromDatabase(
        groupedItems: List<ItemGroupSummary>,
        totalQuantity: Int,
        totalSales: Double,
        totalTransactions: Int
    ) {
        val batchSize = 15 // Process items in batches to avoid UI freezing

        // Create views for each group
        groupedItems.forEach { group ->
            // Create group header
            val groupHeaderView = createGroupHeaderView(group)
            binding.itemSalesContainer.addView(groupHeaderView)

            // Add ALL items from this group in batches
            val items = group.items
            for (i in items.indices step batchSize) {
                val batch = items.subList(i, minOf(i + batchSize, items.size))

                // Add each item in the batch
                batch.forEach { item ->
                    val itemView = createItemSalesView(item)
                    binding.itemSalesContainer.addView(itemView)
                }

                // Small delay to allow UI to update smoothly
                if (i + batchSize < items.size) {
                    delay(10) // 10ms delay between batches
                }
            }

            // Add group total row after each group's items
            val groupTotalRow = createTotalRow(group.totalQuantity, group.totalAmount)
            binding.itemSalesContainer.addView(groupTotalRow)

            // Add spacing between groups
            val spacerView = android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    12
                )
            }
            binding.itemSalesContainer.addView(spacerView)
        }

        // Create grand total summary
        val grandTotalView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 8)
            }
            setPadding(12, 12, 12, 12)
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_dark, null))
        }

        val totalText = TextView(this).apply {
            text = "GRAND TOTAL (${totalQuantity} items, ${totalTransactions} transactions)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val totalAmountText = TextView(this).apply {
            text = "₱${String.format("%.2f", totalSales)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 16f
            gravity = Gravity.END
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        grandTotalView.addView(totalText)
        grandTotalView.addView(totalAmountText)
        binding.itemSalesContainer.addView(grandTotalView)

        // Add print button
        val printButton = Button(this).apply {
            text = "🖨️ Print Item Sales Report"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 12, 0, 8)
            }
            setPadding(16, 12, 16, 12)
            setBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
            setTextColor(resources.getColor(android.R.color.white, null))
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)

            setOnClickListener {
                printItemSalesReport(groupedItems, totalQuantity, totalSales)
            }
        }
        binding.itemSalesContainer.addView(printButton)

        // Hide "View All Items" button since we're showing all items
        binding.viewAllItemsButton.visibility = android.view.View.GONE

        // Show completion message
        Toast.makeText(
            this@ReportsActivity,
            "Loaded all ${totalQuantity} items from ${groupedItems.size} categories",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun showItemSalesLoading() {
        val loadingView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            gravity = Gravity.CENTER
            tag = "loading_view"
        }

        val loadingText = TextView(this).apply {
            text = "🔄 Loading all sold items..."
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16.dpToPx()
            }
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
            gravity = Gravity.CENTER
        }

        val progressBar = ProgressBar(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                48.dpToPx(),
                48.dpToPx()
            )
            isIndeterminate = true
        }

        loadingView.addView(loadingText)
        loadingView.addView(progressBar)
        binding.itemSalesContainer.addView(loadingView)
    }

    private fun hideItemSalesLoading() {
        val loadingView = binding.itemSalesContainer.findViewWithTag<LinearLayout>("loading_view")
        loadingView?.let { binding.itemSalesContainer.removeView(it) }
    }

    private fun showItemSalesError(errorMessage: String) {
        val errorView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 32, 0, 32)
            }
            gravity = Gravity.CENTER
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            setBackgroundColor(resources.getColor(android.R.color.holo_red_light, null))
        }

        val errorText = TextView(this).apply {
            text = "❌ Error: $errorMessage"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.white, null))
            gravity = Gravity.CENTER
        }

        errorView.addView(errorText)
        binding.itemSalesContainer.addView(errorView)
    }
    private fun createGroupHeaderView(group: ItemGroupSummary): LinearLayout {
        val groupView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 4)
            }
            setPadding(12, 10, 12, 10)
            setBackgroundColor(resources.getColor(android.R.color.holo_blue_light, null))
        }

        val groupNameTextView = TextView(this).apply {
            text = "📦 ${group.groupName.uppercase()}"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }


        groupView.addView(groupNameTextView)
//        groupView.addView(groupSummaryTextView)
        return groupView
    }
    fun printItemSalesReport(groupedItems: List<ItemGroupSummary>, totalQuantity: Int, totalAmount: Double) {
        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(
                        formatDateToString(startDate),
                        formatDateToString(endDate)
                    )
                }

                val content = StringBuilder()
                content.append(0x1B.toChar()) // ESC
                content.append('!'.toChar())  // Select print mode
                content.append(0x01.toChar()) // Smallest text size

                // Set minimum line spacing
                content.append(0x1B.toChar()) // ESC
                content.append('3'.toChar())  // Select line spacing
                content.append(50.toChar())

                content.append("==============================\n")
                content.append("        ITEM SALES REPORT     \n")
                content.append("==============================\n")

                val selectedDateRange = if (isSameDay(startDate, endDate)) {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)
                } else {
                    "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}"
                }

                content.append("Date: $selectedDateRange\n")
                content.append("Printed: ${SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}\n")
                content.append("------------------------------\n")
                content.append("Items Sold by Group:\n")
                content.append("------------------------------\n")

                // Print items grouped by category
                groupedItems.forEach { group ->
                    content.append("\n--- ${group.groupName.uppercase()} ---\n")
                    content.append("------------------------------\n")

                    group.items.forEach { item ->
                        val nameWidth = 22
                        val quantityWidth = 4
                        val amountWidth = 9

                        val truncatedName = if (item.name.length > nameWidth) {
                            item.name.substring(0, nameWidth - 3) + "..."
                        } else {
                            item.name.padEnd(nameWidth)
                        }

                        val paddedQuantity = item.quantity.toString().padStart(quantityWidth)
                        val formattedAmount = String.format("%.2f", item.totalAmount).padStart(amountWidth)

                        content.append("$truncatedName $paddedQuantity x$formattedAmount [ ]___\n")
                    }
                    content.append("------------------------------\n")

                    // Align Grand Total with the same structure as items
                    val grandTotalName = "Grand Total:".padEnd(22)
                    val grandTotalQuantity = group.totalQuantity.toString().padStart(4)
                    val grandTotalAmount = String.format("%.2f", group.totalAmount).padStart(9)

                    content.append("$grandTotalName $grandTotalQuantity x$grandTotalAmount [ ]___\n")
                    content.append("------------------------------\n")
                }


                content.append("\nGRAND TOTAL SUMMARY\n")
                content.append("==============================\n")
                content.append("Total Transactions: ${transactions.size}\n")
                content.append("Total Items Sold  : $totalQuantity\n")
                content.append("Total Sales       : ${String.format("%.2f", totalAmount)}\n")
//                content.append("Total Groups      : ${groupedItems.size}\n")
                content.append("==============================\n")

                content.append(0x1B.toChar()) // ESC
                content.append('!'.toChar())  // Select print mode
                content.append(0x00.toChar()) // Reset to normal size

                content.append(0x1B.toChar()) // ESC
                content.append('2'.toChar())

                // Use the correct method for printing generic content
                val printSuccess = bluetoothPrinterHelper.printGenericReceipt(content.toString())

                withContext(Dispatchers.Main) {
                    if (printSuccess) {
                        Toast.makeText(this@ReportsActivity, "Item sales report sent to printer", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@ReportsActivity, "Failed to print report. Check printer connection.", Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ReportsActivity, "Error printing report: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun calculateSalesReport(transactions: List<TransactionSummary>): SalesReport {
        // Separate regular transactions and returns for SALES calculation, but include ALL for OR numbering
        val regularTransactions = transactions.filter { it.transactionStatus == 1 && it.type != 2 }
        val returnTransactions = transactions.filter { it.type == 2 }

        // Calculate item totals using the same method as buildReadReport
        val regularItemCalculations = calculateItemTotals(regularTransactions)
        val returnItemCalculations = calculateItemTotals(returnTransactions)

        // Net calculations (regular sales minus returns)
        val totalGross = regularItemCalculations.totalGross + returnItemCalculations.totalGross
        val totalNetSales = regularTransactions.sumOf { it.netAmount } + returnTransactions.sumOf { it.netAmount }
        val totalDiscount = regularTransactions.sumOf { it.totalDiscountAmount } + returnTransactions.sumOf { it.totalDiscountAmount }
        val totalQuantity = regularItemCalculations.totalQuantity + returnItemCalculations.totalQuantity

        // Calculate total return amount (absolute value for display)
        val totalReturnAmount = kotlin.math.abs(returnTransactions.sumOf { it.netAmount })
        val totalReturnDiscount = kotlin.math.abs(returnTransactions.sumOf { it.totalDiscountAmount })

        // Payment method totals including returns
        val paymentDistribution = PaymentDistribution(
            cash = regularTransactions.sumOf { it.cash } + returnTransactions.sumOf { it.cash },
            card = regularTransactions.sumOf { it.card } + returnTransactions.sumOf { it.card },
            gCash = regularTransactions.sumOf { it.gCash } + returnTransactions.sumOf { it.gCash },
            payMaya = regularTransactions.sumOf { it.payMaya } + returnTransactions.sumOf { it.payMaya },
            loyaltyCard = regularTransactions.sumOf { it.loyaltyCard } + returnTransactions.sumOf { it.loyaltyCard },
            charge = regularTransactions.sumOf { it.charge } + returnTransactions.sumOf { it.charge },
            foodpanda = regularTransactions.sumOf { it.foodpanda } + returnTransactions.sumOf { it.foodpanda },
            grabfood = regularTransactions.sumOf { it.grabfood } + returnTransactions.sumOf { it.grabfood },
            representation = regularTransactions.sumOf { it.representation } + returnTransactions.sumOf { it.representation },
            ar = regularTransactions.filter { it.type == 3 }.sumOf { it.netAmount } +
                    returnTransactions.filter { it.type == 3 }.sumOf { it.netAmount }
        )

        // VAT calculations
        val vatRate = 0.12
        val vatableSales = totalNetSales / (1 + vatRate)
        val vatAmount = totalNetSales - vatableSales

        // Transaction statistics
        val totalTransactions = regularTransactions.size
        val totalReturns = returnTransactions.size

        val (startingOR, endingOR) = getORNumberRange(transactions)

        // Combine item sales from both regular and return transactions
        val combinedItemSales = combineItemSales(regularItemCalculations.itemSales, returnItemCalculations.itemSales)

        return SalesReport(
            totalGross = totalGross,
            totalNetSales = totalNetSales,
            totalDiscount = kotlin.math.abs(totalDiscount),
            totalQuantity = kotlin.math.abs(totalQuantity),
            totalTransactions = totalTransactions,
            totalReturns = totalReturns,
            totalReturnAmount = totalReturnAmount,
            totalReturnDiscount = totalReturnDiscount,
            paymentDistribution = paymentDistribution,
            itemSales = combinedItemSales,
            vatAmount = vatAmount,
            vatableSales = vatableSales,
            startingOR = startingOR,
            endingOR = endingOR
        )
    }

    // FIXED: Update OR number range calculation to use String.toTimestamp() like TransactionAdapter
    private suspend fun getORNumberRange(transactions: List<TransactionSummary>): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            if (transactions.isEmpty()) {
                return@withContext Pair("N/A", "N/A")
            }

            try {
                Log.d("ORCalculation", "=== OR Number Calculation Debug ===")
                Log.d("ORCalculation", "Processing ${transactions.size} transactions (including returns)")

                // FIXED: Sort by date string using the same method as TransactionAdapter
                val sortedTransactions = transactions.sortedBy { it.createdDate.toTimestampForSorting() }

                sortedTransactions.forEach { transaction ->
                    val transactionType = when {
                        transaction.type == 2 -> "RETURN"
                        transaction.type == 3 -> "AR"
                        transaction.transactionStatus != 1 -> "VOID/PENDING"
                        else -> "SALE"
                    }

                    Log.d("ORCalculation", "Transaction ID: ${transaction.transactionId} (${transactionType})")
                    Log.d("ORCalculation", "Receipt ID: ${transaction.receiptId}")
                    Log.d("ORCalculation", "Created Date: ${transaction.createdDate}")
                    Log.d("ORCalculation", "Timestamp: ${transaction.createdDate.toTimestampForSorting()}")
                    Log.d("ORCalculation", "Type: ${transaction.type}, Status: ${transaction.transactionStatus}")
                    Log.d("ORCalculation", "---")
                }

                // Try to get OR numbers from receiptId field
                val orNumbersFromReceiptId = sortedTransactions
                    .mapNotNull { transaction ->
                        extractORFromReceiptId(transaction.receiptId)?.let { orNumber ->
                            val transactionType = when {
                                transaction.type == 2 -> "RETURN"
                                transaction.type == 3 -> "AR"
                                transaction.transactionStatus != 1 -> "VOID/PENDING"
                                else -> "SALE"
                            }
                            Log.d("ORCalculation", "Extracted OR from receiptId '${transaction.receiptId}': $orNumber (${transactionType})")
                            orNumber to transaction.createdDate.toTimestampForSorting()
                        }
                    }
                    .sortedBy { it.second }

                if (orNumbersFromReceiptId.isNotEmpty()) {
                    val startingOR = orNumbersFromReceiptId.first().first.toString()
                    val endingOR = orNumbersFromReceiptId.last().first.toString()
                    Log.d("ORCalculation", "Using receiptId method - Starting OR: $startingOR, Ending OR: $endingOR")
                    return@withContext Pair(startingOR, endingOR)
                }

                // Fallback to sequential numbering
                val lastORNumber = getLastUsedORNumber()
                Log.d("ORCalculation", "Last used OR number: $lastORNumber")

                val startingORNumber = lastORNumber + 1
                val endingORNumber = lastORNumber + sortedTransactions.size

                updateLastUsedORNumber(endingORNumber)

                sortedTransactions.forEachIndexed { index, transaction ->
                    val orNumber = startingORNumber + index
                    val transactionType = when {
                        transaction.type == 2 -> "RETURN"
                        transaction.type == 3 -> "AR"
                        transaction.transactionStatus != 1 -> "VOID/PENDING"
                        else -> "SALE"
                    }

                    storeORAssignment(transaction.transactionId, orNumber)
                    Log.d("ORCalculation", "Assigned OR $orNumber to transaction ${transaction.transactionId} (${transactionType})")
                }

                val startingOR = startingORNumber.toString()
                val endingOR = endingORNumber.toString()

                Log.d("ORCalculation", "Generated sequential - Starting OR: $startingOR, Ending OR: $endingOR")
                return@withContext Pair(startingOR, endingOR)

            } catch (e: Exception) {
                Log.e("ORCalculation", "Error calculating OR numbers", e)
                return@withContext Pair("Error", "Error")
            }
        }
    }


    private suspend fun getLastUsedORNumber(): Int {
        return try {
            val sharedPreferences = getSharedPreferences("ORNumberPreferences", Context.MODE_PRIVATE)
            val lastORFromPrefs = sharedPreferences.getInt("lastORNumber", 0)

            // Also check database for any stored OR numbers
            val lastORFromDB = transactionDao.getMaxORNumber() ?: 0

            val lastOR = maxOf(lastORFromPrefs, lastORFromDB)
            Log.d("ORCalculation", "Last OR from prefs: $lastORFromPrefs, from DB: $lastORFromDB, using: $lastOR")

            lastOR
        } catch (e: Exception) {
            Log.e("ORCalculation", "Error getting last used OR number", e)
            0
        }
    }

    // Helper function to update last used OR number
    private suspend fun updateLastUsedORNumber(orNumber: Int) {
        try {
            val sharedPreferences = getSharedPreferences("ORNumberPreferences", Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putInt("lastORNumber", orNumber)
                apply()
            }
            Log.d("ORCalculation", "Updated last OR number to: $orNumber")
        } catch (e: Exception) {
            Log.e("ORCalculation", "Error updating last used OR number", e)
        }
    }

    // Helper function to store OR assignment for future reference
    private suspend fun storeORAssignment(transactionId: String, orNumber: Int) {
        try {
            // You could store this in a separate table or update the transaction record
            // For now, we'll just log it
            Log.d("ORCalculation", "OR Assignment: Transaction $transactionId -> OR $orNumber")

            // Optional: Update the receiptId field with the OR number
            // transactionDao.updateReceiptId(transactionId, orNumber.toString())

        } catch (e: Exception) {
            Log.e("ORCalculation", "Error storing OR assignment", e)
        }
    }


    private fun combineItemSales(
        regularItemSales: List<ItemSalesSummary>,
        returnItemSales: List<ItemSalesSummary>
    ): List<ItemSalesSummary> {
        val combinedItems = mutableMapOf<String, ItemSalesSummary>()

        // Add regular sales
        regularItemSales.forEach { item ->
            combinedItems[item.name] = item
        }

        // Subtract returns
        returnItemSales.forEach { returnItem ->
            val existing = combinedItems[returnItem.name]
            if (existing != null) {
                combinedItems[returnItem.name] = existing.copy(
                    quantity = existing.quantity + returnItem.quantity, // returnItem.quantity is negative
                    totalAmount = existing.totalAmount + returnItem.totalAmount // returnItem.totalAmount is negative
                )
            } else {
                // If item only exists in returns, add it with negative values
                combinedItems[returnItem.name] = returnItem
            }
        }

        // Filter out items with zero or negative quantities/amounts and sort by total amount
        return combinedItems.values
            .filter { it.quantity > 0 && it.totalAmount > 0 }
            .sortedByDescending { it.totalAmount }
    }
    // New data class to hold item calculation results
    private fun extractORFromReceiptId(receiptId: String?): Int? {
        if (receiptId.isNullOrEmpty()) return null

        return try {
            when {
                // Pattern 1: receiptId like "VICTORIA000000949" - extract the number part
                receiptId.startsWith("VICTORIA", ignoreCase = true) -> {
                    val numberPart = receiptId.substringAfter("VICTORIA", "")
                        .replace("0+".toRegex(), "") // Remove leading zeros
                        .takeIf { it.isNotEmpty() }
                    numberPart?.toIntOrNull()
                }

                // Pattern 2: receiptId is just a number
                receiptId.all { it.isDigit() } -> receiptId.toInt()

                // Pattern 3: receiptId contains "OR" followed by numbers
                receiptId.contains("OR", ignoreCase = true) -> {
                    val orPattern = "OR\\s*(\\d+)".toRegex(RegexOption.IGNORE_CASE)
                    orPattern.find(receiptId)?.groupValues?.get(1)?.toInt()
                }

                // Pattern 4: Extract last sequence of digits
                else -> {
                    val digitPattern = "\\d+".toRegex()
                    digitPattern.findAll(receiptId).lastOrNull()?.value?.toInt()
                }
            }
        } catch (e: Exception) {
            Log.e("ORCalculation", "Error extracting OR from receiptId: $receiptId", e)
            null
        }
    }
    private suspend fun calculateItemTotals(transactions: List<TransactionSummary>): ItemCalculationResult {
        return withContext(Dispatchers.IO) {
            val itemSales = mutableMapOf<String, ItemSalesSummary>()
            var totalGross = 0.0
            var totalQuantity = 0

            transactions.forEach { transaction ->
                val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                items.forEach { item ->
                    val key = item.name

                    val effectivePrice = if (item.priceOverride != null && item.priceOverride > 0.0) {
                        item.priceOverride
                    } else {
                        item.price
                    }

                    val itemTotal = effectivePrice * item.quantity

                    totalGross += itemTotal
                    totalQuantity += item.quantity

                    val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                        name = item.name,
                        quantity = 0,
                        totalAmount = 0.0,
                        itemGroup = item.itemGroup ?: "Unknown"
                    ))

                    itemSales[key] = currentSummary.copy(
                        quantity = currentSummary.quantity + item.quantity,
                        totalAmount = currentSummary.totalAmount + itemTotal
                    )
                }
            }

            ItemCalculationResult(
                totalGross = totalGross,
                totalQuantity = totalQuantity,
                itemSales = itemSales.values.sortedByDescending { it.totalAmount }
            )
        }
    }
    private fun showItemSalesDialog(dateStr: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_item_sales, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.itemSalesRecyclerView)
        val totalSalesTextView = dialogView.findViewById<TextView>(R.id.totalSalesTextView)
        val totalQuantityTextView = dialogView.findViewById<TextView>(R.id.totalQuantityTextView)
        val totalTransactionsTextView = dialogView.findViewById<TextView>(R.id.totalTransactionsTextView)
        val dateTextView = dialogView.findViewById<TextView>(R.id.dateTextView)
        val searchEditText = dialogView.findViewById<EditText>(R.id.searchEditText)
        val printButton = dialogView.findViewById<Button>(R.id.printButton)
        val closeButton = dialogView.findViewById<Button>(R.id.closeButton)

        // Use the same date formatting as showTransactionListDialog
        val selectedDateRange = if (isSameDay(startDate, endDate)) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)
        } else {
            "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}"
        }

        dateTextView.text = "Sales for $selectedDateRange"
        recyclerView.layoutManager = LinearLayoutManager(this)

        var itemSalesAdapter: ItemSalesAdapter? = null
        var groupedItemsList: List<ItemGroupSummary> = emptyList()
        var storedTotalQuantity = 0
        var storedTotalSales = 0.0

        lifecycleScope.launch {
            try {
                Log.d("ItemSalesDialog", "Query startDate: ${formatDateToString(startDate)}")
                Log.d("ItemSalesDialog", "Query endDate: ${formatDateToString(endDate)}")

                val transactions = withContext(Dispatchers.IO) {
                    // Use the same date formatting as showTransactionListDialog
                    val result = transactionDao.getTransactionsByDateRange(
                        formatDateToString(startDate),
                        formatDateToString(endDate)
                    )
                    Log.d("ItemSalesDialog", "Found ${result.size} transactions for item sales")
                    result.forEach { transaction ->
                        Log.d("ItemSalesDialog", "Transaction ${transaction.transactionId}: ${transaction.createdDate}")
                    }
                    result
                }


                // Get all items from these transactions and group them
                val itemSales = mutableMapOf<String, ItemSalesSummary>()
                var totalSales = 0.0
                var totalQuantity = 0

                withContext(Dispatchers.IO) {
                    transactions.forEach { transaction ->
                        val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                        Log.d("ItemSalesDialog", "Transaction ${transaction.transactionId} has ${items.size} items")

                        items.forEach { item ->
                            val key = item.name
                            val itemGroup = item.itemGroup ?: "Unknown"

                            val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                                name = item.name,
                                quantity = 0,
                                totalAmount = 0.0,
                                itemGroup = itemGroup
                            ))

                            val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                            val itemTotal = effectivePrice * item.quantity

                            totalQuantity += item.quantity

                            itemSales[key] = currentSummary.copy(
                                quantity = currentSummary.quantity + item.quantity,
                                totalAmount = currentSummary.totalAmount + itemTotal
                            )

                            totalSales += itemTotal
                        }
                    }
                }

                Log.d("ItemSalesDialog", "Final totals: $totalQuantity items, ₱$totalSales total sales")

                // Group items by itemGroup and sort
                val groupedItems = itemSales.values
                    .groupBy { it.itemGroup }
                    .map { (groupName, items) ->
                        ItemGroupSummary(
                            groupName = groupName,
                            items = items.sortedByDescending { it.totalAmount },
                            totalQuantity = items.sumOf { it.quantity },
                            totalAmount = items.sumOf { it.totalAmount }
                        )
                    }
                    .sortedByDescending { it.totalAmount }

                groupedItemsList = groupedItems
                storedTotalQuantity = totalQuantity
                storedTotalSales = totalSales

                withContext(Dispatchers.Main) {
                    itemSalesAdapter = ItemSalesAdapter(
                        groupedItems,
                        totalSales,
                        totalQuantity,
                        transactions.size
                    )
                    recyclerView.adapter = itemSalesAdapter

                    totalTransactionsTextView.text = transactions.size.toString()
                    totalQuantityTextView.text = totalQuantity.toString()
                    totalSalesTextView.text = String.format("₱%.2f", totalSales)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Error loading item sales: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Search functionality
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                itemSalesAdapter?.filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val dialog = android.app.AlertDialog.Builder(this, R.style.CustomDialogStyle1)
            .setView(dialogView)
            .create()

        printButton.setOnClickListener {
            if (groupedItemsList.isNotEmpty()) {
                printItemSalesReport(groupedItemsList, storedTotalQuantity, storedTotalSales)
            } else {
                Toast.makeText(
                    this@ReportsActivity,
                    "No data to print",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.apply {
            setBackgroundDrawableResource(R.drawable.dialog_background)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        dialog.show()
    }




    private fun updateUI(report: SalesReport) {
        // Update main summary cards with consistent calculations
        binding.totalGrossAmount.text = "₱${String.format("%.2f", report.totalGross)}"
        binding.totalNetSalesAmount.text = "₱${String.format("%.2f", report.totalNetSales)}"
        binding.totalDiscountAmount.text = "₱${String.format("%.2f", report.totalDiscount)}"

        // Update the statistics fields
        binding.totalCostLabel.text = "${report.totalQuantity}"           // Total quantity sold
        binding.totalVatLabel.text = "${report.totalTransactions}"        // Total transactions
        binding.vatableSalesLabel.text = "${report.totalReturns}"         // Total returns

        // NEW: Add total return amount display
        binding.totalReturnAmount?.text = "₱${String.format("%.2f", report.totalReturnAmount)}"

        // NEW: Add total return discount display
        binding.totalReturnDiscount?.text = "₱${String.format("%.2f", report.totalReturnDiscount)}"

        // NEW: Add starting OR number display
        binding.startingORNumber?.text = report.startingOR

        // NEW: Add ending OR number display
        binding.endingORNumber?.text = report.endingOR

        // Update VAT information
//        binding.vatAmount?.text = "₱${String.format("%.2f", report.vatAmount)}"
//        binding.vatableSalesAmount?.text = "₱${String.format("%.2f", report.vatableSales)}"

        // Update payment distribution and item sales
//        updatePaymentDistributionAndItemSales(report.paymentDistribution, report.itemSales)
//    }
        updatePaymentDistributionOnly(report.paymentDistribution)

        // Load ALL item sales from database (not from report.itemSales)
        createAllItemSales() // This now loads directly from database
    }


    private fun updatePaymentDistributionOnly(paymentDistribution: PaymentDistribution) {
        // Clear existing payment distribution views
        binding.paymentDistributionContainer.removeAllViews()

        // Create payment method views - only show methods with positive net amounts
        val paymentMethods = listOf(
            "Cash" to paymentDistribution.cash,
            "Card" to paymentDistribution.card,
            "GCash" to paymentDistribution.gCash,
            "PayMaya" to paymentDistribution.payMaya,
            "Loyalty Card" to paymentDistribution.loyaltyCard,
            "Charge" to paymentDistribution.charge,
            "Foodpanda" to paymentDistribution.foodpanda,
            "GrabFood" to paymentDistribution.grabfood,
            "Representation" to paymentDistribution.representation,
        ).filter { it.second != 0.0 } // Show all non-zero amounts (positive or negative)

        // Group payment methods into rows of 3
        val paymentMethodChunks = paymentMethods.chunked(3)

        paymentMethodChunks.forEach { rowMethods ->
            // Create a horizontal LinearLayout for each row
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 12.dpToPx()
                }
            }

            rowMethods.forEachIndexed { index, (method, amount) ->
                val cardView = createPaymentMethodView(method, amount)

                // Set layout params with weight for equal distribution
                cardView.layoutParams = LinearLayout.LayoutParams(
                    0,
                    120.dpToPx(),
                    1f
                ).apply {
                    when (index) {
                        0 -> marginEnd = 6.dpToPx()
                        1 -> {
                            marginStart = 6.dpToPx()
                            marginEnd = 6.dpToPx()
                        }
                        2 -> marginStart = 6.dpToPx()
                    }
                }

                rowLayout.addView(cardView)
            }

            // If row has less than 3 items, add empty views to maintain grid
            val emptyViewsNeeded = 3 - rowMethods.size
            repeat(emptyViewsNeeded) { index ->
                val emptyView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        120.dpToPx(),
                        1f
                    ).apply {
                        when (rowMethods.size + index) {
                            1 -> {
                                marginStart = 6.dpToPx()
                                marginEnd = 6.dpToPx()
                            }
                            2 -> marginStart = 6.dpToPx()
                        }
                    }
                }
                rowLayout.addView(emptyView)
            }

            binding.paymentDistributionContainer.addView(rowLayout)
        }

        // Calculate total payment amount (net of returns)
        val totalPayments = paymentMethods.sumOf { it.second }

        if (totalPayments != 0.0) {
            // Add a divider before total
            val dividerView = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dpToPx()
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
            }
            binding.paymentDistributionContainer.addView(dividerView)

            // Create total payment summary
            val totalPaymentCard = androidx.cardview.widget.CardView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 8.dpToPx(), 0, 8.dpToPx())
                }
                radius = 12f
                cardElevation = 4f
                setCardBackgroundColor(
                    if (totalPayments >= 0) {
                        resources.getColor(android.R.color.holo_green_dark, null)
                    } else {
                        resources.getColor(android.R.color.holo_red_dark, null)
                    }
                )
            }

            val totalPaymentLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }

            val totalPaymentText = TextView(this).apply {
                text = "NET TOTAL PAYMENTS"
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                textSize = 16f
                setTextColor(resources.getColor(android.R.color.white, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val totalPaymentAmountText = TextView(this).apply {
                text = "₱${String.format("%.2f", totalPayments)}"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                textSize = 20f
                gravity = Gravity.END
                setTextColor(resources.getColor(android.R.color.white, null))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            totalPaymentLayout.addView(totalPaymentText)
            totalPaymentLayout.addView(totalPaymentAmountText)
            totalPaymentCard.addView(totalPaymentLayout)
            binding.paymentDistributionContainer.addView(totalPaymentCard)
        }
    }
    private fun getVoidedTransactionsCount(transactions: List<TransactionSummary>): Int {
        // Count transactions with return comments (same logic as buildReadReport)
        return transactions.count { transaction ->
            transaction.comment.contains("Return:", ignoreCase = true) ||
                    transaction.comment.contains("Return processed:", ignoreCase = true)
        }
    }

    private fun createPaymentMethodView(method: String, amount: Double): androidx.cardview.widget.CardView {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(
                if (amount >= 0) {
                    resources.getColor(android.R.color.white, null)
                } else {
                    resources.getColor(android.R.color.holo_red_light, null)
                }
            )
        }

        val paymentView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            gravity = Gravity.CENTER_VERTICAL
        }

        // Payment method icon
        val iconTextView = TextView(this).apply {
            text = getPaymentMethodIcon(method)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4.dpToPx()
            }
            textSize = 20f
        }

        // Payment method name
        val methodNameTextView = TextView(this).apply {
            text = method.uppercase()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 4.dpToPx()
            }
            textSize = 10f
            setTextColor(Color.parseColor("#64748B"))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Amount with proper sign display
        val amountTextView = TextView(this).apply {
            text = if (amount < 0) {
                "-₱${String.format("%.2f", kotlin.math.abs(amount))}"
            } else {
                "₱${String.format("%.2f", amount)}"
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 20f
            setTextColor(
                if (amount >= 0) {
                    getPaymentMethodColor(method)
                } else {
                    resources.getColor(android.R.color.holo_red_dark, null)
                }
            )
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        paymentView.addView(iconTextView)
        paymentView.addView(methodNameTextView)
        paymentView.addView(amountTextView)

        cardView.addView(paymentView)
        return cardView
    }
    private fun getPaymentMethodColor(method: String): Int {
        return when (method.lowercase()) {
            "cash" -> Color.parseColor("#10B981") // Green like Net Sales
            "card" -> Color.parseColor("#3B82F6") // Blue like Total Gross
            "gcash" -> Color.parseColor("#8B5CF6") // Purple
            "paymaya" -> Color.parseColor("#F59E0B") // Orange like Discount
            "loyalty card", "loyaltycard" -> Color.parseColor("#EF4444") // Red
            "charge" -> Color.parseColor("#6B7280") // Gray
            "foodpanda" -> Color.parseColor("#E91E63") // Pink
            "grabfood" -> Color.parseColor("#4CAF50") // Green
            "representation" -> Color.parseColor("#FF9800") // Orange
//            "ar", "accounts receivable" -> Color.parseColor("#9C27B0") // Purple
            else -> Color.parseColor("#3B82F6") // Default blue
        }
    }
    private fun getPaymentMethodIcon(method: String): String {
        return when (method.lowercase()) {
            "cash" -> "💵"
            "card" -> "💳"
            "gcash" -> "📱"
            "paymaya" -> "💰"
            "loyalty card", "loyaltycard" -> "🎁"
            "charge" -> "📋"
            "foodpanda" -> "🍔"
            "grabfood" -> "🛵"
            "representation" -> "🤝"
//            "ar", "accounts receivable" -> "📊"
            else -> "💳" // Default icon
        }
    }
    private fun createItemSalesView(item: ItemSalesSummary): LinearLayout {
        val itemView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 2, 0, 2)
            }
            setPadding(16, 8, 16, 8)
            setBackgroundResource(android.R.color.white)
        }

        // Table header (only show once - you might want to handle this separately)
        // This would typically be added once at the top of your list

        // Table row for item
        val tableRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 4, 0, 4)
        }

        // Item name column (50% width)
        val itemNameTextView = TextView(this).apply {
            text = item.name
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.START
            setPadding(0, 0, 8, 0)
        }

        // Quantity column (25% width)
        val quantityTextView = TextView(this).apply {
            text = item.quantity.toString()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.CENTER
            setPadding(4, 0, 4, 0)
        }

        // Price column (25% width)
        val priceTextView = TextView(this).apply {
            text = "₱${String.format("%.2f", item.totalAmount)}"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.END
            setPadding(4, 0, 0, 0)
        }

        tableRow.addView(itemNameTextView)
        tableRow.addView(quantityTextView)
        tableRow.addView(priceTextView)

        itemView.addView(tableRow)

        // Add a subtle divider line
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, 4, 0, 0)
            }
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }

        itemView.addView(divider)

        return itemView
    }

    // Add this new function to create the total row
    private fun createTotalRow(totalQuantity: Int, totalAmount: Double): LinearLayout {
        val totalView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 2)
            }
            setPadding(16, 12, 16, 12)
            setBackgroundResource(android.R.color.white)
        }

        // Add a thicker divider line above total

        // Total row
        val totalRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 4, 0, 4)
        }

        // Total label column (50% width)
        val totalLabelTextView = TextView(this).apply {
            text = "TOTAL"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.START
            setPadding(0, 0, 8, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Total quantity column (25% width)
        val totalQuantityTextView = TextView(this).apply {
            text = totalQuantity.toString()
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.CENTER
            setPadding(4, 0, 4, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        // Total price column (25% width)
        val totalPriceTextView = TextView(this).apply {
            text = "₱${String.format("%.2f", totalAmount)}"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 12f
            setTextColor(resources.getColor(android.R.color.black, null))
            gravity = Gravity.END
            setPadding(4, 0, 0, 0)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        totalRow.addView(totalLabelTextView)
        totalRow.addView(totalQuantityTextView)
        totalRow.addView(totalPriceTextView)

        /*
                totalView.addView(thickDivider)
        */
        totalView.addView(totalRow)

        return totalView
    }

// Usage example: Add this after adding all your item views
// Make sure to add the total row to your parent container like this:
// val totalRow = createTotalRow(4, 100.0)
// parentContainer.addView(totalRow)


}