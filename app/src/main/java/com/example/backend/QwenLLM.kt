package com.example.backend

import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.GlobalScope

class QwenLLM(private val context: android.content.Context, private val modelFile: File) {

    private val lock = ReentrantLock()
    private val llamaBridge = LlamaCppBridge(context)

    var isModelAvailable: Boolean = false
        private set

    private var loadedFromUri: Boolean = false
    private var customModelPath: String? = null

    init {
        checkModelAvailability()
        if (isModelAvailable) {
            // LlamaCppBridge is suspend-based, so we load it lazily or start a coroutine
            GlobalScope.launch {
                llamaBridge.loadModel(modelFile)
            }
        }
    }

    fun loadFromUri(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit = {}) {
        // 백그라운드 스레드에서 파일 복사 진행 (안드로이드 11+ NDK mmap 보안 우회)
        Thread {
            try {
                val internalFile = File(context.filesDir, "qwen_model.gguf")
                
                // 파일이 이미 존재하고 크기가 충분히 크다면(예: 1GB 이상) 복사 생략
                var needCopy = true
                if (internalFile.exists() && internalFile.length() > 1000000000L) {
                    needCopy = false
                }

                if (needCopy) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        internalFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                if (internalFile.exists() && internalFile.length() > 0) {
                    val realPath = internalFile.absolutePath
                    
                    // 기성품 엔진(LlamaAndroid) 모델 초기화
                    val result = kotlinx.coroutines.runBlocking {
                        llamaBridge.loadModel(internalFile)
                    }
                    
                    if (result.isSuccess) {
                        loadedFromUri = true
                        isModelAvailable = true
                        customModelPath = "Copied to Internal: $realPath"
                        onComplete(true)
                    } else {
                        loadedFromUri = false
                        isModelAvailable = false
                        onComplete(false)
                    }
                } else {
                    onComplete(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadedFromUri = false
                isModelAvailable = false
                onComplete(false)
            }
        }.start()
    }

    fun checkModelAvailability(): Boolean {
        if (loadedFromUri) {
            isModelAvailable = true
            return true
        }
        isModelAvailable = modelFile.exists() && modelFile.length() > 0
        return isModelAvailable
    }

    fun getModelPath(): String = customModelPath ?: modelFile.absolutePath

    fun generateAnswerStream(query: String, contextDocs: List<SearchResult>): Flow<String> {
        val topMatch = contextDocs.firstOrNull()
        val compName = topMatch?.metadata?.component?.ifBlank { "관련 부품" } ?: "관련 부품"
        val dtcCode = topMatch?.metadata?.dtcCode?.ifBlank { "" } ?: ""
        val dtcStr = if (dtcCode.isNotBlank()) " ($dtcCode)" else ""
        val connLoc = topMatch?.metadata?.connectorLocation?.trim()?.ifBlank { "" } ?: ""
        val connStr = if (connLoc.isNotBlank()) " [$connLoc 위치]" else ""

        val dbAction = if (topMatch != null) {
            val fullText = topMatch.text
            when {
                fullText.contains("[조치사항]") -> fullText.substringAfter("[조치사항]").substringBefore("[").replace("\n", " ").trim()
                fullText.contains("[조치]") -> fullText.substringAfter("[조치]").substringBefore("[").replace("\n", " ").trim()
                else -> fullText.take(90).replace("\n", " ").trim()
            }
        } else {
            "관련 센서 단자 세척 및 전원/배선 텐션을 재점검하십시오."
        }

        val personaResponse = "🤖 \"정비사님, [$compName$dtcStr$connStr] 점검 가이드입니다.\n$dbAction\""

        // 큐웬 2.5 채팅 템플릿 형식 (AI가 이어서 답변하도록 유도)
        val chatPrompt = "<|im_start|>system\n" +
                "당신은 자동차 정비 전문가 AI입니다. 정비사에게 실용적인 조언을 한국어로 제공하세요.<|im_end|>\n" +
                "<|im_start|>user\n" +
                "다음 차량 고장 정보를 분석하고 정비 조치 방안을 알려주세요:\n" +
                "증상: $query\n" +
                "관련 부품: $compName $dtcStr\n" +
                "DB 조치사항: $dbAction<|im_end|>\n" +
                "<|im_start|>assistant\n"

        return flow {
            emit(personaResponse + "\n\n📝 AI 추가 분석:\n")
            try {
                llamaBridge.streamInference(chatPrompt).collect { token ->
                    emit(token)
                }
            } catch (e: Exception) {
                emit("\n[에러: ${e.message}]")
            }
        }
    }

    fun release() {
        llamaBridge.close()
    }
}
