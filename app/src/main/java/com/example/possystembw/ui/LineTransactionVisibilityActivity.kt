//package com.example.possystembw.ui
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Rect
//import android.os.Bundle
//import android.util.Log
//import android.view.View
//import android.widget.Button
//import android.widget.ProgressBar
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.appcompat.widget.SearchView
//import androidx.appcompat.widget.Toolbar
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.possystembw.DAO.LineDetailsApi
//import com.example.possystembw.DAO.LineTransaction
//import com.example.possystembw.DAO.StockCountingApi
//import com.example.possystembw.R
//import com.example.possystembw.adapter.LineTransactionVisibilityAdapter
//import com.example.possystembw.data.AppDatabase
//import com.example.possystembw.data.LineRepository
//import com.example.possystembw.data.LineTransactionWithVisibility
//import com.example.possystembw.data.LineTransactionVisibilityRepository
//import com.example.possystembw.ui.ViewModel.LineTransactionVisibilityViewModel
//import com.google.android.material.snackbar.Snackbar
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import okhttp3.OkHttpClient
//import okhttp3.logging.HttpLoggingInterceptor
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import java.util.concurrent.TimeUnit
//
//class LineTransactionVisibilityActivity : AppCompatActivity() {
//    private lateinit var viewModel: LineTransactionVisibilityViewModel
//    private lateinit var adapter: LineTransactionVisibilityAdapter
//    private lateinit var recyclerView: RecyclerView
//    private lateinit var progressBar: ProgressBar
//    private lateinit var tvError: TextView
//
//    companion object {
//        const val EXTRA_JOURNAL_ID = "extra_journal_id"
//
//        fun createIntent(context: Context, journalId: String): Intent {
//            return Intent(context, LineTransactionVisibilityActivity::class.java).apply {
//                putExtra(EXTRA_JOURNAL_ID, journalId)
//            }
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_line_transaction_visibility)
//
//        setupToolbar()
//        initializeViews()
//        setupRecyclerView()
//        setupViewModel()
//        loadData()
//    }
//
//    private fun setupToolbar() {
//        val toolbar = findViewById<Toolbar>(R.id.toolbar)
//        setSupportActionBar(toolbar)
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.title = "Manage Line Transaction Visibility"
//
//        toolbar.setNavigationOnClickListener {
//            onBackPressed()
//        }
//    }
//
//    private fun initializeViews() {
//        recyclerView = findViewById(R.id.rvLineTransactions)
//        progressBar = findViewById(R.id.progressBar)
//        tvError = findViewById(R.id.tvError)
//    }
//
//    private fun setupRecyclerView() {
//        adapter = LineTransactionVisibilityAdapter { transaction, isVisible ->
//            transaction.itemId?.let { itemId ->
//                if (isVisible) {
//                    viewModel.showLineTransaction(itemId)
//                } else {
//                    viewModel.hideLineTransaction(itemId)
//                }
//            }
//        }
//
//        recyclerView.layoutManager = LinearLayoutManager(this)
//        recyclerView.adapter = adapter
//    }
//
//    private fun setupViewModel() {
//        viewModel = ViewModelProvider(
//            this,
//            LineTransactionVisibilityViewModel.Factory(application)
//        )[LineTransactionVisibilityViewModel::class.java]
//
//        // Observe line transactions with visibility
//        viewModel.lineTransactionsWithVisibility.observe(this) { transactions ->
//            adapter.submitList(transactions)
//
//            // Show/hide empty state
//            if (transactions.isEmpty()) {
//                tvError.text = "No line transactions found"
//                tvError.visibility = View.VISIBLE
//                recyclerView.visibility = View.GONE
//            } else {
//                tvError.visibility = View.GONE
//                recyclerView.visibility = View.VISIBLE
//            }
//        }
//
//        // Observe loading state
//        viewModel.isLoading.observe(this) { isLoading ->
//            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
//        }
//
//        // Observe errors
//        viewModel.error.observe(this) { error ->
//            if (error != null) {
//                tvError.text = error
//                tvError.visibility = View.VISIBLE
//                recyclerView.visibility = View.GONE
//
//                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun loadData() {
//        val journalId = intent.getStringExtra(EXTRA_JOURNAL_ID)
//
//        if (journalId.isNullOrEmpty()) {
//            tvError.text = "Journal ID is required"
//            tvError.visibility = View.VISIBLE
//            recyclerView.visibility = View.GONE
//            return
//        }
//
//        viewModel.loadLineTransactionsWithVisibility(journalId)
//    }
//}
//
