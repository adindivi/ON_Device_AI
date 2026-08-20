package com.example.backend

import android.content.Context
import android.os.Environment
import com.example.data.local.AppDatabase
import com.example.data.local.RagDocument
import com.example.ui.viewmodel.ExtractedMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.regex.Pattern

data class BackendDbStatus(
    val dbFolderPath: String,
    val isQwenModelFound: Boolean,
    val qwenModelPath: String,
    val isVectorDbFound: Boolean,
    val vectorDbPath: String,
    val totalVectorDocuments: Int,
    val isMappingDictFound: Boolean,
    val isDocsFolderFound: Boolean,
    val docsFolderPath: String
)

data class DiagnosisBackendResponse(
    val qwenAnswer: String,
    val rawMatches: List<SearchResult>
)

data class AddDocumentBackendResponse(
    val success: Boolean,
    val insertedId: String?,
    val message: String,
    val storedMetadata: VectorDbMetadata
)

class OnDeviceBackendEngine(private val context: Context) {

    val dbDirectory: File
    val docsDirectory: File
    val qwenModelFile: File
    val vectorDbFile: File

    val vectorDb: SimpleVectorDB
    val onnxBertEngine: OnnxBertEmbeddingEngine
    val ragSearcher: RAGSearcher
    val qwenLlm: QwenLLM
    val documentWatcher: DocumentWatcher

    init {
        // Resolve phone folder "DB" safely by smart auto-detecting folder containing valid rag_vector_database.json
        val candidateDirs = listOf(
            File("/storage/emulated/0/DB"),
            File(Environment.getExternalStorageDirectory(), "DB"),
            context.getExternalFilesDir("DB"),
            context.getExternalFilesDir(null),
            File(context.filesDir, "DB")
        ).filterNotNull()

        // 1. Smart detect directory that actually holds valid rag_vector_database.json (e.g. 6.14MB file)
        val validDbDir = candidateDirs.firstOrNull { dir ->
            val targetJson = File(dir, "rag_vector_database.json")
            targetJson.exists() && targetJson.length() > 100L
        }

        val resolvedDir = validDbDir ?: try {
            val emulatedDb = File("/storage/emulated/0/DB")
            val appExternalDb = context.getExternalFilesDir("DB")
            when {
                emulatedDb.exists() || try { emulatedDb.mkdirs() } catch (e: Exception) { false } -> emulatedDb
                appExternalDb != null && (appExternalDb.exists() || try { appExternalDb.mkdirs() } catch (e: Exception) { false }) -> appExternalDb
                else -> File(context.filesDir, "DB")
            }
        } catch (e: Exception) {
            File(context.filesDir, "DB")
        }

        if (!resolvedDir.exists()) {
            try { resolvedDir.mkdirs() } catch (_: Exception) {}
        }
        dbDirectory = resolvedDir

        docsDirectory = File(dbDirectory, "rag_documents")
        if (!docsDirectory.exists()) {
            try { docsDirectory.mkdirs() } catch (_: Exception) {}
        }

        val primaryModel = File(dbDirectory, "qwen2.5-1.5b-instruct-q8_0.gguf")
        val anyGguf = dbDirectory.listFiles { _, name -> name.endsWith(".gguf", ignoreCase = true) }?.firstOrNull()
        qwenModelFile = when {
            primaryModel.exists() -> primaryModel
            anyGguf != null -> anyGguf
            else -> primaryModel
        }
        vectorDbFile = File(dbDirectory, "rag_vector_database.json")

        // Auto unpack DB from assets if missing or default small file
        unpackAssetsDbIfMissing()

        // 1. Initialize Mapping Dictionary
        MappingDictionary.loadMappingDictionary(dbDirectory)

        // 2. Initialize Vector DB with direct fallback to APK Assets if external load is empty
        vectorDb = SimpleVectorDB(vectorDbFile)
        if (vectorDb.getDocumentCount() == 0) {
            vectorDb.loadFromAssets(context, "DB/rag_vector_database.json")
        }

        // 3. Initialize ONNX BERT Embedding Engine
        val onnxBertModelFile = File(dbDirectory, "ko-sbert-multitask_embedding.onnx")
        val onnxVocabFile = File(dbDirectory, "ko-sbert-multitask_vocab.txt")
        onnxBertEngine = OnnxBertEmbeddingEngine(onnxBertModelFile, onnxVocabFile)

        // 4. Initialize RAG Searcher with ONNX BERT Engine
        ragSearcher = RAGSearcher(vectorDb, onnxBertEngine)

        // 5. Initialize Qwen LLM
        qwenLlm = QwenLLM(qwenModelFile)

        documentWatcher = DocumentWatcher(docsDirectory, vectorDb, ragSearcher) { addedCount ->
            syncVectorDbToRoom()
        }

        clearOldDatabaseAndSyncFresh()
    }

    private fun unpackAssetsDbIfMissing() {
        val mappingFile = File(dbDirectory, MappingDictionary.MAPPING_FILENAME)
        val onnxModelFile = File(dbDirectory, "ko-sbert-multitask_embedding.onnx")
        val onnxVocabFile = File(dbDirectory, "ko-sbert-multitask_vocab.txt")
        try {
            // Preserve user-placed DB files if exist in device DB folder, unpack from assets only if missing
            if (!mappingFile.exists() || mappingFile.length() == 0L) {
                context.assets.open("DB/mapping_dictionary.json").use { input ->
                    mappingFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!vectorDbFile.exists() || vectorDbFile.length() == 0L) {
                context.assets.open("DB/rag_vector_database.json").use { input ->
                    vectorDbFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!onnxModelFile.exists() || onnxModelFile.length() == 0L) {
                try {
                    context.assets.open("DB/ko-sbert-multitask_embedding.onnx").use { input ->
                        onnxModelFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            if (!onnxVocabFile.exists() || onnxVocabFile.length() == 0L) {
                try {
                    context.assets.open("DB/ko-sbert-multitask_vocab.txt").use { input ->
                        onnxVocabFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startWatcher(scope: CoroutineScope) {
        documentWatcher.startWatching(scope)
    }

    private fun populateDefaultDataIfEmpty() {
        if (vectorDb.getDocumentCount() == 0) {
            val defaultEntries = listOf(
                VectorDbEntry(
                    id = "DOC-DEFAULT-1",
                    text = "에어컨 가스 누출 및 냉매 압력센서 P053001 발생 시 컴프레서 신호 배선 단체 및 커넥터 저항값 저하를 점검합니다.",
                    embedding = ragSearcher.getEmbedding("에어컨 가스 누출 냉매 압력센서 P053001 점검"),
                    metadata = VectorDbMetadata(
                        contextQuery = "에어컨 찬바람 불량",
                        dtcCode = "P053001",
                        component = "냉매 압력센서",
                        connectorLocation = "전방 (R) 커넥터",
                        dtcs = listOf("P053001")
                    ),
                    recommendations = 0
                ),
                VectorDbEntry(
                    id = "DOC-DEFAULT-2",
                    text = "ABS/EPB 경고등 및 C120601 코드는 휠속도 센서 회로 단선 단락 오류입니다. 센서 커넥터 저항값(1.1k~1.3kΩ) 측정 및 배선 청소가 필요합니다.",
                    embedding = ragSearcher.getEmbedding("ABS EPB C120601 휠속도 센서 단선 단락"),
                    metadata = VectorDbMetadata(
                        contextQuery = "브레이크 센서 단선",
                        dtcCode = "C120601",
                        component = "휠속도센서",
                        connectorLocation = "후방 좌우 휠 너클",
                        dtcs = listOf("C120601")
                    ),
                    recommendations = 0
                ),
                VectorDbEntry(
                    id = "DOC-DEFAULT-3",
                    text = "에어컨 냉기 부족 및 풍량 저하 시 글로브박스 하부 캐빈 에어컨 필터 막힘 및 에바포레이터 온도 센서 동작 상태를 검사하십시오.",
                    embedding = ragSearcher.getEmbedding("에어컨 바람 풍량 필터 에바포레이터"),
                    metadata = VectorDbMetadata(
                        contextQuery = "에어컨 바람 냉기 필터",
                        dtcCode = "B124111",
                        component = "블로워 모터",
                        connectorLocation = "실내 글로브박스 하부",
                        dtcs = listOf("B124111")
                    ),
                    recommendations = 0
                )
            )
            vectorDb.addDocumentsBatch(defaultEntries)
        }
    }

    private fun syncVectorDbToRoom() {
        val roomDao = AppDatabase.getDatabase(context).carDiagDao()
        CoroutineScope(Dispatchers.IO).launch {
            val allEntries = vectorDb.getAll()
            for (entry in allEntries) {
                val isUserCreated = entry.id.startsWith("USER-") || entry.id.startsWith("DOC-NEW-")
                val existingDoc = roomDao.getRagDocumentByDocCode(entry.id)
                if (existingDoc == null) {
                    val roomDoc = RagDocument(
                        docCode = entry.id,
                        category = if (entry.metadata.dtcCode.isNotBlank()) "DTC Guide" else "Remedy",
                        title = entry.metadata.component.ifBlank { "정비 노하우" },
                        snippet = entry.text.take(60) + "...",
                        fullContent = entry.text,
                        dtcCode = entry.metadata.dtcCode.ifBlank { null },
                        component = entry.metadata.component.ifBlank { null },
                        connectorLocation = entry.metadata.connectorLocation.ifBlank { null },
                        sourceName = if (isUserCreated) "User Added" else "Phone DB",
                        recommendationCount = entry.recommendations,
                        isUserAdded = isUserCreated,
                        dateString = "오늘"
                    )
                    roomDao.insertRagDocument(roomDoc)
                } else {
                    val updatedDoc = existingDoc.copy(
                        fullContent = entry.text,
                        snippet = entry.text.take(60) + "...",
                        recommendationCount = entry.recommendations,
                        isUserAdded = isUserCreated
                    )
                    roomDao.updateRagDocument(updatedDoc)
                }
            }
        }
    }

    fun clearOldDatabaseAndSyncFresh() {
        val roomDao = AppDatabase.getDatabase(context).carDiagDao()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                roomDao.clearAllHistory()
                roomDao.clearAllRagDocuments()

                val allEntries = vectorDb.getAll()
                for (entry in allEntries) {
                    entry.recommendations = 0
                    val isUserCreated = entry.id.startsWith("USER-") || entry.id.startsWith("DOC-NEW-")
                    val roomDoc = RagDocument(
                        docCode = entry.id,
                        category = if (entry.metadata.dtcCode.isNotBlank()) "DTC Guide" else "Remedy",
                        title = entry.metadata.component.ifBlank { "정비 노하우" },
                        snippet = entry.text.take(60) + "...",
                        fullContent = entry.text,
                        dtcCode = entry.metadata.dtcCode.ifBlank { null },
                        component = entry.metadata.component.ifBlank { null },
                        connectorLocation = entry.metadata.connectorLocation.ifBlank { null },
                        sourceName = if (isUserCreated) "User Added" else "Phone DB",
                        recommendationCount = 0,
                        isUserAdded = isUserCreated,
                        dateString = "오늘"
                    )
                    roomDao.insertRagDocument(roomDoc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Backend API Function Implementations ---

    fun updateScoringWeights(weights: ScoringWeights) {
        ragSearcher.updateScoringWeights(weights)
    }

    fun diagnose(query: String, weights: ScoringWeights? = null): DiagnosisBackendResponse {
        val matches = ragSearcher.search(query, topK = 10, weights = weights)  // 🔧 수정: 3 → 10 (더보기 최대 10개)
        val answer = qwenLlm.generateAnswer(query, matches)
        return DiagnosisBackendResponse(qwenAnswer = answer, rawMatches = matches)
    }

    fun recommend(docId: String): Int {
        return vectorDb.updateRecommendation(docId)
    }

    fun extractMetadata(text: String, contextQuery: String = ""): ExtractedMetadata {
        val analysisText = "$text $contextQuery"

        // 1. DTC Code regex extraction (\b([CPBU][0-9A-Z]{4,7})\b)
        var extractedDtc: String? = null
        val dtcMatcher = Pattern.compile("(?i)\\b([CPBU][0-9A-Z]{4,7})\\b").matcher(analysisText)
        if (dtcMatcher.find()) {
            extractedDtc = dtcMatcher.group(1)?.uppercase()
        }

        // 2. Component mapping lookup
        var extractedComp: String? = null
        val compMap = MappingDictionary.getComponents()
        for ((kw, valName) in compMap) {
            if (analysisText.contains(kw)) {
                extractedComp = valName
                break
            }
        }

        // 3. Location mapping lookup
        var extractedLoc: String? = null
        val locMap = MappingDictionary.getLocations()
        for ((kw, valName) in locMap) {
            if (analysisText.contains(kw)) {
                extractedLoc = valName
                break
            }
        }

        return ExtractedMetadata(
            dtcCode = extractedDtc,
            component = extractedComp,
            location = extractedLoc
        )
    }

    fun runOcr(inputText: String): OcrResult {
        return OcrEngine.processOcrText(inputText)
    }

    fun addDocument(
        text: String,
        contextQuery: String = "",
        dtcCode: String? = null,
        component: String? = null,
        connectorLocation: String? = null
    ): AddDocumentBackendResponse {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) {
            return AddDocumentBackendResponse(
                success = false,
                insertedId = null,
                message = "내용을 입력해주세요.",
                storedMetadata = VectorDbMetadata()
            )
        }

        // Check duplicate
        if (vectorDb.isDuplicate(trimmedText)) {
            return AddDocumentBackendResponse(
                success = false,
                insertedId = null,
                message = "이미 동일한 내용의 조치 방안이 DB에 존재합니다.",
                storedMetadata = VectorDbMetadata()
            )
        }

        val embedding = ragSearcher.getEmbedding(trimmedText)
        val timestamp = System.currentTimeMillis()
        val docId = "DOC-NEW-$timestamp-${UUID.randomUUID().toString().take(8)}"

        val metadata = VectorDbMetadata(
            contextQuery = contextQuery,
            dtcCode = dtcCode ?: "",
            component = component ?: "",
            connectorLocation = connectorLocation ?: "",
            dtcs = if (!dtcCode.isNullOrBlank()) listOf(dtcCode) else emptyList()
        )

        val insertedId = vectorDb.addDocument(docId, trimmedText, embedding, metadata)
        syncVectorDbToRoom()

        return AddDocumentBackendResponse(
            success = insertedId != null,
            insertedId = insertedId,
            message = "신규 조치 방안이 성공적으로 등록 및 임베딩되었습니다.",
            storedMetadata = metadata
        )
    }

    fun deleteDocument(docCode: String): Boolean {
        val deleted = vectorDb.deleteDocument(docCode)
        syncVectorDbToRoom()
        return deleted
    }

    fun updateDocument(
        docCode: String,
        text: String,
        dtcCode: String?,
        component: String?,
        connectorLocation: String?
    ): Boolean {
        val embedding = ragSearcher.getEmbedding(text)
        val metadata = VectorDbMetadata(
            contextQuery = "",
            dtcCode = dtcCode ?: "",
            component = component ?: "",
            connectorLocation = connectorLocation ?: "",
            dtcs = if (!dtcCode.isNullOrBlank()) listOf(dtcCode) else emptyList()
        )
        val updated = vectorDb.updateDocument(docCode, text, embedding, metadata)
        syncVectorDbToRoom()
        return updated
    }

    fun getDbStatus(): BackendDbStatus {
        val mappingFile = File(dbDirectory, MappingDictionary.MAPPING_FILENAME)
        return BackendDbStatus(
            dbFolderPath = dbDirectory.absolutePath,
            isQwenModelFound = qwenLlm.checkModelAvailability(),
            qwenModelPath = qwenLlm.getModelPath(),
            isVectorDbFound = vectorDbFile.exists(),
            vectorDbPath = vectorDbFile.absolutePath,
            totalVectorDocuments = vectorDb.getDocumentCount(),
            isMappingDictFound = mappingFile.exists(),
            isDocsFolderFound = docsDirectory.exists(),
            docsFolderPath = docsDirectory.absolutePath
        )
    }
}
