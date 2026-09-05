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
        queryEmb: List<Float>,
        dtcPatterns: List<String>,
        queryTerms: List<String>,
        entry: VectorDbEntry,
        queryEmbeddingProvider: (String) -> List<Float>,
        weights: ScoringWeights = currentWeights
    ): ScoreDetail {
        val docTextLower = entry.text.lowercase()
        val compLower = entry.metadata.component.lowercase().trim()
        val docEmb = entry.embedding.ifEmpty { queryEmbeddingProvider(entry.text) }

        // 1. Cosine Similarity & AI Non-linear Amplification
        val cosSim = computeCosineSimilarity(queryEmb, docEmb)
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

        // 6. DTC Match Boost
        var dtcBoost = 0.0f
        val docDtcs = entry.metadata.dtcs.ifEmpty {
            if (entry.metadata.dtcCode.isNotBlank()) listOf(entry.metadata.dtcCode) else emptyList()
        }

        val typoRatio = if (weights.dtcExactBoost > 0f) (12.0f / 15.0f) else 0.8f
        val fuzzyBoost = weights.dtcExactBoost * typoRatio

        for (docDtc in docDtcs) {
            if (docDtc.isBlank()) continue
            val docDtcClean = docDtc.trim()

            for (pattern in dtcPatterns) {
                if (pattern.length >= 3) {
                    if (docDtcClean.equals(pattern, ignoreCase = true) || docTextLower.contains(pattern)) {
                        dtcBoost = weights.dtcExactBoost
                        break
                    } else if (isDtcFuzzyMatch(docDtcClean, pattern)) {
                        dtcBoost = fuzzyBoost
                        break
                    }
                }
            }

            if (queryLower.contains(docDtcClean.lowercase()) || isDtcFuzzyMatch(docDtcClean, queryLower)) {
                dtcBoost = maxOf(dtcBoost, fuzzyBoost)
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

    private fun isDtcFuzzyMatch(s1: String, s2: String): Boolean {
        val str1 = s1.uppercase().trim()
        val str2 = s2.uppercase().trim()
        if (str1 == str2) return true
        if (str1.length < 4 || str2.length < 4) return false
        if (str1.contains(str2) || str2.contains(str1)) return true

        if (abs(str1.length - str2.length) <= 1) {
            val len = minOf(str1.length, str2.length)
            var diff = 0
            for (i in 0 until len) {
                if (str1[i] != str2[i]) {
                    diff++
                    if (diff > 1) return false
                }
            }
            return diff <= 1
        }
        return false
    }

    private fun computeCosineSimilarity(v1: List<Float>, v2: List<Float>): Float {
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
