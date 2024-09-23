package com.example.possystembw.data

import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import com.example.possystembw.DAO.LoginRequest
import com.example.possystembw.DAO.UserApi
import com.example.possystembw.DAO.UserDao
import com.example.possystembw.database.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Date

class UserRepository(private val userDao: UserDao, private val userApi: UserApi) {
    private val TAG = "UserRepository"

    suspend fun fetchAndStoreUsers(): Result<List<User>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching users from API")
                val response: Response<List<User>> = userApi.getAllUsers()
                Log.d(TAG, "API response received. Is successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val users = response.body()
                    if (users != null) {
                        Log.d(TAG, "Received ${users.size} users")
                        if (users.isEmpty()) {
                            Log.w(TAG, "API returned an empty list of users")
                        } else {
                            Log.d(TAG, "First user: ${users.first()}")
                        }
                        val currentTime = Date()
                        val usersWithTimestamps = users.map { user ->
                            user.copy(
                                created_at = user.created_at ?: currentTime,
                                updated_at = currentTime
                            )
                        }
                        userDao.upsertAll(usersWithTimestamps)
                        Log.d(TAG, "Users upserted into local database")
                        Result.success(usersWithTimestamps)
                    } else {
                        Log.e(TAG, "Fetching users failed: User data is null")
                        Result.failure(Exception("Fetching users failed: User data is null"))
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error details"
                    Log.e(TAG, "API error: ${response.code()} ${response.message()}, Error body: $errorBody")
                    Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while fetching users", e)
                Result.failure(e)
            }
        }
    }

    suspend fun getLocalUser(email: String, password: String): User? {
        return userDao.getUser(email, password)
    }

    suspend fun getLocalUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

}