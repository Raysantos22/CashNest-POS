package com.example.possystembw.ui

import android.content.Context
import android.content.SharedPreferences
import com.example.possystembw.database.StaffEntity
import com.google.gson.Gson

object StaffManager {
    private const val PREF_NAME = "StaffPreferences"
    private const val KEY_STAFF = "current_staff"
    private var currentStaff: StaffEntity? = null
    private var onStaffChangeListener: ((String?) -> Unit)? = null
    private lateinit var prefs: SharedPreferences
    private val gson = Gson()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        // Load saved staff on initialization
        loadSavedStaff()
    }

    private fun loadSavedStaff() {
        val staffJson = prefs.getString(KEY_STAFF, null)
        if (staffJson != null) {
            try {
                currentStaff = gson.fromJson(staffJson, StaffEntity::class.java)
                onStaffChangeListener?.invoke(currentStaff?.name)
            } catch (e: Exception) {
                clearCurrentStaff()
            }
        }
    }

    fun setCurrentStaff(staff: StaffEntity?) {
        currentStaff = staff
        // Save to SharedPreferences
        prefs.edit().apply {
            if (staff != null) {
                putString(KEY_STAFF, gson.toJson(staff))
            } else {
                remove(KEY_STAFF)
            }
            apply()
        }
        onStaffChangeListener?.invoke(staff?.name)
    }

    fun getCurrentStaff(): String? {
        return currentStaff?.name
    }

    fun getCurrentStaffEntity(): StaffEntity? {
        return currentStaff
    }

    fun hasActiveStaff(): Boolean {
        return currentStaff != null
    }

    fun setOnStaffChangeListener(listener: (String?) -> Unit) {
        onStaffChangeListener = listener
        // Trigger listener with current staff when set
        listener(currentStaff?.name)
    }

    fun clearCurrentStaff() {
        currentStaff = null
        prefs.edit().remove(KEY_STAFF).apply()
        onStaffChangeListener?.invoke(null)
    }
}