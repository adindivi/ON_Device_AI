package com.example.ui.viewmodel

import com.example.backend.OnDeviceBackendEngine
import com.example.data.local.DiagnosticHistory

/**
 * UseCase encapsulating the RAG diagnosis business logic.
 * Takes user inputs (DTC and symptoms), invokes backend RAG engine, and constructs DiagnosticHistory report.
 */
class DiagnoseSymptomUseCase(
    private val backendEngine: OnDeviceBackendEngine
) {

    data class DiagnosisResult(
        val history: DiagnosticHistory,
        val matches: List<com.example.backend.SearchResult>
    )

    suspend fun execute(queryDtc: String, querySymptom: String): DiagnosisResult {
        val fullQuery = "$queryDtc $querySymptom".trim()
        val backendResponse = backendEngine.diagnose(fullQuery)
        val allMatches = backendResponse.rawMatches.take(10)  // 🔧 수정: 더보기 최대 10개용
        val topMatches = allMatches.take(3)                   // 추천 카드 Top 3는 별도 유지
        val topMatch = topMatches.firstOrNull()

        val meta = backendEngine.extractMetadata(querySymptom, queryDtc)
        val titleText = if (queryDtc.isNotBlank()) "DTC $queryDtc 코드 및 증상 분석" else "차량 증상 AI 점검 결과"

        val summaryText = if (topMatches.isNotEmpty()) {
            val compList = topMatches.mapIndexed { idx, m ->
                "${idx + 1}순위: ${m.metadata.component.ifBlank { "관련 부품" }}"
            }.joinToString(" | ")
            "Top 3 추천 원인: $compList"
        } else {
            "분석 결과: Vector DB에서 유사도가 일치하는 정비 문서를 찾을 수 없습니다."
        }

        val checksText = if (topMatches.isNotEmpty()) {
            topMatches.mapIndexed { idx, m ->
                val comp = m.metadata.component.ifBlank { "관련 부품" }
                val loc = m.metadata.connectorLocation.ifBlank { "하네스 커넥터" }
                "${idx + 1}순위($comp): $loc 배선 및 핀 텐션 점검"
            }.joinToString("|")
        } else {
            "관련 부품 커넥터 핀 텐션 확인|퓨즈 박스 전원 공급 전압 테스트|스캐너 고장코드 이력 삭제"
        }

        // 🤖 고정 템플릿 삭제 -> Qwen2.5 8비트 온디바이스 LLM의 실시간 자유 생성 답변 바인딩
        val solutionText = backendResponse.qwenAnswer

        val warningText = if (topMatch != null) {
            val comp = topMatch.metadata.component.ifBlank { "해당 부품" }
            val dtc = topMatch.metadata.dtcCode
            if (dtc.isNotBlank()) {
                "DTC $dtc 고장 방치 시 연관 제어기 및 차체 구동계 2차 손상의 원인이 됩니다."
            } else {
                "$comp 고장 방치 시 주행 중 안전 차단 및 과부하 위험이 있으므로 즉시 점검을 권장합니다."
            }
        } else {
            "주행 중 안전 시스템 차단 가능성이 있으므로 즉시 점검을 권장합니다."
        }

        val newHistory = DiagnosticHistory(
            title = titleText,
            dtcCode = queryDtc.ifBlank { meta.dtcCode ?: "DIAG" },
            symptomText = querySymptom,
            summary = summaryText,
            fullAnalysis = backendResponse.qwenAnswer,
            checksListJson = checksText,
            solutionText = solutionText,
            warningText = warningText,
            timestamp = System.currentTimeMillis(),
            statusType = "WARNING"
        )

        return DiagnosisResult(
            history = newHistory,
            matches = allMatches  // 🔧 수정: 최대 10개 반환 (더보기 버튼 지원)
        )
    }
}
