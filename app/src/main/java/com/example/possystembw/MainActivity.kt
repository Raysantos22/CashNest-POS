package com.example.possystembw

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.adapter.WindowAdapter
import com.example.possystembw.database.Window
import com.example.possystembw.ui.ViewModel.WindowViewModel
import com.example.possystembw.ui.Window1
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.possystembw.adapter.WindowTableAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.WindowTableRepository
import com.example.possystembw.database.WindowTable
import com.example.possystembw.ui.ViewModel.WindowTableViewModel
import com.example.possystembw.ui.ViewModel.WindowTableViewModelFactory
import com.example.possystembw.ui.ViewModel.WindowViewModelFactory
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var windowViewModel: WindowViewModel
    private lateinit var windowTableViewModel: WindowTableViewModel
    private lateinit var windowAdapter: WindowAdapter
    private lateinit var windowTableAdapter: WindowTableAdapter
    private lateinit var refreshJob: Job
    private lateinit var refreshButton: Button
    private lateinit var refreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("MainActivity", "onCreate: Initializing ViewModels and Adapters")

        // Initialize ViewModels and Adapters
        val windowFactory = WindowViewModelFactory(application)
        windowViewModel = ViewModelProvider(this, windowFactory).get(WindowViewModel::class.java)

        // Initialize WindowTableViewModel with proper dependencies
        val windowTableDao = AppDatabase.getDatabase(application).windowTableDao()
        val windowTableApi = RetrofitClient.windowTableApi
        val windowTableRepository = WindowTableRepository(windowTableDao, windowTableApi)
        val windowTableFactory = WindowTableViewModelFactory(windowTableRepository)
        windowTableViewModel = ViewModelProvider(this, windowTableFactory).get(WindowTableViewModel::class.java)

        windowAdapter = WindowAdapter { window -> openWindow(window) }
        windowTableAdapter = WindowTableAdapter { windowTable -> showAlignedWindows(windowTable) }

        // Setup UI components
        setupRefreshButton()
        setupSwipeRefreshLayout()
        setupWindowRecyclerView()
        setupWindowTableRecyclerView()

        // Setup observers
        setupObservers()

        // Start periodic refresh
        startPeriodicRefresh()
    }

    private fun setupRefreshButton() {
        refreshButton = findViewById(R.id.refreshButton)
        refreshButton.setOnClickListener {
            performManualRefresh()
        }
    }

    private fun setupSwipeRefreshLayout() {
        refreshLayout = findViewById(R.id.swipeRefreshLayout)
        refreshLayout.setOnRefreshListener {
            performManualRefresh()
        }
    }

    private fun performManualRefresh() {
        lifecycleScope.launch {
            try {
                refreshButton.isEnabled = false
                refreshLayout.isRefreshing = true

                // Attempt to refresh from network
                try {
                    windowViewModel.refreshWindows()
                    windowTableViewModel.refreshWindowTables()
                    showToast("Data refreshed successfully")
                } catch (e: Exception) {
                    Log.w("MainActivity", "Network refresh failed, loading from local database", e)
                    // Load from local database
                    windowViewModel.loadFromLocalDatabase()
                    windowTableViewModel.loadFromLocalDatabase()
                    showToast("Offline mode: Loaded from local database")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error during manual refresh", e)
                showToast("Error refreshing data: ${e.message}")
            } finally {
                refreshButton.isEnabled = true
                refreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            windowViewModel.allWindows.collect { windows ->
                Log.d("MainActivity", "Received windows: ${windows.joinToString { it.description }}")
                windowAdapter.submitList(windows)
            }
        }

        lifecycleScope.launch {
            windowTableViewModel.allWindowTables.collect { windowTables ->
                Log.d("MainActivity", "Received window tables: ${windowTables.joinToString { it.description }}")
                windowTableAdapter.submitList(windowTables)
            }
        }

        lifecycleScope.launch {
            windowViewModel.alignedWindows.collect { alignedWindows ->
                if (alignedWindows.isNotEmpty()) {
                    Log.d("MainActivity", "Received aligned windows: ${alignedWindows.joinToString { it.description }}")
                    windowAdapter.submitList(alignedWindows)
                }
            }
        }

        windowViewModel.errorState.observe(this) { error ->
            error?.let {
                showToast(it)
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupWindowRecyclerView() {
        val windowRecyclerView: RecyclerView = findViewById(R.id.windowRecyclerView)
        windowRecyclerView.adapter = windowAdapter

        // Calculate span count based on screen width
        val displayMetrics = resources.displayMetrics
        val screenWidthDp = displayMetrics.widthPixels / displayMetrics.density
        val spanCount = (screenWidthDp / 400).toInt().coerceAtLeast(2) // minimum 2 columns

        windowRecyclerView.layoutManager = GridLayoutManager(this, spanCount)
    }

    private fun setupWindowTableRecyclerView() {
        Log.d("MainActivity", "Setting up window table RecyclerView")
        val windowTableRecyclerView: RecyclerView = findViewById(R.id.windowTableRecyclerView)
        windowTableRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false) // Horizontal layout
        windowTableRecyclerView.adapter = windowTableAdapter
        Log.d("MainActivity", "Window table RecyclerView set up successfully")
    }

    private fun startPeriodicRefresh() {
        Log.d("MainActivity", "Starting periodic refresh")
        refreshJob = lifecycleScope.launch {
            while (isActive) {
                try {
                    Log.d("MainActivity", "Refreshing windows and window tables")
                    windowViewModel.refreshWindows()
                    windowTableViewModel.refreshWindowTables()
                    Log.d("MainActivity", "Windows and window tables refreshed successfully")
                    delay(60000) // Refresh every 60 seconds
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during periodic refresh", e)
                    Toast.makeText(this@MainActivity, "Error refreshing data: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun openWindow(window: Window) {
        Log.d("MainActivity", "Opening window with ID: ${window.id}")
        val intent = Intent(this, Window1::class.java).apply {
            putExtra("WINDOW_ID", window.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    private fun showAlignedWindows(windowTable: WindowTable) {
        Log.d("MainActivity", "Aligning windows with table ID: ${windowTable.id}")
        windowViewModel.alignWindowsWithTable(windowTable.id)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy: Cancelling refresh job")
        refreshJob.cancel() // Cancel the refresh job when the activity is destroyed
    }
}