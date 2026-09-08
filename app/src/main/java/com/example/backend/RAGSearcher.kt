package com.example.backend

import android.util.Log
import java.util.regex.Pattern
import kotlin.math.sqrt

/**
 * RAG 검색 최종 결과 데이터 클래스
 */
data class SearchResult(
    val id: String,
    val text: String,
    val score: Float,
    val recommendations: Int,
    val metadata: VectorDbMetadata,
    val confidencePercent: Float? = null  // null = ONNX 미로딩 → 신뢰도 칩 숨김
)

/**
 * DTC 매칭 판정 등급 및 RRF/신뢰도 정책 열거형 (Clean Code Enum)
 */
enum class DtcMatchTier(
    val rrfBoost: Float,
    val minConfidence: Float,
    val maxConfidence: Float
) {
    TIER_1_EXACT(10.0f, 98.0f, 100.0f),         // 1등급: 완전 일치 (독점 1위 보장)
    TIER_2_SUFFIX_TYPO(5.0f, 90.0f, 95.0f),     // 2등급: 세부코드 끝자리 오타 (1글자 차이)
    TIER_3_MIDDLE_TYPO(5.0f, 85.0f, 89.0f),     // 3등급: 부품위치 중간자리 오타 (1글자 차이)
    TIER_4_FAMILY_MATCH(2.0f, 75.0f, 84.0f),    // 4등급: 동일 계통 시스템 일치 (앞 3자리 일치, 예: C12...)
    TIER_NONE(0.0f, 0.0f, 74.0f)                // 일반: DTC 불일치 / 순수 자연어 질의
}

/**
 * 온디바이스 RAG 하이브리드 검색 엔진 (Clean Code & SRP 준수)
 * 단일 책임: 벡터 데이터베이스와 키워드/DTC 트랙을 RRF(Reciprocal Rank Fusion) 알고리즘으로 융합하여 최적의 진단 정비 사례를 선별합니다.
 */
class RAGSearcher(
    private val vectorDb: SimpleVectorDB,
    private val onnxBertEngine: OnnxBertEmbeddingEngine? = null,
    val scorer: RecommendationScorer = RecommendationScorer()
) {

    companion object {
        private const val TAG = "RAGSearcher"
        const val VECTOR_DIMENSION = 768
        const val RRF_K_SMOOTHING = 60.0f
        const val WEIGHT_KEYWORD_TRACK = 1.0f
        const val WEIGHT_VECTOR_TRACK = 1.0f

        // 실제 벡터 DB 문서가 있을 때 배제할 기본 샘플 문서 ID 집합 (SSOT)
        val SAMPLE_DOC_IDS = setOf("DOC-DEFAULT-1", "DOC-DEFAULT-2", "DOC-DEFAULT-3", "TSB-03-15", "CASE-11-02", "DIAG-C1206")
    }

    /**
     * [기능 1] 관리자 지정 추천 스코어링 가중치 갱신
     */
    fun updateScoringWeights(weights: ScoringWeights) {
        scorer.currentWeights = weights
    }

    /**
     * [기능 2] ONNX RoBERTa/Ko-SBERT 엔진을 통한 768차원 임베딩 벡터 생성
     * ONNX 모델 미가동 시 런타임에 동적 부품 사전 및 문자 바이그램 해시 기반 폴백 벡터를 안전하게 생성합니다.
     */
    fun getEmbedding(text: String, isQuery: Boolean = false): List<Float> {
        val onnxResult = onnxBertEngine?.getEmbedding(text, isQuery = isQuery)
        if (onnxResult != null && onnxResult.isNotEmpty()) {
            return onnxResult
        }

        Log.w(TAG, "⚠️ [ONNX 미가동 폴백 발생] text='${text.take(30)}', isReady=${onnxBertEngine?.isReady}. 가상 해시 임베딩을 임시 생성합니다.")
        return generateFallbackEmbedding(text)
    }

    /**
     * [기능 3] 메인 하이브리드 RAG 검색 실행
     * 키워드/DTC 트랙과 AI 벡터 트랙의 순위를 RRF로 융합하고 4단계 DTC 계층 신뢰도를 산출하여 상위 topK개를 반환합니다.
     */
    fun search(query: String, topK: Int = 3, weights: ScoringWeights? = null): List<SearchResult> {
        val allEntries = vectorDb.getAll()
        if (allEntries.isEmpty()) return emptyList()

        // 1. 실제 데이터가 존재할 경우 기본 샘플 문서 배제
        val entries = filterRealEntries(allEntries)

        val queryLower = query.lowercase().trim().replace("피워", "파워")

        // 2. 질의어 내 DTC 고장코드 패턴 추출
        val dtcPatterns = extractDtcPatterns(queryLower)

        // 3. 순수 자연어 증상 텍스트 및 DTC 단독 질의 판별
        val cleanSemanticText = extractCleanSemanticText(queryLower, dtcPatterns)
        val isPureDtcQuery = dtcPatterns.isNotEmpty() && cleanSemanticText.isBlank()

        // 4. 임베딩 벡터 준비 (순수 DTC 단독 질의 시에는 AI 문맥 왜곡 방지를 위해 임베딩 생략)
        val queryEmb: List<Float>? = if (isPureDtcQuery) {
            null
        } else {
            getEmbedding(cleanSemanticText.ifBlank { queryLower }, isQuery = true)
        }

        val queryTerms = queryLower.split(Regex("[\\s,.\\[\\]()]+")).filter { it.length >= 2 }
        val activeWeights = weights ?: scorer.currentWeights

        // 5. 문서별 다차원 세부 점수 계산 (중간 엔트리 구성)
        val intermediateList = entries.map { entry ->
            val detail = scorer.calculateScore(
                queryLower = queryLower,
                queryEmb = queryEmb,
                dtcPatterns = dtcPatterns,
                queryTerms = queryTerms,
                entry = entry,
                queryEmbeddingProvider = { getEmbedding(it) },
                weights = activeWeights
            )
            IntermediateEntry(entry, detail)
        }

        // 6. RRF 랭킹 맵 생성 (키워드 트랙 & 벡터 트랙)
        val keywordRankMap = buildKeywordRankMap(intermediateList)
        val vectorRankMap = buildVectorRankMap(intermediateList)

        // 7. RRF 융합 점수 및 4단계 DTC 계층 신뢰도 산출
        val kConstant = RRF_K_SMOOTHING
        val wKeyword = WEIGHT_KEYWORD_TRACK
        val wVector = WEIGHT_VECTOR_TRACK
        val maxTheoreticalRrf = (wKeyword / (kConstant + 1.0f)) + (wVector / (kConstant + 1.0f))

        val rrfResults = intermediateList.map { item ->
            val kRank = keywordRankMap[item.entry.id]
            val vRank = vectorRankMap[item.entry.id] ?: intermediateList.size

            val kwContribution = if (kRank != null && item.scoreDetail.keywordTrackScore > 0f) {
                wKeyword / (kConstant + kRank)
            } else {
                0.0f
            }

            val vecContribution = if (isPureDtcQuery) {
                0.0f
            } else {
                wVector / (kConstant + vRank)
            }
            val baseRrfScore = kwContribution + vecContribution

            // 4단계 계층형 DTC 매칭 등급 판정
            val tier = determineDtcMatchTier(item.scoreDetail.dtcBoost, activeWeights.dtcExactBoost)
            val finalScore = tier.rrfBoost + baseRrfScore

            // 등급별 신뢰도 백분율 계산
            val confidence = calculateConfidence(
                tier = tier,
                dtcBoost = item.scoreDetail.dtcBoost,
                exactThresh = activeWeights.dtcExactBoost,
                cosSim = item.scoreDetail.cosSim,
                baseRrfScore = baseRrfScore,
                maxTheoreticalRrf = maxTheoreticalRrf
            )

            SearchResult(
                id = item.entry.id,
                text = item.entry.text,
                score = finalScore,
                recommendations = item.entry.recommendations,
                metadata = item.entry.metadata,
                confidencePercent = confidence
            )
        }

        // 8. 최종 상위 Top-K 정렬 및 로그 기록
        val topResults = rrfResults.sortedByDescending { it.score }.take(topK)
        Log.i(TAG, "=== RAG Search: '$query' ===")
        topResults.forEachIndexed { idx, res ->
            Log.i(TAG, "  Top ${idx + 1}: [${res.metadata.dtcCode}] ${res.metadata.component} | ${"%.1f".format(res.confidencePercent)}% | ${res.text.take(50)}")
        }
        return topResults
    }

    /**
     * [기능 4] 질의어 내 표준/레거시 DTC 고장코드 패턴 정규식 추출
     */
    fun extractDtcPatterns(queryLower: String): List<String> {
        val dtcPatterns = mutableListOf<String>()
        val stdDtcMatcher = Pattern.compile("(?i)[pbcu][0-9a-z]{4,}").matcher(queryLower)
        while (stdDtcMatcher.find()) {
            dtcPatterns.add(stdDtcMatcher.group().lowercase())
        }
        val legacyDtcMatcher = Pattern.compile("(?i)[a-z]*[0-9]{3,}").matcher(queryLower)
        while (legacyDtcMatcher.find()) {
            val matched = legacyDtcMatcher.group().lowercase()
            if (matched !in dtcPatterns) {
                dtcPatterns.add(matched)
            }
        }
        return dtcPatterns
    }

    /**
     * [기능 5] 복합 질의에서 DTC 코드를 제거한 순수 증상 텍스트 추출
     */
    private fun extractCleanSemanticText(queryLower: String, dtcPatterns: List<String>): String {
        var cleanText = queryLower
        for (pattern in dtcPatterns) {
            cleanText = cleanText.replace(pattern, " ")
        }
        return cleanText.replace(Regex("[\\s,.\\[\\]()_\\-]+"), " ").trim()
    }

    /**
     * [기능 6] 실제 문서가 존재할 때 기본 더미/샘플 문서 필터링
     */
    private fun filterRealEntries(allEntries: List<VectorDbEntry>): List<VectorDbEntry> {
        val hasRealDocs = allEntries.any { it.id !in SAMPLE_DOC_IDS }
        return if (hasRealDocs) {
            allEntries.filter { it.id !in SAMPLE_DOC_IDS }
        } else {
            allEntries
        }
    }

    /**
     * [기능 7] 키워드 트랙 순위 맵 생성 (유효 키워드 점수 > 0인 문서만 대상)
     */
    private fun buildKeywordRankMap(items: List<IntermediateEntry>): Map<String, Int> {
        return items
            .filter { it.scoreDetail.keywordTrackScore > 0f }
            .sortedByDescending { it.scoreDetail.keywordTrackScore + it.scoreDetail.bonusScore }
            .mapIndexed { index, item -> item.entry.id to (index + 1) }
            .toMap()
    }

    /**
     * [기능 8] 벡터 트랙 순위 맵 생성 (의미 유사도 + 추천수 보너스 순)
     */
    private fun buildVectorRankMap(items: List<IntermediateEntry>): Map<String, Int> {
        return items
            .sortedByDescending { it.scoreDetail.vectorTrackScore + it.scoreDetail.bonusScore }
            .mapIndexed { index, item -> item.entry.id to (index + 1) }
            .toMap()
    }

    /**
     * [기능 9] DTC 가산점에 따른 피라미드 4단계 매칭 등급 결정
     */
    fun determineDtcMatchTier(dtcBoost: Float, exactThresh: Float): DtcMatchTier {
        return when {
            dtcBoost >= exactThresh && dtcBoost > 0f -> DtcMatchTier.TIER_1_EXACT
            dtcBoost >= exactThresh * 0.85f          -> DtcMatchTier.TIER_2_SUFFIX_TYPO
            dtcBoost >= exactThresh * 0.70f          -> DtcMatchTier.TIER_3_MIDDLE_TYPO
            dtcBoost >= exactThresh * 0.50f          -> DtcMatchTier.TIER_4_FAMILY_MATCH
            else                                     -> DtcMatchTier.TIER_NONE
        }
    }

    /**
     * [기능 10] 등급별 화면 표시 신뢰도(Confidence %) 정밀 산출
     */
    fun calculateConfidence(
        tier: DtcMatchTier,
        dtcBoost: Float,
        exactThresh: Float,
        cosSim: Float,
        baseRrfScore: Float,
        maxTheoreticalRrf: Float
    ): Float {
        return when (tier) {
            DtcMatchTier.TIER_1_EXACT -> {
                (tier.minConfidence + (cosSim * 2.0f)).coerceIn(tier.minConfidence, tier.maxConfidence)
            }
            DtcMatchTier.TIER_2_SUFFIX_TYPO -> {
                val span = exactThresh * 0.15f
                val ratio = if (span > 0f) ((dtcBoost - (exactThresh * 0.85f)) / span).coerceIn(0.0f, 1.0f) else 0.5f
                (tier.minConfidence + (ratio * (tier.maxConfidence - tier.minConfidence))).coerceIn(tier.minConfidence, tier.maxConfidence)
            }
            DtcMatchTier.TIER_3_MIDDLE_TYPO -> {
                val span = exactThresh * 0.15f
                val ratio = if (span > 0f) ((dtcBoost - (exactThresh * 0.70f)) / span).coerceIn(0.0f, 1.0f) else 0.5f
                (tier.minConfidence + (ratio * (tier.maxConfidence - tier.minConfidence))).coerceIn(tier.minConfidence, tier.maxConfidence)
            }
            DtcMatchTier.TIER_4_FAMILY_MATCH -> {
                val span = exactThresh * 0.20f
                val ratio = if (span > 0f) ((dtcBoost - (exactThresh * 0.50f)) / span).coerceIn(0.0f, 1.0f) else 0.5f
                (tier.minConfidence + (ratio * (tier.maxConfidence - tier.minConfidence))).coerceIn(tier.minConfidence, tier.maxConfidence)
            }
            DtcMatchTier.TIER_NONE -> {
                ((baseRrfScore / maxTheoreticalRrf) * tier.maxConfidence).coerceIn(0.0f, tier.maxConfidence)
            }
        }
    }

    /**
     * [기능 11] ONNX 미가동 시 안전한 가상 해시 임베딩 벡터 생성
     */
    private fun generateFallbackEmbedding(text: String): List<Float> {
        val cleanText = text.lowercase().trim()
        val vectorSize = VECTOR_DIMENSION
        val vec = FloatArray(vectorSize) { 0.001f }

        val dynamicComponents = vectorDb.getAll()
            .map { it.metadata.component.lowercase().trim() }
            .filter { it.length >= 2 }
            .distinct()

        for (i in dynamicComponents.indices) {
            val comp = dynamicComponents[i]
            if (cleanText.contains(comp)) {
                val baseIdx = (i * 19) % vectorSize
                vec[baseIdx] += 0.8f
                vec[(baseIdx + 31) % vectorSize] += 0.5f
                vec[(baseIdx + 127) % vectorSize] += 0.3f
            }
        }

        for (i in 0 until cleanText.length - 1) {
            val bigram = cleanText.substring(i, i + 2)
            val hash1 = (bigram.hashCode() and 0x7fffffff)
            val hash2 = ((bigram.hashCode() * 31 + 17) and 0x7fffffff)
            vec[hash1 % vectorSize] += 0.15f
            vec[hash2 % vectorSize] += 0.08f
        }

        var sumSq = 0.0f
        for (v in vec) {
            sumSq += v * v
        }
        val normVal = sqrt(sumSq.toDouble()).toFloat().coerceAtLeast(1e-6f)
        return vec.map { it / normVal }
    }

    /**
     * 검색 파이프라인 중간 계산 결과 홀더
     */
    private data class IntermediateEntry(
        val entry: VectorDbEntry,
        val scoreDetail: RecommendationScorer.ScoreDetail
    )
}
