package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0, // Room will generate the ID automatically
    @ColumnInfo(name = "accountnum") val accountNum: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "address") val address: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "currency") val currency: String? = null,
    @ColumnInfo(name = "blocked") val blocked: Int = 0, // 0 for active, 1 for blocked
    @ColumnInfo(name = "creditmax") val creditMax: Double? = null,
    @ColumnInfo(name = "country") val country: String? = null,
    @ColumnInfo(name = "zipcode") val zipCode: String? = null,
    @ColumnInfo(name = "state") val state: String? = null,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "cellularphone") val cellularPhone: String? = null,
    @ColumnInfo(name = "gender") val gender: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Date? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Date? = null
)