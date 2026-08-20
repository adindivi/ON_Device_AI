package com.example.backend

import java.util.regex.Pattern
import kotlin.math.min
import kotlin.math.sqrt

data class SearchResult(
    val id: String,
    val text: String,
    val score: Float,
    val recommendations: Int,
    val metadata: VectorDbMetadata,
    val confidencePercent: Float? = null  // null = ONNX 미로딩 → 신뢰도 칩 숨김
)

class RAGSearcher(
    private val vectorDb: SimpleVectorDB,
    private val onnxBertEngine: OnnxBertEmbeddingEngine? = null,
    val scorer: RecommendationScorer = RecommendationScorer()
) {

    fun updateScoringWeights(weights: ScoringWeights) {
        scorer.currentWeights = weights
    }

    fun getEmbedding(text: String, isQuery: Boolean = false): List<Float> {
        val onnxResult = onnxBertEngine?.getEmbedding(text, isQuery = isQuery)
        if (onnxResult != null && onnxResult.isNotEmpty()) {
            return onnxResult
        }

        val cleanText = text.lowercase().trim()
        val vectorSize = 768
        val vec = FloatArray(vectorSize) { 0.001f }

        // 100% Dynamic Component Dictionary Extracted from Vector DB at Runtime
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

        // Char ngram hash distribution for general text similarity across 768 dimensions
        for (i in 0 until cleanText.length - 1) {
            val bigram = cleanText.substring(i, i + 2)
            val hash1 = (bigram.hashCode() and 0x7fffffff)
            val hash2 = ((bigram.hashCode() * 31 + 17) and 0x7fffffff)
            vec[hash1 % vectorSize] += 0.15f
            vec[hash2 % vectorSize] += 0.08f
        }

        // Normalize
        var sumSq = 0.0f
        for (v in vec) {
            sumSq += v * v
        }
        val normVal = sqrt(sumSq.toDouble()).toFloat().coerceAtLeast(1e-6f)
        return vec.map { it / normVal }
    }

    fun search(query: String, topK: Int = 3, weights: ScoringWeights? = null): List<SearchResult> {
        val allEntries = vectorDb.getAll()
        if (allEntries.isEmpty()) return emptyList()

        // Filter out fallback sample entries when real Vector DB documents exist
        val sampleIds = setOf("DOC-DEFAULT-1", "DOC-DEFAULT-2", "DOC-DEFAULT-3", "TSB-03-15", "CASE-11-02")
        val hasRealDocs = allEntries.any { it.id !in sampleIds }
        val entries = if (hasRealDocs) {
            allEntries.filter { it.id !in sampleIds }
        } else {
            allEntries
        }

        val queryLower = query.lowercase().trim()
            .replace("피워", "파워")
        val queryEmb = getEmbedding(queryLower, isQuery = true)

        // Find DTC code patterns from query (e.g. P0A0A12, P00B101, C120601, P0301)
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

        // Extract query terms for term overlap matching
        val queryTerms = queryLower.split(Regex("[\\s,.\\[\\]()]+")).filter { it.length >= 2 }

        val activeWeights = weights ?: scorer.currentWeights
        val results = mutableListOf<SearchResult>()

        // ONNX 로딩 여부 확인 → false면 신뢰도 칩 숨김(null)
        val isOnnxActive = onnxBertEngine?.isReady == true

        // DTC를 제외한 기본 최대 점수 (항상 유효한 분모)
        val baseMaxScore = activeWeights.aiAmpScale +
                           1.0f +
                           activeWeights.compMatchBoost +
                           (activeWeights.textOverlapBoost * 6.0f) +
                           (activeWeights.upvoteBonusScale * 6.6f)

        for (entry in entries) {
            val scoreDetail = scorer.calculateScore(
                queryLower = queryLower,
                queryEmb = queryEmb,
                dtcPatterns = dtcPatterns,
                queryTerms = queryTerms,
                entry = entry,
                queryEmbeddingProvider = { getEmbedding(it) },
                weights = activeWeights
            )

            // 동적 maxScore: DTC 정확 매칭 시 슈퍼 maxScore 연산
            val dynamicMaxScore = if (scoreDetail.dtcBoost >= activeWeights.dtcExactBoost && scoreDetail.dtcBoost > 0f) {
                100.0f + activeWeights.dtcExactBoost + baseMaxScore
            } else if (scoreDetail.dtcBoost > 0f) {
                baseMaxScore + activeWeights.dtcExactBoost
            } else {
                baseMaxScore
            }

            // 항상 일치도 % 계산 (DTC 및 키워드/유사도 종합 가중치 점수 기반 100% 표출)
            val confidence: Float = if (dynamicMaxScore > 0f) {
                (scoreDetail.finalScore / dynamicMaxScore * 100f).coerceIn(0f, 100f)
            } else 0.0f

            results.add(
                SearchResult(
                    id = entry.id,
                    text = entry.text,
                    score = scoreDetail.finalScore,
                    recommendations = entry.recommendations,
                    metadata = entry.metadata,
                    confidencePercent = confidence
                )
            )
        }

        return results.sortedByDescending { it.score }.take(topK)

    }
}
