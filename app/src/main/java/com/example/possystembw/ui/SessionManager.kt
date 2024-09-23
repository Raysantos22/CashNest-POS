package com.example.possystembw.ui

import com.example.possystembw.database.User

object SessionManager {
    private var currentUser: User? = null

    fun setCurrentUser(user: User) {
        currentUser = user
    }

    fun getCurrentUser(): User? {
        return currentUser
    }

    fun clearCurrentUser() {
        currentUser = null
    }
}