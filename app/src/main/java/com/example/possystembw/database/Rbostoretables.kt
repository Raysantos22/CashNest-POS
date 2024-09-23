package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rbostoretables")
data class Rbostoretables(
    @PrimaryKey(autoGenerate = true) val storeId: Int = 0,
    @ColumnInfo(name = "NAME") val name: String,
    @ColumnInfo(name = "ADDRESS") val address: String,
    @ColumnInfo(name = "STREET") val street: String,
    @ColumnInfo(name = "ZIPCODE") val zipCode: String,
    @ColumnInfo(name = "CITY") val city: String,
    @ColumnInfo(name = "STATE") val state: String,
    @ColumnInfo(name = "COUNTRY") val country: String,
    @ColumnInfo(name = "PHONE") val phone: String,
    @ColumnInfo(name = "CURRENCY") val currency: String,
    @ColumnInfo(name = "SQLSERVERNAME") val sqlServerName: String,
    @ColumnInfo(name = "DATABASENAME") val databaseName: String,
    @ColumnInfo(name = "USERNAME") val username: String,
    @ColumnInfo(name = "PASSWORD") val password: String,
    @ColumnInfo(name = "WINDOWSAUTHENTICATION") val windowsAuthentication: Boolean,
    @ColumnInfo(name = "FORMINFOFIELD1") val formInfoField1: String?,
    @ColumnInfo(name = "FORMINFOFIELD2") val formInfoField2: String?,
    @ColumnInfo(name = "FORMINFOFIELD3") val formInfoField3: String?,
    @ColumnInfo(name = "FORMINFOFIELD4") val formInfoField4: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "ROUTES") val routes: String?,
    @ColumnInfo(name = "TYPES") val types: String?,
    @ColumnInfo(name = "BLOCKED") val blocked: Boolean
)
