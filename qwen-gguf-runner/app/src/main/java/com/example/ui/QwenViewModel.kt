package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.LlamaMobileEngine
import com.example.engine.SandboxModelManager
import com.example.engine.StreamTokenEvent
import com.example.model.ChatMessage
import com.example.model.GgufMetadata
import com.example.model.GgufQuantizationType
import com.example.model.GpuAccelerationBackend
import com.example.model.MemoryPolicy
import com.example.model.MessageSender
import com.example.model.ProfilingLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class QwenUiState(
    val loadedModel: GgufMetadata? = null,
    val modelFileName: String? = null,
    val isLoadingModel: Boolean = false,
    val loadingProgress: Float = 0f,
    val isStreaming: Boolean = false,
    val statusMessage: String = "GGUF 모델 파일을 로드하세요",
    val isErrorState: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val sandboxModels: List<File> = emptyList(),
    val profilingLogs: List<ProfilingLog> = emptyList(),
    val latestProfilingLog: ProfilingLog? = null,
    val showProfilingModal: Boolean = false,
    val showSettingsAccordion: Boolean = false,
    val memoryPolicy: MemoryPolicy? = null,
    val liveTps: Double = 0.0,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
    val maxTokens: Int = 1024,
    val threads: Int = 4,
    val gpuBackend: GpuAccelerationBackend = GpuAccelerationBackend.VULKAN,
    val gpuLayers: Int = 24,
    val systemPrompt: String = "You are Qwen, a helpful, accurate, and lightning-fast on-device AI assistant.",
    val errorMessage: String? = null
)

class QwenViewModel(application: Application) : AndroidViewModel(application) {

    private val sandboxManager = SandboxModelManager(application)
    private val engine = LlamaMobileEngine(sandboxManager)

    private val _uiState = MutableStateFlow(QwenUiState())
    val uiState: StateFlow<QwenUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        refreshSandboxModels()
        loadInitialPresetIfAvailable()
        loadProfilingLogs()
        updateMemoryPolicy()
    }

    private fun updateMemoryPolicy() {
        val quant = _uiState.value.loadedModel?.quantizationType ?: GgufQuantizationType.Q4_K_M
        val policy = sandboxManager.getDeviceMemoryPolicy(quant)
        _uiState.update { it.copy(memoryPolicy = policy, threads = engine.threadCount, gpuLayers = engine.gpuLayers) }
    }

    fun refreshSandboxModels() {
        val models = sandboxManager.listSandboxModels()
        _uiState.update { it.copy(sandboxModels = models) }
    }

    private fun loadInitialPresetIfAvailable() {
        viewModelScope.launch {
            val models = sandboxManager.listSandboxModels()
            if (models.isNotEmpty()) {
                loadModelFromFile(models.first())
            } else {
                // Auto create standard Qwen sandbox preset so app is immediately usable
                val preset = sandboxManager.createPresetQwenModel()
                refreshSandboxModels()
                loadModelFromFile(preset)
            }
        }
    }

    fun importGgufUri(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingModel = true,
                    loadingProgress = 0f,
                    statusMessage = "샌드박스 디렉토리로 GGUF 복사 중...",
                    isErrorState = false,
                    errorMessage = null
                )
            }

            val copyResult = sandboxManager.importGgufToSandbox(uri, fileName) { progress ->
                _uiState.update { it.copy(loadingProgress = progress) }
            }

            if (copyResult.isFailure) {
                val err = "실패: ${copyResult.exceptionOrNull()?.message ?: "파일 복사 실패"}"
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        statusMessage = err,
                        isErrorState = true,
                        errorMessage = err
                    )
                }
                return@launch
            }

            val copiedFile = copyResult.getOrThrow()
            refreshSandboxModels()
            loadModelFromFile(copiedFile)
        }
    }

    fun loadModelFromFile(file: File) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoadingModel = true,
                    loadingProgress = 1f,
                    statusMessage = "llama.cpp 엔진 초기화 및 VRAM 할당 중...",
                    isErrorState = false,
                    errorMessage = null
                )
            }

            val result = engine.loadModel(file)
            if (result.isFailure) {
                val err = result.exceptionOrNull()?.message ?: "실패: 모델 로딩 중 오류가 발생했습니다"
                _uiState.update {
                    it.copy(
                        isLoadingModel = false,
                        loadedModel = null,
                        modelFileName = file.name,
                        statusMessage = err,
                        isErrorState = true,
                        errorMessage = err
                    )
                }
                loadProfilingLogs()
                return@launch
            }

            val metadata = result.getOrThrow()
            updateMemoryPolicy()
            loadProfilingLogs()

            _uiState.update {
                it.copy(
                    isLoadingModel = false,
                    loadedModel = metadata,
                    modelFileName = file.name,
                    statusMessage = "준비 완료 (${metadata.toSummary()})",
                    isErrorState = false,
                    errorMessage = null,
                    gpuLayers = engine.gpuLayers
                )
            }
        }
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value.isStreaming) return

        if (!engine.isModelLoaded()) {
            val err = "실패: 모델이 로드되지 않았습니다. GGUF 파일을 먼저 로드해 주세요."
            _uiState.update {
                it.copy(
                    statusMessage = err,
                    isErrorState = true,
                    errorMessage = err
                )
            }
            return
        }

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            content = userText.trim()
        )

        val assistantMessageId = java.util.UUID.randomUUID().toString()
        val initialAssistantMessage = ChatMessage(
            id = assistantMessageId,
            sender = MessageSender.ASSISTANT,
            content = "",
            isStreaming = true
        )

        val currentMessages = _uiState.value.messages
        val updatedMessages = currentMessages + userMessage + initialAssistantMessage

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                isStreaming = true,
                statusMessage = "토큰 스트리밍 생성 중...",
                isErrorState = false
            )
        }

        streamingJob = viewModelScope.launch {
            engine.streamChatCompletion(userText, currentMessages).collect { event ->
                when (event) {
                    is StreamTokenEvent.Token -> {
                        _uiState.update { state ->
                            val newMsgs = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(
                                        content = event.accumulatedText,
                                        tokensGenerated = event.tokensGenerated,
                                        tokensPerSecond = event.tokensPerSecond
                                    )
                                } else msg
                            }
                            state.copy(
                                messages = newMsgs,
                                liveTps = event.tokensPerSecond
                            )
                        }
                    }

                    is StreamTokenEvent.Completed -> {
                        _uiState.update { state ->
                            val newMsgs = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(
                                        content = event.fullText,
                                        isStreaming = false,
                                        tokensGenerated = event.totalTokens,
                                        tokensPerSecond = event.tokensPerSecond
                                    )
                                } else msg
                            }
                            state.copy(
                                messages = newMsgs,
                                isStreaming = false,
                                liveTps = event.tokensPerSecond,
                                latestProfilingLog = event.profilingLog,
                                statusMessage = "추론 완료 (${event.totalTokens} 토큰, ${String.format("%.1f", event.tokensPerSecond)} T/s)"
                            )
                        }
                        loadProfilingLogs()
                    }

                    is StreamTokenEvent.Error -> {
                        _uiState.update { state ->
                            val newMsgs = state.messages.map { msg ->
                                if (msg.id == assistantMessageId) {
                                    msg.copy(
                                        content = event.message,
                                        isStreaming = false,
                                        isFailure = true,
                                        errorMessage = event.message
                                    )
                                } else msg
                            }
                            state.copy(
                                messages = newMsgs,
                                isStreaming = false,
                                statusMessage = event.message,
                                isErrorState = true
                            )
                        }
                        loadProfilingLogs()
                    }
                }
            }
        }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        _uiState.update { state ->
            val newMsgs = state.messages.map { msg ->
                if (msg.isStreaming) msg.copy(isStreaming = false) else msg
            }
            state.copy(
                messages = newMsgs,
                isStreaming = false,
                statusMessage = "사용자에 의해 생성이 중단되었습니다"
            )
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList(), statusMessage = "대화 기록이 초기화되었습니다") }
    }

    fun updateParameters(
        temperature: Float = _uiState.value.temperature,
        topP: Float = _uiState.value.topP,
        maxTokens: Int = _uiState.value.maxTokens,
        threads: Int = _uiState.value.threads,
        gpuBackend: GpuAccelerationBackend = _uiState.value.gpuBackend,
        gpuLayers: Int = _uiState.value.gpuLayers,
        systemPrompt: String = _uiState.value.systemPrompt
    ) {
        engine.temperature = temperature
        engine.topP = topP
        engine.maxTokens = maxTokens
        engine.updateThreadCount(threads)
        engine.accelerationBackend = gpuBackend
        engine.gpuLayers = gpuLayers
        engine.systemPrompt = systemPrompt

        _uiState.update {
            it.copy(
                temperature = temperature,
                topP = topP,
                maxTokens = maxTokens,
                threads = threads,
                gpuBackend = gpuBackend,
                gpuLayers = gpuLayers,
                systemPrompt = systemPrompt
            )
        }
    }

    fun toggleSettingsAccordion() {
        _uiState.update { it.copy(showSettingsAccordion = !it.showSettingsAccordion) }
    }

    fun setProfilingModalVisible(visible: Boolean) {
        _uiState.update { it.copy(showProfilingModal = visible) }
    }

    fun dismissErrorMessage() {
        _uiState.update { it.copy(errorMessage = null, isErrorState = false) }
    }

    fun loadProfilingLogs() {
        viewModelScope.launch {
            val logs = sandboxManager.readProfilingLogs()
            _uiState.update {
                it.copy(
                    profilingLogs = logs,
                    latestProfilingLog = logs.firstOrNull() ?: it.latestProfilingLog
                )
            }
        }
    }

    fun clearProfilingLogs() {
        viewModelScope.launch {
            sandboxManager.clearProfilingLogs()
            _uiState.update { it.copy(profilingLogs = emptyList(), latestProfilingLog = null) }
        }
    }

    fun deleteSandboxModel(file: File) {
        viewModelScope.launch {
            sandboxManager.deleteModel(file)
            refreshSandboxModels()
            if (_uiState.value.modelFileName == file.name) {
                _uiState.update {
                    it.copy(
                        loadedModel = null,
                        modelFileName = null,
                        statusMessage = "모델이 삭제되었습니다. 새 GGUF를 로드하세요"
                    )
                }
            }
        }
    }
}
