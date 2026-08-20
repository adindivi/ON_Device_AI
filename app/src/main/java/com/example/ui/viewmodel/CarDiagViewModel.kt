package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backend.BackendDbStatus
import com.example.backend.OnDeviceBackendEngine
import com.example.data.local.AppDatabase
import com.example.data.local.DiagnosticHistory
import com.example.data.local.RagDocument
import com.example.data.repository.CarDiagRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class VehicleInfo(
    val name: String = "Tesla Model 3",
    val vin: String = "5YJ3E...892",
    val statusText: String = "정상",
    val lastCheckTime: String = "10.24 14:30",
    val batteryPercent: Int = 88,
    val oilStatus: String = "7.2k 남음",
    val tireStatus: String = "적정",
    val isConnected: Boolean = true
)

data class ExtractedMetadata(
    val dtcCode: String? = null,
    val component: String? = null,
    val location: String? = null
)

class CarDiagViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CarDiagRepository
    val backendEngine: OnDeviceBackendEngine = OnDeviceBackendEngine(application)

    val historyList: StateFlow<List<DiagnosticHistory>>
    val ragDocuments: StateFlow<List<RagDocument>>
    val userManualDocuments: StateFlow<List<RagDocument>>

    private val _backendDbStatus = MutableStateFlow<BackendDbStatus>(backendEngine.getDbStatus())
    val backendDbStatus: StateFlow<BackendDbStatus> = _backendDbStatus.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = CarDiagRepository(db.carDiagDao())

        historyList = repository.allHistory.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        val sampleCodes = setOf("DOC-DEFAULT-1", "DOC-DEFAULT-2", "DOC-DEFAULT-3", "TSB-03-15", "CASE-11-02", "DIAG-C1206")

        ragDocuments = repository.allRagDocuments.map { list ->
            val hasRealDocs = list.any { it.docCode !in sampleCodes }
            if (hasRealDocs) {
                list.filter { it.docCode !in sampleCodes }
            } else {
                list
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userManualDocuments = repository.userManualDocuments.map { list ->
            val hasRealDocs = list.any { it.docCode !in sampleCodes }
            if (hasRealDocs) {
                list.filter { it.docCode !in sampleCodes }
            } else {
                list
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Start watching the phone "DB/rag_documents" directory
        backendEngine.startWatcher(viewModelScope)
        refreshBackendStatus()
    }

    fun refreshBackendStatus() {
        _backendDbStatus.value = backendEngine.getDbStatus()
    }

    // Vehicle State
    private val _vehicle = MutableStateFlow(VehicleInfo())
    val vehicle: StateFlow<VehicleInfo> = _vehicle.asStateFlow()

    // Dynamic Text Size Scale
    private val _textSizeScale = MutableStateFlow(1.0f)
    val textSizeScale: StateFlow<Float> = _textSizeScale.asStateFlow()

    // Diagnostic Workbench State
    private val _dtcInput = MutableStateFlow("")
    val dtcInput: StateFlow<String> = _dtcInput.asStateFlow()

    private val _symptomInput = MutableStateFlow("")
    val symptomInput: StateFlow<String> = _symptomInput.asStateFlow()

    private val _isDiagnosing = MutableStateFlow(false)
    val isDiagnosing: StateFlow<Boolean> = _isDiagnosing.asStateFlow()

    private val _diagnosisStep = MutableStateFlow(0) // 0 = Idle, 1 = Plan, 2 = Search, 3 = Reasoning, 4 = Complete
    val diagnosisStep: StateFlow<Int> = _diagnosisStep.asStateFlow()

    private val _activeResult = MutableStateFlow<DiagnosticHistory?>(null)
    val activeResult: StateFlow<DiagnosticHistory?> = _activeResult.asStateFlow()

    private val _activeResultMatches = MutableStateFlow<List<com.example.backend.SearchResult>>(emptyList())
    val activeResultMatches: StateFlow<List<com.example.backend.SearchResult>> = _activeResultMatches.asStateFlow()

    private val _isGuideExpanded = MutableStateFlow(true)
    val isGuideExpanded: StateFlow<Boolean> = _isGuideExpanded.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Scoring Weights & Admin Modals
    private val _scoringWeights = MutableStateFlow(com.example.backend.ScoringWeights())
    val scoringWeights: StateFlow<com.example.backend.ScoringWeights> = _scoringWeights.asStateFlow()

    private val _showPasswordModal = MutableStateFlow(false)
    val showPasswordModal: StateFlow<Boolean> = _showPasswordModal.asStateFlow()

    private val _showWeightSettingsModal = MutableStateFlow(false)
    val showWeightSettingsModal: StateFlow<Boolean> = _showWeightSettingsModal.asStateFlow()

    fun openPasswordModal() {
        _showPasswordModal.value = true
    }

    fun closePasswordModal() {
        _showPasswordModal.value = false
    }

    fun verifyPassword(inputPin: String): Boolean {
        return if (inputPin == "1234") {
            _showPasswordModal.value = false
            _showWeightSettingsModal.value = true
            showToast("🔑 관리자 인증이 완료되었습니다.")
            true
        } else {
            showToast("🚫 비밀번호를 다시 확인해주세요.")
            false
        }
    }

    fun closeWeightSettingsModal() {
        _showWeightSettingsModal.value = false
    }

    fun updateScoringWeights(newWeights: com.example.backend.ScoringWeights) {
        _scoringWeights.value = newWeights
        backendEngine.updateScoringWeights(newWeights)
        _showWeightSettingsModal.value = false
        showToast("⚙️ 분석 가중치 설정이 적용되었습니다.")
    }

    fun resetScoringWeights() {
        val defaultWeights = com.example.backend.ScoringWeights()
        _scoringWeights.value = defaultWeights
        backendEngine.updateScoringWeights(defaultWeights)
        showToast("↺ 기본 가중치로 설정되었습니다.")
    }

    // Modal/Dialog Control States
    private val _showScannerModal = MutableStateFlow(false)
    val showScannerModal: StateFlow<Boolean> = _showScannerModal.asStateFlow()

    private val _showAddRemedyModal = MutableStateFlow(false)
    val showAddRemedyModal: StateFlow<Boolean> = _showAddRemedyModal.asStateFlow()

    private val _showContributionDashboardModal = MutableStateFlow(false)
    val showContributionDashboardModal: StateFlow<Boolean> = _showContributionDashboardModal.asStateFlow()

    private val _editingDocument = MutableStateFlow<RagDocument?>(null)
    val editingDocument: StateFlow<RagDocument?> = _editingDocument.asStateFlow()

    private val _selectedDetailHistory = MutableStateFlow<DiagnosticHistory?>(null)
    val selectedDetailHistory: StateFlow<DiagnosticHistory?> = _selectedDetailHistory.asStateFlow()

    // Text Size Control
    fun changeTextSize(delta: Float) {
        val newScale = (_textSizeScale.value + delta).coerceIn(0.85f, 1.35f)
        _textSizeScale.value = newScale
        val percentage = (newScale * 100).toInt()
        showToast("글자 크기: ${percentage}%")
    }

    fun updateDtcInput(text: String) {
        _dtcInput.value = text
    }

    fun updateSymptomInput(text: String) {
        _symptomInput.value = text
    }

    fun addSymptomChip(chip: String) {
        val current = _symptomInput.value
        if (current.isBlank()) {
            _symptomInput.value = chip
        } else if (!current.contains(chip)) {
            _symptomInput.value = "$current, $chip"
        }
    }

    fun toggleGuideExpanded() {
        _isGuideExpanded.value = !_isGuideExpanded.value
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun setScannerModalVisible(visible: Boolean) {
        _showScannerModal.value = visible
    }

    fun setAddRemedyModalVisible(visible: Boolean) {
        _showAddRemedyModal.value = visible
    }

    fun setContributionDashboardModalVisible(visible: Boolean) {
        _showContributionDashboardModal.value = visible
    }

    fun setEditingDocument(doc: RagDocument?) {
        _editingDocument.value = doc
    }

    fun setSelectedDetailHistory(history: DiagnosticHistory?) {
        _selectedDetailHistory.value = history
    }

    private val diagnoseUseCase = DiagnoseSymptomUseCase(backendEngine)

    // Start On-Device AI Diagnosis Engine
    fun startDiagnosis() {
        val queryDtc = _dtcInput.value.trim().uppercase()
        val querySymptom = _symptomInput.value.trim()

        if (queryDtc.isBlank() && querySymptom.isBlank()) {
            showToast("증상 또는 고장 코드를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _isDiagnosing.value = true
            _activeResult.value = null

            // Step 1: 계획 수립 (Plan)
            _diagnosisStep.value = 1
            delay(500)

            // Step 2: 벡터 DB RAG 검색 (rag_vector_database.json)
            _diagnosisStep.value = 2
            delay(600)

            // Step 3: 온디바이스 Qwen-2.5 LLM 원인 분석
            _diagnosisStep.value = 3
            delay(800)

            // Step 4: 종합 진단 보고서 작성
            _diagnosisStep.value = 4
            delay(400)

            val diagnosisResult = diagnoseUseCase.execute(queryDtc, querySymptom)
            val newHistory = diagnosisResult.history
            _activeResultMatches.value = diagnosisResult.matches

            // Save to DB
            repository.insertHistory(newHistory)

            _activeResult.value = newHistory
            _isDiagnosing.value = false
            _isGuideExpanded.value = true
            refreshBackendStatus()
            showToast("✅ 스마트 정비 진단서가 작성되었습니다.")
        }
    }

    fun incrementRecommendation(docId: Long, docCode: String? = null) {
        viewModelScope.launch {
            repository.incrementRecommendation(docId)
            if (!docCode.isNullOrBlank()) {
                backendEngine.recommend(docCode)
            }
            // Instant UI state update for immediate feedback
            _activeResultMatches.value = _activeResultMatches.value.map { match ->
                if (match.id == docCode || match.id == docId.toString()) {
                    match.copy(recommendations = match.recommendations + 1)
                } else {
                    match
                }
            }
            refreshBackendStatus()
            showToast("💙 유용한 정보로 추천되었습니다.")
        }
    }

    fun extractMetadata(text: String): ExtractedMetadata {
        return backendEngine.extractMetadata(text)
    }

    fun addNewRemedy(
        text: String,
        dtcCode: String?,
        component: String?,
        location: String?
    ) {
        viewModelScope.launch {
            val response = backendEngine.addDocument(
                text = text,
                contextQuery = _symptomInput.value,
                dtcCode = dtcCode,
                component = component,
                connectorLocation = location
            )

            if (response.success) {
                val newDoc = RagDocument(
                    docCode = response.insertedId ?: "USER-${System.currentTimeMillis() % 10000}",
                    category = "User Remedy",
                    title = if (!component.isNullOrBlank()) "$component 점검 조치 노하우" else "현장 정비 조치 방안",
                    snippet = text.take(60) + "...",
                    fullContent = text,
                    dtcCode = dtcCode?.ifBlank { null },
                    component = component?.ifBlank { null },
                    connectorLocation = location?.ifBlank { null },
                    sourceName = "오프라인 DB",
                    recommendationCount = 1,
                    isUserAdded = true,
                    dateString = "오늘"
                )
                repository.insertRagDocument(newDoc)
                _showAddRemedyModal.value = false
                refreshBackendStatus()
                showToast("📁 정비 노하우가 오프라인 DB에 저장되었습니다.")
            } else {
                showToast("⚠️ ${response.message}")
            }
        }
    }

    fun updateRemedy(doc: RagDocument) {
        viewModelScope.launch {
            repository.updateRagDocument(doc)
            backendEngine.updateDocument(
                docCode = doc.docCode,
                text = doc.fullContent,
                dtcCode = doc.dtcCode,
                component = doc.component,
                connectorLocation = doc.connectorLocation
            )
            _editingDocument.value = null
            refreshBackendStatus()
            showToast("✏️ 지식 정보가 성공적으로 수정되었습니다.")
        }
    }

    fun deleteRemedy(id: Long) {
        viewModelScope.launch {
            val doc = repository.getRagDocumentById(id)
            if (doc != null) {
                backendEngine.deleteDocument(doc.docCode)
            }
            repository.deleteRagDocument(id)
            _editingDocument.value = null
            refreshBackendStatus()
            showToast("🗑️ 선택한 지식이 삭제되었습니다.")
        }
    }

    fun applyOcrCode(code: String) {
        val ocrResult = backendEngine.runOcr(code)
        val extractedDtc = ocrResult.dtcCodes.firstOrNull() ?: code
        _dtcInput.value = extractedDtc
        _showScannerModal.value = false
        showToast("📷 OCR 판독 완료: DTC $extractedDtc 코드 입력됨")
    }
}
