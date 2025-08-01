package com.example.possystembw.ui

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.adapter.LineAdapter
import com.example.possystembw.adapter.LineTransactionAdapter
import com.example.possystembw.database.LineTransactionEntity
import com.example.possystembw.ui.ViewModel.LineViewModel
import com.example.possystembw.ui.ViewModel.LineViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.possystembw.DAO.LineTransaction
import com.example.possystembw.ui.ViewModel.BluetoothPrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.DeviceUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class LineActivity : AppCompatActivity() {
    private lateinit var viewModel: LineViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LineAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private var currentSortField = "itemId"
    private var isAscending = true
    private var originalList: List<LineTransaction> = emptyList()  // Updated this line
    private lateinit var printer: BluetoothPrinterHelper
    private var printerAddress: String? = null
    private var hasLoadedData = false  // Add this flag

    private var autoSaveJob: Job? = null

    private lateinit var progressDialog: AlertDialog
    private var currentSyncDialog: AlertDialog? = null

    private var currentErrorDialog: AlertDialog? = null
    private var isShowingDialog = false  // Add this at class level

    private var isFilterActive = false

    companion object {
        private const val EXTRA_STORE_ID = "extra_store_id"
        private const val EXTRA_JOURNAL_ID = "extra_journal_id"
        private const val TAG = "LineActivity"

        fun createIntent(context: Context, storeId: String, journalId: String): Intent {
            return Intent(context, LineActivity::class.java).apply {
                putExtra(EXTRA_STORE_ID, storeId)
                putExtra(EXTRA_JOURNAL_ID, journalId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_line)
        DeviceUtils.setOrientationBasedOnDevice(this)

        // Initialize views first
        setupViews()

        // Initialize adapter before setting up back pressed handler
        adapter = LineAdapter()
        recyclerView.adapter = adapter

        // Set up back button handling AFTER adapter is initialized
        setupBackHandling()

        // Rest of your existing onCreate code
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        BluetoothPrinterHelper.initialize(applicationContext)
        setupObservers()
        loadingProgressBar = findViewById(R.id.loadingLineDetails)
        loadingProgressBar.visibility = View.VISIBLE  // Show loading immediately

        setupSendButton()
        viewModel = ViewModelProvider(this, LineViewModelFactory(application))[LineViewModel::class.java]
        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)

        if (storeId != null && journalId != null) {
            // Show loading and fetch data
            loadingProgressBar.visibility = View.VISIBLE
            viewModel.setIds(storeId, journalId)
            viewModel.fetchLineDetails(storeId, journalId)
        } else {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupProgressDialog() // Initialize progress dialog early
        setupTableHeaderClicks()
        setupSearch()
        setupButtons()
//        setupAutoSave()
        setupSyncStatusObserver()

        recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null // Disable animations for better performance
            recycledViewPool.setMaxRecycledViews(0, 20) // Increase view pool size
            layoutManager = LinearLayoutManager(this@LineActivity).apply {
                initialPrefetchItemCount = 4 // Prefetch items
                isItemPrefetchEnabled = true
            }
        }

        lifecycleScope.launch {
            while (true) {
                checkAndPerformAutoSync()
                delay(30000) // Check every 30 seconds
            }
        }

        if (!hasLoadedData) {
            val storeId = intent.getStringExtra(EXTRA_STORE_ID)
            val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
            if (storeId != null && journalId != null) {
                viewModel.fetchLineDetails(storeId, journalId)
                hasLoadedData = true
            }
        }

        printerAddress = getSharedPreferences("printer_prefs", MODE_PRIVATE)
            .getString("last_printer_address", null)

        // Setup printer and auto-connect if we have a saved address
        setupPrinter()
    }
// Add this to your LineActivity class

    // Override onBackPressed to show confirmation dialog


    // Add this to handle action bar back button clicks
    private fun setupBackHandling() {
        // Create a new callback
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check for real modifications in the adapter
                if (::adapter.isInitialized && adapter.hasModifications()) {
                    Log.d(TAG, "Back pressed with modifications, showing dialog")
                    showBackConfirmationDialog()
                } else {
                    Log.d(TAG, "Back pressed without modifications, exiting normally")
                    isEnabled = false
                    finish()
                }
            }
        }

        // Add the callback to the dispatcher
        onBackPressedDispatcher.addCallback(this, callback)

        // Log for debugging
        Log.d(TAG, "Back handler set up successfully")
    }

    private fun testApiConnection() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                Log.d(TAG, "Testing API with storeId: '$storeId', journalId: '$journalId'")

                // Create repository instance to test
                val testResult = viewModel.repository.testApiConnectivity(storeId, journalId)

                Log.d("API_TEST", testResult)

                // Show result in dialog for immediate feedback
                runOnUiThread {
                    AlertDialog.Builder(this@LineActivity)
                        .setTitle("API Connection Test")
                        .setMessage(testResult)
                        .setPositiveButton("OK", null)
                        .show()
                }

            } catch (e: Exception) {
                Log.e("API_TEST", "Test failed", e)
                runOnUiThread {
                    Toast.makeText(this@LineActivity, "API test failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    // Update your showBackConfirmationDialog method with improved code
//    private fun showBackConfirmationDialog() {
//        // Add logging for debugging
//        Log.d(TAG, "Showing back confirmation dialog")
//
//        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
//        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
//        messageTextView.text = "You have unsaved changes. What would you like to do?"
//
//        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
//            .setTitle("Unsaved Changes")
//            .setView(dialogView)
//            .setPositiveButton("Save & Exit") { _, _ ->
//                // Save changes first
//                lifecycleScope.launch {
//                    try {
//                        val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
//                        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
//                        val updatedList = adapter.getUpdatedItems()
//
//                        val result = viewModel.saveLineDetails(storeId, journalId, updatedList)
//
//                        // Exit after save attempt regardless of result
//                        withContext(Dispatchers.Main) {
//                            if (result) {
//                                Toast.makeText(this@LineActivity, "Changes saved", Toast.LENGTH_SHORT).show()
//                            } else {
//                                Toast.makeText(this@LineActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
//                            }
//                            // Use finish() to ensure activity closes
//                            finish()
//                        }
//                    } catch (e: Exception) {
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(this@LineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
//                            finish()
//                        }
//                    }
//                }
//            }
//            .setNegativeButton("Exit Without Saving") { _, _ ->
//                // Exit without saving
//                finish()
//            }
//            .setNeutralButton("Cancel") { dialog, _ ->
//                // Stay on the current screen
//                dialog.dismiss()
//            }
//            .setCancelable(false)  // Prevent dismissing by clicking outside
//            .create()
//
//        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
//        dialog.show()
//    }
    private fun showBackConfirmationDialog() {
        // Add logging for debugging
        Log.d(TAG, "Showing back confirmation dialog")

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "You have unsaved changes. What would you like to do?"

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Unsaved Changes")
            .setView(dialogView)
            .setPositiveButton("Save & Exit") { _, _ ->
                // Save changes first
                lifecycleScope.launch {
                    try {
                        val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
                        val updatedList = adapter.getUpdatedItems()

                        val result = viewModel.saveLineDetails(storeId, journalId, updatedList)

                        // Exit after save attempt regardless of result
                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(this@LineActivity, "Changes saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@LineActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
                            }
                            // Use finish() to ensure activity closes
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@LineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                // Stay on the current screen
                dialog.dismiss()
            }
            .setCancelable(false)  // Prevent dismissing by clicking outside
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    // Also update your onOptionsItemSelected for consistency
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Handle the back button in the action bar
            if (::adapter.isInitialized && adapter.hasModifications()) {
                showBackConfirmationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    private fun setupProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_sync_progress, null)
        progressDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Processing")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    }


    private fun setupSendButton() {
        findViewById<Button>(R.id.btnSend).setOnClickListener {
            checkAndPerformSync()
        }
    }
    private fun checkAndPerformSync() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                // Save any pending changes first
                if (adapter.hasModifications()) {
                    Log.d(TAG, "Saving modifications before checking sync status")
                    val updatedList = adapter.getUpdatedItems()
                    val saveResult = viewModel.saveLineDetails(storeId, journalId, updatedList)

                    if (!saveResult) {
                        Toast.makeText(this@LineActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                val unsyncedCount = viewModel.getUnsyncedCount(journalId)

                if (unsyncedCount > 0) {
                    // Show dialog about unsynced items
                    AlertDialog.Builder(this@LineActivity)
                        .setTitle("Unsynced Items")
                        .setMessage("There are $unsyncedCount items that need to be synced first. Would you like to sync them now?")
                        .setPositiveButton("Yes") { _, _ ->
                            performSyncedSend()
                        }
                        .setNegativeButton("No", null)
                        .show()
                } else {
                    // Proceed with stock counting
                    AlertDialog.Builder(this@LineActivity)
                        .setTitle("Complete Stock Counting")
                        .setMessage("All items are synced. Would you like to complete the stock counting?")
                        .setPositiveButton("Yes") { _, _ ->
                            completeStockCounting()
                        }
                        .setNegativeButton("No", null)
                        .show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@LineActivity,
                    "Error checking sync status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    //    private fun setupSendButton() {
//        findViewById<Button>(R.id.btnSend).setOnClickListener {
//            AlertDialog.Builder(this)
//                .setTitle("Send Data")
//                .setMessage("Are you sure you want to send the modified data?")
//                .setPositiveButton("Yes") { _, _ ->
//                    performSyncedSend()
//                }
//                .setNegativeButton("No", null)
//                .show()
//        }
//    }
    private fun performSyncedSend() {
        setupProgressDialog()
        progressDialog.show()
        isShowingDialog = false  // Reset the flag

        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                // FIRST: Save any unsaved changes from the adapter
                if (adapter.hasModifications()) {
                    Log.d(TAG, "Saving unsaved modifications before sync")

                    // Update progress to show saving
                    progressDialog.apply {
                        findViewById<TextView>(R.id.tvCurrentItem)?.text = "Saving changes..."
                        findViewById<ProgressBar>(R.id.progressBar)?.isIndeterminate = true
                    }

                    val updatedList = adapter.getUpdatedItems()
                    val saveResult = viewModel.saveLineDetails(storeId, journalId, updatedList)

                    if (!saveResult) {
                        progressDialog.dismiss()
                        showActionDialog(
                            title = "Save Error",
                            message = "Failed to save changes before sync. Please try again.",
                            positiveButton = "Retry" to {
                                isShowingDialog = false
                                performSyncedSend()
                            },
                            negativeButton = "Cancel" to {
                                isShowingDialog = false
                            }
                        )
                        return@launch
                    }

                    Log.d(TAG, "Successfully saved ${updatedList.size} items before sync")

                    // Reset progress bar for sync
                    progressDialog.apply {
                        findViewById<ProgressBar>(R.id.progressBar)?.isIndeterminate = false
                    }
                }

                // THEN: Proceed with sync
                viewModel.syncProgress.observe(this@LineActivity) { progress ->
                    when {
                        !progress.isComplete -> {
                            // Update progress
                            progressDialog.apply {
                                findViewById<ProgressBar>(R.id.progressBar)?.let { bar ->
                                    bar.max = progress.totalItems
                                    bar.setProgress(progress.currentItem)
                                }
                                findViewById<TextView>(R.id.tvProgress)?.text =
                                    "${progress.currentItem}/${progress.totalItems}"
                                findViewById<TextView>(R.id.tvCurrentItem)?.text =
                                    "Syncing item ${progress.currentItemId}..."
                            }
                        }
                        progress.errorMessage != null -> {
                            if (!isShowingDialog) {
                                isShowingDialog = true
                                progressDialog.dismiss()
                                // Show error dialog with retry option
                                showActionDialog(
                                    title = if (progress.errorMessage.contains("No internet")) "No Internet Connection" else "Sync Error",
                                    message = if (progress.errorMessage.contains("No internet"))
                                        "Please check your internet connection and try again."
                                    else
                                        progress.errorMessage,
                                    positiveButton = "Try Again" to {
                                        isShowingDialog = false
                                        performSyncedSend()
                                    },
                                    negativeButton = "Cancel" to {
                                        isShowingDialog = false
                                    }
                                )
                            }
                        }
                        else -> {
                            if (!isShowingDialog) {
                                isShowingDialog = true
                                progressDialog.dismiss()
                                if (progress.totalItems > 0) {
                                    // Show completion dialog
                                    showActionDialog(
                                        title = "Sync Complete",
                                        message = "Successfully synced ${progress.totalItems} items. Would you like to complete stock counting?",
                                        positiveButton = "Complete Stock Counting" to {
                                            isShowingDialog = false
                                            completeStockCounting()
                                        },
                                        negativeButton = "Close" to {
                                            isShowingDialog = false
                                        }
                                    )
                                } else {
                                    // No items were synced (all were already synced)
                                    showActionDialog(
                                        title = "Already Synced",
                                        message = "All items are already synced. Would you like to complete stock counting?",
                                        positiveButton = "Complete Stock Counting" to {
                                            isShowingDialog = false
                                            completeStockCounting()
                                        },
                                        negativeButton = "Close" to {
                                            isShowingDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Start the actual sync
                viewModel.syncModifiedData()

            } catch (e: Exception) {
                progressDialog.dismiss()
                Log.e(TAG, "Error in performSyncedSend", e)
                showActionDialog(
                    title = "Error",
                    message = "Error preparing sync: ${e.message}",
                    positiveButton = "Try Again" to {
                        performSyncedSend()
                    },
                    negativeButton = "Cancel" to null
                )
            }
        }
    }



    private fun showActionDialog(
        title: String,
        message: String,
        positiveButton: Pair<String, (() -> Unit)?>,
        negativeButton: Pair<String, (() -> Unit)?>,
        cancelable: Boolean = true
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = message

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(positiveButton.first) { dialog, _ ->
                dialog.dismiss()
                positiveButton.second?.invoke()
            }
            .setNegativeButton(negativeButton.first) { dialog, _ ->
                dialog.dismiss()
                negativeButton.second?.invoke()
            }
            .setCancelable(cancelable)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showSyncingDialog(progress: LineViewModel.SyncProgress) {
        progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.let { progressBar ->
            progressBar.max = progress.totalItems
            progressBar.setProgress(progress.currentItem)  // Use setProgress instead of direct assignment
        }

        progressDialog.findViewById<TextView>(R.id.tvProgress)?.text =
            "${progress.currentItem}/${progress.totalItems}"

        progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text =
            "Syncing item ${progress.currentItemId}..."
    }

    private fun showNoInternetDialog() {
        dismissCurrentErrorDialog()

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "Please check your internet connection and try again."

        currentErrorDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("No Internet Connection")
            .setView(dialogView)
            .setPositiveButton("Try Again") { _, _ ->
                currentErrorDialog = null
                performSyncedSend()
            }
            .setNegativeButton("Cancel") { _, _ ->
                currentErrorDialog = null
            }
            .create()

        currentErrorDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        currentErrorDialog?.show()
    }
    private fun showErrorDialog(message: String) {
        dismissCurrentErrorDialog()

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = message

        currentErrorDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Sync Error")
            .setView(dialogView)
            .setPositiveButton("Try Again") { _, _ ->
                currentErrorDialog = null
                performSyncedSend()
            }
            .setNegativeButton("Cancel") { _, _ ->
                currentErrorDialog = null
            }
            .create()

        currentErrorDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        currentErrorDialog?.show()
    }
    private fun showSuccessDialog(totalItems: Int) {
        dismissCurrentErrorDialog()

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "Successfully synced $totalItems items."

        currentErrorDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Sync Complete")
            .setView(dialogView)
            .setPositiveButton("Complete Stock Counting") { _, _ ->
                currentErrorDialog = null
                completeStockCounting()
            }
            .setNegativeButton("Close") { _, _ ->
                currentErrorDialog = null
            }
            .create()

        currentErrorDialog?.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        currentErrorDialog?.show()
    }
    private fun dismissCurrentErrorDialog() {
        currentErrorDialog?.dismiss()
        currentErrorDialog = null
    }
    private fun updateProgressDialog(progress: LineViewModel.SyncProgress) {
        progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.apply {
            max = progress.totalItems
            setProgress(progress.currentItem)
        }

        progressDialog.findViewById<TextView>(R.id.tvProgress)?.text =
            "${progress.currentItem}/${progress.totalItems}"

        progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text =
            if (progress.currentItemId.isNotEmpty()) {
                "Currently syncing: ${progress.currentItemId}"
            } else {
                "Preparing..."
            }
    }
    private fun performParallelSend() {
        loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                // Filter only items with actual non-zero values
                val itemsToUpdate = adapter.getUpdatedItems().filter { item ->
                    val hasActualValues = (
                            (item.adjustment?.toDoubleOrNull() ?: 0.0) > 0 ||
                                    (item.receivedCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                                    (item.transferCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                                    (item.wasteCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                                    (item.counted?.toDoubleOrNull() ?: 0.0) > 0 ||
                                    (item.wasteType != null && item.wasteType != "none" && item.wasteType.isNotBlank())
                            )

                    // Log skipped items
                    if (!hasActualValues) {
                        Log.d(TAG, "Skipping item with all zero values: ${item.itemId}")
                    }

                    hasActualValues
                }

                if (itemsToUpdate.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LineActivity, "No changes to send", Toast.LENGTH_SHORT).show()
                        loadingProgressBar.visibility = View.GONE
                    }
                    return@launch
                }

                Log.d(TAG, "Found ${itemsToUpdate.size} items with non-zero values to update")

                // Post stock counting first
                Log.d(TAG, "Posting stock counting update...")
                val stockCountingResult = viewModel.postStockCounting(storeId, journalId)
                if (!stockCountingResult) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LineActivity, "Failed to update stock counting", Toast.LENGTH_SHORT).show()
                        loadingProgressBar.visibility = View.GONE
                    }
                    return@launch
                }

                // Process items in parallel batches
                val batchSize = 5 // Process 5 items at a time
                val results = mutableListOf<Result<String>>()

                itemsToUpdate.chunked(batchSize).forEach { batch ->
                    val batchResults = withContext(Dispatchers.IO) {
                        batch.map { item ->
                            async {
                                try {
                                    val result = viewModel.postLineDetails(
                                        itemId = item.itemId ?: "",
                                        storeId = storeId,
                                        journalId = journalId,
                                        adjustment = (item.adjustment?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                        receivedCount = (item.receivedCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                        transferCount = (item.transferCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                        wasteCount = (item.wasteCount?.toDoubleOrNull() ?: 0.0).toInt().toString(),
                                        wasteType = if (item.wasteType.isNullOrBlank()) "none" else item.wasteType,
                                        counted = (item.counted?.toDoubleOrNull() ?: 0.0).toInt().toString()
                                    )
                                    if (result) {
                                        Log.d(TAG, "Successfully sent item: ${item.itemId}")
                                        Result.success(item.itemId ?: "")
                                    } else {
                                        Log.e(TAG, "Failed to send item: ${item.itemId}")
                                        Result.failure(Exception("Failed to update ${item.itemId}"))
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error sending item: ${item.itemId}", e)
                                    Result.failure(e)
                                }
                            }
                        }.awaitAll()
                    }
                    results.addAll(batchResults)

                    // Add a small delay between batches to prevent overwhelming the server
                    delay(100) // 100ms delay between batches
                }

                // Process results
                val successCount = results.count { it.isSuccess }
                val failureMessages = results.mapNotNull { result ->
                    result.exceptionOrNull()?.message
                }

                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    if (failureMessages.isEmpty()) {
                        Toast.makeText(this@LineActivity, "All data sent successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        showSendResultDialog(successCount, itemsToUpdate.size, failureMessages)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in send operation", e)
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(this@LineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
//    private fun setupAutoSave() {
//        // Use a longer delay for auto-save (5 seconds)
//        val AUTO_SAVE_DELAY = 5000L // 5 seconds
//
//        adapter.setOnItemModifiedListener { updatedItems ->
//            // Cancel any pending auto-save job
//            autoSaveJob?.cancel()
//
//            // Only start a new auto-save job if enabled
//            autoSaveJob = lifecycleScope.launch {
//                delay(AUTO_SAVE_DELAY) // Wait longer before auto-saving
//                autoSave() // Call separate auto-save function
//            }
//        }
//    }


    private fun autoSave() {
        if (!adapter.hasModifications()) {
            return  // Silent return for auto-save
        }

        loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
                val updatedList = adapter.getUpdatedItems()

                val result = viewModel.saveLineDetails(storeId, journalId, updatedList)

                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    if (result) {
                        // Update adapter with new data
                        adapter.updateItems(updatedList)
                        // Show a smaller toast for auto-save
                        Toast.makeText(
                            this@LineActivity,
                            "Changes saved",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                }
                Log.e(TAG, "Error in auto-save", e)
            }
        }
    }

    private fun setupSyncStatusObserver() {
        viewModel.syncStatus.observe(this) { status ->
            when (status) {
                is LineViewModel.SyncStatus.Success -> {
                    adapter.markItemAsSent(status.itemId)
                }
                is LineViewModel.SyncStatus.Error -> {
                    Log.e(TAG, "Failed to sync item: ${status.itemId}, error: ${status.error}")
                }
            }
        }
    }
    private fun showSendResultDialog(
        successCount: Int,
        totalCount: Int,
        failureMessages: List<String>
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)

        val message = buildString {
            append("Sent $successCount out of $totalCount items\n\n")
            if (failureMessages.isNotEmpty()) {
                append("Errors:\n${failureMessages.joinToString("\n")}")
            }
        }

        messageTextView.text = message

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Send Results")
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    private fun setupViews() {
        loadingProgressBar = findViewById(R.id.loadingLineDetails)
        recyclerView = findViewById(R.id.rvLineDetails)

        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
        val timeTextView = findViewById<TextView>(R.id.tvTime)
        val storeIdTextView = findViewById<TextView>(R.id.tvStoreId)

        if (storeId == null || journalId == null) {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        storeIdTextView.text = storeId

        // Setup time updates
        val handler = Handler(Looper.getMainLooper())
        handler.post(object : Runnable {
            override fun run() {
                timeTextView.text = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                    .format(Date())
                handler.postDelayed(this, 1000)
            }
        })

        // Initialize RecyclerView
        adapter = LineAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@LineActivity)
            adapter = this@LineActivity.adapter
        }

        // Initialize ViewModel
        viewModel =
            ViewModelProvider(this, LineViewModelFactory(application))[LineViewModel::class.java]
    }


    //    private fun setupObservers() {
//        viewModel.lineDetailsResult.observe(this) { result ->
//            loadingProgressBar.visibility = View.GONE
//            findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false
//
//            result?.onSuccess { transactions ->
//                originalList = transactions
//                adapter.updateItems(transactions)
//            }?.onFailure { exception ->
//                // Show retry dialog on failure
//                AlertDialog.Builder(this)
//                    .setTitle("Error Loading Data")
//                    .setMessage("Failed to load data: ${exception.message}")
//                    .setPositiveButton("Retry") { _, _ ->
//                        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
//                        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
//                        if (storeId != null && journalId != null) {
//                            loadingProgressBar.visibility = View.VISIBLE
//                            viewModel.fetchLineDetails(storeId, journalId, forceRefresh = true)
//                        }
//                    }
//                    .setNegativeButton("Cancel") { _, _ ->
//                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
//                        finish()
//                    }
//                    .show()
//            }
//        }
//
//        viewModel.isLoading.observe(this) { isLoading ->
//            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//
//        // Initial data load
//        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
//        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
//        if (storeId != null && journalId != null) {
//            loadingProgressBar.visibility = View.VISIBLE
//            viewModel.fetchLineDetails(storeId, journalId)
//        }
//    }
    private fun setupObservers() {
        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

            // Hide empty state while loading to prevent premature messages
            if (isLoading) {
                findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.GONE
            }
        }

        // Enhanced data result observer with refresh feedback
        viewModel.lineDetailsResult.observe(this) { result ->
            loadingProgressBar.visibility = View.GONE
            findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false

            result?.onSuccess { transactions ->
                Log.d(TAG, "Successfully received ${transactions.size} transactions")

                if (transactions.isEmpty()) {
                    // Only show empty state after loading is completely done
                    findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    // Show data
                    findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // Check if this is a refresh operation by comparing with current data
                    val isRefreshOperation = originalList.isNotEmpty() && transactions != originalList

                    originalList = transactions
                    adapter.setItems(transactions)

                    // Show appropriate feedback
                    if (isRefreshOperation) {
                        Toast.makeText(this, "✅ Data refreshed successfully! Loaded ${transactions.size} items", Toast.LENGTH_SHORT).show()
                    } else if (originalList.isEmpty()) {
                        // This is initial load
                        Toast.makeText(this, "Loaded ${transactions.size} items", Toast.LENGTH_SHORT).show()
                    }
                }
            }?.onFailure { exception ->
                Log.e(TAG, "Failed to load data", exception)

                // Show error dialog
                val errorMessage = when {
                    exception.message?.contains("UnknownHostException") == true ||
                            exception.message?.contains("ConnectException") == true -> {
                        "Network connection failed. Please check your internet connection."
                    }
                    exception.message?.contains("SocketTimeoutException") == true -> {
                        "Request timed out. Please try again."
                    }
                    exception.message?.contains("Client error 4") == true -> {
                        "Invalid request. Please check your store ID and batch number."
                    }
                    exception.message?.contains("Server error 5") == true -> {
                        "Server is currently unavailable. Please try again later."
                    }
                    exception.message?.contains("Force API") == true -> {
                        "Failed to refresh data from server. Please try again."
                    }
                    else -> {
                        "Failed to load data: ${exception.message}"
                    }
                }

                AlertDialog.Builder(this)
                    .setTitle("Error Loading Data")
                    .setMessage(errorMessage)
                    .setPositiveButton("Retry") { _, _ ->
                        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
                        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
                        if (storeId != null && journalId != null) {
                            loadingProgressBar.visibility = View.VISIBLE
                            viewModel.fetchLineDetails(storeId, journalId, forceRefresh = true)
                        }
                    }
                    .setNeutralButton("Refresh") { _, _ ->
                        performReget()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        if (originalList.isEmpty()) {
                            finish() // Close if no data loaded
                        }
                    }
                    .show()
            }
        }
    }
    private fun setupTableHeaderClicks() {
        findViewById<TextView>(R.id.tvHeaderItemId).setOnClickListener { sortBy("itemId") }
        findViewById<TextView>(R.id.tvHeaderItemName).setOnClickListener { sortBy("itemName") }
        findViewById<TextView>(R.id.tvHeaderCategory).setOnClickListener { sortBy("category") }
    }

    private fun sortBy(field: String) {
        if (currentSortField == field) {
            isAscending = !isAscending
        } else {
            currentSortField = field
            isAscending = true
        }

        val sortedList = when (field) {
            "itemId" -> adapter.getCurrentList()
                .sortedBy { if (isAscending) it.itemId?.lowercase() ?: "" else it.itemId?.reversed()?.lowercase() ?: "" }

            "itemName" -> adapter.getCurrentList()
                .sortedBy { if (isAscending) it.itemName?.lowercase() ?: "" else it.itemName?.reversed()?.lowercase() ?: "" }

            "category" -> adapter.getCurrentList()
                .sortedBy { if (isAscending) it.itemDepartment?.lowercase() ?: "" else it.itemDepartment?.reversed()?.lowercase() ?: "" }

            else -> adapter.getCurrentList()
        }
        adapter.updateItems(sortedList)
    }

    private fun setupSearch() {
        val searchEditText = findViewById<EditText>(R.id.etSearch)
        val clearButton = findViewById<ImageView>(R.id.clearSearch) // Add this ImageView to your layout

        // Set initial visibility of clear button
        clearButton.visibility = if (searchEditText.text.isNotEmpty()) View.VISIBLE else View.GONE

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Show/hide clear button based on text
                clearButton.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
                adapter.filterItems(s.toString())
            }
        })

        // Set click listener for clear button
        clearButton.setOnClickListener {
            searchEditText.setText("")  // Clear the text
            adapter.filterItems("")     // Reset the filter
        }
    }
    private fun loadInitialData(transactions: List<LineTransaction>) {
        adapter.setItems(transactions)  // Use setItems instead of updateItems
    }
    private fun filterItems(query: String) {
        val filteredList = if (query.isEmpty()) {
            originalList
        } else {
            originalList.filter {
                (it.itemId?.contains(query, ignoreCase = true) == true) ||
                        (it.itemName?.contains(query, ignoreCase = true) == true) ||
                        (it.itemDepartment?.contains(query, ignoreCase = true) == true)
            }
        }
        adapter.updateItems(filteredList)
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnPrint).setOnClickListener {
            printLineDetails()
        }

        findViewById<Button>(R.id.btnGenerate).setOnClickListener {
            generateCSV()
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveLineDetails()
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            sendData()
        }
        findViewById<Button>(R.id.btnFilterTransactions).setOnClickListener {
            toggleTransactionFilter()
        }
        // Add the new Reget/Refresh button
        findViewById<Button>(R.id.btnReget).setOnClickListener {
            showRegetConfirmationDialog()
        }
    }

    private fun toggleTransactionFilter() {
        isFilterActive = !isFilterActive

        val filterButton = findViewById<Button>(R.id.btnFilterTransactions)

        Log.d("LineActivity", "Filter toggled. isFilterActive: $isFilterActive")

        if (isFilterActive) {
            filterButton.text = "All Item"
            filterButton.setBackgroundResource(R.drawable.update_button_background)
            applyTransactionFilter()
        } else {
            filterButton.text = "Movement Product"
            filterButton.setBackgroundResource(R.drawable.update_button_background1)
            clearTransactionFilter()
        }
    }

    // Fixed applyTransactionFilter method with detailed logging
    private fun applyTransactionFilter() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                Log.d("LineActivity", "Applying filter - StoreId: $storeId, JournalId: $journalId")
                Log.d("LineActivity", "Original list size: ${originalList.size}")

                // Get filtered items from ViewModel
                val filteredItems = viewModel.getItemsWithTransactions(storeId, journalId)

                Log.d("LineActivity", "Filtered items returned: ${filteredItems.size}")

                withContext(Dispatchers.Main) {
                    if (filteredItems.isNotEmpty()) {
                        adapter.setItems(filteredItems)
                        Toast.makeText(
                            this@LineActivity,
                            "Showing ${filteredItems.size} items with transactions",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.d("LineActivity", "Filter applied successfully - ${filteredItems.size} items")
                    } else {
                        Toast.makeText(
                            this@LineActivity,
                            "No items found with transactions",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.w("LineActivity", "No items found with transactions after filtering")
                    }
                }
            } catch (e: Exception) {
                Log.e("LineActivity", "Error filtering items", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LineActivity,
                        "Error filtering items: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    // Fixed clearTransactionFilter method with logging
    private fun clearTransactionFilter() {
        Log.d("LineActivity", "Clearing filter - showing all ${originalList.size} items")
        adapter.setItems(originalList)
        Toast.makeText(this, "Showing all ${originalList.size} items", Toast.LENGTH_SHORT).show()
    }

    private fun showRegetConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)

        val message = if (adapter.hasModifications()) {
            "You have unsaved changes. Getting fresh data will lose your current changes. Do you want to continue?"
        } else {
            "This will fetch the latest data from the server. Continue?"
        }

        messageTextView.text = message

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Refresh Data")
            .setView(dialogView)
            .setPositiveButton("Yes, Refresh") { _, _ ->
                if (adapter.hasModifications()) {
                    // Show additional warning for unsaved changes
                    showForceRefreshDialog()
                } else {
                    performReget()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showForceRefreshDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "Are you sure? All unsaved changes will be lost!"

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Confirm Refresh")
            .setView(dialogView)
            .setPositiveButton("Yes, Lose Changes") { _, _ ->
                performReget()
            }
            .setNegativeButton("No, Keep Changes", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun performReget() {
        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)

        if (storeId == null || journalId == null) {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show()
            return
        }

        loadingProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                // Clear local data first to force API call
                viewModel.clearLocalData(journalId)

                // Force refresh from API
                viewModel.fetchLineDetails(storeId, journalId, forceRefresh = true)

                Toast.makeText(this@LineActivity, "Refreshing data from server...", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(this@LineActivity, "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCompleteStockDialog(storeId: String, journalId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "All items are synced. Would you like to complete the stock counting?"

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Complete Stock Counting")
            .setView(dialogView)
            .setPositiveButton("Yes") { _, _ ->
                completeStockCounting()
            }
            .setNegativeButton("No", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }


    private fun completeStockCounting() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                // First check if there are any unsynced items
                val unsyncedCount = viewModel.getUnsyncedCount(journalId)
                if (unsyncedCount > 0) {
                    showActionDialog(
                        title = "Unsynced Items",
                        message = "There are $unsyncedCount items that need to be synced first. Would you like to sync them now?",
                        positiveButton = "Sync Now" to { performSyncedSend() },
                        negativeButton = "Cancel" to null
                    )
                    return@launch
                }

                // Show progress for stock counting
                progressDialog.apply {
                    findViewById<TextView>(R.id.tvProgress)?.text = "Completing stock counting..."
                    findViewById<ProgressBar>(R.id.progressBar)?.isIndeterminate = true
                    show()
                }

                // Attempt stock counting
                val result = viewModel.postStockCounting(storeId, journalId)
                progressDialog.dismiss()

                if (result) {
                    // Delete all related data after successful completion
                    try {
                        viewModel.deleteAllData(journalId)
                        showActionDialog(
                            title = "Success",
                            message = "Stock counting completed successfully. All data has been cleared.",
                            positiveButton = "OK" to {
                                setResult(Activity.RESULT_OK)
                                finish()
                            },
                            negativeButton = "Cancel" to null  // Fixed this line
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deleting data", e)
                        showActionDialog(
                            title = "Success",
                            message = "Stock counting completed successfully, but there was an error clearing the data.",
                            positiveButton = "OK" to { finish() },
                            negativeButton = "Cancel" to null  // Fixed this line
                        )
                    }
                } else {
                    showActionDialog(
                        title = "Error",
                        message = "Failed to complete stock counting",
                        positiveButton = "Try Again" to { completeStockCounting() },
                        negativeButton = "Cancel" to null
                    )
                }
            } catch (e: Exception) {
                progressDialog.dismiss()
                showActionDialog(
                    title = "Error",
                    message = "Error: ${e.message}",
                    positiveButton = "Try Again" to { completeStockCounting() },
                    negativeButton = "Cancel" to null
                )
            }
        }
    }

    private fun sendData() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                val unsyncedTransactions = viewModel.getUnsyncedTransactions(journalId)

                withContext(Dispatchers.Main) {
                    if (unsyncedTransactions.isNotEmpty()) {
                        showUnsyncedItemsDialog(unsyncedTransactions)
                    } else {
                        showCompleteStockDialog(storeId, journalId)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LineActivity,
                        "Error checking sync status: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun showUnsyncedItemsDialog(unsyncedItems: List<LineTransaction>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)

        val message = buildString {
            appendLine("Found ${unsyncedItems.size} unsynced items:")
            appendLine()
            unsyncedItems.take(5).forEach { item ->
                appendLine("• ${item.itemId} - ${item.itemName}")
            }
            if (unsyncedItems.size > 5) {
                appendLine("... and ${unsyncedItems.size - 5} more items")
            }
            appendLine()
            appendLine("Would you like to sync these items now?")
        }

        messageTextView.text = message

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Unsynced Items")
            .setView(dialogView)
            .setPositiveButton("Sync Now") { _, _ ->
                performSyncedSend()
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    // Add this to LineTransaction

    private fun performSend() {
        loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                // Get the updated items
                val updatedItems = adapter.getUpdatedItems()
                var successCount = 0
                var failureMessages = mutableListOf<String>()

                updatedItems.forEach { item ->
                    try {
                        Log.d(TAG, "Sending item: ${item.itemId}")

                        // Format numbers to integers
                        val adjustment = (item.adjustment?.toDoubleOrNull() ?: 0.0).toInt().toString()
                        val receivedCount = (item.receivedCount?.toDoubleOrNull() ?: 0.0).toInt().toString()
                        val transferCount = (item.transferCount?.toDoubleOrNull() ?: 0.0).toInt().toString()
                        val wasteCount = (item.wasteCount?.toDoubleOrNull() ?: 0.0).toInt().toString()
                        val counted = (item.counted?.toDoubleOrNull() ?: 0.0).toInt().toString()

                        // Set default waste type if empty
                        val wasteType = if (item.wasteType.isNullOrBlank()) "none" else item.wasteType

                        val result = viewModel.postLineDetails(
                            itemId = item.itemId ?: "",
                            storeId = storeId,
                            journalId = journalId,
                            adjustment = adjustment,
                            receivedCount = receivedCount,
                            transferCount = transferCount,
                            wasteCount = wasteCount,
                            wasteType = wasteType,
                            counted = counted
                        )
                        if (result) {
                            successCount++
                            Log.d(TAG, "Successfully sent item: ${item.itemId}")
                        } else {
                            failureMessages.add("Failed to send item: ${item.itemId}")
                            Log.e(TAG, "Failed to send item: ${item.itemId}")
                        }
                    } catch (e: Exception) {
                        failureMessages.add("Error sending ${item.itemId}: ${e.message}")
                        Log.e(TAG, "Error sending item: ${item.itemId}", e)
                    }
                }

                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    if (successCount == updatedItems.size) {
                        Toast.makeText(this@LineActivity, "All data sent successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        showSendResultDialog(successCount, updatedItems.size, failureMessages)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in send operation", e)
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(this@LineActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveLineDetails() {
        if (!adapter.hasModifications()) {
            Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
            return
        }

        loadingProgressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
                val updatedList = adapter.getUpdatedItems()

                val result = viewModel.saveLineDetails(storeId, journalId, updatedList)

                withContext(Dispatchers.Main) {
                    if (result) {
                        // Fetch fresh data from database
                        try {
                            val freshData = viewModel.getLocalLineDetails(journalId)
                            freshData.onSuccess { transactions ->
                                adapter.setItems(transactions)  // Use setItems instead of updateItems
                                Toast.makeText(
                                    this@LineActivity,
                                    "Data saved successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { exception ->
                                Log.e(TAG, "Error fetching fresh data", exception)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error refreshing data", e)
                        }
                    } else {
                        Toast.makeText(
                            this@LineActivity,
                            "Failed to save data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    loadingProgressBar.visibility = View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingProgressBar.visibility = View.GONE
                    Toast.makeText(
                        this@LineActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private suspend fun handleSaveResult(result: Boolean, modifiedCount: Int) {
        withContext(Dispatchers.Main) {
            loadingProgressBar.visibility = View.GONE
            if (result) {
                Log.d(TAG, "Successfully saved $modifiedCount items")
                Toast.makeText(
                    this@LineActivity,
                    "Saved $modifiedCount modified items",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.e(TAG, "Failed to save items")
                Toast.makeText(
                    this@LineActivity,
                    "Failed to save data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun checkAndPerformAutoSync() {
        lifecycleScope.launch {
            try {
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch
                val unsyncedCount = viewModel.getUnsyncedCount(journalId)

                if (unsyncedCount > 0) {
                    Log.d(TAG, "Found $unsyncedCount unsynced items, starting auto sync")
                    performAutoSync()
                } else {
                    Log.d(TAG, "No unsynced items found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for unsynced items", e)
            }
        }
    }
    //    private fun performAutoSync() {
//        lifecycleScope.launch {
//            try {
//                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch
//                val unsyncedCount = viewModel.getUnsyncedCount(journalId)
//
//                if (unsyncedCount > 0) {
//                    Log.d(TAG, "Starting auto sync for $unsyncedCount items")
//                    viewModel.syncModifiedData()
//                } else {
//                    Log.d(TAG, "No items to sync")
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Error in auto sync", e)
//            }
//        }
//    }
    private fun performAutoSync() {
        viewModel.syncModifiedData()
    }
    private fun setupPrinter() {
        printer = BluetoothPrinterHelper.getInstance()

        // Get paired devices and show dialog to select printer
        findViewById<Button>(R.id.btnPrint).setOnClickListener {
            if (printer.isConnected()) {
                printLineDetails()
            } else {
                showPrinterSelectionDialog()
            }
        }
    }
    private fun showPrinterSelectionDialog() {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        val pairedDevices = bluetoothAdapter.bondedDevices.toList()
        val deviceNames = pairedDevices.map { it.name ?: "Unknown Device" }.toTypedArray()

        val dialogView = layoutInflater.inflate(R.layout.dialog_list, null)
        val listView = dialogView.findViewById<ListView>(R.id.listView)
        val adapter = ArrayAdapter(this, R.layout.item_printer_device, deviceNames)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Select Printer")
            .setView(dialogView)
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = pairedDevices[position]
            printerAddress = device.address

            getSharedPreferences("printer_prefs", MODE_PRIVATE)
                .edit()
                .putString("last_printer_address", printerAddress)
                .apply()

            if (printer.connect(printerAddress!!)) {
                printLineDetails()
            } else {
                Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }
    private fun printLineDetails() {
        val printer = BluetoothPrinterHelper.getInstance()
        if (!printer.isConnected()) {
            Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
        if (journalId.isEmpty()) {
            Toast.makeText(this, "Invalid batch number", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val content = StringBuilder()
            content.append(0x1B.toChar()) // ESC
            content.append('!'.toChar())  // Select print mode
            content.append(0x01.toChar()) // Smallest text size

            // Set minimum line spacing
            content.append(0x1B.toChar()) // ESC
            content.append('3'.toChar())  // Select line spacing
            content.append(50.toChar())

            content.appendLine("==============================")
            content.appendLine("     BATCH COUNTING REPORT      ")
            content.appendLine("==============================")
            content.appendLine("Store: ${intent.getStringExtra(EXTRA_STORE_ID)}")
            content.appendLine("BATCH: BATCH$journalId")
            content.appendLine("Date: ${SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
            content.appendLine("------------------------------")

            // Log the number of items in the current list
            val currentList = adapter.getAllItems() // Use getAllItems() instead of getCurrentList()
            Log.d("PrintDetails", "Number of items: ${currentList.size}")

            // Get items from adapter's current list with non-zero values
            currentList.filter { item ->
                val hasValues = (item.adjustment?.toDoubleOrNull() ?: 0.0) > 0 ||
                        (item.receivedCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                        (item.wasteCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                        (item.transferCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                        (item.counted?.toDoubleOrNull() ?: 0.0) > 0

                // Log item values for debugging
                Log.d("PrintDetails", "Item ${item.itemId}: hasValues=$hasValues, " +
                        "adj=${item.adjustment}, rcv=${item.receivedCount}, " +
                        "waste=${item.wasteCount}, trans=${item.transferCount}, " +
                        "count=${item.counted}")

                hasValues
            }.forEach { item ->
                // Item info
                val itemInfo = "${item.itemId} - ${item.itemName}"
                content.appendLine(itemInfo)

                val adjustment = item.adjustment?.toDoubleOrNull() ?: 0.0
                val receivedCount = item.receivedCount?.toDoubleOrNull() ?: 0.0
                val wasteCount = item.wasteCount?.toDoubleOrNull() ?: 0.0
                val transferCount = item.transferCount?.toDoubleOrNull() ?: 0.0
                val counted = item.counted?.toDoubleOrNull() ?: 0.0

                // Create two-column lines for values
                if (adjustment > 0 && receivedCount > 0) {
                    content.appendLine(String.format("%-20s%-20s",
                        "Order: ${adjustment.toInt()} [] ___",
                        "Rcvd: ${receivedCount.toInt()} [] ___"))
                } else {
                    if (adjustment > 0) content.appendLine(String.format("%-40s", "Order: ${adjustment.toInt()} [] ___"))
                    if (receivedCount > 0) content.appendLine(String.format("%-40s", "Rcvd: ${receivedCount.toInt()} [] ___"))
                }

                if (wasteCount > 0) {
                    content.appendLine(String.format("%-40s", "Waste: ${wasteCount.toInt()} [] ___"))
                    if (!item.wasteType.isNullOrEmpty() && item.wasteType != "Select type") {
                        content.appendLine(String.format("%-40s", "Type: ${item.wasteType}"))
                    }
                }

                if (transferCount > 0 && counted > 0) {
                    content.appendLine(String.format("%-20s%-20s",
                        "Trans: ${transferCount.toInt()} [] ___",
                        "Count: ${counted.toInt()} [] ___"))
                } else {
                    if (transferCount > 0) content.appendLine(String.format("%-40s", "Trans: ${transferCount.toInt()} [] ___"))
                    if (counted > 0) content.appendLine(String.format("%-40s", "Count: ${counted.toInt()} [] ___"))
                }

                content.appendLine("------------------------------")
            }

            content.appendLine("End of Report")
            content.appendLine("==============================")
            content.append(0x1B.toChar())
            content.append('!'.toChar())
            content.append(0x00.toChar())
            content.append(0x1B.toChar())
            content.append('2'.toChar())

            // Log the final content for debugging
            Log.d("PrintDetails", "Content to print: ${content.toString()}")

            val success = printer.printGenericReceipt(content.toString())
            if (success) {
                Toast.makeText(this, "Report printed successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to print report: ${printer.getLastError()}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("PrintDetails", "Error printing", e)
            Toast.makeText(this, "Error printing: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun generateCSV() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val fileName = "BATCH_aCOUNTING_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(null), fileName)

                file.bufferedWriter().use { writer ->
                    // Write headers
                    writer.write("Item ID,Item Name,Category,Order,Received,Variance,Transfer,Waste Count,Waste Type,Actual Count\n")

                    // Write data
                    adapter.getCurrentList().forEach { item ->
                        writer.write(
                            "${item.itemId},${item.itemName},${item.itemDepartment},${item.adjustment}," +
                                    "${item.receivedCount},${item.variantId ?: "0"},${item.transferCount ?: "0"}," +
                                    "${item.wasteCount},${item.wasteType ?: ""},${item.counted}\n"
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LineActivity,
                        "CSV file saved: ${file.absolutePath}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@LineActivity,
                        "Error generating CSV: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}