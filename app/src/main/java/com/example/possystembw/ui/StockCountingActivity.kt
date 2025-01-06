package com.example.possystembw.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.possystembw.MainActivity
import com.example.possystembw.R
import com.example.possystembw.adapter.StockCountingAdapter
import com.example.possystembw.database.StockCountingEntity
import com.example.possystembw.ui.ViewModel.StockCountingViewModel
import com.example.possystembw.ui.ViewModel.StockCountingViewModelFactory
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StockCountingActivity : AppCompatActivity() {
    private lateinit var fetchButton: ImageButton  // Changed from Button to ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var viewModel: StockCountingViewModel
    private lateinit var adapter: StockCountingAdapter
    private var originalList = listOf<StockCountingEntity>()
    private lateinit var timeTextView: TextView
    private lateinit var storeIdTextView: TextView
    private val timeHandler = Handler(Looper.getMainLooper())
    private lateinit var loadingProgressBar: ProgressBar

    private lateinit var sidebarLayout: ConstraintLayout
    private lateinit var toggleButton: ImageButton
    private lateinit var buttonContainer: LinearLayout
    private lateinit var ecposTitle: TextView
    private var isSidebarExpanded = true
    private var isAnimating = false

    private val timeRunnable = object : Runnable {
        override fun run() {
            updateTime()
            timeHandler.postDelayed(this, 1000)
        }
    }
    companion object {
        private const val LINE_ACTIVITY_REQUEST_CODE = 100
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock_counting)
        loadingProgressBar = findViewById(R.id.loadingStockCounting)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this,
            StockCountingViewModelFactory(application)
        )[StockCountingViewModel::class.java]

        // Initialize views
        fetchButton = findViewById(R.id.btnFetchStockCounting)
        backButton = findViewById(R.id.btnBack)
        searchEditText = findViewById(R.id.etSearch)
        recyclerView = findViewById(R.id.rvStockCounting)
        timeTextView = findViewById(R.id.tvTime)
        storeIdTextView = findViewById(R.id.tvStoreId)

        initializeSidebarComponents()
        setupSidebar()

        // Setup RecyclerView
        adapter = StockCountingAdapter { storeId, journalId ->
            val intent = LineActivity.createIntent(this, storeId, journalId)
            startActivityForResult(intent, LINE_ACTIVITY_REQUEST_CODE)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@StockCountingActivity)
            adapter = this@StockCountingActivity.adapter
        }

        // Get store ID from SessionManager
        val currentUser = SessionManager.getCurrentUser()
        val storeId = currentUser?.storeid

        if (storeId.isNullOrEmpty()) {
            Toast.makeText(this, "Store ID not found. Please login first.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        storeIdTextView.text = storeId

        // Start time updates
        timeHandler.post(timeRunnable)
        // Setup click listeners
        fetchButton.setOnClickListener {
            viewModel.fetchStockCounting(storeId)
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // Setup search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterList(s.toString())
            }
        })

        // Observe the results
        viewModel.stockCountingResult.observe(this) { result ->
            loadingProgressBar.visibility = View.GONE

            result?.onSuccess { stockCounting ->
                adapter.updateItems(stockCounting)
            }?.onFailure { exception ->
                Toast.makeText(this, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }// In both activities
        viewModel.isLoading.observe(this) { isLoading ->
            loadingProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Show loading when fetching
        fetchButton.setOnClickListener {
            loadingProgressBar.visibility = View.VISIBLE
            viewModel.fetchStockCounting(storeId)
        }

        // Show loading on initial load
        loadingProgressBar.visibility = View.VISIBLE
        viewModel.fetchStockCounting(storeId)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LINE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh the stock counting data
            val currentUser = SessionManager.getCurrentUser()
            val storeId = currentUser?.storeid
            if (!storeId.isNullOrEmpty()) {
                loadingProgressBar.visibility = View.VISIBLE
                viewModel.fetchStockCounting(storeId)
            }
        }
    }
    private fun updateTime() {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        timeTextView.text = sdf.format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        timeHandler.removeCallbacks(timeRunnable)
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
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/dashboard")
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.button3).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("web_url", "https://eljin.org/order")
            startActivity(intent)
        }

//        findViewById<ImageButton>(R.id.button4).setOnClickListener {
//            val intent = Intent(this, MainActivity::class.java)
//            intent.putExtra("web_url", "https://eljin.org/StockCounting")
//            startActivity(intent)
//        }
        findViewById<ImageButton>(R.id.stockcounting).setOnClickListener {
            val intent = Intent (this, StockCountingActivity::class.java)
            startActivity(intent)

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
        }
        findViewById<ImageButton>(R.id.printerSettingsButton).setOnClickListener {
            val intent = Intent(this, PrinterSettingsActivity::class.java)
            startActivity(intent)
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
    private fun filterList(query: String) {
        if (query.isEmpty()) {
            adapter.updateItems(originalList)
        } else {
            val filteredList = originalList.filter {
                it.description.contains(query, ignoreCase = true)
            }
            adapter.updateItems(filteredList)
        }
    }
}