package com.example.possystembw.ui.ViewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.possystembw.RetrofitClient
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.UserRepository
import com.example.possystembw.database.User
import kotlinx.coroutines.launch
import at.favre.lib.crypto.bcrypt.BCrypt

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository
    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    private val _fetchUsersResult = MutableLiveData<Result<List<User>>>()
    val fetchUsersResult: LiveData<Result<List<User>>> = _fetchUsersResult

    init {
        val userDao = AppDatabase.getDatabase(application).userDao()
        val userApi = RetrofitClient.userApi
        userRepository = UserRepository(userDao, userApi)
    }

    fun fetchUsers() {
        viewModelScope.launch {
            val result = userRepository.fetchAndStoreUsers()
            result.onSuccess { users ->
                Log.d("LoginViewModel", "Successfully fetched ${users.size} users")
                users.forEach { user ->
                    Log.d("LoginViewModel", "User: ${user.name}, Email: ${user.email}, Role: ${user.role}")
                }
            }.onFailure { error ->
                Log.e("LoginViewModel", "Failed to fetch users", error)
            }
            _fetchUsersResult.value = result
        }
    }


    fun login(email: String, password: String) {
        viewModelScope.launch {
            val user = userRepository.getLocalUserByEmail(email)
            if (user != null) {
                val result = BCrypt.verifyer().verify(password.toCharArray(), user.password)
                if (result.verified) {
                    _loginResult.value = Result.success(user)
                } else {
                    _loginResult.value = Result.failure(Exception("Invalid credentials"))
                }
            } else {
                _loginResult.value = Result.failure(Exception("User not found"))
            }
        }
    }
}