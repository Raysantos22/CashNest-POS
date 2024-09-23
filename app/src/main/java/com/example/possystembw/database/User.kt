package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.*
import java.util.Date

@Entity(tableName = "logins")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val storeid: String,
    val email_verified_at: Date?,
    val password: String,
    val two_factor_secret: String?,
    val two_factor_recovery_codes: String?,
    val two_factor_confirmed_at: Date?,
    val remember_token: String?,
    val current_team_id: String?,
    val profile_photo_path: String?,
    val role: String,
    val created_at: Date,
    val updated_at: Date
)
