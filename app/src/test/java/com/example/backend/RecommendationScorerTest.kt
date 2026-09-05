package com.example.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Clean Code Unit Tests for RecommendationScorer & ScoringWeights.
 * 
 * Follows Single Responsibility Principle (SRP), DRY with test fixtures,
 * and covers Happy Path, Edge Cases, and Penalty Scenarios.
 */
class RecommendationScorerTest {

    private lateinit var scorer: RecommendationScorer
    private val defaultWeights = ScoringWeights()

    @Before
    fun setUp() {
        scorer = RecommendationScorer(defaultWeights)
    }

    // ── Test Fixture Helper (DRY Principle) ──────────────────────────────────
    private fun createTestEntry(
        id: String = "DOC-TEST-001",
        text: String = "에어컨 바람이 시원하지 않아요",
        dtcCode: String = "B120813",
        component: String = "내외기 액추에이터 위치센서",
        embedding: List<Float> = listOf(1.0f, 0.0f, 0.0f),
        recommendations: Int = 0
    ): VectorDbEntry {
        return VectorDbEntry(
            id = id,
            text = text,
            embedding = embedding,
            metadata = VectorDbMetadata(
                dtcCode = dtcCode,
                component = component,
                dtcs = listOf(dtcCode)
            ),
            recommendations = recommendations
        )
    }

    // ────────────────────────────────────────────────────────────────────────
    // 1. Happy Path (정상 케이스)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculateScore - DTC 정확 일치 시 100점 프리패스 및 dtcExactBoost 가산 검증`() {
        // Given
        val entry = createTestEntry(dtcCode = "B120813")
        val queryDtc = "B120813"
        val queryEmbedding = listOf(1.0f, 0.0f, 0.0f)

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = queryDtc.lowercase(),
            queryEmb = queryEmbedding,
            dtcPatterns = listOf(queryDtc),
            queryTerms = listOf(queryDtc),
            entry = entry,
            queryEmbeddingProvider = { queryEmbedding }
        )

        // Then: DTC 정확 일치 시 dtcExactBoost(15.0점) 부여 및 100점 이상 프리패스 점수 획득
        assertEquals(defaultWeights.dtcExactBoost, scoreDetail.dtcBoost, 0.001f)
        assertTrue(
            "DTC 정확 일치 시 finalScore는 100점을 초과해야 합니다 (실제: ${scoreDetail.finalScore})",
            scoreDetail.finalScore >= 100.0f
        )
    }

    @Test
    fun `calculateScore - 부품명 직접 일치 및 증상 텍스트 키워드 매칭 검증`() {
        // Given
        val entry = createTestEntry(
            text = "에어컨 냉방 불량 및 찬바람 안 나옴",
            component = "증발기 센서"
        )
        val query = "증발기 센서 에어컨 냉방"
        val terms = listOf("증발기", "센서", "에어컨", "냉방")
        val queryEmbedding = listOf(0.0f, 0.0f, 0.0f)

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = query.lowercase(),
            queryEmb = queryEmbedding,
            dtcPatterns = emptyList(),
            queryTerms = terms,
            entry = entry,
            queryEmbeddingProvider = { queryEmbedding }
        )

        // Then: 부품명 가산(2.0점) 및 텍스트 텀 가산(textOverlapBoost * 2개 이상) 확인
        assertEquals(defaultWeights.compMatchBoost, scoreDetail.dynamicCompBoost, 0.001f)
        assertTrue(
            "텍스트 일치 점수는 0보다 커야 합니다 (실제: ${scoreDetail.dynamicTextBoost})",
            scoreDetail.dynamicTextBoost >= defaultWeights.textOverlapBoost
        )
    }

    @Test
    fun `calculateScore - 코사인 유사도 0_5 이상 시 AI 비선형 증폭 가산점 검증`() {
        // Given: 완전 동일한 임베딩 벡터 -> cosSim = 1.0f
        val sameEmbedding = listOf(1.0f, 0.0f, 0.0f)
        val entry = createTestEntry(embedding = sameEmbedding)

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = "무관한 단어",
            queryEmb = sameEmbedding,
            dtcPatterns = emptyList(),
            queryTerms = emptyList(),
            entry = entry,
            queryEmbeddingProvider = { sameEmbedding }
        )

        // Then: cosSim = 1.0 -> aiAmpBoost = ((1.0 - 0.5) / 0.5) * 4.0 = 4.0점
        assertEquals(1.0f, scoreDetail.cosSim, 0.001f)
        assertEquals(defaultWeights.aiAmpScale, scoreDetail.aiAmpBoost, 0.001f)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 2. Edge Cases (경계 및 엣지 케이스)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculateScore - 빈 검색어 및 빈 키워드 입력 시 크래시 없이 기본값 반환 검증`() {
        // Given
        val entry = createTestEntry()
        val emptyEmbedding = emptyList<Float>()

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = "",
            queryEmb = emptyEmbedding,
            dtcPatterns = emptyList(),
            queryTerms = emptyList(),
            entry = entry,
            queryEmbeddingProvider = { emptyList() }
        )

        // Then: 에러 없이 0점대로 안전 반환
        assertEquals(0.0f, scoreDetail.cosSim, 0.001f)
        assertEquals(0.0f, scoreDetail.dtcBoost, 0.001f)
        assertEquals(0.0f, scoreDetail.dynamicCompBoost, 0.001f)
        assertEquals(0.0f, scoreDetail.dynamicTextBoost, 0.001f)
    }

    @Test
    fun `calculateScore - 영벡터 입력 시 코사인 유사도 0_0f 안전 반환 검증`() {
        // Given: 영벡터(0, 0, 0)
        val zeroEmbedding = listOf(0.0f, 0.0f, 0.0f)
        val docEmbedding = listOf(1.0f, 2.0f, 3.0f)
        val entry = createTestEntry(embedding = docEmbedding)

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = "테스트",
            queryEmb = zeroEmbedding,
            dtcPatterns = emptyList(),
            queryTerms = emptyList(),
            entry = entry,
            queryEmbeddingProvider = { zeroEmbedding }
        )

        // Then: 0으로 나누기 예외(NaN/Infinity) 없이 0.0f 반환
        assertEquals(0.0f, scoreDetail.cosSim, 0.001f)
        assertEquals(0.0f, scoreDetail.aiAmpBoost, 0.001f)
    }

    @Test
    fun `calculateScore - DTC 1글자 오타 또는 하위 코드 Fuzzy 매칭 및 끝자리 오타 가산점 검증`() {
        // Given: DB에는 B120813, 사용자는 1자리 끝자리 오타 B120812 또는 중간 오타 B120413 입력
        val entry = createTestEntry(dtcCode = "B120813")
        val trailingTypoDtc = "B120812"
        val middleTypoDtc = "B120413"

        // When: 끝자리 오타
        val trailingDetail = scorer.calculateScore(
            queryLower = trailingTypoDtc.lowercase(),
            queryEmb = null,
            dtcPatterns = listOf(trailingTypoDtc),
            queryTerms = listOf(trailingTypoDtc),
            entry = entry,
            queryEmbeddingProvider = { emptyList() }
        )

        // When: 중간 오타
        val middleDetail = scorer.calculateScore(
            queryLower = middleTypoDtc.lowercase(),
            queryEmb = null,
            dtcPatterns = listOf(middleTypoDtc),
            queryTerms = listOf(middleTypoDtc),
            entry = entry,
            queryEmbeddingProvider = { emptyList() }
        )

        // Then: 기본 Fuzzy Boost (12.0점 이상) 확인 및 끝자리 오타 점수가 중간 오타 점수보다 높음 확인
        val baseFuzzyBoost = defaultWeights.dtcExactBoost * (12.0f / 15.0f)
        assertTrue(trailingDetail.dtcBoost >= baseFuzzyBoost)
        assertTrue("끝자리 오타 점수(13.5)가 중간 오타 점수(11.5)보다 높아야 합니다", trailingDetail.dtcBoost > middleDetail.dtcBoost)
    }

    @Test
    fun `calculateScore - 피라미드 4단계 계층형 DTC 매칭 점수 검증 (1등급 완전일치, 2등급 끝자리오타, 3등급 중간오타, 4등급 계통일치)`() {
        // Given: 기준 고장코드 C120602
        val queryDtc = "C120602"
        val exactEntry = createTestEntry(id = "T1", dtcCode = "C120602")
        val suffixTypoEntry = createTestEntry(id = "T2", dtcCode = "C120601") // 2등급: 끝자리 오타
        val middleTypoEntry = createTestEntry(id = "T3", dtcCode = "C120402") // 3등급: 중간자리 오타
        val familyEntry = createTestEntry(id = "T4", dtcCode = "C128702")     // 4등급: 앞 3자리 계통(C12...) 일치
        val unrelatedEntry = createTestEntry(id = "T5", dtcCode = "B124111")  // 무관한 코드

        // When
        fun getDtcScore(entry: VectorDbEntry): Float {
            return scorer.calculateScore(
                queryLower = queryDtc.lowercase(),
                queryEmb = null,
                dtcPatterns = listOf(queryDtc),
                queryTerms = listOf(queryDtc),
                entry = entry,
                queryEmbeddingProvider = { emptyList() }
            ).dtcBoost
        }

        val exactScore = getDtcScore(exactEntry)
        val suffixScore = getDtcScore(suffixTypoEntry)
        val middleScore = getDtcScore(middleTypoEntry)
        val familyScore = getDtcScore(familyEntry)
        val unrelatedScore = getDtcScore(unrelatedEntry)

        // Then: 엄격한 4계층 점수 격차 확인
        // 1등급(15.0) > 2등급(13.5) > 3등급(11.5) > 4등급(8.25) > 무관(0.0)
        assertEquals(15.0f, exactScore, 0.001f)
        assertTrue("2등급(끝자리 오타)은 13.0 ~ 13.5점 사이여야 합니다 (실제: $suffixScore)", suffixScore in 13.0f..13.5f)
        assertTrue("3등급(중간자리 오타)은 11.0 ~ 12.0점 사이여야 합니다 (실제: $middleScore)", middleScore in 11.0f..12.0f)
        assertTrue("4등급(앞3자리 계통)은 8.25 ~ 9.0점 사이여야 합니다 (실제: $familyScore)", familyScore in 8.25f..9.0f)
        assertEquals(0.0f, unrelatedScore, 0.001f)

        assertTrue("1등급 > 2등급", exactScore > suffixScore)
        assertTrue("2등급 > 3등급", suffixScore > middleScore)
        assertTrue("3등급 > 4등급", middleScore > familyScore)
        assertTrue("4등급 > 무관", familyScore > unrelatedScore)
    }

    // ────────────────────────────────────────────────────────────────────────
    // 3. Penalty & Bonus Cases (페널티 및 특수 보너스 케이스)
    // ────────────────────────────────────────────────────────────────────────

    @Test
    fun `calculateScore - 질의에는 주요 부품어가 있으나 문서에는 관련 키워드가 전혀 없을 때 페널티(-2_0점) 검증`() {
        // Given: 검색어에는 주요 부품 키워드("에어컨")가 있으나 대상 문서는 무관한 일반 전원/접지 부품
        val entry = createTestEntry(
            text = "전원 공급 불안정 및 단자 체결 불량",
            component = "접지 단자"
        )
        val query = "에어컨 바람 냉방 불량"

        // When
        val scoreDetail = scorer.calculateScore(
            queryLower = query.lowercase(),
            queryEmb = emptyList(),
            dtcPatterns = emptyList(),
            queryTerms = listOf("에어컨", "바람"),
            entry = entry,
            queryEmbeddingProvider = { emptyList() }
        )

        // Then: 주요 부품 키워드 미보유 문서에 대해 -2.0점 페널티 적용 확인
        assertEquals(-2.0f, scoreDetail.compPenalty, 0.001f)
    }

    @Test
    fun `calculateScore - 정비사 추천수(Upvote) 구간별 가산 보너스 검증`() {
        // Given: 추천수 10회 이상 부품
        val popularEntry = createTestEntry(recommendations = 10)
        val normalEntry = createTestEntry(recommendations = 0)

        // When
        val popularDetail = scorer.calculateScore(
            queryLower = "테스트",
            queryEmb = emptyList(),
            dtcPatterns = emptyList(),
            queryTerms = emptyList(),
            entry = popularEntry,
            queryEmbeddingProvider = { emptyList() }
        )
        val normalDetail = scorer.calculateScore(
            queryLower = "테스트",
            queryEmb = emptyList(),
            dtcPatterns = emptyList(),
            queryTerms = emptyList(),
            entry = normalEntry,
            queryEmbeddingProvider = { emptyList() }
        )

        // Then: 10회 추천 시 upvoteBonusScale * 6.6f 가산, 0회는 0.0f
        assertEquals(defaultWeights.upvoteBonusScale * 6.6f, popularDetail.bonusScore, 0.001f)
        assertEquals(0.0f, normalDetail.bonusScore, 0.001f)
    }
}
