package com.example.backend

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class SimpleVectorDB(private val dbFile: File) {

    private val dataList = mutableListOf<VectorDbEntry>()
    private val lock = ReentrantReadWriteLock()

    init {
        loadDb()
    }

    fun loadFromAssets(context: android.content.Context, assetPath: String = "DB/rag_vector_database.json") {
        lock.write {
            dataList.clear()
            try {
                val content = context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseAndPopulateEntries(content)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reloadFromDisk() {
        loadDb()
    }

    private fun loadDb() {
        lock.write {
            dataList.clear()
            if (dbFile.exists() && dbFile.length() > 0) {
                try {
                    val content = dbFile.readText(Charsets.UTF_8)
                    parseAndPopulateEntries(content)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun parseAndPopulateEntries(jsonContent: String) {
        val jsonArray = JSONArray(jsonContent)
        for (i in 0 until jsonArray.length()) {
            try {
                val obj = jsonArray.getJSONObject(i)
                dataList.add(VectorDbEntry.fromJsonObject(obj))
            } catch (_: Exception) {
                // Skip broken single entry and keep parsing all remaining docs
            }
        }
    }

    private fun saveDbInternal() {
        try {
            val dbDir = dbFile.parentFile
            if (dbDir != null && !dbDir.exists()) {
                dbDir.mkdirs()
            }
            val jsonArray = JSONArray()
            dataList.forEach { entry ->
                jsonArray.put(entry.toJsonObject())
            }
            dbFile.writeText(jsonArray.toString(2), Charsets.UTF_8)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isDuplicate(text: String): Boolean {
        lock.read {
            val target = text.trim()
            return dataList.any { it.text.trim() == target }
        }
    }

    fun addDocument(
        docId: String,
        text: String,
        embedding: List<Float>,
        metadata: VectorDbMetadata = VectorDbMetadata()
    ): String? {
        lock.write {
            if (isDuplicate(text)) {
                return null
            }
            val entry = VectorDbEntry(
                id = docId,
                text = text,
                embedding = embedding,
                metadata = metadata,
                recommendations = 0,
                createdAt = System.currentTimeMillis()
            )
            dataList.add(entry)
            saveDbInternal()
            return docId
        }
    }

    fun addDocumentsBatch(entries: List<VectorDbEntry>): Int {
        lock.write {
            var addedCount = 0
            for (entry in entries) {
                if (dataList.any { it.text.trim() == entry.text.trim() }) {
                    continue
                }
                dataList.add(entry)
                addedCount++
            }
            if (addedCount > 0) {
                saveDbInternal()
            }
            return addedCount
        }
    }

    fun updateRecommendation(docId: String): Int {
        lock.write {
            val entry = dataList.find { it.id == docId }
            if (entry != null) {
                entry.recommendations += 1
                saveDbInternal()
                return entry.recommendations
            }
            return 0
        }
    }

    fun deleteDocument(docId: String): Boolean {
        lock.write {
            val removed = dataList.removeIf { it.id == docId }
            if (removed) {
                saveDbInternal()
            }
            return removed
        }
    }

    fun updateDocument(docId: String, newText: String, newEmbedding: List<Float>, newMetadata: VectorDbMetadata): Boolean {
        lock.write {
            val idx = dataList.indexOfFirst { it.id == docId }
            if (idx != -1) {
                val old = dataList[idx]
                dataList[idx] = old.copy(
                    text = newText,
                    embedding = newEmbedding,
                    metadata = newMetadata
                )
                saveDbInternal()
                return true
            }
            return false
        }
    }

    fun getAll(): List<VectorDbEntry> {
        lock.read {
            return dataList.toList()
        }
    }

    fun getDbFilePath(): String = dbFile.absolutePath
    fun getDocumentCount(): Int = lock.read { dataList.size }
}
