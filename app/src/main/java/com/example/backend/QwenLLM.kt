package com.example.backend

import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class QwenLLM(private val modelFile: File) {

    private val lock = ReentrantLock()
    private val llamaBridge = LlamaNapiBridge()

    var isModelAvailable: Boolean = false
        private set

    init {
        checkModelAvailability()
        if (isModelAvailable) {
            llamaBridge.initModel(modelFile.absolutePath)
        }
    }

    fun checkModelAvailability(): Boolean {
        isModelAvailable = modelFile.exists() && modelFile.length() > 0
        return isModelAvailable
    }

    fun getModelPath(): String = modelFile.absolutePath

    fun generateAnswer(query: String, contextDocs: List<SearchResult>): String {
        return lock.withLock {
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

            // 🤖 100% DB 지침 1:1 동적 바인딩 + 정비사 페르소나 톤만 부여 (고정 템플릿 문구 완전 제거)
            val personaResponse = "🤖 \"정비사님, [$compName$dtcStr$connStr] 점검 가이드입니다.\n$dbAction\""

            // Execute Native C++ JNI Bridge
            val nativeRes = llamaBridge.generateResponse(personaResponse, 512)
            if (nativeRes.isNotBlank()) {
                nativeRes
            } else {
                personaResponse
            }
        }
    }

    fun release() {
        llamaBridge.release()
    }
}
