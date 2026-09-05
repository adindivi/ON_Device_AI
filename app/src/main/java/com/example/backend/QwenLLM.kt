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
                
                val uriLength = try {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { it.statSize } ?: -1L
                } catch (_: Exception) { -1L }

                val needCopy = !internalFile.exists() || (uriLength > 0L && internalFile.length() != uriLength) || internalFile.length() < 100_000_000L

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
                        android.util.Log.i("QwenLLM", "✅ [Qwen 모델 로드 성공] 경로: $realPath, 크기: ${internalFile.length()} bytes")
                        true
                    } else {
                        android.util.Log.e("QwenLLM", "❌ [Qwen 모델 로드 실패] loadModel 결과: $result")
                        false
                    }
                } else {
                    false
                }
            } catch (e: Exception) {
                android.util.Log.e("QwenLLM", "❌ [Qwen 모델 복사/로드 실패] URI: $uri, 예외: ${e.message}", e)
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

        // 큐웬 2.5 공식 RAG 최적화 프롬프트 템플릿 (XML 태그 격리 및 3단계 체크리스트 유도)
        val assistantPrefix = "1. 외관 점검: "
        val candidatesInfo = if (contextDocs.size > 1) {
            val others = contextDocs.drop(1).take(2).mapIndexed { idx, doc ->
                val c = doc.metadata.component.ifBlank { "부품" }
                val d = if (doc.metadata.dtcCode.isNotBlank()) " (${doc.metadata.dtcCode})" else ""
                "- 연관 후보 ${idx + 2}: $c$d"
            }.joinToString("\n")
            "\n$others"
        } else {
            ""
        }

        val chatPrompt = "<|im_start|>system\n" +
                "당신은 차량 정비 현장 지침을 요약 전달하는 테크니컬 어시스턴트입니다.\n" +
                "아래 규칙을 엄격히 준수하십시오:\n" +
                "1. 인사말, 서론, 맺음말은 일절 출력하지 않습니다.\n" +
                "2. 반드시 제공된 <context> 내의 공식 정비 정보에만 근거하여 작성하십시오.\n" +
                "3. 한국어로 전문적이고 간결한 체크리스트 형식으로 작성하십시오.<|im_end|>\n" +
                "<|im_start|>user\n" +
                "<context>\n" +
                "- 고장 코드: ${dtcCode.ifBlank { "해당 없음" }}\n" +
                "- 대상 부품: $compName$connStr\n" +
                "- 공식 지침서: $dbAction\n" +
                "- 입력 증상: $query$candidatesInfo\n" +
                "</context>\n\n" +
                "<instruction>\n" +
                "위 <context>를 바탕으로 정비사가 현장에서 즉시 점검할 3단계 조치 절차를 작성하십시오.\n" +
                "- 1단계: 외관/배선 점검\n" +
                "- 2단계: 측정/신호 점검\n" +
                "- 3단계: 부품 조치 기준\n" +
                "각 단계는 1문장 이내로 핵심만 작성하십시오.\n" +
                "</instruction><|im_end|>\n" +
                "<|im_start|>assistant\n" +
                assistantPrefix

        return flow {
            emit(personaResponse + "\n\n📝 AI 현장 점검 3단계:\n" + assistantPrefix)
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
