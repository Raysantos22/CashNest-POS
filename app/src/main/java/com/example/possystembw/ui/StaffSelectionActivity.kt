//package com.example.possystembw.ui
//
//import android.content.Intent
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.lifecycleScope
//import androidx.recyclerview.widget.DividerItemDecoration
//import androidx.recyclerview.widget.LinearLayoutManager
//import com.example.possystembw.DAO.AttendanceDao
//import com.example.possystembw.DAO.StaffDao
//import com.example.possystembw.Repository.StaffRepository
//import com.example.possystembw.RetrofitClient
//import com.example.possystembw.adapter.StaffAttendanceAdapter
//import com.example.possystembw.data.AppDatabase
//import com.example.possystembw.database.StaffEntity
//import com.example.possystembw.databinding.ActivityStaffSelectionBinding
//import com.example.possystembw.ui.ViewModel.StaffViewModel
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//
//class StaffSelectionActivity : AppCompatActivity() {
//    private lateinit var binding: ActivityStaffSelectionBinding
//    private lateinit var viewModel: StaffViewModel
//    private lateinit var adapter: StaffAttendanceAdapter
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        binding = ActivityStaffSelectionBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        setupViewModel()
//        setupRecyclerView()
//        observeData()
//        loadStaffData()
//    }
//
//    private fun setupViewModel() {
//        val repository = StaffRepository(
//            RetrofitClient.staffApi,  // Changed from RetrofitInstance to RetrofitClient
//            AppDatabase.getDatabase(application).staffDao()
//        )
//        viewModel = ViewModelProvider(
//            this,
//            StaffViewModel.StaffViewModelFactory(repository)
//        )[StaffViewModel::class.java]
//    }
//
//    private fun setupRecyclerView() {
//        adapter = StaffAttendanceAdapter { staff ->
//            StaffManager.setCurrentStaff(staff)
//            startActivity(Intent(this, AttendanceActivity::class.java))
//        }
//
//        binding.recyclerView.apply {
//            layoutManager = LinearLayoutManager(this@StaffSelectionActivity)
//            adapter = this@StaffSelectionActivity.adapter
//        }
//    }
//
//    private fun observeData() {
//        lifecycleScope.launch {
//            viewModel.staffData.collect { staffList ->
//                adapter.submitList(staffList)
//            }
//        }
//
//        viewModel.error.observe(this) { error ->
//            showToast(error)
//        }
//    }
//
//    private fun loadStaffData() {
//        SessionManager.getCurrentUser()?.storeid?.let { storeId ->
//            viewModel.refreshStaffData(storeId)
//        }
//    }
//
//    private fun showToast(message: String) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//    }
//}