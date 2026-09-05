package com.example.backend

import android.util.Log
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

        data class IntermediateEntry(
            val entry: VectorDbEntry,
            val scoreDetail: RecommendationScorer.ScoreDetail
        )

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

        // 1. Keyword Track Ranking (1-indexed)
        val keywordRanked = intermediateList.sortedByDescending { it.scoreDetail.keywordTrackScore + it.scoreDetail.bonusScore }
        val keywordRankMap = keywordRanked.mapIndexed { index, item -> item.entry.id to (index + 1) }.toMap()

        // 2. Vector Track Ranking (1-indexed)
        val vectorRanked = intermediateList.sortedByDescending { it.scoreDetail.vectorTrackScore + it.scoreDetail.bonusScore }
        val vectorRankMap = vectorRanked.mapIndexed { index, item -> item.entry.id to (index + 1) }.toMap()

        // 3. RRF Hyperparameter & Weight Scaling from Active Weights
        val kConstant = 60.0f
        val wKeyword = 1.0f
        val wVector = 1.0f

        val maxTheoreticalRrf = (wKeyword / (kConstant + 1.0f)) + (wVector / (kConstant + 1.0f))

        val rrfResults = intermediateList.map { item ->
            val kRank = keywordRankMap[item.entry.id] ?: intermediateList.size
            val vRank = vectorRankMap[item.entry.id] ?: intermediateList.size

            val rrfScore = (wKeyword / (kConstant + kRank)) + (wVector / (kConstant + vRank))

            // Check if exact DTC match occurred
            val isExactDtcMatch = item.scoreDetail.dtcBoost >= activeWeights.dtcExactBoost && item.scoreDetail.dtcBoost > 0f

            // Calculate confidence percentage smoothly scaled to 0~100%
            val confidence: Float = if (isExactDtcMatch) {
                (98.0f + (item.scoreDetail.cosSim * 2.0f)).coerceIn(98.0f, 100.0f)
            } else {
                ((rrfScore / maxTheoreticalRrf) * 100.0f).coerceIn(0.0f, 100.0f)
            }

            SearchResult(
                id = item.entry.id,
                text = item.entry.text,
                score = rrfScore,
                recommendations = item.entry.recommendations,
                metadata = item.entry.metadata,
                confidencePercent = confidence
            )
        }

        val topResults = rrfResults.sortedByDescending { it.score }.take(topK)
        Log.i("RAGSearcher", "=== RAG Search: '$query' ===")
        topResults.forEachIndexed { idx, res ->
            Log.i("RAGSearcher", "  Top ${idx + 1}: [${res.metadata.dtcCode}] ${res.metadata.component} | ${"%.1f".format(res.confidencePercent)}% | ${res.text.take(50)}")
        }
        return topResults

    }
}
