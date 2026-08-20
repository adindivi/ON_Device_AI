package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_history")
data class DiagnosticHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dtcCode: String,
    val symptomText: String,
    val summary: String,
    val fullAnalysis: String,
    val checksListJson: String,
    val solutionText: String,
    val warningText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val statusType: String = "WARNING"
)
