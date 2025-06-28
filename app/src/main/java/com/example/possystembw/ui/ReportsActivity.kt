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
import android.os.Build
import android.provider.Settings

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


    private lateinit var transactionRepository: TransactionRepository // Add this if not already present

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
        val paymentDistribution: PaymentDistribution = PaymentDistribution(),
        val itemSales: List<ItemSalesSummary> = emptyList()
    )
    data class ItemGroupSummary(
        val groupName: String,
        val items: List<ItemSalesSummary>,
        val totalQuantity: Int,
        val totalAmount: Double
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupDatePickers()
        setupButtons()
        loadTodaysReport()
        checkForExistingCashFund()
        setupAutomaticZRead()
        checkYesterdayTransactionsOnStartup()

    }

    // New method to check if current date has Z-Read
    private suspend fun hasZReadForCurrentDate(): Boolean {
        return withContext(Dispatchers.IO) {
            val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingZRead = zReadDao.getZReadByDate(currentDateString)
            existingZRead != null
        }
    }
    // X-Read functionality
    private fun performXRead() {

        lifecycleScope.launch {
            try {
                val currentDate = Date()
                val selectedDate = endDate // Using endDate as the selected date

                // Check if the selected date is today
                val isToday = isSameDay(currentDate, selectedDate)

                if (!isToday) {
                    // If not today, check if Z-Read exists for the selected date and offer to reprint
                    val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate)
                    val existingZRead = zReadDao.getZReadByDate(selectedDateString)

                    withContext(Dispatchers.Main) {
                        if (existingZRead != null) {
                            // Show dialog to reprint Z-Read for the selected date
                            val dialog = AlertDialog.Builder(this@ReportsActivity)
                                .setTitle("Z-Read Available")
                                .setMessage(
                                    "X-Read is only available for today's transactions.\n\n" +
                                            "A Z-Read exists for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}.\n\n" +
                                            "Would you like to reprint the Z-Read instead?"
                                )
                                .setPositiveButton("Reprint Z-Read") { _, _ ->
                                    reprintZRead(existingZRead)
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                            dialog.show()
                        } else {
                            // No Z-Read exists for the selected date
                            val dialog = AlertDialog.Builder(this@ReportsActivity)
                                .setTitle("X-Read Not Available")
                                .setMessage(
                                    "X-Read is only available for today's transactions go Z-Read first.\n\n" +
                                            "No Z-Read found for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate)}."
                                )
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .create()
                            dialog.show()
                        }
                    }
                    return@launch
                }

                // Get all transactions for today (regardless of Z-Read status)
                val todayStart = Calendar.getInstance().apply {
                    time = currentDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                val todayEnd = Calendar.getInstance().apply {
                    time = currentDate
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.time

                // Always get today's transactions - this shows actual sales data
                val transactions = transactionDao.getTransactionsByDateRange(todayStart, todayEnd)
                val currentTenderDeclaration = tenderDeclarationDao.getLatestTenderDeclaration()

                // Check if Z-Read exists for informational purposes only
                val hasZRead = hasZReadForCurrentDate()

                withContext(Dispatchers.Main) {
                    val dialog = AlertDialog.Builder(this@ReportsActivity)
                        .setTitle("Confirm X-Read")
                        .setMessage(
                            if (hasZRead) {
                                "X-Read will show today's actual sales data.\n(Z-Read has been completed for today)"
                            } else {
                                "X-Read will show current sales data for today."
                            }
                        )
                        .setPositiveButton("Yes") { _, _ ->
                            printXReadReport(transactions, currentTenderDeclaration, hasZRead)
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .create()

                    dialog.show()
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
            val cashFundEntity = Cashfund(cashFund = cashFund, status = status, date = getCurrentDate())
            cashFundRepository.insert(cashFundEntity)
        }
    }

    private fun checkForExistingCashFund() {
        lifecycleScope.launch {
            try {
                val currentDate = getCurrentDate()
                val existingCashFund = cashFundRepository.getCashFundByDate(currentDate)

                if (existingCashFund != null) {
                    currentCashFund = existingCashFund.cashFund
                    isCashFundEntered = true
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for existing cash fund", e)
            }
        }
    }

    private fun getCurrentDate(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Manila")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date())
    }


    private fun getCurrentTime(): String {
        val timeZone = TimeZone.getTimeZone("Asia/Manila")
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date())
    }

    // Rest of the existing methods...
    private fun showReprintZReadSelection() {
        lifecycleScope.launch {
            try {
                val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate)
                val existingZRead = zReadDao.getZReadByDate(selectedDateString)

                if (existingZRead != null) {
                    reprintZRead(existingZRead)
                } else {
                    Toast.makeText(
                        this@ReportsActivity,
                        "No Z-Read found for the selected date",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for Z-Read", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error checking Z-Read: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
            numberSequenceRemoteDao = numberSequenceRemoteDao
        )

        // REMOVE THE 'val' KEYWORD HERE - this should assign to the class property
        transactionRepository = TransactionRepository(
            transactionDao = transactionDao,
            numberSequenceRemoteRepository = numberSequenceRemoteRepository
        )

        val arApi = RetrofitClient.arApi
        val arDao = database.arDao()
        val arRepository = ARRepository(arApi, arDao)
        val arViewModelFactory = ARViewModelFactory(arRepository)
        arViewModel = ViewModelProvider(this, arViewModelFactory)[ARViewModel::class.java]

        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val todayString = dateFormat.format(currentDate.time)
        bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()

        // IMPORTANT: Inject the transactionDao into the printer helper
        bluetoothPrinterHelper.setTransactionDao(transactionDao)
        binding.startDatePickerButton.text = todayString
        binding.endDatePickerButton.text = todayString

        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        startDate = startOfDay.time
        endDate = endOfDay.time
    }

    private fun setupDatePickers() {
        binding.startDatePickerButton.setOnClickListener {
            showDatePicker(true) { selectedDate ->
                startDate = selectedDate
                binding.startDatePickerButton.text =
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(selectedDate)

                // If start date is after end date, adjust end date
                if (startDate.after(endDate)) {
                    endDate = selectedDate
                    binding.endDatePickerButton.text =
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(selectedDate)
                }

                loadReportForDateRange()
            }
        }

        binding.endDatePickerButton.setOnClickListener {
            showDatePicker(false) { selectedDate ->
                endDate = selectedDate
                binding.endDatePickerButton.text =
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(selectedDate)

                // If end date is before start date, automatically adjust start date
                if (endDate.before(startDate)) {
                    startDate = Calendar.getInstance().apply {
                        time = selectedDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.time
                    binding.startDatePickerButton.text =
                        SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(startDate)
                }

                loadReportForDateRange()
            }
        }
    }

    private fun showDatePicker(isStartDate: Boolean, onDateSelected: (Date) -> Unit) {
        val currentDate = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                    if (isStartDate) {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                }
                onDateSelected(selectedCalendar.time)
            },
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH),
            currentDate.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun setupButtons() {
        // Show the cash management buttons
//        binding.xreadButton.visibility = android.view.View.VISIBLE
//        binding.cashfundButton.visibility = android.view.View.VISIBLE
//        binding.pulloutButton.visibility = android.view.View.VISIBLE
//        binding.tenderButton.visibility = android.view.View.VISIBLE

        // Set up button click listeners
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

        // Z-Read functionality
        binding.zreadButton.setOnClickListener {
            checkForExistingZRead()
        }
        binding.itemsalesButton.setOnClickListener {
            // Remove the hardcoded current date logic
            val selectedDateRange = if (isSameDay(startDate, endDate)) {
                SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)
            } else {
                "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}"
            }
            showItemSalesDialog(selectedDateRange)
        }



        // Add reprint Z-Read button
        binding.reprintZreadButton.setOnClickListener {
            showReprintZReadSelection()
        }
    }
    private fun checkForExistingZRead() {
        lifecycleScope.launch {
            try {
                val selectedDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate)

                // Check if there's already a Z-Read record for this date
                val existingZRead = zReadDao.getZReadByDate(selectedDateString)

                if (existingZRead != null) {
                    showReprintZReadDialog(existingZRead)
                    return@launch
                }

                // Get transactions for the selected date
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
                        .filter { it.transactionStatus == 1 } // Only completed transactions
                }

                if (transactions.isEmpty()) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "No completed transactions found for Z-Read generation",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Check if any of these transactions already have a zReportId
                val transactionsWithZRead = transactions.filter {
                    !it.zReportId.isNullOrEmpty()
                }

                if (transactionsWithZRead.isNotEmpty()) {
                    // Some transactions already have Z-Read assigned
                    val firstZReportId = transactionsWithZRead.first().zReportId

                    // Check if all transactions have the same zReportId
                    val allSameZReportId = transactionsWithZRead.all {
                        it.zReportId == firstZReportId
                    }

                    if (allSameZReportId && transactionsWithZRead.size == transactions.size) {
                        // All transactions have the same Z-Report ID, treat as existing Z-Read
                        showExistingZReadDialog(firstZReportId!!, transactions)
                    } else {
                        // Mixed state - some transactions have Z-Read, some don't
                        showMixedZReadStateDialog(transactions, transactionsWithZRead)
                    }
                } else {
                    // No transactions have Z-Read assigned, proceed with normal Z-Read generation
                    showZReadConfirmationDialog(transactions.size)
                }

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error checking for existing Z-Read", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error checking Z-Read status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                    "Would you like to reprint it?")
            .setPositiveButton("Reprint") { _, _ ->
                reprintExistingZRead(zReportId, transactions)
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
                // Use Philippines timezone
                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = philippinesTimeZone
                }.format(yesterday)

                // Check if Z-Read already exists for yesterday
                val existingZRead = withContext(Dispatchers.IO) {
                    zReadDao.getZReadByDate(yesterdayString)
                }

                if (existingZRead != null) {
                    Log.d("AutoZRead", "Z-Read already exists for $yesterdayString")
                    return@launch
                }

                // Get transactions for yesterday
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
                    transactionDao.getTransactionsByDateRange(yesterdayStart, yesterdayEnd)
                        .filter { it.transactionStatus == 1 } // Only completed transactions
                }

                if (allTransactions.isEmpty()) {
                    Log.d("AutoZRead", "No transactions found for $yesterdayString")
                    return@launch
                }

                // IMPORTANT: Only process transactions that don't have Z-Report ID
                val transactionsWithoutZRead = allTransactions.filter {
                    it.zReportId.isNullOrEmpty() || it.zReportId!!.isBlank()
                }

                if (transactionsWithoutZRead.isEmpty()) {
                    Log.d("AutoZRead", "All transactions already have Z-Read for $yesterdayString")
                    return@launch
                }

                Log.d("AutoZRead", "Found ${transactionsWithoutZRead.size} transactions without Z-Read for $yesterdayString")

                // Generate automatic Z-Read for only these specific transactions
                generateAutomaticZReadSilent(transactionsWithoutZRead, yesterdayString)

            } catch (e: Exception) {
                Log.e("AutoZRead", "Error in automatic Z-Read check", e)
            }
        }
    }
    private fun checkYesterdayTransactionsOnStartup() {
        lifecycleScope.launch {
            try {
                Log.d("StartupZRead", "Checking for yesterday's transactions without Z-Read...")

                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = philippinesTimeZone
                }.format(yesterday)

                // Check if Z-Read already exists for yesterday
                val existingZRead = withContext(Dispatchers.IO) {
                    zReadDao.getZReadByDate(yesterdayString)
                }

                if (existingZRead != null) {
                    Log.d("StartupZRead", "Z-Read already exists for $yesterdayString")
                    return@launch
                }

                // Get yesterday's date range
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

                // Get all completed transactions from yesterday
                val yesterdayTransactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(yesterdayStart, yesterdayEnd)
                        .filter { it.transactionStatus == 1 } // Only completed transactions
                }

                if (yesterdayTransactions.isEmpty()) {
                    Log.d("StartupZRead", "No transactions found for $yesterdayString")
                    return@launch
                }

                // Find transactions without Z-Read ID
                val transactionsWithoutZRead = yesterdayTransactions.filter {
                    it.zReportId.isNullOrEmpty() || it.zReportId!!.isBlank()
                }

                if (transactionsWithoutZRead.isEmpty()) {
                    Log.d("StartupZRead", "All yesterday's transactions already have Z-Read")
                    return@launch
                }

                Log.i("StartupZRead", "Found ${transactionsWithoutZRead.size} transactions from $yesterdayString without Z-Read. Auto-generating Z-Read...")

                // Show a notification to staff (optional)
//                withContext(Dispatchers.Main) {
////                    showAutoZReadNotification(yesterdayString, transactionsWithoutZRead.size)
//                }

                // Generate automatic Z-Read for yesterday's transactions
                generateAutomaticZReadSilent(transactionsWithoutZRead, yesterdayString)

            } catch (e: Exception) {
                Log.e("StartupZRead", "Error checking yesterday's transactions on startup", e)
            }
        }
    }

    private suspend fun generateAutomaticZReadSilent(
        transactions: List<TransactionSummary>,
        dateString: String
    ) {
        try {
            // Generate Z-Report ID
            val zReportId = generateZReportId()

            Log.d("AutoZRead", "Generating Z-Read #$zReportId for $dateString with ${transactions.size} transactions")

            // Update ONLY the specific transactions with Z-Report ID
            withContext(Dispatchers.IO) {
                transactions.forEach { transaction ->
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
                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply {
                    timeZone = philippinesTimeZone
                }.format(Date())

                val zReadRecord = ZRead(
                    zReportId = zReportId,
                    date = dateString,
                    time = currentTime,
                    totalTransactions = transactions.size,
                    totalAmount = transactions.sumOf { it.netAmount }
                )
                zReadDao.insert(zReadRecord)
            }

            Log.i("AutoZRead", "Successfully generated automatic Z-Read #$zReportId for $dateString")

            // Optional: Try to sync with server (but don't fail if it doesn't work)
            val storeId = SessionManager.getCurrentUser()?.storeid
            if (storeId != null) {
                try {
                    val syncResult = transactionRepository.updateTransactionsZReport(storeId, zReportId)
                    syncResult.onSuccess {
                        Log.d("AutoZRead", "Successfully synced Z-Read to server")
                    }.onFailure {
                        Log.w("AutoZRead", "Server sync failed but local Z-Read completed")
                    }
                } catch (e: Exception) {
                    Log.w("AutoZRead", "Server sync failed: ${e.message}")
                }
            }

        } catch (e: Exception) {
            Log.e("AutoZRead", "Error generating automatic Z-Read for $dateString", e)
        }
    }
    private fun manualCheckYesterdayTransactions() {
        lifecycleScope.launch {
            try {
                val philippinesTimeZone = TimeZone.getTimeZone("Asia/Manila")
                val yesterday = Calendar.getInstance(philippinesTimeZone).apply {
                    add(Calendar.DAY_OF_MONTH, -1)
                }.time

                val yesterdayString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                    timeZone = philippinesTimeZone
                }.format(yesterday)

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
                    transactionDao.getTransactionsByDateRange(yesterdayStart, yesterdayEnd)
                        .filter { it.transactionStatus == 1 }
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
                Log.e("ManualZRead", "Error checking yesterday's transactions", e)
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
            .setMessage("Z-Read for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)} already exists.\n\nZ-Report ID: ${zRead.zReportId}\nTime: ${zRead.time}\n\nWould you like to reprint it?")
            .setPositiveButton("Reprint") { _, _ ->
                reprintZRead(zRead)
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
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
                }

                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                val reportContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    isZRead = true,
                    zReportId = zRead.zReportId,
                    tenderDeclaration = tenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(reportContent)) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Z-Read reprinted successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Failed to reprint Z-Read",
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
                        transactionDao.getTransactionsByDateRange(startDate, endDate)
                            .filter { it.transactionStatus == 1 }
                    }
                }

                if (transactions.isEmpty()) {
                    Toast.makeText(
                        this@ReportsActivity,
                        "No completed transactions found for the selected date range",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Generate Z-Report ID asynchronously
                val zReportId = generateZReportId()
                Log.d("ZRead", "Generated Z-Report ID: $zReportId")

                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                val reportContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    true,
                    zReportId,
                    tenderDeclaration
                )

                // Update specific transactions with Z-Report ID and sync to server
                val storeId = SessionManager.getCurrentUser()?.storeid
                if (storeId != null) {
                    val updateResult = updateSpecificTransactionsWithZReportId(storeId, zReportId, transactions)

                    if (updateResult.isSuccess) {
                        Log.d("ZRead", "Successfully updated transactions with Z-Report ID: $zReportId")
                    } else {
                        Log.w("ZRead", "Failed to sync Z-Report ID to server, but continuing with local update")
                    }
                }

                if (bluetoothPrinterHelper.printGenericReceipt(reportContent)) {
                    // Save Z-Read record after successful printing
                    saveZReadRecord(zReportId, transactions)

                    Toast.makeText(
                        this@ReportsActivity,
                        "Z-Read #$zReportId completed successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    // Refresh the data
                    loadReportForDateRange()
                } else {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Failed to print Z-Read #$zReportId",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error generating Z-Read", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error generating Z-Read: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate)
                    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

                    val zReadRecord = ZRead(
                        zReportId = zReportId,
                        date = currentDate,
                        time = currentTime,
                        totalTransactions = transactions.filter { it.transactionStatus == 1 }.size,
                        totalAmount = transactions.filter { it.transactionStatus == 1 }.sumOf { it.netAmount }
                    )
                    zReadDao.insert(zReadRecord)
                }
            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error saving Z-Read record", e)
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

    private fun loadTodaysReport() {
        loadReportForDateRange()
    }

    private fun loadReportForDateRange() {
        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
                }

                val report = calculateSalesReport(transactions)
                updateUI(report)

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error loading report", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error loading report: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
            val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val existingZRead = zReadDao.getZReadByDate(currentDateString)
            existingZRead != null
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
            Toast.makeText(this, "Failed to print Tender Declaration Receipt", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePaymentDistributionAndItemSales(
        paymentDistribution: PaymentDistribution,
        itemSales: List<ItemSalesSummary>
    ) {
        // Clear existing payment distribution views
        binding.paymentDistributionContainer.removeAllViews()

        // Create payment method views
        val paymentMethods = listOf(
            "Cash" to paymentDistribution.cash,
            "Card" to paymentDistribution.card,
            "GCash" to paymentDistribution.gCash,
            "PayMaya" to paymentDistribution.payMaya,
            "Loyalty Card" to paymentDistribution.loyaltyCard,
            "Charge" to paymentDistribution.charge,
            "Foodpanda" to paymentDistribution.foodpanda,
            "GrabFood" to paymentDistribution.grabfood,
            "Representation" to paymentDistribution.representation
//            "AR" to paymentDistribution.ar
        ).filter { it.second > 0 }

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

        // Calculate total payment amount
        val totalPayments = paymentMethods.sumOf { it.second }

        if (totalPayments > 0.0) {
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
                setCardBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
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
                text = "TOTAL PAYMENTS"
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

//            totalPaymentLayout.addView(totalPaymentText)
//            totalPaymentLayout.addView(totalPaymentAmountText)
//            totalPaymentCard.addView(totalPaymentLayout)
            binding.paymentDistributionContainer.addView(totalPaymentCard)
        }

        // Update item sales with grouping
        createAllItemSales(itemSales)
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
    private fun createAllItemSales(itemSales: List<ItemSalesSummary>) {
        binding.itemSalesContainer.removeAllViews()

        if (itemSales.isEmpty()) {
            binding.noItemSalesText.visibility = android.view.View.VISIBLE
            binding.viewAllItemsButton.visibility = android.view.View.GONE
            return
        }

        binding.noItemSalesText.visibility = android.view.View.GONE

        // Group items by itemGroup
        val groupedItems = itemSales
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

        // Create views for each group
        groupedItems.forEach { group ->
            // Create group header
            val groupHeaderView = createGroupHeaderView(group)
            binding.itemSalesContainer.addView(groupHeaderView)

            // Add ALL items from this group
            group.items.forEach { item ->
                val itemView = createItemSalesView(item)
                binding.itemSalesContainer.addView(itemView)
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

        val totalQuantity = itemSales.sumOf { it.quantity }
        val totalAmount = itemSales.sumOf { it.totalAmount }

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
            text = "GRAND TOTAL (${totalQuantity} items)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val totalAmountText = TextView(this).apply {
            text = "₱${String.format("%.2f", totalAmount)}"
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
                printItemSalesReport(groupedItems, totalQuantity, totalAmount)
            }
        }
        binding.itemSalesContainer.addView(printButton)

        // Hide "View All Items" button since we're showing all items
        binding.viewAllItemsButton.visibility = android.view.View.GONE
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
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
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
        val validTransactions = transactions.filter { it.transactionStatus == 1 } // Only completed transactions
        val returnTransactions = transactions.filter { it.type == 2 } // Assuming type 2 is returns

        // Calculate totals from individual items instead of transaction summaries
        val itemCalculations = calculateItemTotals(validTransactions)

        val totalGross = itemCalculations.totalGross
        val totalNetSales = itemCalculations.totalGross - validTransactions.sumOf { it.totalDiscountAmount }
        val totalDiscount = validTransactions.sumOf { it.totalDiscountAmount }
        val totalQuantity = itemCalculations.totalQuantity

        // Count total transactions and returns
        val totalTransactions = validTransactions.size
        val totalReturns = returnTransactions.size

        val paymentDistribution = PaymentDistribution(
            cash = validTransactions.sumOf { it.cash },
            card = validTransactions.sumOf { it.card },
            gCash = validTransactions.sumOf { it.gCash },
            payMaya = validTransactions.sumOf { it.payMaya },
            loyaltyCard = validTransactions.sumOf { it.loyaltyCard },
            charge = validTransactions.sumOf { it.charge },
            foodpanda = validTransactions.sumOf { it.foodpanda },
            grabfood = validTransactions.sumOf { it.grabfood },
            representation = validTransactions.sumOf { it.representation },
            ar = validTransactions.filter { it.type == 3 }.sumOf { it.netAmount }
        )

        return SalesReport(
            totalGross = totalGross,
            totalNetSales = totalNetSales,
            totalDiscount = totalDiscount,
            totalQuantity = totalQuantity,
            totalTransactions = totalTransactions,
            totalReturns = totalReturns,
            paymentDistribution = paymentDistribution,
            itemSales = itemCalculations.itemSales
        )
    }

    // New data class to hold item calculation results
    data class ItemCalculationResult(
        val totalGross: Double,
        val totalQuantity: Int,
        val itemSales: List<ItemSalesSummary>
    )
    private suspend fun calculateItemTotals(transactions: List<TransactionSummary>): ItemCalculationResult {
        return withContext(Dispatchers.IO) {
            val itemSales = mutableMapOf<String, ItemSalesSummary>()
            var totalGross = 0.0
            var totalQuantity = 0

            transactions.forEach { transaction ->
                val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                items.forEach { item ->
                    val key = item.name

                    // Get product details to fetch itemgroup
                    val product =
                        item.itemId?.let { transactionDao.getProductByItemId(it) } // You'll need this method
                    val itemGroup = product?.itemGroup ?: "Unknown"

                    // Use consistent pricing logic
                    val effectivePrice = if (item.priceOverride != null && item.priceOverride > 0.0) {
                        item.priceOverride
                    } else {
                        item.price
                    }

                    val itemTotal = effectivePrice * item.quantity

                    // Add to gross total
                    totalGross += itemTotal
                    totalQuantity += item.quantity

                    // Update item sales summary with itemGroup
                    val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                        name = item.name,
                        quantity = 0,
                        totalAmount = 0.0,
                        itemGroup = itemGroup
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

        // Use the selected date range instead of hardcoded current date
        val selectedDateRange = if (isSameDay(startDate, endDate)) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)
        } else {
            "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}"
        }

        dateTextView.text = "Sales for $selectedDateRange"
        recyclerView.layoutManager = LinearLayoutManager(this)

        var itemSalesAdapter: ItemSalesAdapter? = null
        var groupedItemsList: List<ItemGroupSummary> = emptyList() // Store for print functionality
        var storedTotalQuantity = 0
        var storedTotalSales = 0.0

        lifecycleScope.launch {
            try {
                // Use the class-level startDate and endDate instead of parsing dateStr
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
                }

                // Get all items from these transactions and group them
                val itemSales = mutableMapOf<String, ItemSalesSummary>()
                var totalSales = 0.0
                var totalQuantity = 0

                withContext(Dispatchers.IO) {
                    transactions.forEach { transaction ->
                        val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                        items.forEach { item ->
                            val key = item.name

                            // Get product details for itemgroup
                            val product = item.itemId?.let { transactionDao.getProductByItemId(it) }
                            val itemGroup = product?.itemGroup ?: "Unknown"

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

                // Store grouped items and totals for print functionality
                groupedItemsList = groupedItems
                storedTotalQuantity = totalQuantity
                storedTotalSales = totalSales

                // Update UI on main thread
                withContext(Dispatchers.Main) {
                    itemSalesAdapter = ItemSalesAdapter(
                        groupedItems,
                        totalSales,
                        totalQuantity,
                        transactions.size
                    )
                    recyclerView.adapter = itemSalesAdapter

                    // Update summary views
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

        // Create dialog first
        val dialog = android.app.AlertDialog.Builder(this, R.style.CustomDialogStyle1)
            .setView(dialogView)
            .create()

        // Print functionality - use the ReportsActivity's print method
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
            dialog.dismiss() // Optionally close dialog after printing
        }

        // Close button functionality
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
        // Update main summary cards
        binding.totalGrossAmount.text = "₱${String.format("%.2f", report.totalGross)}"
        binding.totalNetSalesAmount.text = "₱${String.format("%.2f", report.totalNetSales)}"
        binding.totalDiscountAmount.text = "₱${String.format("%.2f", report.totalDiscount)}"

        // Update the changed fields
        binding.totalCostLabel.text = "${report.totalQuantity}"           // Now shows total quantity
        binding.totalVatLabel.text = "${report.totalTransactions}"        // Now shows total transactions
        binding.vatableSalesLabel.text = "${report.totalReturns}"         // Now shows total returns

        // Update payment distribution and item sales
        updatePaymentDistributionAndItemSales(report.paymentDistribution, report.itemSales)
    }


    private fun createPaymentMethodView(method: String, amount: Double): androidx.cardview.widget.CardView {
        val cardView = androidx.cardview.widget.CardView(this).apply {
            radius = 12f
            cardElevation = 2f
            setCardBackgroundColor(resources.getColor(android.R.color.white, null))
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

        // Payment method name (styled like summary card labels)
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

        // Amount (styled like summary card amounts)
        val amountTextView = TextView(this).apply {
            text = "₱${String.format("%.2f", amount)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 20f
            setTextColor(getPaymentMethodColor(method))
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
    override fun onResume() {
        super.onResume()

        // Check if permission was granted and reschedule if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                // Permission was granted, schedule exact alarm
                scheduleAutomaticZRead()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        try {
            if (::autoZReadReceiver.isInitialized) {
                unregisterReceiver(autoZReadReceiver)
            }
        } catch (e: Exception) {
            Log.e("AutoZRead", "Error unregistering receiver", e)
        }
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