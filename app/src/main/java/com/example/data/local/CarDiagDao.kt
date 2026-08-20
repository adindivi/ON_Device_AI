package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CarDiagDao {
    @Query("SELECT * FROM diagnostic_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DiagnosticHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: DiagnosticHistory): Long

    @Query("DELETE FROM diagnostic_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("SELECT * FROM rag_documents ORDER BY isUserAdded DESC, id DESC")
    fun getAllRagDocuments(): Flow<List<RagDocument>>

    @Query("SELECT * FROM rag_documents WHERE isUserAdded = 1 ORDER BY id DESC")
    fun getUserManualDocuments(): Flow<List<RagDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRagDocument(doc: RagDocument): Long

    @Update
    suspend fun updateRagDocument(doc: RagDocument)

    @Query("SELECT * FROM rag_documents WHERE docCode = :docCode LIMIT 1")
    suspend fun getRagDocumentByDocCode(docCode: String): RagDocument?

    @Query("SELECT * FROM rag_documents WHERE id = :id LIMIT 1")
    suspend fun getRagDocumentById(id: Long): RagDocument?

    @Query("DELETE FROM rag_documents WHERE id = :id")
    suspend fun deleteRagDocument(id: Long)

    @Query("UPDATE rag_documents SET recommendationCount = recommendationCount + 1 WHERE id = :id")
    suspend fun incrementRecommendation(id: Long)

    @Query("DELETE FROM diagnostic_history")
    suspend fun clearAllHistory()

    @Query("DELETE FROM rag_documents")
    suspend fun clearAllRagDocuments()
}
