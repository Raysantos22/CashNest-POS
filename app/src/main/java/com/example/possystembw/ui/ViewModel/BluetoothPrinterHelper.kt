package com.example.possystembw.ui.ViewModel

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.database.CartItem
import com.example.possystembw.database.Customer
import com.example.possystembw.database.TenderDeclaration
import com.example.possystembw.database.TransactionRecord
import com.example.possystembw.database.TransactionSummary
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BluetoothPrinterHelper(private val context: Context) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothSocket: BluetoothSocket? = null
    var outputStream: OutputStream? = null
    private var printerAddress: String? = null

    enum class ReceiptType {
        ORIGINAL,
        REPRINT,
        RETURN,
        AR  // New AR type
    }

    fun connect(address: String): Boolean {
        if (!checkBluetoothPermissions()) {
            Log.e("BluetoothPrinterHelper", "Bluetooth permission not granted")
            return false
        }
        if (isConnected() && address == printerAddress) {
            return true
        }
        if (isConnected()) {
            disconnect()
        }
        try {
            val device: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(address)
                ?: throw IOException("Device not found")
            bluetoothSocket =
                device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            printerAddress = address
            return true
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error connecting to printer: ${e.message}")
            return false
        } catch (se: SecurityException) {
            Log.e("BluetoothPrinterHelper", "SecurityException: ${se.message}")
            return false
        }
    }

    fun printReceipt(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: ReceiptType = ReceiptType.ORIGINAL
    ): Boolean {
        if (!checkBluetoothPermissions()) {
            Log.e("BluetoothPrinterHelper", "Bluetooth permission not granted")
            return false
        }

        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        try {
            val receiptContent = generateReceiptContent(transaction, items, receiptType)
            outputStream?.write(receiptContent.toByteArray())
            outputStream?.flush()

            // Add cut command after printing the receipt
            cutPaper()

            return true
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error printing receipt: ${e.message}")
            disconnect()  // Disconnect on error to allow reconnection attempt
            return false
        } catch (se: SecurityException) {
            Log.e("BluetoothPrinterHelper", "SecurityException: ${se.message}")
            return false
        }
    }

    fun cutPaper() {
        try {
            // Add proper spacing before cut
            outputStream?.write("\n\n\n\n\n\n\n\n".toByteArray())

            // ESC/POS command for full cut with feed
            val cutCommand = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
            outputStream?.write(cutCommand)
            outputStream?.flush()
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error cutting paper: ${e.message}")
        }
    }


    fun isConnected(): Boolean {
        return bluetoothSocket?.isConnected == true
    }

    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error disconnecting: ${e.message}")
        } finally {
            outputStream = null
            bluetoothSocket = null
            printerAddress = null
        }
    }

     fun checkBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    private fun addFooterSpacingAndCut(sb: StringBuilder) {
        // Add consistent spacing before cut
        repeat(2) {
            sb.appendLine()
        }
    }
    fun printGenericReceipt(receiptContent: String): Boolean {
        if (!checkBluetoothPermissions() || !isConnected()) {
            return false
        }

        try {
            val sb = StringBuilder(receiptContent)
            addFooterSpacingAndCut(sb)

            // Write the content
            outputStream?.write(sb.toString().toByteArray())
            outputStream?.flush()

            // Add cut command
            val cutCommand = byteArrayOf(0x1D, 0x56, 0x41, 0x00)
            outputStream?.write(cutCommand)
            outputStream?.flush()

            return true
        } catch (e: IOException) {
            Log.e("BluetoothPrinterHelper", "Error printing receipt: ${e.message}")
            disconnect()
            return false
        }
    }


    fun printCashFundReceipt(cashFund: Double, status: String): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {

            appendLine("CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount: ${"%.2f".format(cashFund)}")
            appendLine("Status: $status")

        }

        return printGenericReceipt(receiptContent)
    }

    fun printPulloutCashFundReceipt(pulloutAmount: Double): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {

            appendLine("PULL-OUT CASH FUND RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("Amount: ${"%.2f".format(pulloutAmount)}")

        }

        return printGenericReceipt(receiptContent)
    }

    fun printTenderDeclarationReceipt(cashAmount: Double, arAmounts: Map<String, Double>): Boolean {
        if (!isConnected()) {
            Log.e("BluetoothPrinterHelper", "Not connected to a printer")
            return false
        }

        val receiptContent = buildString {

            appendLine("TENDER DECLARATION RECEIPT")
            appendLine("Date: ${getCurrentDate()}")
            appendLine("Time: ${getCurrentTime()}")
            appendLine("-".repeat(46))
            appendLine("Cash Amount: ${String.format("%.2f", cashAmount)}")
            appendLine("-".repeat(46))
            appendLine("AR Amounts:")
            arAmounts.forEach { (arType, amount) ->
                if (arType != "Cash") {  // Exclude Cash from AR amounts
                    appendLine("$arType: ${String.format("%.2f", amount)}")
                }
            }
            appendLine("-".repeat(46))
            val totalAmount = cashAmount + arAmounts.filterKeys { it != "Cash" }.values.sum()
            appendLine("Total Amount: ${String.format("%.2f", totalAmount)}")

        }

        return printGenericReceipt(receiptContent)
    }

    fun buildReadReport(
        transactions: List<TransactionSummary>,
        isZRead: Boolean,
        zReportId: String = "",
        tenderDeclaration: TenderDeclaration? = null
    ): String {
        // Calculate all totals from TransactionSummary records
        val totalSales = transactions.sumOf { it.netAmount }
        val totalGross = transactions.sumOf { it.grossAmount }
        val totalVat = transactions.sumOf { it.taxIncludedInPrice }
        val totalDiscount = transactions.sumOf { it.totalDiscountAmount } // Total discount
        val vatableSales = transactions.sumOf { it.vatableSales }
        val vatExemptSales = transactions.sumOf { it.vatExemptAmount }

        // Payment method totals
        val cashSales = transactions.sumOf { it.cash }
        val cardSales = transactions.sumOf { it.card }
        val gCashSales = transactions.sumOf { it.gCash }
        val payMayaSales = transactions.sumOf { it.payMaya }
        val arSales = transactions.filter { it.type == 3 }.sumOf { it.netAmount }

        // Transaction statistics
        val totalReturns = transactions.filter { it.type == 2 }.sumOf { it.netAmount }
        val totalItemsSold = transactions.filter { it.type != 2 }.sumOf { it.numberOfItems.toInt() }
        val totalItemsReturned = transactions.filter { it.type == 2 }.sumOf { it.numberOfItems.toInt() }
        val voidedTransactions = transactions.count { it.transactionStatus == 0 }
        val returnTransactions = transactions.count { it.type == 2 }
        val customerTransactions = transactions.count { it.customerAccount.isNotBlank() }

        // Group payments by method for the tender report
        val paymentMethodTotals = transactions
            .groupBy { it.paymentMethod }
            .mapValues { (_, transactions) -> transactions.sumOf { it.netAmount } }

        return buildString {

            appendLine("BRANCH: ${getBranchName()}")
            appendLine("TARLAC CITY")
            appendLine("REG TIN: ${getRegTin()}")
            appendLine("Permit: ${getPermitNumber()}")
            appendLine("Min: ${getMinNumber()}")
            appendLine("Serial: ${getSerialNumber()}")
            appendLine("Operator: ${getOperatorName()}")
            appendLine(
                "Date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            )
            appendLine()

            if (isZRead) {
                appendLine("Z-READ ID: $zReportId")
                appendLine("Zreading")
            } else {
                appendLine("Xreading")
            }

            appendLine("_".repeat(35))
            appendLine("SALES REPORT AMT INC                  AMOUNT")
            appendLine("Gross sales                           ${"%.2f".format(totalGross)}")
            appendLine("Purchase                              ${"%.2f".format(totalSales)}")
/*
            appendLine("Returns/refund                        ${"%.2f".format(totalReturns)}")
*/
            appendLine("Total Discount                        ${"%.2f".format(totalDiscount)}") // Total discount
            appendLine("Total netsales                        ${"%.2f".format(totalSales)}")

            appendLine("-".repeat(46))
            appendLine("Statistics                                  qty")
            appendLine("-".repeat(46))
            appendLine("No. of sales trans                    ${transactions.size}")
            appendLine("No. of items sold                     $totalItemsSold")

            val returnCommentsCount = transactions.count { transaction ->
                transaction.comment.contains("Returned:", ignoreCase = true) ||
                        transaction.comment.contains("Return processed:", ignoreCase = true)
            }

            appendLine("No. of void trans                     $returnCommentsCount") // Adjusted label

            appendLine("-".repeat(46))
            appendLine("Tender reports")
            appendLine("-".repeat(46))
            appendLine("Tender name                          amount")
            appendLine("_".repeat(35))

            paymentMethodTotals.forEach { (method, amount) ->
                appendLine("${method.padEnd(35)}${"%.2f".format(amount)}")
            }

            appendLine()
            appendLine("total amount                          ${"%.2f".format(totalSales)}")

            appendLine("-".repeat(46))
            appendLine("Tax report")
            appendLine("-".repeat(46))
            appendLine("Vatable sales                         ${"%.2f".format(vatableSales)}")
            appendLine("vat amount                            ${"%.2f".format(totalVat)}")
            appendLine("vat exempt sales                      ${"%.2f".format(vatExemptSales)}")
            appendLine("zero Rated sales                      ${"%.2f".format(0.0)}")

            if (tenderDeclaration != null) {
                appendLine("-".repeat(46))
                appendLine("Tender Declaration")
                appendLine("Cash Amount:                          ${"%.2f".format(tenderDeclaration.cashAmount)}")

                val gson = Gson()
                val arAmounts = gson.fromJson<Map<String, Double>>(
                    tenderDeclaration.arAmounts,
                    object : TypeToken<Map<String, Double>>() {}.type
                )
                var totalAr = 0.0
                arAmounts.forEach { (arType, amount) ->
                    if (arType != "Cash") {
                        appendLine("$arType:                              ${"%.2f".format(amount)}")
                        totalAr += amount
                    }
                }

                appendLine("Total AR:                             ${"%.2f".format(totalAr)}")
                val totalDeclared = tenderDeclaration.cashAmount + totalAr
                appendLine("Total Declared:                       ${"%.2f".format(totalDeclared)}")
                appendLine("Short/Over:                           ${"%.2f".format(totalDeclared - totalSales)}")
            }

            appendLine("-".repeat(45))
            appendLine("Pos Provider: IT WARRIOR")
            appendLine("ELJIN CORP")
            appendLine("tin: ${getPosProviderTin()}")
            appendLine("acc No: ${getPosProviderAccNo()}")
            appendLine(
                "Date issued: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}"
            )
            appendLine("valid until: ${getValidUntilDate()}")

            if (isZRead) {
                appendLine()
                appendLine("-".repeat(32))
                appendLine("End of Z-Read Report")


            }
        }
    }


    // Helper functions for calculations
    private fun getSeniorCitizenDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.discountType.equals("SC", ignoreCase = true) }
            .sumOf { it.customerDiscountAmount }
    }

    private fun getPWDDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.discountType.equals("PWD", ignoreCase = true) }
            .sumOf { it.customerDiscountAmount }
    }

    private fun getOtherDiscount(transactions: List<TransactionSummary>): Double {
        return transactions.sumOf { it.discountAmount } - (getSeniorCitizenDiscount(transactions) + getPWDDiscount(
            transactions
        ))
    }

    private fun getVoidedTransactionsCount(transactions: List<TransactionSummary>): Int {
        return transactions.count { it.transactionStatus == 0 } // Assuming 0 represents voided transactions
    }

    private fun getReturnTransactionsCount(transactions: List<TransactionSummary>): Int {
        return transactions.count { it.type == 2 } // Assuming type 2 represents return transactions
    }

    private fun getCustomerTransactionsCount(transactions: List<TransactionSummary>): Int {
        return transactions.count { it.customerAccount.isNotBlank() }
    }

    private fun getVatExemptSales(transactions: List<TransactionSummary>): Double {
        return transactions.filter { it.vatExemptAmount > 0 }.sumOf { it.vatExemptAmount }
    }

    private fun getZeroRatedSales(transactions: List<TransactionSummary>): Double {
        // Implement this based on your business logic for zero-rated sales
        return 0.0
    }

    // Helper functions to be implemented
    private fun getBranchName(): String {
        // Implement this to return the branch name
        return "ELJIN CORP"
    }

    private fun getRegTin(): String {
        // Implement this to return the registration TIN
        return "Your Reg TIN"
    }

    private fun getPermitNumber(): String {
        // Implement this to return the permit number
        return "Permit Number"
    }

    private fun getMinNumber(): String {
        // Implement this to return the MIN number
        return "MIN Number"
    }

    private fun getSerialNumber(): String {
        // Implement this to return the serial number
        return "Serial Number"
    }

    private fun getOperatorName(): String {
        // Implement this to return the operator name
        return "Operator Name"
    }

    private fun getStoreName(): String {
        // Implement this to return the store name
        return "Store Name"
    }

    private fun getTerminalNumber(): String {
        // Implement this to return the terminal number
        return "Terminal Number"
    }

    private fun getStartOrNo(): String {
        // Implement this to return the start OR number
        return "Start OR Number"
    }

    private fun getEndOrNo(): String {
        // Implement this to return the end OR number
        return "End OR Number"
    }

    private fun getReturnsRefundAmount(transactions: List<TransactionSummary>): Double {
        // Implement this to calculate and return the total returns/refund amount
        return 0.0
    }


    private fun getItemsSoldCount(transactions: List<TransactionSummary>): Int {
        // Implement this to return the total count of items sold
        return 0
    }

    private fun getItemsReturnedCount(transactions: List<TransactionSummary>): Int {
        // Implement this to return the total count of items returned
        return 0
    }

    private fun getDrawerOpeningCount(): Int {
        // Implement this to return the count of drawer openings
        return 0
    }

    private fun getLoginsCount(): Int {
        // Implement this to return the count of logins
        return 0
    }

    private fun getSuspendedTransactionsCount(): Int {
        // Implement this to return the count of suspended transactions
        return 0
    }

    private fun getPosProviderTin(): String {
        // Implement this to return the POS provider's TIN
        return "POS Provider TIN"
    }

    private fun getPosProviderAccNo(): String {
        // Implement this to return the POS provider's ACC number
        return "POS Provider ACC Number"
    }

    private fun getValidUntilDate(): String {
        // Implement this to return the valid until date
        return SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.getDefault()
        ).format(Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000L))
    }


    fun generateReceiptContent(
        transaction: TransactionSummary,
        items: List<TransactionRecord>,
        receiptType: BluetoothPrinterHelper.ReceiptType,
        isAR: Boolean = false,
        copyType: String? = null
    ): String {
        val sb = StringBuilder()
        val isReturn = receiptType == BluetoothPrinterHelper.ReceiptType.RETURN

        // For return receipts, only show returned items
        // For regular receipts, show all items but handle returned items differently
        val filteredItems = if (isReturn) {
            items.filter { it.isReturned }
        } else {
            // Show all items, but calculate net quantities after returns
            items.groupBy { it.itemId }.map { (_, groupedItems) ->
                val originalItem = groupedItems.first { !it.isReturned }
                val returnedQuantity = groupedItems
                    .filter { it.isReturned }
                    .sumOf { it.quantity }

                // Create a new item with adjusted quantity
                originalItem.copy(
                    quantity = originalItem.quantity + returnedQuantity, // returnedQuantity is negative
                    isReturned = false
                )
            }
        }
        // Store Header
        sb.appendLine("ELJIN CORP")
        sb.appendLine("Address Line 1")
        sb.appendLine("Address Line 2")
        sb.appendLine("TIN: Your TIN Number")
        sb.appendLine("ACC: Your ACC Number")
        sb.appendLine("Contact #: Your Contact Number")
        sb.appendLine("-".repeat(32))

        // Receipt Type Header (unchanged)
        when (receiptType) {
            BluetoothPrinterHelper.ReceiptType.AR -> {
                sb.appendLine("       ACCOUNTS RECEIVABLE        ")
                sb.appendLine("         $copyType          ")
            }

            BluetoothPrinterHelper.ReceiptType.REPRINT -> sb.appendLine("*** REPRINT ***")
            BluetoothPrinterHelper.ReceiptType.RETURN -> {
                sb.appendLine("*** RETURN TRANSACTION ***")
                sb.appendLine("Original Receipt #: ${transaction.receiptId}")
            }

            else -> {} // No special header for ORIGINAL
        }
        sb.appendLine("-".repeat(32))

        // Transaction Details
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sb.appendLine("Date Issued: ${dateFormat.format(transaction.createdDate)}")
        sb.appendLine("Receipt ID: ${transaction.receiptId}")
        sb.appendLine("Transaction ID: ${transaction.transactionId}")
        sb.appendLine("Store: ${transaction.store}")
        sb.appendLine("Cashier: ${transaction.staff}")
        sb.appendLine("Window Number: ${transaction.windowNumber}")


        // AR Customer Details if applicable
        if (isAR) {
            sb.appendLine("-".repeat(32))
            sb.appendLine("Customer Account: ${transaction.customerAccount}")
            sb.appendLine("Customer Name: ${transaction.customerName ?: "N/A"}")
            sb.appendLine("AR Type: ${transaction.paymentMethod}")
        }
        sb.appendLine("-".repeat(32))

        // Items Section
        sb.appendLine("Item Name              Price    Qty    Total")

        // Display items and calculate totals
        items.forEach { item ->
            val effectivePrice = if (item.priceOverride!! > 0.0) item.priceOverride else item.price
            val itemTotal = effectivePrice * item.quantity

            // Display item line
            sb.appendLine(
                "${item.name.take(20).padEnd(20)} ${
                    String.format(
                        "%10.2f",
                        effectivePrice
                    )
                } ${String.format("%5d", item.quantity)} ${String.format("%8.2f", itemTotal)}"
            )

            // Show original price if overridden
            if (item.priceOverride > 0.0 && item.priceOverride != item.price) {
                sb.appendLine("  Original Price: ${String.format("%.2f", item.price)}")
                sb.appendLine("  Price Override: ${String.format("%.2f", item.priceOverride)}")
            }

            // Modified discount display logic
            when (item.discountType.uppercase()) {
                "PERCENTAGE", "PWD", "SC" -> {
                    val discountAmount = itemTotal * (item.discountRate)
                    if (discountAmount > 0) {
                        sb.appendLine(
                            "  Discount (${item.discountType}): ${
                                String.format(
                                    "%.2f",
                                    discountAmount
                                )
                            }"
                        )
                        sb.appendLine(
                            "  Discount Rate: ${
                                String.format(
                                    "%.1f",
                                    item.discountRate * 100
                                )
                            }%"
                        )
                    }
                }
                "FIXED" -> {
                    val perItemDiscount = item.discountAmount
                    val totalDiscount = perItemDiscount * item.quantity
                    if (perItemDiscount > 0) {
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
                    // Always show FIXEDTOTAL discount for items that have it
                    if (item.discountAmount > 0 || item.lineDiscountAmount!! > 0) {
                        val discountToShow = if (item.discountAmount > 0) item.discountAmount else item.lineDiscountAmount
                        sb.appendLine(
                            "  Discount (Fixed Total): ${
                                String.format(
                                    "%.2f",
                                    discountToShow
                                )
                            }"
                        )
                    }
                }
            }
        }

        sb.appendLine("-".repeat(32))

        // Display totals section
        sb.appendLine("Gross Amount:        ${String.format("%12.2f", transaction.grossAmount)}")
        sb.appendLine(
            "Total Discounts:     ${
                String.format(
                    "%12.2f",
                    transaction.totalDiscountAmount
                )
            }"
        )

       /* if (transaction.partialPayment > 0) {
            sb.appendLine(
                "Partial Payment:     ${
                    String.format(
                        "%12.2f",
                        transaction.partialPayment
                    )
                }"
            )
        }*/

        sb.appendLine("Net Amount:          ${String.format("%12.2f", transaction.netAmount)}")

        // Only show amount paid and change for non-AR transactions
        if (!isAR) {
            sb.appendLine(
                "Amount Paid:         ${
                    String.format(
                        "%12.2f",
                        transaction.totalAmountPaid
                    )
                }"
            )
            sb.appendLine(
                "Change:              ${
                    String.format(
                        "%12.2f",
                        transaction.changeGiven
                    )
                }"
            )
        }

        // Payment Method Section
        sb.appendLine("-".repeat(32))
        sb.appendLine("Payment Method: ${transaction.paymentMethod}")

        // Show payment method amounts
        if (transaction.cash > 0) {
            sb.appendLine("Cash Payment:         ${String.format("%12.2f", transaction.cash)}")
        }
        if (transaction.card > 0) {
            sb.appendLine("Card Payment:         ${String.format("%12.2f", transaction.card)}")
        }
        if (transaction.gCash > 0) {
            sb.appendLine("GCash Payment:        ${String.format("%12.2f", transaction.gCash)}")
        }
        if (transaction.payMaya > 0) {
            sb.appendLine("PayMaya Payment:      ${String.format("%12.2f", transaction.payMaya)}")
        }

        // Payment Summary Section
        sb.appendLine("-".repeat(32))
        sb.appendLine("Payment Summary")
        sb.appendLine("-".repeat(32))

        if (transaction.partialPayment > 0) {
            sb.appendLine("Total Bill Amount:    ${String.format("%12.2f", transaction.netAmount)}")
            sb.appendLine("Previous Payment:     ${String.format("%12.2f", transaction.partialPayment)}")
            sb.appendLine("Current Payment:      ${String.format("%12.2f", transaction.totalAmountPaid)}")
            sb.appendLine("Total Paid:           ${String.format("%12.2f", (transaction.partialPayment + transaction.totalAmountPaid) - transaction.changeGiven)}")
        } else {
            sb.appendLine("Total Amount:         ${String.format("%12.2f", transaction.netAmount)}")
            sb.appendLine("Amount Paid:          ${String.format("%12.2f", transaction.totalAmountPaid)}")
            sb.appendLine("Change:               ${String.format("%12.2f", transaction.changeGiven)}")
        }

        // VAT Information
        sb.appendLine("-".repeat(32))
        sb.appendLine("Vatable Sales:       ${String.format("%12.2f", transaction.vatableSales)}")
        sb.appendLine("VAT Amount (12%):    ${String.format("%12.2f", transaction.vatAmount)}")
        sb.appendLine("Vat Exempt           ${String.format("%12.2f", 0.0)}")
        sb.appendLine("Zero Rated Sales:    ${String.format("%12.2f", 0.0)}")

        // Transaction Details
        sb.appendLine("-".repeat(32))


        // Transaction Comment
        if (!transaction.comment.isNullOrBlank()) {
            sb.appendLine("-".repeat(32))
            sb.appendLine("Comment: ${transaction.comment}")
        }

        // Footer
        sb.appendLine("-".repeat(32))
        if (!isReturn) {
            sb.appendLine("ID/OSCA/PWD: ")
            sb.appendLine("NAME: ")
            sb.appendLine("Signature: ")
        }
        sb.appendLine("-".repeat(32))
        sb.appendLine("This serves as your official receipt")
        sb.appendLine("This invoice/receipt shall be valid for")
        sb.appendLine("five (5) years from the date of the")
        sb.appendLine("permit to use")
        sb.appendLine("-".repeat(32))
        sb.appendLine("POS Provider: IT WARRIORS")
        sb.appendLine("ELJIN CORP")
        sb.appendLine("ADDRESS")
        sb.appendLine("-".repeat(32))
        sb.appendLine("Thank you for your business!")

        // AR Copy Type
        if (isAR) {
            sb.appendLine("-".repeat(32))
            sb.appendLine(copyType)
        }

        // Add extra spacing at the bottom


        return sb.toString()
    }
}