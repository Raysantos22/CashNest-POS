package com.example.possystembw

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import kotlin.math.min

/**
 * Utility class for detecting device type and setting appropriate orientation
 */
object DeviceUtils {
    
    /**
     * Determines if the current device is a tablet based on screen size
     * @param context Application context
     * @return true if device is tablet, false if phone
     */
    fun isTablet(context: Context): Boolean {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val dpWidth = metrics.widthPixels / density
        val dpHeight = metrics.heightPixels / density
        val smallestWidth = min(dpWidth, dpHeight)
        
        // Consider devices with smallest width >= 600dp as tablets
        val isTablet = smallestWidth >= 600
        
        Log.d("DeviceUtils", "Screen: ${dpWidth}x${dpHeight}dp, Smallest: ${smallestWidth}dp, IsTablet: $isTablet")
        
        return isTablet
    }
    
    /**
     * Sets screen orientation based on device type
     * Tablets: Landscape orientation (disable portrait)
     * Phones: Portrait orientation (disable landscape)
     */
    fun setOrientationBasedOnDevice(activity: Activity) {
        if (isTablet(activity)) {
            // Tablet: Force landscape, disable portrait
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            Log.d("DeviceUtils", "Tablet detected - Setting LANDSCAPE orientation")
        } else {
            // Mobile: Force portrait, disable landscape
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Log.d("DeviceUtils", "Mobile detected - Setting PORTRAIT orientation")
        }
    }
    
    /**
     * Forces landscape orientation regardless of device type
     * Use for POS/Main activities that should always be landscape
     */
    fun setAlwaysLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Log.d("DeviceUtils", "Setting ALWAYS LANDSCAPE orientation")
    }
    
    /**
     * Forces portrait orientation regardless of device type
     */
    fun setAlwaysPortrait(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Log.d("DeviceUtils", "Setting ALWAYS PORTRAIT orientation")
    }
    
    /**
     * Allows both orientations (sensor-based rotation)
     */
    fun setAutoRotation(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        Log.d("DeviceUtils", "Setting AUTO ROTATION orientation")
    }
    
    /**
     * Gets device type as string for debugging
     */
    fun getDeviceTypeString(context: Context): String {
        return if (isTablet(context)) "Tablet" else "Phone"
    }
    
    /**
     * Gets current screen size information
     */
    fun getScreenInfo(context: Context): String {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val dpWidth = metrics.widthPixels / density
        val dpHeight = metrics.heightPixels / density
        val smallestWidth = min(dpWidth, dpHeight)
        
        return "Screen: ${dpWidth.toInt()}x${dpHeight.toInt()}dp (smallest: ${smallestWidth.toInt()}dp)"
    }
}