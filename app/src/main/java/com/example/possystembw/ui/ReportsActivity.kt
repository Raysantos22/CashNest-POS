package com.example.possystembw.ui

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.DAO.*
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.*
import com.example.possystembw.databinding.ActivityReportsBinding
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReportsBinding
    private lateinit var transactionDao: TransactionDao
    private lateinit var zReadDao: ZReadDao
    private lateinit var bluetoothPrinterHelper: BluetoothPrinterHelper
    private lateinit var tenderDeclarationDao: TenderDeclarationDao
    private var startDate: Date = Date()
    private var endDate: Date = Date()

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
        val totalAmount: Double
    )

    data class SalesReport(
        val totalGross: Double = 0.0,
        val totalNetSales: Double = 0.0,
        val totalDiscount: Double = 0.0,
        val totalCost: Double = 0.0,
        val totalVat: Double = 0.0,
        val vatableSales: Double = 0.0,
        val paymentDistribution: PaymentDistribution = PaymentDistribution(),
        val itemSales: List<ItemSalesSummary> = emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReportsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeComponents()
        setupDatePickers()
        setupButtons()
        loadTodaysReport()
    }
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

    // 3. Update payment distribution to show 2 columns
    private fun updatePaymentDistributionAndItemSales(paymentDistribution: PaymentDistribution, itemSales: List<ItemSalesSummary>) {
        binding.paymentDistributionContainer.removeAllViews()
        binding.itemSalesContainer.removeAllViews()

        // Create payment distribution in 2 columns
        createPaymentDistributionTwoColumns(paymentDistribution)

        // Show ALL item sales (not just top 10)
        createAllItemSales(itemSales)
    }

    // 4. New method for 2-column payment distribution
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
            "AR" to paymentDistribution.ar
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

        // Create rows with 2 columns each
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

            // First column
            val firstMethod = methods[i]
            val firstPaymentView = createPaymentMethodView(firstMethod.first, firstMethod.second)
            firstPaymentView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            rowLayout.addView(firstPaymentView)

            // Second column (if exists)
            if (i + 1 < methods.size) {
                val secondMethod = methods[i + 1]
                val secondPaymentView = createPaymentMethodView(secondMethod.first, secondMethod.second)
                secondPaymentView.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginStart = 8
                }
                rowLayout.addView(secondPaymentView)
            } else {
                // Add empty space for alignment
                val emptyView = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                rowLayout.addView(emptyView)
            }

            binding.paymentDistributionContainer.addView(rowLayout)
        }
    }

    // 5. New method to show ALL item sales
    private fun createAllItemSales(itemSales: List<ItemSalesSummary>) {
        if (itemSales.isEmpty()) {
            val noItemsText = TextView(this).apply {
                text = "No items sold for this date range"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = Gravity.CENTER
                setPadding(16, 32, 16, 32)
            }
            binding.itemSalesContainer.addView(noItemsText)
            return
        }

        // Show ALL items (not just top 10)
        itemSales.forEach { item ->
            val itemView = createItemSalesView(item)
            binding.itemSalesContainer.addView(itemView)
        }

        // Add total summary at the end
        val totalQuantity = itemSales.sumOf { it.quantity }
        val totalAmount = itemSales.sumOf { it.totalAmount }

        val summaryView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 16, 0, 8)
            }
            setPadding(12, 12, 12, 12)
            setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }

        val totalText = TextView(this).apply {
            text = "TOTAL (${itemSales.size} items)"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 14f
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val totalAmountText = TextView(this).apply {
            text = "₱${String.format("%.2f", totalAmount)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 14f
            gravity = Gravity.END
            setTextColor(resources.getColor(android.R.color.white, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        summaryView.addView(totalText)
        summaryView.addView(totalAmountText)
        binding.itemSalesContainer.addView(summaryView)
    }
    private fun initializeComponents() {
        val database = AppDatabase.getDatabase(this)
        transactionDao = database.transactionDao()
        zReadDao = database.zReadDao()
        tenderDeclarationDao = database.tenderDeclarationDao()
        bluetoothPrinterHelper = BluetoothPrinterHelper.getInstance()

        // Set current date as default for both start and end dates
        val currentDate = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        val todayString = dateFormat.format(currentDate.time)

        // Set both buttons to today's date
        binding.startDatePickerButton.text = todayString
        binding.endDatePickerButton.text = todayString

        // Set both dates to today at specific times
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
                loadReportForDateRange()
            }
        }

        binding.endDatePickerButton.setOnClickListener {
            showDatePicker(false) { selectedDate ->
                endDate = selectedDate
                binding.endDatePickerButton.text =
                    SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(selectedDate)
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
        // Hide unwanted buttons
        binding.xreadButton.visibility = android.view.View.GONE
        binding.cashfundButton.visibility = android.view.View.GONE
        binding.pulloutButton.visibility = android.view.View.GONE
        binding.tenderButton.visibility = android.view.View.GONE

        // Z-Read functionality
        binding.zreadButton.setOnClickListener {
            checkForExistingZRead()
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
                val existingZRead = zReadDao.getZReadByDate(selectedDateString)

                if (existingZRead != null) {
                    showReprintZReadDialog(existingZRead)
                } else {
                    showZReadConfirmationDialog()
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

    private fun showReprintZReadDialog(zRead: ZRead) {
        AlertDialog.Builder(this)
            .setTitle("Z-Read Already Exists")
            .setMessage("Z-Read for ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)} already exists.\n\nZ-Report ID: ${zRead.zReportId}\nTime: ${zRead.time}\n\nWould you like to reprint it?")
            .setPositiveButton("Reprint") { _, _ ->
                reprintZRead(zRead)
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private fun showZReadConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Z-Read Confirmation")
            .setMessage("Z-Read will generate a final sales report for the selected date range. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                generateZRead()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun generateZRead() {
        lifecycleScope.launch {
            try {
                val transactions = withContext(Dispatchers.IO) {
                    transactionDao.getTransactionsByDateRange(startDate, endDate)
                }

                val zReportId = generateZReportId()
                val tenderDeclaration = withContext(Dispatchers.IO) {
                    tenderDeclarationDao.getLatestTenderDeclaration()
                }

                val reportContent = bluetoothPrinterHelper.buildReadReport(
                    transactions,
                    true,
                    zReportId,
                    tenderDeclaration
                )

                if (bluetoothPrinterHelper.printGenericReceipt(reportContent)) {
                    // Save Z-Read record
                    saveZReadRecord(zReportId, transactions)

                    Toast.makeText(
                        this@ReportsActivity,
                        "Z-Read completed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this@ReportsActivity,
                        "Failed to print Z-Read",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {
                Log.e("ReportsActivity", "Error generating Z-Read", e)
                Toast.makeText(
                    this@ReportsActivity,
                    "Error generating Z-Read: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun generateZReportId(): String {
        val sharedPreferences = getSharedPreferences("ZReadPreferences", Context.MODE_PRIVATE)
        val currentNumber = sharedPreferences.getInt("lastSequentialNumber", 0)
        val nextNumber = currentNumber + 1

        with(sharedPreferences.edit()) {
            putInt("lastSequentialNumber", nextNumber)
            apply()
        }

        return String.format("%08d", nextNumber)
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

    private suspend fun calculateSalesReport(transactions: List<TransactionSummary>): SalesReport {
        val validTransactions = transactions.filter { it.transactionStatus == 1 } // Only completed transactions

        val totalGross = validTransactions.sumOf { it.grossAmount }
        val totalNetSales = validTransactions.sumOf { it.netAmount }
        val totalDiscount = validTransactions.sumOf { it.totalDiscountAmount }
        val totalVat = validTransactions.sumOf { it.vatAmount }
        val vatableSales = validTransactions.sumOf { it.vatableSales }

        // Calculate total cost (you might need to adjust this based on your product cost calculation)
        val totalCost = validTransactions.sumOf { it.vatableSales } // Add your cost calculation logic here

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

        // Calculate item sales
        val itemSales = calculateItemSales(validTransactions)

        return SalesReport(
            totalGross = totalGross,
            totalNetSales = totalNetSales,
            totalDiscount = totalDiscount,
            totalCost = totalCost,
            totalVat = totalVat,
            vatableSales = vatableSales,
            paymentDistribution = paymentDistribution,
            itemSales = itemSales
        )
    }

    private suspend fun calculateItemSales(transactions: List<TransactionSummary>): List<ItemSalesSummary> {
        return withContext(Dispatchers.IO) {
            val itemSales = mutableMapOf<String, ItemSalesSummary>()

            transactions.forEach { transaction ->
                val items = transactionDao.getTransactionRecordsByTransactionId(transaction.transactionId)
                items.forEach { item ->
                    val key = item.name
                    val currentSummary = itemSales.getOrDefault(key, ItemSalesSummary(
                        name = item.name,
                        quantity = 0,
                        totalAmount = 0.0
                    ))

                    val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
                    val itemTotal = effectivePrice * item.quantity

                    itemSales[key] = currentSummary.copy(
                        quantity = currentSummary.quantity + item.quantity,
                        totalAmount = currentSummary.totalAmount + itemTotal
                    )
                }
            }

            // Sort items by total amount (descending)
            itemSales.values.sortedByDescending { it.totalAmount }
        }
    }

    private fun updateUI(report: SalesReport) {
        // Update main summary cards
        binding.totalGrossAmount.text = "₱${String.format("%.2f", report.totalGross)}"
        binding.totalNetSalesAmount.text = "₱${String.format("%.2f", report.totalNetSales)}"
        binding.totalDiscountAmount.text = "₱${String.format("%.2f", report.totalDiscount)}"
        binding.totalCostAmount.text = "₱${String.format("%.2f", report.totalCost)}"
        binding.totalVatAmount.text = "₱${String.format("%.2f", report.totalVat)}"
        binding.vatableSalesAmount.text = "₱${String.format("%.2f", report.vatableSales)}"

        // Update payment distribution and item sales
        updatePaymentDistributionAndItemSales(report.paymentDistribution, report.itemSales)
    }

//    private fun updatePaymentDistributionAndItemSales(paymentDistribution: PaymentDistribution, itemSales: List<ItemSalesSummary>) {
//        binding.paymentDistributionContainer.removeAllViews()
//
//        // Create horizontal layout for 2 columns
//        val horizontalLayout = LinearLayout(this).apply {
//            orientation = LinearLayout.HORIZONTAL
//            layoutParams = LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT,
//                LinearLayout.LayoutParams.WRAP_CONTENT
//            )
//        }
//
//        // Payment Distribution Column
//        val paymentColumn = createPaymentDistributionColumn(paymentDistribution)
//        horizontalLayout.addView(paymentColumn)
//
//        // Item Sales Column
//        val itemSalesColumn = createItemSalesColumn(itemSales)
//        horizontalLayout.addView(itemSalesColumn)
//
//        binding.paymentDistributionContainer.addView(horizontalLayout)
//    }

    private fun createPaymentDistributionColumn(paymentDistribution: PaymentDistribution): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 12
            }
            setPadding(16, 0, 8, 0)
        }

        // Payment Distribution Title
        val paymentTitle = TextView(this).apply {
            text = "Payment Distribution"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
        column.addView(paymentTitle)

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
            "AR" to paymentDistribution.ar
        )

        val hasPayments = paymentMethods.any { it.value > 0 }

        if (hasPayments) {
            paymentMethods.forEach { (method, amount) ->
                if (amount > 0) {
                    val paymentView = createPaymentMethodView(method, amount)
                    column.addView(paymentView)
                }
            }
        } else {
            val noPaymentsText = TextView(this).apply {
                text = "No payments recorded"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }
            column.addView(noPaymentsText)
        }

        return column
    }

    private fun createItemSalesColumn(itemSales: List<ItemSalesSummary>): LinearLayout {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 12
            }
            setPadding(8, 0, 16, 0)
        }

        // Item Sales Title with Date Range
        val dateRange = if (startDate.time == endDate.time) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate)
        } else {
            "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(startDate)} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(endDate)}"
        }

        val itemSalesTitle = TextView(this).apply {
            text = "Item Sales ($dateRange)"
            textSize = 16f
            setTextColor(resources.getColor(android.R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 12
            }
        }
        column.addView(itemSalesTitle)

        if (itemSales.isNotEmpty()) {
            // Show top 10 items or all if less than 10
            val displayItems = itemSales.take(10)
            displayItems.forEach { item ->
                val itemView = createItemSalesView(item)
                column.addView(itemView)
            }

            // If there are more items, show count
            if (itemSales.size > 10) {
                val moreItemsText = TextView(this).apply {
                    text = "... and ${itemSales.size - 10} more items"
                    textSize = 12f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = 8
                    }
                }
                column.addView(moreItemsText)
            }
        } else {
            val noItemsText = TextView(this).apply {
                text = "No items sold"
                textSize = 14f
                setTextColor(resources.getColor(android.R.color.darker_gray, null))
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 16
                }
            }
            column.addView(noItemsText)
        }

        return column
    }

    private fun createPaymentMethodView(method: String, amount: Double): LinearLayout {
        val paymentView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setPadding(12, 8, 12, 8)
            setBackgroundResource(android.R.color.white)
        }

        val methodNameTextView = TextView(this).apply {
            text = method
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 13f
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        val amountTextView = TextView(this).apply {
            text = "₱${String.format("%.2f", amount)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 13f
            gravity = Gravity.END
            setTextColor(resources.getColor(android.R.color.black, null))
        }

        paymentView.addView(methodNameTextView)
        paymentView.addView(amountTextView)
        return paymentView
    }

    private fun createItemSalesView(item: ItemSalesSummary): LinearLayout {
        val itemView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setPadding(12, 8, 12, 8)
            setBackgroundResource(android.R.color.white)
        }

        // Item name and amount row
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val itemNameTextView = TextView(this).apply {
            text = item.name
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            textSize = 13f
            setTextColor(resources.getColor(android.R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        val amountTextView = TextView(this).apply {
            text = "₱${String.format("%.2f", item.totalAmount)}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 13f
            gravity = Gravity.END
            setTextColor(resources.getColor(android.R.color.black, null))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        topRow.addView(itemNameTextView)
        topRow.addView(amountTextView)

        // Quantity row
        val quantityTextView = TextView(this).apply {
            text = "Qty: ${item.quantity}"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            textSize = 11f
            setTextColor(resources.getColor(android.R.color.darker_gray, null))
        }

        itemView.addView(topRow)
        itemView.addView(quantityTextView)
        return itemView
    }
}