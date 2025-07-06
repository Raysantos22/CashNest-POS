package com.example.possystembw.ui.ViewModel

import com.example.possystembw.database.WindowTable
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class WindowTableListDeserializer : JsonDeserializer<List<WindowTable>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<WindowTable> {
        return if (json.isJsonObject) {
            val jsonObject = json.asJsonObject
            val windowTablesArray = jsonObject.getAsJsonArray("windowtables")
            context.deserialize(windowTablesArray, object : TypeToken<List<WindowTable>>() {}.type)
        } else {
            // If it's already an array, deserialize normally
            context.deserialize(json, object : TypeToken<List<WindowTable>>() {}.type)
        }
    }
}