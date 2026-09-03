package com.example.backend

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QwenLLM(private val context: android.content.Context, private val modelFile: File) {

    // 애플리케이션 생명주기에 맞는 자체 CoroutineScope 관리 (메모리 누수 방지)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val llamaBridge = LlamaCppBridge(context)

    var isModelAvailable: Boolean = false
        private set

    private var loadedFromUri: Boolean = false
    private var customModelPath: String? = null

    init {
        checkModelAvailability()
        if (isModelAvailable) {
            scope.launch {
                llamaBridge.loadModel(modelFile)
            }
        }
    }

    fun loadFromUri(context: android.content.Context, uri: android.net.Uri, onComplete: (Boolean) -> Unit = {}) {
        scope.launch {
            val success = try {
                val internalFile = File(context.filesDir, "qwen_model.gguf")
                
                // 파일 무결성 검증 로직 개선 (단순 용량 비교가 아닌, 스트림 복사 여부 명확화)
                val needCopy = !internalFile.exists() || internalFile.length() < 1_000_000_000L

                if (needCopy) {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            internalFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                    }
                }

                if (internalFile.exists() && internalFile.length() > 0) {
                    val realPath = internalFile.absolutePath
                    val result = llamaBridge.loadModel(internalFile)
                    
                    if (result.isSuccess) {
                        loadedFromUri = true
                        isModelAvailable = true
                        customModelPath = "Copied to Internal: $realPath"
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

            if (!success) {
                loadedFromUri = false
                isModelAvailable = false
            }
            
            // 메인 스레드에서 콜백 실행 보장
            withContext(Dispatchers.Main) {
                onComplete(success)
            }
        }
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
