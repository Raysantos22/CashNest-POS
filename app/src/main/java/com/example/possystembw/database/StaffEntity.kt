package com.example.possystembw.database


import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val passcode: String = "",
    val image: String? = null,
    val role: String = "",
    val storeId: String = ""  // Changed to default empty string
)

data class StaffResponse(
    @SerializedName("name") val name: String?,
    @SerializedName("passcode") val passcode: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("storeid") val storeid: String?  // Note: API returns "storeid" not "storeId"
)
