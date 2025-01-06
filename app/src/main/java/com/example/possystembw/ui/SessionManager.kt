package com.example.possystembw.ui

import android.content.Context
import android.content.SharedPreferences
import com.example.possystembw.DAO.NumberSequenceValue
import com.example.possystembw.database.User
import com.google.gson.Gson

//object SessionManager {
//    private var currentUser: User? = null
//
//    fun setCurrentUser(user: User) {
//        currentUser = user
//    }
//
//    fun getCurrentUser(): User? {
//        return currentUser
//    }
//
//    fun clearCurrentUser() {
//        currentUser = null
//    }
//}

/*object SessionManager {
    private const val PREF_NAME = "AppSession"
    private const val KEY_USER = "user"
    private const val KEY_NUMBER_SEQUENCE = "number_sequence"
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrentUser(user: User) {
        if (user.storeid.isNullOrEmpty()) {
            throw IllegalArgumentException("Cannot set user without store ID")
        }
        val userJson = gson.toJson(user)
        prefs.edit().putString(KEY_USER, userJson).apply()
    }

    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    fun clearCurrentUser() {
        prefs.edit().remove(KEY_USER).apply()
    }


    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
    fun setCurrentNumberSequence(numberSequence: NumberSequenceValue) {
        val numberSequenceJson = gson.toJson(numberSequence)
        prefs.edit().putString(KEY_NUMBER_SEQUENCE, numberSequenceJson).apply()
    }


    fun getCurrentNumberSequence(): NumberSequenceValue? {
        val numberSequenceJson = prefs.getString(KEY_NUMBER_SEQUENCE, null)
        return if (numberSequenceJson != null) {
            try {
                gson.fromJson(numberSequenceJson, NumberSequenceValue::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
}*/
//object SessionManager {
//    private const val PREF_NAME = "AppSession"
//    private const val KEY_USER = "user"
//    private const val KEY_NUMBER_SEQUENCE = "number_sequence"
//    private const val KEY_WEB_COOKIES = "web_cookies"
//    private lateinit var prefs: SharedPreferences
//    private val gson = Gson()
//
//    fun init(context: Context) {
//        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//    }
//
//    fun setCurrentUser(user: User) {
//        if (user.storeid.isNullOrEmpty()) {
//            throw IllegalArgumentException("Cannot set user without store ID")
//        }
//        val userJson = gson.toJson(user)
//        prefs.edit().putString(KEY_USER, userJson).apply()
//    }
//
//    fun getCurrentUser(): User? {
//        val userJson = prefs.getString(KEY_USER, null)
//        return if (userJson != null) {
//            try {
//                gson.fromJson(userJson, User::class.java)
//            } catch (e: Exception) {
//                null
//            }
//        } else null
//    }
//
////    fun setWebSessionCookies(cookies: String) {
////        prefs.edit().putString(KEY_WEB_COOKIES, cookies).apply()
////    }
////
////    fun getWebSessionCookies(): String? {
////        return prefs.getString(KEY_WEB_COOKIES, null)
////    }
//
//    fun clearCurrentUser() {
//        prefs.edit()
//            .remove(KEY_USER)
//            .remove(KEY_WEB_COOKIES)
//            .apply()
//    }
//
//    fun isLoggedIn(): Boolean {
//        return getCurrentUser() != null
//    }
//
//    fun isWebSessionActive(): Boolean {
//        return getWebSessionCookies() != null
//    }
//
//    fun setCurrentNumberSequence(numberSequence: NumberSequenceValue) {
//        val numberSequenceJson = gson.toJson(numberSequence)
//        prefs.edit().putString(KEY_NUMBER_SEQUENCE, numberSequenceJson).apply()
//    }
////    fun getWebSessionCookies(): String? {
////        return prefs.getString(KEY_WEB_COOKIES, null)
////    }
////
////    fun setWebSessionCookies(cookies: String?) {
////        cookies?.let {
////            prefs.edit().putString("web_session_cookies", it).apply()
////        }
////    }
//fun setWebSessionCookies(cookies: String?) {
//    cookies?.let {
//        prefs.edit().putString(KEY_WEB_COOKIES, it).apply()
//    }
//}
//
//    fun getWebSessionCookies(): String? {
//        return prefs.getString(KEY_WEB_COOKIES, null)
//    }
//
//
//    fun getCurrentNumberSequence(): NumberSequenceValue? {
//        val numberSequenceJson = prefs.getString(KEY_NUMBER_SEQUENCE, null)
//        return if (numberSequenceJson != null) {
//            try {
//                gson.fromJson(numberSequenceJson, NumberSequenceValue::class.java)
//            } catch (e: Exception) {
//                null
//            }
//        } else null
//    }
//}
//object SessionManager {
//    private const val PREF_NAME = "AppSession"
//    private const val KEY_USER = "user"
//    private const val KEY_NUMBER_SEQUENCE = "number_sequence"
//    private const val KEY_WEB_COOKIES = "web_cookies"
//    private const val KEY_SESSION_TIMESTAMP = "session_timestamp"
//    private const val SESSION_DURATION = 365L * 24 * 60 * 60 * 1000 // 1 year in milliseconds
//
//    private lateinit var prefs: SharedPreferences
//    private val gson = Gson()
//
//    fun init(context: Context) {
//        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
//        checkAndRefreshSession()
//    }
//
//    private fun checkAndRefreshSession() {
//        val lastSessionTime = prefs.getLong(KEY_SESSION_TIMESTAMP, 0)
//        // Update session timestamp regardless of current time
//        prefs.edit().putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis()).apply()
//    }
//
//    fun setCurrentUser(user: User) {
//        if (user.storeid.isNullOrEmpty()) {
//            throw IllegalArgumentException("Cannot set user without store ID")
//        }
//        val userJson = gson.toJson(user)
//        prefs.edit()
//            .putString(KEY_USER, userJson)
//            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
//            .apply()
//    }
//
//    fun getCurrentUser(): User? {
//        checkAndRefreshSession()
//        val userJson = prefs.getString(KEY_USER, null)
//        return if (userJson != null) {
//            try {
//                gson.fromJson(userJson, User::class.java)
//            } catch (e: Exception) {
//                null
//            }
//        } else null
//    }
//
//    fun setWebSessionCookies(cookies: String?) {
//        cookies?.let {
//            prefs.edit()
//                .putString(KEY_WEB_COOKIES, it)
//                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
//                .apply()
//        }
//    }
//
//    fun getWebSessionCookies(): String? {
//        checkAndRefreshSession()
//        return prefs.getString(KEY_WEB_COOKIES, null)
//    }
//
//    fun clearCurrentUser() {
//        prefs.edit()
//            .remove(KEY_USER)
//            .remove(KEY_WEB_COOKIES)
//            .remove(KEY_SESSION_TIMESTAMP)
//            .apply()
//    }
//
//    fun isLoggedIn(): Boolean {
//        if (getCurrentUser() != null) {
//            checkAndRefreshSession()
//            return true
//        }
//        return false
//    }
//
//    fun isWebSessionActive(): Boolean {
//        return getWebSessionCookies() != null
//    }
//
//    fun setCurrentNumberSequence(numberSequence: NumberSequenceValue) {
//        val numberSequenceJson = gson.toJson(numberSequence)
//        prefs.edit()
//            .putString(KEY_NUMBER_SEQUENCE, numberSequenceJson)
//            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
//            .apply()
//    }
//
//    fun getCurrentNumberSequence(): NumberSequenceValue? {
//        checkAndRefreshSession()
//        val numberSequenceJson = prefs.getString(KEY_NUMBER_SEQUENCE, null)
//        return if (numberSequenceJson != null) {
//            try {
//                gson.fromJson(numberSequenceJson, NumberSequenceValue::class.java)
//            } catch (e: Exception) {
//                null
//            }
//        } else null
//    }
//}

object SessionManager {
    private const val PREF_NAME = "AppSession"
    private const val KEY_USER = "user"
    private const val KEY_NUMBER_SEQUENCE = "number_sequence"
    private const val KEY_WEB_COOKIES = "web_cookies"
    private const val KEY_SESSION_TIMESTAMP = "session_timestamp" // Add this for session tracking
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun setCurrentUser(user: User) {
        if (user.storeid.isNullOrEmpty()) {
            throw IllegalArgumentException("Cannot set user without store ID")
        }
        val userJson = gson.toJson(user)
        prefs.edit()
            .putString(KEY_USER, userJson)
            .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun getCurrentUser(): User? {
        val userJson = prefs.getString(KEY_USER, null)
        return if (userJson != null) {
            try {
                gson.fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    fun setWebSessionCookies(cookies: String?) {
        cookies?.let {
            prefs.edit()
                .putString(KEY_WEB_COOKIES, it)
                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }
    }

    fun getWebSessionCookies(): String? {
        return prefs.getString(KEY_WEB_COOKIES, null)
    }

    fun clearCurrentUser() {
        prefs.edit()
            .remove(KEY_USER)
            .remove(KEY_WEB_COOKIES)
            .remove(KEY_SESSION_TIMESTAMP)
            .apply()
    }

    fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    fun isWebSessionActive(): Boolean {
        return getWebSessionCookies() != null
    }

    fun setCurrentNumberSequence(numberSequence: NumberSequenceValue) {
        val numberSequenceJson = gson.toJson(numberSequence)
        prefs.edit()
            .putString(KEY_NUMBER_SEQUENCE, numberSequenceJson)
            .apply()
    }

    fun getCurrentNumberSequence(): NumberSequenceValue? {
        val numberSequenceJson = prefs.getString(KEY_NUMBER_SEQUENCE, null)
        return if (numberSequenceJson != null) {
            try {
                gson.fromJson(numberSequenceJson, NumberSequenceValue::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // New method to refresh session
    fun refreshSession() {
        if (isLoggedIn()) {
            prefs.edit()
                .putLong(KEY_SESSION_TIMESTAMP, System.currentTimeMillis())
                .apply()
        }
    }

    // Method to check if session is valid (always returns true unless explicitly logged out)
    fun isSessionValid(): Boolean {
        return isLoggedIn()
    }
}