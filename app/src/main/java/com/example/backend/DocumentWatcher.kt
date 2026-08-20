package com.example.backend

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DocumentWatcher(
    private val docsDir: File,
    private val vectorDb: SimpleVectorDB,
    private val ragSearcher: RAGSearcher,
    private val onNewDocumentsAdded: (Int) -> Unit = {}
) {

    private val processedFiles = mutableMapOf<String, Long>()
    private var watcherJob: Job? = null

    fun startWatching(scope: CoroutineScope) {
        stopWatching()
        if (!docsDir.exists()) {
            docsDir.mkdirs()
        }

        watcherJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    scanDirectory()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(10000) // Poll every 10 seconds like python script
            }
        }
    }

    fun stopWatching() {
        watcherJob?.cancel()
        watcherJob = null
    }

    fun scanDirectory(): Int {
        if (!docsDir.exists()) return 0

        var totalAdded = 0
        // .json 및 .txt 파일 모두 감시하도록 강화
        val filesToScan = docsDir.listFiles { _, name ->
            val lower = name.lowercase()
            (lower.endsWith(".json") || lower.endsWith(".txt")) &&
                    lower != "rag_vector_database.json" &&
                    lower != "mapping_dictionary.json"
        } ?: emptyArray()

        for (file in filesToScan) {
            val lastMtime = processedFiles[file.absolutePath] ?: 0L
            val currentMtime = file.lastModified()

            if (currentMtime > lastMtime) {
                try {
                    val content = file.readText(Charsets.UTF_8).trim()
                    val newEntries = mutableListOf<VectorDbEntry>()
                    val timestamp = System.currentTimeMillis()

                    if (file.name.lowercase().endsWith(".txt")) {
                        // ── 일반 .txt 메모장 파일 파싱 ─────────────────────────────────
                        if (content.isNotBlank() && !vectorDb.isDuplicate(content)) {
                            // DTC 고장코드 추출 (예: P0301, C1206 등)
                            val dtcMatch = Regex("(?i)[a-z]*[0-9]{3,}").find(content)?.value ?: ""
                            val emb = ragSearcher.getEmbedding(content)
                            newEntries.add(
                                VectorDbEntry(
                                    id = "DOC-TXT-$timestamp-1",
                                    text = content,
                                    embedding = emb,
                                    metadata = VectorDbMetadata(
                                        contextQuery = content.take(30),
                                        dtcCode = dtcMatch
                                    ),
                                    recommendations = 0,
                                    createdAt = timestamp
                                )
                            )
                        }
                    } else if (content.startsWith("[")) {
                        // ── JSON 배열 파싱 ──────────────────────────────────────
                        val jsonArr = JSONArray(content)
                        for (i in 0 until jsonArr.length()) {
                            val obj = jsonArr.getJSONObject(i)
                            val text = obj.optString("text", "")
                            if (text.isNotBlank() && !vectorDb.isDuplicate(text)) {
                                val metaObj = obj.optJSONObject("metadata")
                                val meta = VectorDbMetadata.fromJsonObject(metaObj)
                                val emb = ragSearcher.getEmbedding(text)
                                newEntries.add(
                                    VectorDbEntry(
                                        id = "DOC-BATCH-$timestamp-${i + 1}",
                                        text = text,
                                        embedding = emb,
                                        metadata = meta,
                                        recommendations = 0,
                                        createdAt = timestamp
                                    )
                                )
                            }
                        }
                    } else if (content.startsWith("{")) {
                        // ── 단일 JSON 객체 파싱 ──────────────────────────────────
                        val obj = JSONObject(content)
                        val text = obj.optString("text", "")
                        if (text.isNotBlank() && !vectorDb.isDuplicate(text)) {
                            val metaObj = obj.optJSONObject("metadata")
                            val meta = VectorDbMetadata.fromJsonObject(metaObj)
                            val emb = ragSearcher.getEmbedding(text)
                            newEntries.add(
                                VectorDbEntry(
                                    id = "DOC-BATCH-$timestamp-1",
                                    text = text,
                                    embedding = emb,
                                    metadata = meta,
                                    recommendations = 0,
                                    createdAt = timestamp
                                )
                            )
                        }
                    }

                    if (newEntries.isNotEmpty()) {
                        val added = vectorDb.addDocumentsBatch(newEntries)
                        totalAdded += added
                    }
                    processedFiles[file.absolutePath] = currentMtime
                } catch (e: Exception) {
                    // Ignore transient write errors
                }
            }
        }

        if (totalAdded > 0) {
            onNewDocumentsAdded(totalAdded)
        }
        return totalAdded
    }
}
