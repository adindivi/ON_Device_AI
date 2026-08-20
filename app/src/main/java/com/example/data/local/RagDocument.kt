package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rag_documents")
data class RagDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docCode: String,
    val category: String,
    val title: String,
    val snippet: String,
    val fullContent: String,
    val dtcCode: String? = null,
    val component: String? = null,
    val connectorLocation: String? = null,
    val sourceName: String = "KB",
    val recommendationCount: Int = 0,
    val isUserAdded: Boolean = false,
    val dateString: String = "03-15"
)
