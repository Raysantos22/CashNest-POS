package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName


// 2. Update your WindowTable entity to match the API field names
@Entity(tableName = "windowtable")
data class WindowTable(
    @PrimaryKey
    @SerializedName("ID") // Map the API field name to your property
    val id: Int,

    @SerializedName("DESCRIPTION") // Map the API field name to your property
    val description: String
)

