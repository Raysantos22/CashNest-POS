package com.example.possystembw.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mix_match_line_groups")
data class MixMatchLineGroup(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mixMatchId: String,
    val lineGroup: String,
    val description: String,
    val noOfItemsNeeded: Int
)