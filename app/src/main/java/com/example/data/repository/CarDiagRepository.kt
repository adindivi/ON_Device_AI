package com.example.data.repository

import com.example.data.local.CarDiagDao
import com.example.data.local.DiagnosticHistory
import com.example.data.local.RagDocument
import kotlinx.coroutines.flow.Flow

class CarDiagRepository(private val dao: CarDiagDao) {
    val allHistory: Flow<List<DiagnosticHistory>> = dao.getAllHistory()
    val allRagDocuments: Flow<List<RagDocument>> = dao.getAllRagDocuments()
    val userManualDocuments: Flow<List<RagDocument>> = dao.getUserManualDocuments()

    suspend fun insertHistory(item: DiagnosticHistory): Long = dao.insertHistory(item)
    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)

    suspend fun insertRagDocument(doc: RagDocument): Long = dao.insertRagDocument(doc)
    suspend fun updateRagDocument(doc: RagDocument) = dao.updateRagDocument(doc)
    suspend fun getRagDocumentById(id: Long): RagDocument? = dao.getRagDocumentById(id)
    suspend fun deleteRagDocument(id: Long) = dao.deleteRagDocument(id)

    suspend fun incrementRecommendation(id: Long) = dao.incrementRecommendation(id)
}
