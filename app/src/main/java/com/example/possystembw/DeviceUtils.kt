package com.example.possystembw

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Log
import kotlin.math.min

/**
 * Simple utility class for device detection and orientation setting
 * Works with your layout qualifiers: w320dp (mobile) and w600dp+ (tablet)
 */
object DeviceUtils {

    private const val TAG = "DeviceUtils"
    private const val MOBILE_MAX_WIDTH_DP = 599  // Below 600dp = mobile

    /**
     * Determines if device is mobile based on your layout breakpoints
     * Mobile: width < 600dp (uses w320dp layouts)
     * Tablet: width >= 600dp (uses w600dp+ layouts or default)
     */
    fun isMobile(context: Context): Boolean {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val widthDp = metrics.widthPixels / density
        val heightDp = metrics.heightPixels / density
        val smallestWidthDp = min(widthDp, heightDp)

        val isMobile = smallestWidthDp <= MOBILE_MAX_WIDTH_DP

        Log.d(TAG, "Screen: ${widthDp.toInt()}x${heightDp.toInt()}dp, Smallest: ${smallestWidthDp.toInt()}dp")
        Log.d(TAG, "Device type: ${if (isMobile) "Mobile" else "Tablet"}")

        return isMobile
    }

    /**
     * Determines if device is tablet (opposite of isMobile for backward compatibility)
     */
    fun isTablet(context: Context): Boolean {
        return !isMobile(context)
    }

    /**
     * YOUR EXISTING METHOD NAME - Sets orientation based on device type
     * Mobile (w320dp): Portrait
     * Tablet (w600dp+/default): Landscape
     *
     * This is the method you're already using in all your activities!
     */
    fun setOrientationBasedOnDevice(activity: Activity) {
        try {
            if (isMobile(activity)) {
                // Mobile: Portrait orientation
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Log.d(TAG, "✅ Mobile detected - Set PORTRAIT orientation")
            } else {
                // Tablet: Landscape orientation
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                Log.d(TAG, "✅ Tablet detected - Set LANDSCAPE orientation")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set orientation, using landscape fallback", e)
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    /**
     * Always use landscape (for specific activities that need it)
     */
    fun setAlwaysLandscape(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        Log.d(TAG, "Set LANDSCAPE ONLY orientation")
    }

    /**
     * Always use portrait (for specific activities that need it)
     */
    fun setAlwaysPortrait(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        Log.d(TAG, "Set PORTRAIT ONLY orientation")
    }

    /**
     * Allow auto rotation (sensor-based)
     */
    fun setAutoRotation(activity: Activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
        Log.d(TAG, "Set AUTO ROTATION orientation")
    }

    /**
     * Get device type as string for debugging
     */
    fun getDeviceTypeString(context: Context): String {
        return if (isMobile(context)) "Mobile" else "Tablet"
    }

    /**
     * Get device info string for debugging
     */
    fun getScreenInfo(context: Context): String {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        val widthDp = metrics.widthPixels / density
        val heightDp = metrics.heightPixels / density
        val smallestWidthDp = min(widthDp, heightDp)
        val deviceType = if (isMobile(context)) "Mobile" else "Tablet"

        return "Device: $deviceType | Screen: ${widthDp.toInt()}x${heightDp.toInt()}dp | Smallest: ${smallestWidthDp.toInt()}dp"
    }
}