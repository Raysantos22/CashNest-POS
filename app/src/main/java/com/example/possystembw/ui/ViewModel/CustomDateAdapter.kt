package com.example.possystembw.ui.ViewModel

//import com.google.gson.TypeAdapter
//import com.google.gson.stream.JsonReader
//import com.google.gson.stream.JsonWriter
//import java.text.ParseException
//import java.text.SimpleDateFormat
//import java.util.Date
//import java.util.Locale
//import java.util.TimeZone
//
//class CustomDateAdapter : TypeAdapter<Date>() {
//    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
//        timeZone = TimeZone.getTimeZone("UTC")
//    }
//
//    override fun write(out: JsonWriter, value: Date?) {
//        if (value == null) {
//            out.nullValue()
//        } else {
//            out.value(dateFormat.format(value))
//        }
//    }
//
//    override fun read(input: JsonReader): Date? {
//        return try {
//            val dateString = input.nextString()
//            dateFormat.parse(dateString)
//        } catch (e: ParseException) {
//            println("Error parsing date: ${e.message}")
//            null
//        }
//    }
//}

import android.util.Log
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.lang.reflect.Type
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CustomDateAdapter : JsonDeserializer<Date> {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", Locale.US)

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Date {
        return try {
            dateFormat.parse(json.asString) ?: Date()
        } catch (e: Exception) {
            Log.e("CustomDateAdapter", "Error parsing date", e)
            Date()
        }
    }
}