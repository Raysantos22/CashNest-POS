package com.example.possystembw.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.possystembw.DAO.LineTransaction

@Entity(tableName = "line_transaction_visibility")
data class LineTransactionVisibility(
    @PrimaryKey val itemId: String,
    @ColumnInfo(name = "is_hidden") val isHidden: Boolean = false
)
data class LineTransactionWithVisibility(
    val lineTransaction: LineTransaction,
    val isVisible: Boolean
)