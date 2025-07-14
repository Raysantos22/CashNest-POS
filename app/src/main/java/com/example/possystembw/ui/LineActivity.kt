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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class LineActivity : AppCompatActivity() {
    private lateinit var viewModel: LineViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LineAdapter
    private lateinit var loadingProgressBar: ProgressBar
    private var currentSortField = "itemId"
    private var isAscending = true
    private var originalList: List<LineTransaction> = emptyList()
    private lateinit var printer: BluetoothPrinterHelper
    private var printerAddress: String? = null
    private var hasLoadedData = false

    private lateinit var progressDialog: AlertDialog
    private var currentSyncDialog: AlertDialog? = null
    private var currentErrorDialog: AlertDialog? = null
    private var isShowingDialog = false
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

        setupViews()
        adapter = LineAdapter()
        recyclerView.adapter = adapter
        setupBackHandling()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        BluetoothPrinterHelper.initialize(applicationContext)
        setupObservers()
        loadingProgressBar = findViewById(R.id.loadingLineDetails)
        loadingProgressBar.visibility = View.VISIBLE

        setupSendButton()
        viewModel = ViewModelProvider(this, LineViewModelFactory(application))[LineViewModel::class.java]
        val storeId = intent.getStringExtra(EXTRA_STORE_ID)
        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)

        if (storeId != null && journalId != null) {
            loadingProgressBar.visibility = View.VISIBLE
            viewModel.setIds(storeId, journalId)
            viewModel.fetchLineDetails(storeId, journalId)
        } else {
            Toast.makeText(this, "Missing required parameters", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupProgressDialog()
        setupTableHeaderClicks()
        setupSearch()
        setupButtons()
        setupSyncStatusObserver()

        recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            recycledViewPool.setMaxRecycledViews(0, 20)
            layoutManager = LinearLayoutManager(this@LineActivity).apply {
                initialPrefetchItemCount = 4
                isItemPrefetchEnabled = true
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

        setupPrinter()

        // Set up adapter listener for data changes
        adapter.setOnItemModifiedListener { updatedItems ->
            Log.d(TAG, "Items modified: ${updatedItems.size}")
        }
    }

    private fun setupBackHandling() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
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
        onBackPressedDispatcher.addCallback(this, callback)
        Log.d(TAG, "Back handler set up successfully")
    }

    private fun showBackConfirmationDialog() {
        Log.d(TAG, "Showing back confirmation dialog")

        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = "You have unsaved changes. What would you like to do?"

        val dialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle("Unsaved Changes")
            .setView(dialogView)
            .setPositiveButton("Save & Exit") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""
                        val updatedList = adapter.getUpdatedItems()

                        val result = viewModel.saveLineDetails(storeId, journalId, updatedList)

                        withContext(Dispatchers.Main) {
                            if (result) {
                                Toast.makeText(this@LineActivity, "Changes saved", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@LineActivity, "Failed to save changes", Toast.LENGTH_SHORT).show()
                            }
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
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
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
            performCompleteStockCountingFlow()
        }
    }

    /**
     * Complete stock counting flow - handles everything in one go
     */
    private fun performCompleteStockCountingFlow() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                Log.d(TAG, "Starting complete stock counting flow")

                // Show the complete stock counting progress dialog
                showStockCountingProgressDialog()

                // Step 1: Save any pending changes from adapter to database
                updateProgressDialog("Saving pending changes...", 5)
                if (adapter.hasModifications()) {
                    val updatedList = adapter.getUpdatedItems()
                    val originalItems = viewModel.getCurrentData()

                    // Only mark actually modified items as unsynced
                    val actuallyModifiedItems = updatedList.filter { updatedItem ->
                        val originalItem = originalItems.find { it.itemId == updatedItem.itemId }
                        originalItem == null || !areItemsEqual(originalItem, updatedItem)
                    }.map { item ->
                        item.copy(syncStatus = 0) // Mark as unsynced
                    }

                    if (actuallyModifiedItems.isNotEmpty()) {
                        val saveResult = viewModel.saveModifiedLineDetails(storeId, journalId, actuallyModifiedItems)
                        if (!saveResult) {
                            dismissProgressDialog()
                            showErrorDialog("Failed to save changes to database. Please try again.")
                            return@launch
                        }
                        Log.d(TAG, "Successfully saved ${actuallyModifiedItems.size} modified items to database")
                    }
                }

                // Step 2: Check for unsynced items that have actual values
                updateProgressDialog("Checking for items to sync...", 10)
                val unsyncedEditedItems = viewModel.getUnsyncedTransactions(journalId).filter {
                    it.hasAnyValue()
                }

                Log.d(TAG, "Found ${unsyncedEditedItems.size} unsynced items with values")

                // Step 3: Sync items if any exist
                if (unsyncedEditedItems.isNotEmpty()) {
                    updateProgressDialog("Syncing ${unsyncedEditedItems.size} items to server...", 15)

                    // Perform sync with progress tracking
                    val syncResult = performSyncWithProgress(unsyncedEditedItems.size)
                    if (!syncResult) {
                        return@launch // Error already handled in performSyncWithProgress
                    }

                    updateProgressDialog("All items synced successfully", 50)
                } else {
                    updateProgressDialog("No items to sync", 50)
                }

                // Step 4: Complete stock counting (same as before)
                updateProgressDialog("Completing stock counting...", 60)
                val stockCountingResult = viewModel.postStockCounting(storeId, journalId)

                if (!stockCountingResult) {
                    dismissProgressDialog()
                    showErrorDialog("Failed to complete stock counting. Please check your internet connection and try again.")
                    return@launch
                }

                // Step 5: Mark all items as posted
                updateProgressDialog("Updating item status...", 80)
                delay(500)

                try {
                    viewModel.markAllItemsAsPosted(journalId)
                    Log.d(TAG, "All items marked as posted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking items as posted", e)
                }

                // Step 6: Clear local data
                updateProgressDialog("Clearing local data...", 95)
                delay(500)

                try {
                    viewModel.deleteAllData(journalId)
                    Log.d(TAG, "All local data cleared")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing data", e)
                    dismissProgressDialog()
                    showActionDialog(
                        title = "Partial Success",
                        message = "Stock counting completed successfully, but there was an error clearing local data.",
                        positiveButton = "OK" to {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    )
                    return@launch
                }

                // Step 7: Complete
                updateProgressDialog("Stock counting completed!", 100)
                delay(1000)

                dismissProgressDialog()

                showActionDialog(
                    title = "Success!",
                    message = "Stock counting has been completed successfully.\n\nAll data has been posted and cleared from local storage.",
                    positiveButton = "Finish" to {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    cancelable = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in complete stock counting flow", e)
                dismissProgressDialog()

                val errorMessage = when {
                    e.message?.contains("UnknownHostException") == true ||
                            e.message?.contains("ConnectException") == true ||
                            e.message?.contains("No internet") == true -> {
                        "No internet connection. Please check your internet connection and try again."
                    }
                    else -> "An error occurred: ${e.message}"
                }

                showActionDialog(
                    title = "Error",
                    message = errorMessage,
                    positiveButton = "Retry" to { performCompleteStockCountingFlow() },
                    negativeButton = "Cancel" to { }
                )
            }
        }
    }

    /**
     * Performs sync with accurate progress tracking
     */
    private suspend fun performSyncWithProgress(totalItems: Int): Boolean {
        return try {
            withContext(Dispatchers.Main) {
                isShowingDialog = false
            }

            // Set up sync progress observer with accurate tracking
            val progressObserver = { progress: LineViewModel.SyncProgress ->
                when {
                    !progress.isComplete -> {
                        // Calculate accurate progress percentage for sync portion (15% to 50%)
                        val syncProgress = if (progress.totalItems > 0) {
                            15 + ((progress.currentItem * 35) / progress.totalItems)
                        } else {
                            15
                        }

                        updateProgressDialog(
                            message = if (progress.currentItemId.isNotEmpty())
                                "Syncing: ${progress.currentItemId} (${progress.currentItem}/${progress.totalItems})"
                            else
                                "Preparing to sync...",
                            progress = syncProgress
                        )
                    }
                    progress.errorMessage != null -> {
                        if (!isShowingDialog) {
                            isShowingDialog = true
                            dismissProgressDialog()

                            val errorMessage = when {
                                progress.errorMessage.contains("UnknownHostException") ||
                                        progress.errorMessage.contains("ConnectException") ||
                                        progress.errorMessage.contains("No internet") -> {
                                    "No internet connection. Please check your internet connection and try again."
                                }
                                else -> progress.errorMessage
                            }

                            showActionDialog(
                                title = "Sync Error",
                                message = errorMessage,
                                positiveButton = "Retry" to {
                                    isShowingDialog = false
                                    performCompleteStockCountingFlow()
                                },
                                negativeButton = "Cancel" to {
                                    isShowingDialog = false
                                }
                            )
                        }
                    }
                    else -> {
                        // Sync completed successfully
                        refreshAdapterAfterSync()
                    }
                }
            }

            // Remove any existing observers and add new one
            withContext(Dispatchers.Main) {
                viewModel.syncProgress.removeObservers(this@LineActivity)
                viewModel.syncProgress.observe(this@LineActivity, progressObserver)
            }

            // Start the sync
            viewModel.syncModifiedData()

            // Wait for sync to complete
            var syncCompleted = false
            var syncSuccess = false

            withContext(Dispatchers.Main) {
                viewModel.syncProgress.observe(this@LineActivity) { progress ->
                    if (progress.isComplete) {
                        syncCompleted = true
                        syncSuccess = progress.errorMessage == null
                    }
                }
            }

            // Wait for completion (with timeout)
            var waitTime = 0
            while (!syncCompleted && waitTime < 30000) { // 30 second timeout
                delay(100)
                waitTime += 100
            }

            if (!syncCompleted) {
                dismissProgressDialog()
                showErrorDialog("Sync operation timed out. Please try again.")
                return false
            }

            syncSuccess

        } catch (e: Exception) {
            Log.e(TAG, "Error in performSyncWithProgress", e)
            dismissProgressDialog()

            val errorMessage = when {
                e.message?.contains("UnknownHostException") == true ||
                        e.message?.contains("ConnectException") == true -> {
                    "No internet connection. Please check your internet connection and try again."
                }
                else -> "Sync failed: ${e.message}"
            }

            showErrorDialog(errorMessage)
            false
        }
    }

    private fun handleSyncError(errorMessage: String) {
        val title = if (errorMessage.contains("No internet")) "No Internet Connection" else "Sync Error"
        val message = if (errorMessage.contains("No internet"))
            "Please check your internet connection and try again."
        else
            errorMessage

        showActionDialog(
            title = title,
            message = message,
            positiveButton = "Retry" to {
                isShowingDialog = false
                performSaveAndSync()
            },
            negativeButton = "Cancel" to {
                isShowingDialog = false
            }
        )
    }

    private fun handleSyncSuccess(totalItems: Int) {
        // Refresh adapter after successful sync
        refreshAdapterAfterSync()

        if (totalItems > 0) {
            showActionDialog(
                title = "Sync Complete ✓",
                message = "Successfully synced $totalItems items.\n\nAll changes have been saved to the server.",
                positiveButton = "OK" to {
                    isShowingDialog = false
                }
            )
        } else {
            showActionDialog(
                title = "All Items Synced",
                message = "All edited items are already synced.",
                positiveButton = "OK" to {
                    isShowingDialog = false
                }
            )
        }
    }

    private fun refreshAdapterAfterSync() {
        lifecycleScope.launch {
            try {
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch
                val freshData = viewModel.getLocalLineDetails(journalId)

                freshData.onSuccess { transactions ->
                    withContext(Dispatchers.Main) {
                        originalList = transactions
                        adapter.setItems(transactions)
                        Log.d(TAG, "Adapter refreshed after sync with ${transactions.size} items")

                        Toast.makeText(
                            this@LineActivity,
                            "✓ All changes synced successfully",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Error refreshing adapter after sync", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in refreshAdapterAfterSync", e)
            }
        }
    }

    private fun showActionDialog(
        title: String,
        message: String,
        positiveButton: Pair<String, (() -> Unit)?>,
        negativeButton: Pair<String, (() -> Unit)?>? = null,
        cancelable: Boolean = true
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_message, null)
        val messageTextView = dialogView.findViewById<TextView>(R.id.tvMessage)
        messageTextView.text = message

        val dialogBuilder = AlertDialog.Builder(this, R.style.CustomDialogStyle)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(positiveButton.first) { dialog, _ ->
                dialog.dismiss()
                positiveButton.second?.invoke()
            }
            .setCancelable(cancelable)

        negativeButton?.let { negButton ->
            dialogBuilder.setNegativeButton(negButton.first) { dialog, _ ->
                dialog.dismiss()
                negButton.second?.invoke()
            }
        }

        val dialog = dialogBuilder.create()
        dialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        dialog.show()
    }

    private fun showErrorDialog(message: String) {
        showActionDialog(
            title = "Error",
            message = message,
            positiveButton = "OK" to null
        )
    }


    // Fix: Add missing performSyncOperation function
    private fun performSyncOperation() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                // Show sync progress dialog
                showSyncProgressDialog()

                // Set up sync progress observer
                val progressObserver = { progress: LineViewModel.SyncProgress ->
                    when {
                        !progress.isComplete -> {
                            updateSyncProgressDialog(
                                message = if (progress.currentItemId.isNotEmpty())
                                    "Syncing: ${progress.currentItemId} (${progress.currentItem}/${progress.totalItems})"
                                else
                                    "Preparing to sync...",
                                current = progress.currentItem,
                                total = progress.totalItems
                            )
                        }
                        progress.errorMessage != null -> {
                            if (!isShowingDialog) {
                                isShowingDialog = true
                                dismissSyncProgressDialog()

                                val errorMessage = when {
                                    progress.errorMessage.contains("UnknownHostException") ||
                                            progress.errorMessage.contains("ConnectException") ||
                                            progress.errorMessage.contains("No internet") -> {
                                        "No internet connection. Please check your internet connection and try again."
                                    }
                                    else -> progress.errorMessage
                                }

                                showActionDialog(
                                    title = "Sync Error",
                                    message = errorMessage,
                                    positiveButton = "Retry" to {
                                        isShowingDialog = false
                                        performSyncOperation()
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
                                dismissSyncProgressDialog()
                                refreshAdapterAfterSync()

                                // After successful sync, continue with stock counting
                                showActionDialog(
                                    title = "Sync Complete ✓",
                                    message = "Successfully synced ${progress.totalItems} items.\n\nNow completing stock counting...",
                                    positiveButton = "Continue" to {
                                        isShowingDialog = false
                                        completeStockCounting()
                                    }
                                )
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isShowingDialog = false
                    viewModel.syncProgress.removeObservers(this@LineActivity)
                    viewModel.syncProgress.observe(this@LineActivity, progressObserver)
                }

                // Start the sync
                viewModel.syncModifiedData()

            } catch (e: Exception) {
                Log.e(TAG, "Error in performSyncOperation", e)
                withContext(Dispatchers.Main) {
                    dismissSyncProgressDialog()

                    val errorMessage = when {
                        e.message?.contains("UnknownHostException") == true ||
                                e.message?.contains("ConnectException") == true -> {
                            "No internet connection. Please check your internet connection and try again."
                        }
                        else -> "Sync failed: ${e.message}"
                    }

                    showErrorDialog(errorMessage)
                }
            }
        }
    }

    // Fix: Correct the completeStockCounting function
    private fun completeStockCounting() {
        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

                Log.d(TAG, "Starting complete stock counting process")

                // Show the stock counting progress dialog
                showStockCountingProgressDialog()

                // Step 1: Save any pending changes from adapter to database
                updateProgressDialog("Saving pending changes...", 5)
                if (adapter.hasModifications()) {
                    val updatedItems = adapter.getUpdatedItems()
                    val modifiedItems = updatedItems.map { item ->
                        item.copy(syncStatus = 0) // Mark as unsynced
                    }

                    val saveResult = viewModel.saveLineDetails(storeId, journalId, modifiedItems)
                    if (!saveResult) {
                        dismissProgressDialog()
                        showErrorDialog("Failed to save your changes. Please try again.")
                        return@launch
                    }
                    Log.d(TAG, "Successfully saved pending changes")
                }

                // Step 2: Check for unsynced items
                updateProgressDialog("Checking for unsynced items...", 25)
                val unsyncedEditedItems = viewModel.getUnsyncedTransactions(journalId).filter {
                    it.hasAnyValue()
                }

                if (unsyncedEditedItems.isNotEmpty()) {
                    dismissProgressDialog()
                    showActionDialog(
                        title = "Unsaved Edits Found",
                        message = "You have ${unsyncedEditedItems.size} edited items that need to be synced first.\n\nWould you like to sync them now?",
                        positiveButton = "Sync Now" to { performSyncOperation() },
                        negativeButton = "Cancel" to { }
                    )
                    return@launch
                }

                // Step 3: Proceed with stock counting completion
                updateProgressDialog("Completing stock counting...", 60)

                val stockCountingResult = viewModel.postStockCounting(storeId, journalId)

                if (!stockCountingResult) {
                    dismissProgressDialog()
                    showActionDialog(
                        title = "Error",
                        message = "Failed to complete stock counting. Please check your internet connection and try again.",
                        positiveButton = "Retry" to { completeStockCounting() },
                        negativeButton = "Cancel" to { }
                    )
                    return@launch
                }

                // Step 4: Update item status
                updateProgressDialog("Updating item status...", 80)
                delay(500)

                try {
                    viewModel.markAllItemsAsPosted(journalId)
                    Log.d(TAG, "All items marked as posted")
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking items as posted", e)
                    // Continue anyway since stock counting was successful
                }

                // Step 5: Clear local data
                updateProgressDialog("Clearing local data...", 95)
                delay(500)

                try {
                    viewModel.deleteAllData(journalId)
                    Log.d(TAG, "All local data cleared")
                } catch (e: Exception) {
                    Log.e(TAG, "Error clearing data", e)
                    dismissProgressDialog()
                    showActionDialog(
                        title = "Partial Success",
                        message = "Stock counting completed successfully, but there was an error clearing local data.",
                        positiveButton = "OK" to {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    )
                    return@launch
                }

                // Step 6: Complete
                updateProgressDialog("Stock counting completed!", 100)
                delay(1000)

                dismissProgressDialog()

                showActionDialog(
                    title = "Success!",
                    message = "Stock counting has been completed successfully.\n\nAll data has been posted and cleared from local storage.",
                    positiveButton = "Finish" to {
                        setResult(Activity.RESULT_OK)
                        finish()
                    },
                    cancelable = false
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error in completeStockCounting", e)
                dismissProgressDialog()

                val errorMessage = when {
                    e.message?.contains("UnknownHostException") == true ||
                            e.message?.contains("ConnectException") == true ||
                            e.message?.contains("No internet") == true -> {
                        "No internet connection. Please check your internet connection and try again."
                    }
                    else -> "An error occurred while completing stock counting: ${e.message}"
                }

                showActionDialog(
                    title = "Error",
                    message = errorMessage,
                    positiveButton = "Retry" to { completeStockCounting() },
                    negativeButton = "Cancel" to { }
                )
            }
        }
    }


//    private fun updateProgressDialog(message: String, progress: Int) {
//        progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
//        progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = "$progress%"
//        progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text = message
//    }


    private fun showStockCountingProgressDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_sync_progress, null)

    progressDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
        .setTitle("Completing Stock Counting")
        .setView(dialogView)
        .setCancelable(false)
        .create()

    progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    progressDialog.show()

    progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.apply {
        max = 100
        progress = 0
        isIndeterminate = false
    }
    progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = "0%"
    progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text = "Initializing..."
}

private fun updateProgressDialog(message: String, progress: Int) {
    progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress
    progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = "$progress%"
    progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text = message
}

private fun dismissProgressDialog() {
    if (::progressDialog.isInitialized && progressDialog.isShowing) {
        progressDialog.dismiss()
    }
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

    val handler = Handler(Looper.getMainLooper())
    handler.post(object : Runnable {
        override fun run() {
            timeTextView.text = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
                .format(Date())
            handler.postDelayed(this, 1000)
        }
    })

    adapter = LineAdapter()
    recyclerView.apply {
        layoutManager = LinearLayoutManager(this@LineActivity)
        adapter = this@LineActivity.adapter
    }

    viewModel =
        ViewModelProvider(this, LineViewModelFactory(application))[LineViewModel::class.java]
}

private fun setupObservers() {
    viewModel.isLoading.observe(this) { isLoading ->
        loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE

        if (isLoading) {
            findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.GONE
        }
    }

    viewModel.lineDetailsResult.observe(this) { result ->
        loadingProgressBar.visibility = View.GONE
        findViewById<SwipeRefreshLayout>(R.id.swipeRefreshLayout)?.isRefreshing = false

        result?.onSuccess { transactions ->
            Log.d(TAG, "Successfully received ${transactions.size} transactions")

            if (transactions.isEmpty()) {
                findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                findViewById<TextView>(R.id.tvEmptyState)?.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE

                val isRefreshOperation = originalList.isNotEmpty() && transactions != originalList

                originalList = transactions
                adapter.setItems(transactions)

                if (isRefreshOperation) {
                    Toast.makeText(this, "✅ Data refreshed successfully! Loaded ${transactions.size} items", Toast.LENGTH_SHORT).show()
                } else if (originalList.isEmpty()) {
                    Toast.makeText(this, "Loaded ${transactions.size} items", Toast.LENGTH_SHORT).show()
                }
            }
        }?.onFailure { exception ->
            Log.e(TAG, "Failed to load data", exception)

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
                        finish()
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
    val clearButton = findViewById<ImageView>(R.id.clearSearch)

    clearButton.visibility = if (searchEditText.text.isNotEmpty()) View.VISIBLE else View.GONE

    searchEditText.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            clearButton.visibility = if (s?.isNotEmpty() == true) View.VISIBLE else View.GONE
            adapter.filterItems(s.toString())
        }
    })

    clearButton.setOnClickListener {
        searchEditText.setText("")
        adapter.filterItems("")
    }
}

private fun setupButtons() {
    findViewById<Button>(R.id.btnPrint).setOnClickListener {
        printLineDetails()
    }

    findViewById<Button>(R.id.btnGenerate).setOnClickListener {
        generateCSV()
    }

    // Save Button - Now syncs immediately to server
    findViewById<Button>(R.id.btnSave).setOnClickListener {
        performSaveAndSync()
    }

    findViewById<Button>(R.id.btnFilterTransactions).setOnClickListener {
        toggleTransactionFilter()
    }

    findViewById<Button>(R.id.btnReget).setOnClickListener {
        showRegetConfirmationDialog()
    }
}

/**
 * Save and sync function - saves locally then syncs to server
 */
    /**
     * Save and sync function - saves locally then syncs only NEWLY MODIFIED items to server
     */
    private fun performSaveAndSync() {
        if (!adapter.hasModifications()) {
            Toast.makeText(this, "No changes detected", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: ""
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: ""

                // Show sync progress dialog
                showSyncProgressDialog()

                // Step 1: Get only the items that have been modified in the adapter
                updateSyncProgressDialog("Identifying modified items...", 0, 100)

                val updatedList = adapter.getUpdatedItems()

                // Find only the items that are actually different from originalList
                val actuallyModifiedItems = updatedList.filter { updatedItem ->
                    val originalItem = originalList.find { it.itemId == updatedItem.itemId }
                    originalItem == null || !areItemsEqual(originalItem, updatedItem)
                }

                if (actuallyModifiedItems.isEmpty()) {
                    dismissProgressDialog()
                    Toast.makeText(this@LineActivity, "No new changes to save", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                Log.d(TAG, "Found ${actuallyModifiedItems.size} actually modified items")

                // Step 2: Update ONLY the modified items in database (don't replace all data)
                updateSyncProgressDialog("Saving ${actuallyModifiedItems.size} modified items locally...", 15, 100)

                // FIXED: Use updateSpecificItems instead of saveLineDetails to avoid replacing all data
                val saveResult = updateSpecificItemsInDatabase(journalId, actuallyModifiedItems)
                if (!saveResult) {
                    dismissProgressDialog()
                    showErrorDialog("Failed to save changes to database. Please try again.")
                    return@launch
                }

                // Step 3: Update the originalList with the saved changes (keep all other items)
                originalList = originalList.map { originalItem ->
                    val modifiedItem = actuallyModifiedItems.find { it.itemId == originalItem.itemId }
                    if (modifiedItem != null) {
                        modifiedItem.copy(syncStatus = 0) // Mark modified items as unsynced
                    } else {
                        originalItem // Keep original item unchanged
                    }
                }

                // Step 4: Update adapter with the updated list (all items preserved)
                withContext(Dispatchers.Main) {
                    adapter.setItems(originalList)
                }

                // Step 5: Get only the unsynced items that have actual values
                val unsyncedEditedItems = actuallyModifiedItems.filter { it.hasAnyValue() }

                if (unsyncedEditedItems.isEmpty()) {
                    dismissProgressDialog()
                    Toast.makeText(this@LineActivity, "Changes saved locally (no items to sync)", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // Step 6: Sync only the newly modified items to server
                updateSyncProgressDialog("Starting sync to server...", 25, 100)

                val progressObserver = { progress: LineViewModel.SyncProgress ->
                    when {
                        !progress.isComplete -> {
                            val syncProgress = if (progress.totalItems > 0) {
                                25 + ((progress.currentItem * 65) / progress.totalItems)
                            } else {
                                25
                            }

                            updateSyncProgressDialog(
                                message = if (progress.currentItemId.isNotEmpty())
                                    "Syncing: ${progress.currentItemId} (${progress.currentItem}/${progress.totalItems})"
                                else
                                    "Preparing to sync...",
                                current = syncProgress,
                                total = 100
                            )
                        }
                        progress.errorMessage != null -> {
                            if (!isShowingDialog) {
                                isShowingDialog = true
                                dismissProgressDialog()

                                val errorMessage = when {
                                    progress.errorMessage.contains("UnknownHostException") ||
                                            progress.errorMessage.contains("ConnectException") ||
                                            progress.errorMessage.contains("No internet") -> {
                                        "No internet connection. Please check your internet connection and try again."
                                    }
                                    else -> progress.errorMessage
                                }

                                showActionDialog(
                                    title = "Sync Error",
                                    message = errorMessage,
                                    positiveButton = "Retry" to {
                                        isShowingDialog = false
                                        performSaveAndSync()
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
                                lifecycleScope.launch {
                                    updateSyncProgressDialog("Sync completed successfully!", 100, 100)
                                    delay(500)
                                    dismissProgressDialog()

                                    // Update sync status of successfully synced items
                                    updateSyncStatusAfterSuccess(unsyncedEditedItems)

                                    showActionDialog(
                                        title = "Save & Sync Complete ✓",
                                        message = "Successfully saved and synced ${progress.totalItems} items to the server.\n\nAll new changes have been saved.",
                                        positiveButton = "OK" to {
                                            isShowingDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    isShowingDialog = false
                    viewModel.syncProgress.removeObservers(this@LineActivity)
                    viewModel.syncProgress.observe(this@LineActivity, progressObserver)
                }

                // Start the sync
                viewModel.syncModifiedData()

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    dismissProgressDialog()

                    val errorMessage = when {
                        e.message?.contains("UnknownHostException") == true ||
                                e.message?.contains("ConnectException") == true -> {
                            "No internet connection. Please check your internet connection and try again."
                        }
                        else -> "Error: ${e.message}"
                    }

                    showErrorDialog(errorMessage)
                }
            }
        }
    }
    private suspend fun updateSyncStatusAfterSuccess(syncedItems: List<LineTransaction>) {
        withContext(Dispatchers.IO) {
            try {
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@withContext

                // Mark synced items as synced (syncStatus = 1) in database
                syncedItems.forEach { item ->
                    viewModel.updateSyncStatus(journalId, item.itemId ?: "", 1)
                }

                // Update originalList to reflect sync status
                originalList = originalList.map { originalItem ->
                    val syncedItem = syncedItems.find { it.itemId == originalItem.itemId }
                    if (syncedItem != null) {
                        originalItem.copy(syncStatus = 1) // Mark as synced
                    } else {
                        originalItem // Keep unchanged
                    }
                }

                // Refresh adapter with updated sync status
                withContext(Dispatchers.Main) {
                    adapter.setItems(originalList)
                }

                Log.d(TAG, "Updated sync status for ${syncedItems.size} items")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating sync status after success", e)
            }
        }
    }
    private suspend fun updateSpecificItemsInDatabase(journalId: String, modifiedItems: List<LineTransaction>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating ${modifiedItems.size} specific items in database")

                // Update each item individually in the database
                modifiedItems.forEach { item ->
                    viewModel.updateSingleItem(journalId, item)
                }

                Log.d(TAG, "Successfully updated ${modifiedItems.size} specific items")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error updating specific items in database", e)
                false
            }
        }
    }

    /**
     * Compare two LineTransaction items to see if they're actually different
     */
    private fun areItemsEqual(item1: LineTransaction, item2: LineTransaction): Boolean {
        return item1.adjustment == item2.adjustment &&
                item1.receivedCount == item2.receivedCount &&
                item1.transferCount == item2.transferCount &&
                item1.wasteCount == item2.wasteCount &&
                item1.counted == item2.counted &&
                item1.wasteType == item2.wasteType
    }

    /**
     * Extension function for LineTransaction.hasAnyValue()
     */
    private fun LineTransaction.hasAnyValue(): Boolean {
        return (adjustment?.toDoubleOrNull() ?: 0.0) > 0 ||
                (receivedCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                (transferCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                (wasteCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                (counted?.toDoubleOrNull() ?: 0.0) > 0 ||
                (!wasteType.isNullOrBlank() && wasteType != "none" && wasteType != "Select type")
    }
    private fun refreshAdapterAfterSave() {
        lifecycleScope.launch {
            try {
                val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch
                val freshData = viewModel.getLocalLineDetails(journalId)

                freshData.onSuccess { transactions ->
                    withContext(Dispatchers.Main) {
                        originalList = transactions
                        adapter.setItems(transactions)
                        Log.d(TAG, "Adapter refreshed after save with ${transactions.size} items")
                    }
                }.onFailure { exception ->
                    Log.e(TAG, "Error refreshing adapter after save", exception)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in refreshAdapterAfterSave", e)
            }
        }
    }


    private fun showSyncProgressDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_sync_progress, null)

    progressDialog = AlertDialog.Builder(this, R.style.CustomDialogStyle)
        .setTitle("Syncing Changes")
        .setView(dialogView)
        .setCancelable(false)
        .create()

    progressDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
    progressDialog.show()

    // Initialize progress
    progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.apply {
        max = 100
        progress = 0
        isIndeterminate = false
    }
    progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = "0%"
    progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text = "Preparing..."
}

private fun updateSyncProgressDialog(message: String, current: Int, total: Int) {
    val percentage = if (total > 0) (current * 100) / total else 0

    progressDialog.findViewById<ProgressBar>(R.id.progressBar)?.apply {
        max = 100
        progress = percentage
        isIndeterminate = false
    }
    progressDialog.findViewById<TextView>(R.id.tvProgress)?.text = "$percentage%"
    progressDialog.findViewById<TextView>(R.id.tvCurrentItem)?.text = message
}

private fun dismissSyncProgressDialog() {
    if (::progressDialog.isInitialized && progressDialog.isShowing) {
        progressDialog.dismiss()
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

private fun applyTransactionFilter() {
    lifecycleScope.launch {
        try {
            val storeId = intent.getStringExtra(EXTRA_STORE_ID) ?: return@launch
            val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID) ?: return@launch

            Log.d("LineActivity", "Applying filter - StoreId: $storeId, JournalId: $journalId")
            Log.d("LineActivity", "Original list size: ${originalList.size}")

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
            viewModel.clearLocalData(journalId)
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

private fun setupPrinter() {
    printer = BluetoothPrinterHelper.getInstance()

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
        content.append(0x1B.toChar())
        content.append('!'.toChar())
        content.append(0x01.toChar())

        content.append(0x1B.toChar())
        content.append('3'.toChar())
        content.append(50.toChar())

        content.appendLine("==============================")
        content.appendLine("     BATCH COUNTING REPORT      ")
        content.appendLine("==============================")
        content.appendLine("Store: ${intent.getStringExtra(EXTRA_STORE_ID)}")
        content.appendLine("BATCH: BATCH$journalId")
        content.appendLine("Date: ${SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
        content.appendLine("------------------------------")

        val currentList = adapter.getAllItems()
        Log.d("PrintDetails", "Number of items: ${currentList.size}")

        currentList.filter { item ->
            val hasValues = (item.adjustment?.toDoubleOrNull() ?: 0.0) > 0 ||
                    (item.receivedCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                    (item.wasteCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                    (item.transferCount?.toDoubleOrNull() ?: 0.0) > 0 ||
                    (item.counted?.toDoubleOrNull() ?: 0.0) > 0

            Log.d("PrintDetails", "Item ${item.itemId}: hasValues=$hasValues, " +
                    "adj=${item.adjustment}, rcv=${item.receivedCount}, " +
                    "waste=${item.wasteCount}, trans=${item.transferCount}, " +
                    "count=${item.counted}")

            hasValues
        }.forEach { item ->
            val itemInfo = "${item.itemId} - ${item.itemName}"
            content.appendLine(itemInfo)

            val adjustment = item.adjustment?.toDoubleOrNull() ?: 0.0
            val receivedCount = item.receivedCount?.toDoubleOrNull() ?: 0.0
            val wasteCount = item.wasteCount?.toDoubleOrNull() ?: 0.0
            val transferCount = item.transferCount?.toDoubleOrNull() ?: 0.0
            val counted = item.counted?.toDoubleOrNull() ?: 0.0

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
            val fileName = "BATCH_COUNTING_${System.currentTimeMillis()}.csv"
            val file = File(getExternalFilesDir(null), fileName)

            file.bufferedWriter().use { writer ->
                writer.write("Item ID,Item Name,Category,Order,Received,Variance,Transfer,Waste Count,Waste Type,Actual Count\n")

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