package com.example.possystembw.ui.ViewModel

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CustomDateDeserializer : JsonDeserializer<Date> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
        if (json == null || json.isJsonNull) return Date()

        val dateString = json.asString

        // Handle the API format: 2025-07-01T01:22:35.000000Z
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",  // API format with microseconds
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",     // API format with milliseconds
            "yyyy-MM-dd'T'HH:mm:ss'Z'",         // API format basic
            "yyyy-MM-dd HH:mm:ss",              // Simple format
            "yyyy-MM-dd"                        // Date only
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                if (format.contains("'Z'")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                } else {
                    sdf.timeZone = TimeZone.getTimeZone("Asia/Manila")
                }
                return sdf.parse(dateString) ?: Date()
            } catch (e: Exception) {
                // Try next format
            }
        }

        Log.w("DateDeserializer", "Could not parse date: $dateString, using current date")
        return Date()
    }
}