package com.example.backend

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Dynamic Scoring Weights configurable by Administrator
 */
data class ScoringWeights(
    val dtcExactBoost: Float = 15.0f,    // ⚖️ DTC 우대 및 AI 증상 유사도 균형 조화 수치
    val compMatchBoost: Float = 2.0f,    // 🔩 부품명 직접 매칭 점수 (기본 2.0점)
    val textOverlapBoost: Float = 1.0f,  // 💬 증상 키워드 일치 점수 (기본 1.0점)
    val aiAmpScale: Float = 4.0f,        // 🧠 AI 코사인 유사도 증폭 최고점 (기본 4.0점)
    val upvoteBonusScale: Float = 1.0f   // 👍 현장 정비사 추천 보너스 (기본 1.0점)
)

/**
 * Dedicated Recommendation Scoring Engine for On-Device RAG
 * Single Responsibility: Calculates diagnostic suitability scores for vector DB entries.
 */
class RecommendationScorer(
    var currentWeights: ScoringWeights = ScoringWeights()
) {

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
        val docEmb = if (queryEmb != null && queryEmb.isNotEmpty()) {
            entry.embedding.ifEmpty { queryEmbeddingProvider(entry.text) }
        } else {
            emptyList()
        }

        // 1. Cosine Similarity & AI Non-linear Amplification (null-safe for pure DTC queries)
        val cosSim = if (queryEmb != null && queryEmb.isNotEmpty()) {
            computeCosineSimilarity(queryEmb, docEmb)
        } else {
            0.0f
        }
        var aiAmpBoost = 0.0f
        if (cosSim >= 0.50f) {  // 🔧 변경: 0.65 → 0.50 (중간 유사도 문서도 점수 부여)
            aiAmpBoost = ((cosSim - 0.50f) / 0.50f) * weights.aiAmpScale
        }

        // 2. Upvote Confidence Bonus for Recommendations
        val recCount = entry.recommendations
        val bonusScore = when {
            recCount >= 10 -> weights.upvoteBonusScale * 6.6f
            recCount >= 3  -> weights.upvoteBonusScale * 3.3f
            recCount >= 1  -> weights.upvoteBonusScale
            else           -> 0.0f
        }

        // 3. Dynamic Component Match Boost
        val genericTerms = setOf("센서", "스위치", "모듈", "회로", "라인", "이상")
        var dynamicCompBoost = 0.0f
        if (compLower.isNotBlank()) {
            if (queryLower.contains(compLower)) {
                dynamicCompBoost = if (compLower in genericTerms) 0.3f else weights.compMatchBoost
            } else {
                val compTerms = compLower.split(Regex("[\\s,.\\[\\]()]+")).filter { it.length >= 2 && it !in genericTerms }
                val matchedCompTerms = compTerms.count { term -> queryLower.contains(term) }
                if (matchedCompTerms > 0) {
                    dynamicCompBoost = minOf(matchedCompTerms * 0.5f, weights.compMatchBoost * 0.8f)
                }
            }
        }

        // 4. Component Mis-match Penalty
        val keyComponentKeywords = setOf(
            "에어컨", "히터", "슬라이딩도어", "테일게이트", "트렁크", "브레이크", "스마트키",
            "시동", "엔진", "조향", "핸들", "덕트", "블로워", "휠속도센서", "압력센서"
        )
        var compPenalty = 0.0f
        if (compLower.isNotBlank()) {
            val queryHasCompKw = keyComponentKeywords.any { kw -> queryLower.contains(kw) }
            val docHasCompKw = keyComponentKeywords.any { kw -> compLower.contains(kw) || docTextLower.contains(kw) }
            if (queryHasCompKw && !docHasCompKw) {
                compPenalty = -2.0f
            }
        }

        // 5. Dynamic Text Term Overlap Boost
        var dynamicTextBoost = 0.0f
        for (term in queryTerms) {
            if (docTextLower.contains(term)) {
                dynamicTextBoost += weights.textOverlapBoost
            }
        }
        dynamicTextBoost = minOf(dynamicTextBoost, weights.textOverlapBoost * 6.0f)

        // 6. DTC Match Boost with Position-Aware Typo Precision (끝자리 오타 우선순위)
        var dtcBoost = 0.0f
        val docDtcs = entry.metadata.dtcs.ifEmpty {
            if (entry.metadata.dtcCode.isNotBlank()) listOf(entry.metadata.dtcCode) else emptyList()
        }

        for (docDtc in docDtcs) {
            if (docDtc.isBlank()) continue
            val docDtcClean = docDtc.trim()

            for (pattern in dtcPatterns) {
                if (pattern.length >= 3) {
                    val matchScore = calculateDtcMatchScore(docDtcClean, pattern, docTextLower, weights.dtcExactBoost)
                    if (matchScore > dtcBoost) {
                        dtcBoost = matchScore
                    }
                }
            }

            if (queryLower.contains(docDtcClean.lowercase())) {
                dtcBoost = maxOf(dtcBoost, weights.dtcExactBoost)
            }
        }

        val keywordTrackScore = dtcBoost + dynamicCompBoost + dynamicTextBoost + compPenalty
        val vectorTrackScore = cosSim + aiAmpBoost

        // DTC 정확 일치 시 무조건 독점 1위 100점 프리미엄 부여
        val finalScore = if (dtcBoost >= weights.dtcExactBoost && dtcBoost > 0f) {
            100.0f + dtcBoost + cosSim + bonusScore + dynamicCompBoost + dynamicTextBoost
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
     * 피라미드 4단계 계층형 DTC 일치/퍼지 점수 계산
     * - 1등급 (완전 일치): exactBoost (15.0점)
     * - 2등급 (세부코드 끝자리 오타): exactBoost * 0.85 + 끝자리 보너스 (13.0 ~ 13.5점)
     * - 3등급 (부품위치 중간자리 오타): exactBoost * 0.70 + 위치 보너스 (11.0 ~ 12.0점)
     * - 4등급 (동일 계통 앞 3자리 일치, 예: C12...): exactBoost * 0.55 ~ 0.60 (8.25 ~ 9.0점)
     */
    fun calculateDtcMatchScore(
        docDtc: String,
        pattern: String,
        docTextLower: String,
        exactBoost: Float
    ): Float {
        val s1 = docDtc.uppercase().trim()
        val s2 = pattern.uppercase().trim()
        if (s1 == s2 || docTextLower.contains(s2.lowercase())) {
            return exactBoost
        }
        if (s1.length < 3 || s2.length < 3) return 0.0f

        // 1. 동일 길이: 1글자 오타 차등 판정 (2등급 세부코드 오타 vs 3등급 부품위치 오타)
        if (s1.length == s2.length && s1.length >= 4) {
            var diffCount = 0
            var diffIndex = -1
            for (i in s1.indices) {
                if (s1[i] != s2[i]) {
                    diffCount++
                    diffIndex = i
                }
            }
            if (diffCount == 1) {
                val posRatio = diffIndex.toFloat() / (s1.length - 1).toFloat()
                return if (diffIndex >= s1.length - 2) {
                    // 2등급: 끝자리 부근 세부코드 오타 (13.0 ~ 13.5점)
                    (exactBoost * 0.85f) + (posRatio * 0.75f)
                } else {
                    // 3등급: 중간자리 부품위치 오타 (11.0 ~ 12.0점)
                    (exactBoost * 0.70f) + (posRatio * 1.5f)
                }
            }
        }

        // 2. 1자 삽입/삭제 길이 차이 (11.25점)
        if (abs(s1.length - s2.length) == 1 && s1.length >= 4 && s2.length >= 4) {
            if (isDtcLengthDiffMatch(s1, s2)) {
                return exactBoost * 0.75f
            }
        }

        // 3. 4등급: 동일 제어기/시스템 계통 일치 (앞 3자리 일치, 예: C12..., B12..., P0A...)
        if (s1.length >= 3 && s2.length >= 3) {
            val prefix1 = s1.take(3)
            val prefix2 = s2.take(3)
            if (prefix1 == prefix2) {
                val has4Prefix = s1.length >= 4 && s2.length >= 4 && s1.take(4) == s2.take(4)
                return if (has4Prefix) exactBoost * 0.60f else exactBoost * 0.55f // 9.0f 또는 8.25f
            }
        }

        if (s1.contains(s2) || s2.contains(s1)) {
            return exactBoost * 0.50f
        }
        return 0.0f
    }

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
