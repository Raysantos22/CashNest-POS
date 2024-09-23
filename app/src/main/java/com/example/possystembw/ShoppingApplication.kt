package com.example.possystembw

import android.app.Application
import android.util.Log
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import com.example.possystembw.data.AppDatabase
import com.example.possystembw.data.CartRepository
import com.example.possystembw.data.ProductRepository

class ShoppingApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val productApi by lazy { RetrofitClient.productApi }
    val categoryApi by lazy { RetrofitClient.categoryApi }

    val repository by lazy {
        ProductRepository(
            productDao = database.productDao(),
            categoryDao = database.categoryDao(),
            productApi = productApi,
            categoryApi = categoryApi
        )
    }

    val cartRepository by lazy { CartRepository(database.cartDao()) }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
        }
    }
}