package com.example.possystembw.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.possystembw.MainActivity
import com.example.possystembw.R
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.database.User
import com.example.possystembw.ui.ViewModel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

class LoginActivity : AppCompatActivity() {
    private lateinit var viewModel: LoginViewModel
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(LoginViewModel::class.java)

        // Initialize views
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)

        addSampleUserIfFirstRun()

        // Set up login button click listener
        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passwordInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Log.d("LoginActivity", "Attempting login with email: $email")  // Log email before login
                viewModel.login(email, password)
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            }
        }

        // Observe login result
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                // Set the current user in SessionManager
                SessionManager.setCurrentUser(user)

                // Show success message
                Toast.makeText(this, "Login successful for ${user.name}", Toast.LENGTH_SHORT).show()

                // Log the successful login with user details
                Log.d("LoginActivity", "Login successful - User: ${user.name}, Store: ${user.storeid}")

                // Navigate to MainActivity
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }.onFailure { error ->
                // Clear any existing session on login failure
                SessionManager.clearCurrentUser()

                // Show error message
                Toast.makeText(this, "Login failed: ${error.message}", Toast.LENGTH_SHORT).show()

                // Log the error
                Log.e("LoginActivity", "Login failed", error)
            }
        }

        // Fetch users from API when the activity starts
        Log.d("LoginActivity", "Fetching users from API...")
        viewModel.fetchUsers()
    }

    override fun onStart() {
        super.onStart()
        // Clear any existing session when returning to login screen
        SessionManager.clearCurrentUser()
    }

    // Add this method to check if user is already logged in
    private fun checkExistingSession() {
        if (SessionManager.getCurrentUser() != null) {
            // User is already logged in, redirect to MainActivity
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun addSampleUserIfFirstRun() {
        val prefs = getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("isFirstRun", true)

        if (isFirstRun) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val userDao = AppDatabase.getDatabase(applicationContext).userDao()
                    val currentDate = Date()
                    val sampleUser = User(
                        id = 0,
                        name = "Ray Santos",
                        email = "ray",
                        storeid = "1",
                        email_verified_at = null,
                        password = "$2y$12$3DjPPc0yzbdTwI5HGzC9Fe29Nv6o4oN/lqcMznk/JXwHPL.V5SH5a", // Bcrypt hash for "welcome"
                        two_factor_secret = null,
                        two_factor_recovery_codes = null,
                        two_factor_confirmed_at = null,
                        remember_token = null,
                        current_team_id = null,
                        profile_photo_path = null,
                        role = "user",
                        created_at = currentDate,
                        updated_at = currentDate
                    )

                    try {
                        userDao.insertUser(sampleUser)
                        Log.d("LoginActivity", "Sample user added: ${sampleUser.email}")
                    } catch (e: Exception) {
                        Log.e("LoginActivity", "Error inserting user: ${e.message}")
                    }
                }
            }
            prefs.edit().putBoolean("isFirstRun", false).apply()
        }
    }
}
