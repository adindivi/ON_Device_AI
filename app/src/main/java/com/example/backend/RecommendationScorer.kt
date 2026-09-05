package com.example.backend

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * 관리자 설정 가능한 동적 추천 가중치 데이터 클래스
 */
data class ScoringWeights(
    val dtcExactBoost: Float = 15.0f,    // ⚖️ DTC 우대 및 AI 증상 유사도 균형 조화 수치
    val compMatchBoost: Float = 2.0f,    // 🔩 부품명 직접 매칭 점수 (기본 2.0점)
    val textOverlapBoost: Float = 1.0f,  // 💬 증상 키워드 일치 점수 (기본 1.0점)
    val aiAmpScale: Float = 4.0f,        // 🧠 AI 코사인 유사도 증폭 최고점 (기본 4.0점)
    val upvoteBonusScale: Float = 1.0f   // 👍 현장 정비사 추천 보너스 (기본 1.0점)
)

/**
 * 온디바이스 RAG 전용 추천 스코어링 엔진 (Clean Code & SRP 준수)
 * 단일 책임: 단일 문서(VectorDbEntry)와 사용자 검색어 간의 다차원 적합도 점수를 산출합니다.
 */
class RecommendationScorer(
    var currentWeights: ScoringWeights = ScoringWeights()
) {

    companion object {
        // AI 코사인 유사도 증폭 적용 기준 임계값
        private const val AI_SIMILARITY_THRESHOLD = 0.50f

        // 차량 주요 시스템 키워드 불일치 시 감점 페널티
        private const val COMPONENT_MISMATCH_PENALTY = -2.0f

        // 텍스트 단어 일치 점수 상한 배수 (기본 가중치의 최대 6배)
        private const val MAX_TEXT_OVERLAP_MULTIPLIER = 6.0f

        // DTC 코드 완전 일치 시 최우선 순위 보장용 프리미엄 점수
        private const val DTC_EXACT_MATCH_PREMIUM = 100.0f

        // 과도한 가산점을 방지하기 위한 일반적/포괄적 부품 단어 집합
        private val GENERIC_COMPONENT_TERMS = setOf("센서", "스위치", "모듈", "회로", "라인", "이상")

        // 오진단 방지 검사용 차량 핵심 서브시스템 키워드 집합
        private val KEY_VEHICLE_SYSTEMS = setOf(
            "에어컨", "히터", "슬라이딩도어", "테일게이트", "트렁크", "브레이크", "스마트키",
            "시동", "엔진", "조향", "핸들", "덕트", "블로워", "휠속도센서", "압력센서"
        )
    }

    /**
     * 산출된 세부 점수 내역 (디버깅, RRF 융합, 가중치 분석에 활용)
     */
    data class ScoreDetail(
        val cosSim: Float,
        val aiAmpBoost: Float,
        val dtcBoost: Float,
        val dynamicCompBoost: Float,
        val dynamicTextBoost: Float,
        val compPenalty: Float,
        val bonusScore: Float,
        val finalScore: Float,
        val keywordTrackScore: Float = 0f,
        val vectorTrackScore: Float = 0f
    )

    /**
     * [기능 1] 문서별 종합 적합도 점수 산출
     * 코사인 유사도, 현장 추천수, 부품명, 증상 텍스트, DTC 매칭을 종합하여 점수를 계산합니다.
     */
    fun calculateScore(
        queryLower: String,
        queryEmb: List<Float>?,
        dtcPatterns: List<String>,
        queryTerms: List<String>,
        entry: VectorDbEntry,
        queryEmbeddingProvider: (String) -> List<Float>,
        weights: ScoringWeights = currentWeights
    ): ScoreDetail {
        val docTextLower = entry.text.lowercase()
        val compLower = entry.metadata.component.lowercase().trim()

        // 1. 임베딩 벡터 준비 (캐시된 벡터가 없으면 프로바이더를 통해 생성)
        val docEmb = if (queryEmb != null && queryEmb.isNotEmpty()) {
            entry.embedding.ifEmpty { queryEmbeddingProvider(entry.text) }
        } else {
            emptyList()
        }

        // 2. 벡터 트랙 점수 계산: 코사인 유사도 및 AI 비선형 증폭
        val cosSim = if (queryEmb != null && queryEmb.isNotEmpty()) {
            computeCosineSimilarity(queryEmb, docEmb)
        } else {
            0.0f
        }
        val aiAmpBoost = computeAiAmplification(cosSim, weights.aiAmpScale)

        // 3. 정비사 추천수(Upvote) 신뢰도 보너스
        val bonusScore = computeUpvoteBonus(entry.recommendations, weights.upvoteBonusScale)

        // 4. 부품명 매칭 가산점 및 시스템 불일치 감점
        val dynamicCompBoost = computeComponentBoost(compLower, queryLower, weights.compMatchBoost)
        val compPenalty = computeComponentMismatchPenalty(compLower, docTextLower, queryLower)

        // 5. 증상 텍스트 단어 중복 가산점
        val dynamicTextBoost = computeTextOverlapBoost(docTextLower, queryTerms, weights.textOverlapBoost)

        // 6. DTC 4단계 계층형 매칭 가산점
        val dtcBoost = computeEntryDtcBoost(entry, dtcPatterns, queryLower, docTextLower, weights.dtcExactBoost)

        // 7. 트랙별 점수 합산 (RRF 융합용)
        val keywordTrackScore = dtcBoost + dynamicCompBoost + dynamicTextBoost + compPenalty
        val vectorTrackScore = cosSim + aiAmpBoost

        // 8. 최종 점수 산출 (DTC 완전 일치 시 100점 프리미엄 부여)
        val finalScore = if (dtcBoost >= weights.dtcExactBoost && dtcBoost > 0f) {
            DTC_EXACT_MATCH_PREMIUM + dtcBoost + cosSim + bonusScore + dynamicCompBoost + dynamicTextBoost
        } else {
            cosSim + aiAmpBoost + bonusScore + dtcBoost + dynamicCompBoost + dynamicTextBoost + compPenalty
        }

        return ScoreDetail(
            cosSim = cosSim,
            aiAmpBoost = aiAmpBoost,
            dtcBoost = dtcBoost,
            dynamicCompBoost = dynamicCompBoost,
            dynamicTextBoost = dynamicTextBoost,
            compPenalty = compPenalty,
            bonusScore = bonusScore,
            finalScore = finalScore,
            keywordTrackScore = keywordTrackScore,
            vectorTrackScore = vectorTrackScore
        )
    }

    /**
     * [기능 2] AI 코사인 유사도 비선형 증폭 계산
     * 임계값(0.50) 이상인 고유사도 문서에 대해 차등 가산점을 부여합니다.
     */
    private fun computeAiAmplification(cosSim: Float, scale: Float): Float {
        return if (cosSim >= AI_SIMILARITY_THRESHOLD) {
            ((cosSim - AI_SIMILARITY_THRESHOLD) / (1.0f - AI_SIMILARITY_THRESHOLD)) * scale
        } else {
            0.0f
        }
    }

    /**
     * [기능 3] 현장 정비사 추천수(Upvote) 구간별 가산 보너스 계산
     * 10회 이상(6.6배), 3회 이상(3.3배), 1회 이상(1.0배) 등 신뢰도에 따른 차등 보너스를 부여합니다.
     */
    private fun computeUpvoteBonus(recommendations: Int, scale: Float): Float {
        return when {
            recommendations >= 10 -> scale * 6.6f
            recommendations >= 3  -> scale * 3.3f
            recommendations >= 1  -> scale
            else                  -> 0.0f
        }
    }

    /**
     * [기능 4] 부품명 동적 매칭 가산점 계산
     * 검색어와 부품명의 완전 일치 또는 토큰 단위 부분 일치 여부를 판별합니다.
     */
    private fun computeComponentBoost(compLower: String, queryLower: String, matchBoost: Float): Float {
        if (compLower.isBlank()) return 0.0f

        return if (queryLower.contains(compLower)) {
            // 일반 포괄어("센서", "모듈" 등)는 0.3점, 구체적 부품명은 전체 가중치 부여
            if (compLower in GENERIC_COMPONENT_TERMS) 0.3f else matchBoost
        } else {
            // 부품명을 분절하여 부분 토큰 일치 개수 반영
            val compTerms = compLower.split(Regex("[\\s,.\\[\\]()]+"))
                .filter { it.length >= 2 && it !in GENERIC_COMPONENT_TERMS }
            val matchedTerms = compTerms.count { term -> queryLower.contains(term) }
            if (matchedTerms > 0) {
                minOf(matchedTerms * 0.5f, matchBoost * 0.8f)
            } else {
                0.0f
            }
        }
    }

    /**
     * [기능 5] 부품/계통 불일치 감점(Penalty) 계산
     * 질의에 주요 차량 계통 키워드(예: '에어컨')가 있으나 문서에 전혀 없는 경우 오진단 방지 감점을 적용합니다.
     */
    private fun computeComponentMismatchPenalty(compLower: String, docTextLower: String, queryLower: String): Float {
        if (compLower.isBlank()) return 0.0f

        val queryHasSystemKw = KEY_VEHICLE_SYSTEMS.any { kw -> queryLower.contains(kw) }
        val docHasSystemKw = KEY_VEHICLE_SYSTEMS.any { kw -> compLower.contains(kw) || docTextLower.contains(kw) }

        return if (queryHasSystemKw && !docHasSystemKw) {
            COMPONENT_MISMATCH_PENALTY
        } else {
            0.0f
        }
    }

    /**
     * [기능 6] 증상 텍스트 키워드 중복 가산점 계산
     * 질의어 단어가 문서 텍스트에 포함될 때마다 누적 가산점을 부여합니다 (최대 6배 한도).
     */
    private fun computeTextOverlapBoost(docTextLower: String, queryTerms: List<String>, overlapBoost: Float): Float {
        var boost = 0.0f
        for (term in queryTerms) {
            if (docTextLower.contains(term)) {
                boost += overlapBoost
            }
        }
        return minOf(boost, overlapBoost * MAX_TEXT_OVERLAP_MULTIPLIER)
    }

    /**
     * [기능 7] 문서 보유 고장코드(DTC)들에 대한 최고 매칭 점수 계산
     * 문서에 등록된 단일 또는 다중 DTC 목록과 검색어 내 DTC 패턴 간의 최고 매칭 점수를 선택합니다.
     */
    private fun computeEntryDtcBoost(
        entry: VectorDbEntry,
        dtcPatterns: List<String>,
        queryLower: String,
        docTextLower: String,
        exactBoost: Float
    ): Float {
        var bestBoost = 0.0f
        val docDtcs = entry.metadata.dtcs.ifEmpty {
            if (entry.metadata.dtcCode.isNotBlank()) listOf(entry.metadata.dtcCode) else emptyList()
        }

        for (docDtc in docDtcs) {
            if (docDtc.isBlank()) continue
            val docDtcClean = docDtc.trim()

            for (pattern in dtcPatterns) {
                if (pattern.length >= 3) {
                    val matchScore = calculateDtcMatchScore(docDtcClean, pattern, docTextLower, exactBoost)
                    if (matchScore > bestBoost) {
                        bestBoost = matchScore
                    }
                }
            }

            if (queryLower.contains(docDtcClean.lowercase())) {
                bestBoost = maxOf(bestBoost, exactBoost)
            }
        }
        return bestBoost
    }

    /**
     * [기능 8] 피라미드 4단계 계층형 DTC 매칭 점수 산출
     * - 1등급 (완전 일치): exactBoost (15.0점)
     * - 2등급 (세부코드 끝자리 오타): exactBoost * 0.85 + 끝자리 가중치 (13.0 ~ 13.5점)
     * - 3등급 (부품위치 중간자리 오타): exactBoost * 0.70 + 위치 가중치 (11.0 ~ 12.0점)
     * - 4등급 (동일 시스템 계통 앞 3자리 일치, 예: C12...): exactBoost * 0.55 ~ 0.60 (8.25 ~ 9.0점)
     */
    fun calculateDtcMatchScore(
        docDtc: String,
        pattern: String,
        docTextLower: String,
        exactBoost: Float
    ): Float {
        val s1 = docDtc.uppercase().trim()
        val s2 = pattern.uppercase().trim()

        // 1등급: 완전 일치 또는 본문 내 완전 포함
        if (s1 == s2 || docTextLower.contains(s2.lowercase())) {
            return exactBoost
        }
        if (s1.length < 3 || s2.length < 3) return 0.0f

        // 2 & 3등급: 동일 길이 코드의 1글자 오타 위치별 차등 판정
        if (s1.length == s2.length && s1.length >= 4) {
            val diffInfo = findSingleCharDiff(s1, s2)
            if (diffInfo != null) {
                val (diffIndex, posRatio) = diffInfo
                return if (diffIndex >= s1.length - 2) {
                    // 2등급: 끝자리 부근 세부코드 오타 (13.0 ~ 13.5점)
                    (exactBoost * 0.85f) + (posRatio * 0.75f)
                } else {
                    // 3등급: 중간자리 부품위치 오타 (11.0 ~ 12.0점)
                    (exactBoost * 0.70f) + (posRatio * 1.5f)
                }
            }
        }

        // 1자 삽입/삭제 길이 차이 허용 (11.25점)
        if (abs(s1.length - s2.length) == 1 && s1.length >= 4 && s2.length >= 4) {
            if (isDtcLengthDiffMatch(s1, s2)) {
                return exactBoost * 0.75f
            }
        }

        // 4등급: 동일 제어기/시스템 계통 일치 (앞 3자리 일치, 예: C12..., B12..., P0A...)
        if (s1.length >= 3 && s2.length >= 3) {
            if (s1.take(3) == s2.take(3)) {
                val has4Prefix = s1.length >= 4 && s2.length >= 4 && s1.take(4) == s2.take(4)
                return if (has4Prefix) exactBoost * 0.60f else exactBoost * 0.55f // 9.0f 또는 8.25f
            }
        }

        // 부분 포함 관계 (7.5점)
        if (s1.contains(s2) || s2.contains(s1)) {
            return exactBoost * 0.50f
        }

        return 0.0f
    }

    /**
     * [기능 9] 1글자 오타 판별 및 위치 비율 산출 도우미
     */
    private fun findSingleCharDiff(s1: String, s2: String): Pair<Int, Float>? {
        var diffCount = 0
        var diffIndex = -1
        for (i in s1.indices) {
            if (s1[i] != s2[i]) {
                diffCount++
                diffIndex = i
            }
        }
        return if (diffCount == 1) {
            val ratio = diffIndex.toFloat() / (s1.length - 1).toFloat()
            Pair(diffIndex, ratio)
        } else {
            null
        }
    }

    /**
     * [기능 10] 1글자 누락/추가(길이 1 차이) 편집 거리 일치 판별 도우미
     */
    private fun isDtcLengthDiffMatch(s1: String, s2: String): Boolean {
        val shorter = if (s1.length < s2.length) s1 else s2
        val longer = if (s1.length < s2.length) s2 else s1
        var i = 0
        var j = 0
        var diff = 0
        while (i < shorter.length && j < longer.length) {
            if (shorter[i] == longer[j]) {
                i++
                j++
            } else {
                diff++
                if (diff > 1) return false
                j++
            }
        }
        return true
    }

    /**
     * [기능 11] 두 임베딩 벡터 간 코사인 유사도(Cosine Similarity) 산출
     */
    private fun computeCosineSimilarity(v1: List<Float>?, v2: List<Float>?): Float {
        if (v1 == null || v2 == null) return 0f
        val minSize = min(v1.size, v2.size)
        if (minSize == 0) return 0f

        var dot = 0.0f
        var norm1 = 0.0f
        var norm2 = 0.0f

        for (i in 0 until minSize) {
            dot += v1[i] * v2[i]
            norm1 += v1[i] * v1[i]
            norm2 += v2[i] * v2[i]
        }

        val denom = (sqrt(norm1.toDouble()) * sqrt(norm2.toDouble())).toFloat()
        return if (denom > 1e-9f) dot / denom else 0f
    }
}
