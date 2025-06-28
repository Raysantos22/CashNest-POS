package com.example.possystembw.ui

import android.content.Context
import android.util.Log
import java.net.NetworkInterface

class LocalDataManager(private val context: Context) {
    private var server: LocalDataServer? = null
    private val TAG = "LocalDataManager"
    private val PORT = 8000

    fun startServer(): String? {
        return try {
            stopServer() // Stop any existing server
            
            server = LocalDataServer(context, PORT)
            server?.start()
            
            val ipAddress = getLocalIpAddress()
            val url = "http://$ipAddress:$PORT"
            
            Log.d(TAG, "Local data server started at: $url")
            url
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start server", e)
            null
        }
    }

    fun stopServer() {
        server?.let {
            if (it.isAlive) {
                it.stop()
                Log.d(TAG, "Local data server stopped")
            }
        }
        server = null
    }

    fun isServerRunning(): Boolean {
        return server?.isAlive == true
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address.address.size == 4) {
                        return address.hostAddress ?: "localhost"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP address", e)
        }
        return "localhost"
    }
}
