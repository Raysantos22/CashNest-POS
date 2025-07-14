package com.example.possystembw.ui

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.R
import com.example.possystembw.adapter.WindowVisibilityAdapter
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.WindowTableRepository
import com.example.possystembw.data.WindowRepository
import com.example.possystembw.database.Window
import com.example.possystembw.database.WindowTable
import com.example.possystembw.ui.ViewModel.WindowTableViewModel
import com.example.possystembw.ui.ViewModel.WindowTableViewModelFactory
import com.example.possystembw.ui.ViewModel.WindowViewModel
import com.example.possystembw.ui.ViewModel.WindowViewModelFactory
import com.example.possystembw.RetrofitClient
import com.example.possystembw.ui.ViewModel.WindowVisibilityViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WindowVisibilityActivity : AppCompatActivity() {
    private lateinit var windowTableViewModel: WindowTableViewModel
    private lateinit var windowViewModel: WindowViewModel
    private lateinit var windowVisibilityViewModel: WindowVisibilityViewModel
    private lateinit var adapter: WindowVisibilityAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var cardToggleAllOn: CardView
    private lateinit var cardToggleAllOff: CardView
    private lateinit var typeButtonsContainer: LinearLayout

    private var selectedType: String? = null
    private var allWindowsWithVisibility: List<WindowWithVisibility> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_window_visibility)

        setupToolbar()
        initializeViews()
        initializeViewModels()
        setupRecyclerView()
        setupToggleButtons()
        observeData()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Show/Hide Windows"
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewWindows)
        searchView = findViewById(R.id.searchView)
        cardToggleAllOn = findViewById(R.id.cardToggleAllOn)
        cardToggleAllOff = findViewById(R.id.cardToggleAllOff)
        typeButtonsContainer = findViewById(R.id.typeButtonsContainer)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterWindows()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterWindows()
                return true
            }
        })
    }

    private fun initializeViewModels() {
        val database = AppDatabase.getDatabase(application)
        val windowTableRepository = WindowTableRepository(
            database.windowTableDao(),
            RetrofitClient.windowTableApi
        )
        val windowRepository = WindowRepository(
            database.windowDao(),
            RetrofitClient.apiService
        )

        windowTableViewModel = ViewModelProvider(
            this,
            WindowTableViewModelFactory(windowTableRepository)
        )[WindowTableViewModel::class.java]

        windowViewModel = ViewModelProvider(
            this,
            WindowViewModelFactory(application)
        )[WindowViewModel::class.java]

        windowVisibilityViewModel = ViewModelProvider(
            this,
            WindowVisibilityViewModel.WindowVisibilityViewModelFactory(application)
        )[WindowVisibilityViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = WindowVisibilityAdapter { window, isEnabled ->
            toggleWindowVisibility(window, isEnabled)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val spacing = resources.getDimensionPixelSize(R.dimen.grid_spacing)
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                outRect.bottom = spacing
            }
        })
    }

    private fun setupToggleButtons() {
        cardToggleAllOn.setOnClickListener {
            toggleAllWindows(true)
        }

        cardToggleAllOff.setOnClickListener {
            toggleAllWindows(false)
        }

        setupTypeButtons()
    }

    private fun setupTypeButtons() {
        val types = listOf("All", "WindowTable", "Window")
        
        types.forEach { type ->
            val button = createTypeButton(type)
            typeButtonsContainer.addView(button)
        }
    }

    private fun createTypeButton(type: String): android.widget.Button {
        val button = android.widget.Button(this).apply {
            text = type
            textSize = 14f
            setPadding(32, 16, 32, 16)

            updateTypeButtonAppearance(this, type == selectedType)

            setOnClickListener {
                selectType(type)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
            }
            layoutParams = params
        }

        return button
    }

    private fun updateTypeButtonAppearance(button: android.widget.Button, isSelected: Boolean) {
        if (isSelected) {
            button.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            button.elevation = 8f
        } else {
            button.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            button.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary))
            button.background = ContextCompat.getDrawable(this, R.drawable.category_button_unselected)

            button.elevation = 2f
        }
    }

    private fun selectType(type: String) {
        selectedType = if (selectedType == type) null else type

        Log.d("WindowVisibility", "Selected type: ${selectedType ?: "None"}")

        for (i in 0 until typeButtonsContainer.childCount) {
            val button = typeButtonsContainer.getChildAt(i) as android.widget.Button
            val buttonType = when (i) {
                0 -> "All"
                1 -> "WindowTable"
                2 -> "Window"
                else -> ""
            }
            updateTypeButtonAppearance(button, buttonType == selectedType)
        }

        filterWindows()
    }

    private fun updateToggleButtonStates() {
        val currentList = adapter.currentList
        val visibleCount = currentList.count { it.isVisible }
        val totalCount = currentList.size

        if (visibleCount == totalCount) {
            cardToggleAllOn.alpha = 0.5f
            cardToggleAllOff.alpha = 1.0f
        } else if (visibleCount == 0) {
            cardToggleAllOn.alpha = 1.0f
            cardToggleAllOff.alpha = 0.5f
        } else {
            cardToggleAllOn.alpha = 1.0f
            cardToggleAllOff.alpha = 1.0f
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            combine(
                windowTableViewModel.allWindowTables,
                windowViewModel.allWindows,
                windowVisibilityViewModel.getHiddenWindows()
            ) { windowTables, windows, hiddenWindows ->
                val hiddenWindowIds = hiddenWindows.map { it.windowId }.toSet()
                val hiddenWindowTableIds = hiddenWindows.map { it.windowTableId }.toSet()

                val windowTableItems = windowTables.map { windowTable ->
                    WindowWithVisibility(
                        id = windowTable.id,
                        description = windowTable.description,
                        type = "WindowTable",
                        isVisible = windowTable.id !in hiddenWindowTableIds
                    )
                }

                val windowItems = windows.map { window ->
                    WindowWithVisibility(
                        id = window.id,
                        description = window.description,
                        type = "Window",
                        isVisible = window.id !in hiddenWindowIds
                    )
                }

                windowTableItems + windowItems
            }.collect { windowsWithVisibility ->
                allWindowsWithVisibility = windowsWithVisibility
                Log.d("WindowVisibility", "Updated windows with visibility: ${allWindowsWithVisibility.size}")
                filterWindows()
            }
        }
    }

    private fun filterWindows() {
        val query = searchView.query?.toString()
        Log.d("WindowVisibility", "Filtering windows - Query: '$query', Selected Type: '${selectedType}'")

        val filteredList = allWindowsWithVisibility.filter { windowWithVisibility ->
            val matchesSearch = query.isNullOrBlank() ||
                    windowWithVisibility.description.contains(query, ignoreCase = true)

            val matchesType = selectedType?.let { type ->
                when (type) {
                    "All" -> true
                    else -> windowWithVisibility.type == type
                }
            } ?: true

            matchesSearch && matchesType
        }

        Log.d("WindowVisibility", "Filtered ${filteredList.size} windows from ${allWindowsWithVisibility.size} total")
        adapter.submitList(filteredList)
        updateToggleButtonStates()
        updateResultsInfo(filteredList.size, allWindowsWithVisibility.size)
    }

    private fun updateResultsInfo(filtered: Int, total: Int) {
        supportActionBar?.subtitle = if (filtered == total) {
            "$total windows"
        } else {
            "$filtered of $total windows"
        }
    }

    private fun toggleWindowVisibility(window: WindowWithVisibility, isEnabled: Boolean) {
        lifecycleScope.launch {
            if (window.type == "WindowTable") {
                if (isEnabled) {
                    windowVisibilityViewModel.showWindowTable(window.id)
                } else {
                    windowVisibilityViewModel.hideWindowTable(window.id)
                }
            } else {
                if (isEnabled) {
                    windowVisibilityViewModel.showWindow(window.id)
                } else {
                    windowVisibilityViewModel.hideWindow(window.id)
                }
            }
        }
    }

    private fun toggleAllWindows(showAll: Boolean) {
        val currentList = adapter.currentList

        lifecycleScope.launch {
            currentList.forEach { windowWithVisibility ->
                if (windowWithVisibility.type == "WindowTable") {
                    if (showAll) {
                        windowVisibilityViewModel.showWindowTable(windowWithVisibility.id)
                    } else {
                        windowVisibilityViewModel.hideWindowTable(windowWithVisibility.id)
                    }
                } else {
                    if (showAll) {
                        windowVisibilityViewModel.showWindow(windowWithVisibility.id)
                    } else {
                        windowVisibilityViewModel.hideWindow(windowWithVisibility.id)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                val message = if (showAll) "All windows shown" else "All windows hidden"
                Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }
}

data class WindowWithVisibility(
    val id: Int,
    val description: String,
    val type: String,
    val isVisible: Boolean
)