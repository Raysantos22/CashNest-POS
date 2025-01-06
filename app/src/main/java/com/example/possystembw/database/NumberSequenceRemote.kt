package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "number_sequence_remote", primaryKeys = ["storeId"])
data class NumberSequenceRemoteEntity(
    val storeId: String,
    val numberSequence: String,
    var nextRec: Int,
    var cartNextRec: Int,
    var bundleNextRec: Int,
    var discountNextRec: Int,
    var wasteRec: Int,
    var toNextRec: Int,
    var stockNextRec: Int,
    val createdAt: String?,
    val updatedAt: String?
)

//fun Response<*>.logResponse(tag: String) {
//    Log.d(tag, "Response Code: $code")
//    Log.d(tag, "Response Message: $message")
//    Log.d(tag, "Response Body: ${body()}")
//    Log.d(tag, "Response Headers: $headers")
//    if (!isSuccessful) {
//        Log.e(tag, "Error Body: ${errorBody()?.string()}")
//    }
//}