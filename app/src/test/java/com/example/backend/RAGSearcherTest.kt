package com.example.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Clean Code Unit Tests for RAGSearcher & Reciprocal Rank Fusion (RRF).
 * 
 * Verifies:
 * 1. DTC Exact Match Priority (Tier 1: score >= 10.0, confidence >= 98%)
 * 2. DTC Fuzzy Typo Match Priority (Tier 2: score >= 5.0, confidence >= 90%)
 * 3. Zero-Score Keyword Document Exclusion from Keyword Track RRF
 * 4. Natural Language Hybrid RRF Ranking without DTC
 */
class RAGSearcherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dbFile: File
    private lateinit var vectorDb: SimpleVectorDB
    private lateinit var ragSearcher: RAGSearcher

    @Before
    fun setUp() {
        dbFile = tempFolder.newFile("test_rag_vector_db.json")
        vectorDb = SimpleVectorDB(dbFile)
        ragSearcher = RAGSearcher(vectorDb = vectorDb, onnxBertEngine = null)
    }

    private fun createEntry(
        id: String,
        text: String,
        dtcCode: String = "",
        component: String = "",
        recommendations: Int = 0,
        embedding: List<Float> = List(RAGSearcher.VECTOR_DIMENSION) { 0.01f }
    ): VectorDbEntry {
        return VectorDbEntry(
            id = id,
            text = text,
            embedding = embedding,
            metadata = VectorDbMetadata(
                dtcCode = dtcCode,
                component = component,
                dtcs = if (dtcCode.isNotBlank()) listOf(dtcCode) else emptyList()
            ),
            recommendations = recommendations
        )
    }

    @Test
    fun `search - DTC 정확 일치 시 무관한 고추천 문서를 제치고 RRF 최우선 1위 보장`() {
        // Given: DTC 일치 문서와 추천수가 많아 기존 RRF에서 1위를 가로채던 다른 문서 준비
        val targetEntry = createEntry(
            id = "DOC-TARGET-DTC",
            text = "[증상] AAF1과 통신이 끊겨 냉방이 안 돼요 [조치] 스캐너를 통해 P05C08C 고장 코드를 확인하세요.",
            dtcCode = "P05C08C",
            component = "AAF1 LIN",
            recommendations = 0
        )
        val popularIrrelevantEntry = createEntry(
            id = "DOC-POPULAR",
            text = "[증상] 브레이크 경고등 점등 [조치] 휠속도센서 배선을 점검하세요.",
            dtcCode = "C120601",
            component = "휠속도센서",
            recommendations = 15 // 추천수가 높아 과거 키워드 트랙 상위권을 가로챘던 케이스
        )
        val anotherEntry = createEntry(
            id = "DOC-OTHER",
            text = "[증상] 계기판 경고등 [조치] 배터리 전압 점검",
            dtcCode = "B124111",
            component = "블로워 모터",
            recommendations = 5
        )

        vectorDb.addDocumentsBatch(listOf(targetEntry, popularIrrelevantEntry, anotherEntry))

        // When: p05c08c 고장코드 단독 검색
        val results = ragSearcher.search("p05c08c", topK = 3)

        // Then: 
        // 1. 결과가 존재하고 1위가 반드시 DOC-TARGET-DTC 이어야 함
        assertTrue("검색 결과가 존재해야 합니다", results.isNotEmpty())
        val topResult = results[0]
        assertEquals("DOC-TARGET-DTC", topResult.id)
        assertEquals("P05C08C", topResult.metadata.dtcCode)

        // 2. 점수는 10.0 이상 (Tier 1 독점 프리미엄)
        assertTrue("DTC 정확 일치 점수는 10.0 이상이어야 합니다 (실제: ${topResult.score})", topResult.score >= 10.0f)

        // 3. 신뢰도는 98% 이상
        assertNotNull("신뢰도가 산출되어야 합니다", topResult.confidencePercent)
        assertTrue("DTC 정확 일치 신뢰도는 98% 이상이어야 합니다 (실제: ${topResult.confidencePercent})", topResult.confidencePercent!! >= 98.0f)
    }

    @Test
    fun `search - DTC 1자 오타 퍼지 일치 시 우선권 보장 (Tier 2 점수 5점 이상, 신뢰도 90퍼센트 이상)`() {
        // Given
        val targetEntry = createEntry(
            id = "DOC-TARGET-DTC",
            text = "스캐너를 통해 P05C08C 고장 코드를 확인하세요.",
            dtcCode = "P05C08C",
            component = "AAF1 LIN"
        )
        val irrelevantEntry = createEntry(
            id = "DOC-OTHER",
            text = "휠속도센서 배선 점검",
            dtcCode = "C120601",
            component = "휠속도센서",
            recommendations = 4
        )
        vectorDb.addDocumentsBatch(listOf(targetEntry, irrelevantEntry))

        // When: 끝자리 1자 오타 ('p05c08d') 입력
        val results = ragSearcher.search("p05c08d", topK = 2)

        // Then: 1자 오타 문서가 1위를 차지하고 Tier 2 점수 5.0 이상, 신뢰도 90% 이상 획득
        assertTrue(results.isNotEmpty())
        val topResult = results[0]
        assertEquals("DOC-TARGET-DTC", topResult.id)
        assertTrue("1자 오타 퍼지 매칭 점수는 5.0 이상이어야 합니다 (실제: ${topResult.score})", topResult.score >= 5.0f)
        assertTrue("1자 오타 퍼지 매칭 신뢰도는 90% 이상이어야 합니다 (실제: ${topResult.confidencePercent})", topResult.confidencePercent!! >= 90.0f)
    }

    @Test
    fun `search - 키워드 0점 문서는 키워드 트랙 RRF 점수를 획득하지 못함`() {
        // Given
        val matchingEntry = createEntry(
            id = "DOC-MATCH",
            text = "에어컨 블로워 모터 소음 고장 수리",
            component = "블로워 모터"
        )
        val zeroMatchEntry = createEntry(
            id = "DOC-ZERO",
            text = "트렁크 래치 힌지 교체",
            component = "트렁크 래치"
        )
        vectorDb.addDocumentsBatch(listOf(matchingEntry, zeroMatchEntry))

        // When
        val results = ragSearcher.search("블로워 모터 에어컨", topK = 2)

        // Then: 키워드 일치 문서가 1위
        assertEquals("DOC-MATCH", results[0].id)
        assertTrue("DOC-MATCH 점수가 DOC-ZERO 점수보다 월등히 높아야 합니다", results[0].score > results[1].score)
    }

    @Test
    fun `search - 순수 자연어 질문 시 RRF 하이브리드 정상 정렬 및 기본 신뢰도 범위 유지`() {
        // Given
        val entry1 = createEntry(
            id = "DOC-BATTERY",
            text = "시동이 안 걸리고 배터리 방전 발생",
            component = "배터리"
        )
        val entry2 = createEntry(
            id = "DOC-TIRE",
            text = "타이어 공기압 경고등 점등",
            component = "타이어 공기압센서"
        )
        vectorDb.addDocumentsBatch(listOf(entry1, entry2))

        // When: DTC 없는 순수 자연어 질의
        val results = ragSearcher.search("시동 배터리 방전", topK = 2)

        // Then
        assertEquals("DOC-BATTERY", results[0].id)
        assertTrue("DTC 매칭이 없으므로 점수는 일반 RRF 범위(< 1.0f)여야 합니다 (실제: ${results[0].score})", results[0].score < 1.0f)
        assertTrue("일반 자연어 검색 신뢰도는 89% 이하여야 합니다 (실제: ${results[0].confidencePercent})", results[0].confidencePercent!! <= 89.0f)
    }

    @Test
    fun `search - 실제 DB 대표 고장코드(C120601, B124111, P0A0A12, U006488) 1위 우선권 검증`() {
        val entry1 = createEntry(
            id = "DOC-C120601",
            text = "[증상] 브레이크 경고등이 켜졌어요, 계기판에 ABS 불이 들어와요 [고장 내용] 뒤좌측 휠속도 센서 단선/단락",
            dtcCode = "C120601",
            component = "휠속도센서",
            recommendations = 2
        )
        val entry2 = createEntry(
            id = "DOC-B124111",
            text = "[증상] 에어컨 냉방이 잘 안 돼요 [고장 내용] 센서 단락(신호값 낮음)",
            dtcCode = "B124111",
            component = "증발기 센서",
            recommendations = 5
        )
        val entry3 = createEntry(
            id = "DOC-P0A0A12",
            text = "[증상] 고전압 배터리 연결이 안 돼서 차량이 시동이 안 걸려요 [고장 내용] 고전압 시스템 인터록 회로 이상",
            dtcCode = "P0A0A12",
            component = "인터록 회로",
            recommendations = 1
        )
        val entry4 = createEntry(
            id = "DOC-U006488",
            text = "[증상] 차량 네트워크와 테일게이트 통신이 끊겼어요 [고장 내용] 버스 차단",
            dtcCode = "U006488",
            component = "차량 통신 E",
            recommendations = 0
        )
        val popularIrrelevant = createEntry(
            id = "DOC-OTHER",
            text = "기타 일반 정비 사례",
            dtcCode = "B14A023",
            component = "슬라이딩도어",
            recommendations = 20
        )

        vectorDb.addDocumentsBatch(listOf(entry1, entry2, entry3, entry4, popularIrrelevant))

        val testCases = listOf(
            "C120601" to "DOC-C120601",
            "B124111" to "DOC-B124111",
            "P0A0A12" to "DOC-P0A0A12",
            "U006488" to "DOC-U006488"
        )

        for ((dtc, expectedId) in testCases) {
            val results = ragSearcher.search(dtc, topK = 3)
            assertTrue("[$dtc] 검색 결과가 비어있지 않아야 합니다", results.isNotEmpty())
            val top = results[0]
            assertEquals("[$dtc] 1위 결과는 $expectedId 이어야 합니다", expectedId, top.id)
            assertTrue("[$dtc] 1위 점수는 Tier 1 (10.0 이상)이어야 합니다 (실제: ${top.score})", top.score >= 10.0f)
            assertTrue("[$dtc] 1위 신뢰도는 98% 이상이어야 합니다 (실제: ${top.confidencePercent})", top.confidencePercent!! >= 98.0f)
        }
    }

    @Test
    fun `search - C120602 오타 입력 시 중간 오타 C120402를 제치고 끝자리 오타 C120601이 1위로 우선 추천됨`() {
        // Given: C120601 (끝자리 1자 오타)과 C120402 (중간자리 1자 오타) 문서 준비
        val trailingTypoDoc = createEntry(
            id = "DOC-C120601",
            text = "[증상] 브레이크 경고등 [고장 내용] 뒤좌측 휠속도센서 단선",
            dtcCode = "C120601",
            component = "뒤좌측 휠속도센서"
        )
        val middleTypoDoc = createEntry(
            id = "DOC-C120402",
            text = "[증상] 오른쪽 앞바퀴 속도 이상 [고장 내용] 앞우측 휠속도센서 성능 이상",
            dtcCode = "C120402",
            component = "앞우측 휠속도센서"
        )
        vectorDb.addDocumentsBatch(listOf(middleTypoDoc, trailingTypoDoc))

        // When: c120602 입력
        val results = ragSearcher.search("c120602", topK = 2)

        // Then: 끝자리 오타 가산점으로 인해 DOC-C120601이 1위여야 함
        assertTrue(results.isNotEmpty())
        assertEquals("DOC-C120601", results[0].id)
        assertEquals("DOC-C120402", results[1].id)
        assertTrue("C120601의 점수가 C120402보다 높아야 합니다", results[0].score > results[1].score)
        assertTrue("퍼지 일치 신뢰도는 90% 이상이어야 합니다", results[0].confidencePercent!! >= 90.0f)
    }

    @Test
    fun `search - 순수 DTC 단독 질의 시 Vector Track은 바이패스되고 고장코드 순수 순위로 정렬됨`() {
        // Given: DTC 일치 문서와 높은 코사인 유사도를 가진 무관 문서
        val dtcDoc = createEntry(
            id = "DOC-DTC",
            text = "휠속도센서 배선 단선 수리 가이드",
            dtcCode = "C120601",
            component = "휠속도센서"
        )
        val semanticDoc = createEntry(
            id = "DOC-SEMANTIC",
            text = "기타 다른 부품 점검 안내",
            dtcCode = "B124111",
            component = "증발기 센서"
        )
        vectorDb.addDocumentsBatch(listOf(dtcDoc, semanticDoc))

        // When: 순수 DTC "C120601" 단독 질의
        val results = ragSearcher.search("C120601", topK = 2)

        // Then: DOC-DTC가 1위이며 Tier 1 점수(10.0 이상) 획득
        assertEquals("DOC-DTC", results[0].id)
        assertTrue(results[0].score >= 10.0f)
    }

    @Test
    fun `search - 4단계 계층형 DTC 매칭 순위 및 신뢰도 구간 검증 (완전일치 - 끝자리오타 - 중간오타 - 계통일치 - 무관문서)`() {
        // Given: C120602 검색에 대해 5가지 계층의 문서
        val docExact = createEntry(
            id = "DOC-EXACT",
            text = "C120602 고장 코드 정확 일치",
            dtcCode = "C120602",
            component = "뒤좌측 휠속도센서 회로 이상"
        )
        val docSuffixTypo = createEntry(
            id = "DOC-SUFFIX-TYPO",
            text = "C120601 고장 코드 끝자리 오타",
            dtcCode = "C120601",
            component = "뒤좌측 휠속도센서 단선"
        )
        val docMiddleTypo = createEntry(
            id = "DOC-MIDDLE-TYPO",
            text = "C120402 고장 코드 중간 오타",
            dtcCode = "C120402",
            component = "앞우측 휠속도센서 성능 이상"
        )
        val docFamilyMatch = createEntry(
            id = "DOC-FAMILY",
            text = "C128702 ABS 제어기 계통 코드 (앞 3자리 일치)",
            dtcCode = "C128702",
            component = "브레이크 압력 센서"
        )
        val docUnrelated = createEntry(
            id = "DOC-UNRELATED",
            text = "B124111 에어컨 센서 단락",
            dtcCode = "B124111",
            component = "증발기 센서"
        )

        vectorDb.addDocumentsBatch(listOf(docFamilyMatch, docMiddleTypo, docUnrelated, docExact, docSuffixTypo))

        // When: c120602 검색
        val results = ragSearcher.search("c120602", topK = 5)

        // Then: 5개 결과의 완벽한 순서 보장
        assertEquals(5, results.size)
        assertEquals("DOC-EXACT", results[0].id)
        assertEquals("DOC-SUFFIX-TYPO", results[1].id)
        assertEquals("DOC-MIDDLE-TYPO", results[2].id)
        assertEquals("DOC-FAMILY", results[3].id)
        assertEquals("DOC-UNRELATED", results[4].id)

        // 신뢰도 구간 검증
        // 1등급: 98 ~ 100%
        assertTrue("Tier 1 신뢰도 98~100% (실제: ${results[0].confidencePercent})", results[0].confidencePercent!! in 98.0f..100.0f)
        assertTrue("Tier 1 점수 10.0 이상 (실제: ${results[0].score})", results[0].score >= 10.0f)

        // 2등급: 90 ~ 95%
        assertTrue("Tier 2 신뢰도 90~95% (실제: ${results[1].confidencePercent})", results[1].confidencePercent!! in 90.0f..95.0f)
        assertTrue("Tier 2 점수 5.0 이상 (실제: ${results[1].score})", results[1].score >= 5.0f)

        // 3등급: 85 ~ 89%
        assertTrue("Tier 3 신뢰도 85~89% (실제: ${results[2].confidencePercent})", results[2].confidencePercent!! in 85.0f..89.0f)
        assertTrue("Tier 3 점수 5.0 이상 (실제: ${results[2].score})", results[2].score >= 5.0f)

        // 4등급: 75 ~ 84%
        assertTrue("Tier 4 신뢰도 75~84% (실제: ${results[3].confidencePercent})", results[3].confidencePercent!! in 75.0f..84.0f)
        assertTrue("Tier 4 점수 2.0 이상 (실제: ${results[3].score})", results[3].score >= 2.0f)

        // 무관: 0 ~ 74%
        assertTrue("무관 문서 신뢰도 <= 74% (실제: ${results[4].confidencePercent})", results[4].confidencePercent!! <= 74.0f)
        assertTrue("무관 문서 점수 < 1.0 (실제: ${results[4].score})", results[4].score < 1.0f)
    }

    @Test
    fun `search - 표준 RRF 벡터 트랙 가중치 aiTrackWeight 변동 시 점수 반영 검증`() {
        // Given
        val entry1 = createEntry(
            id = "DOC-A",
            text = "시동 불량 배터리 전압 저하",
            component = "배터리"
        )
        val entry2 = createEntry(
            id = "DOC-B",
            text = "타이어 공기압 센서 경고등",
            component = "타이어"
        )
        vectorDb.addDocumentsBatch(listOf(entry1, entry2))

        // When 1: aiTrackWeight = 1.0 (기본)
        val defaultResults = ragSearcher.search("시동 배터리", weights = ScoringWeights(aiTrackWeight = 1.0f))

        // When 2: aiTrackWeight = 2.0 (AI 2배 반영)
        val amplifiedResults = ragSearcher.search("시동 배터리", weights = ScoringWeights(aiTrackWeight = 2.0f))

        // When 3: aiTrackWeight = 0.0 (AI 반영 제외)
        val zeroAiResults = ragSearcher.search("시동 배터리", weights = ScoringWeights(aiTrackWeight = 0.0f))

        // Then: AI 가중치가 높을수록 RRF 베이스 점수가 단계적으로 상승해야 함
        assertTrue(
            "AI 2배 가중치 점수(${amplifiedResults[0].score}) > 기본 가중치 점수(${defaultResults[0].score})",
            amplifiedResults[0].score > defaultResults[0].score
        )
        assertTrue(
            "기본 가중치 점수(${defaultResults[0].score}) > AI 0배 가중치 점수(${zeroAiResults[0].score})",
            defaultResults[0].score > zeroAiResults[0].score
        )
    }
}


