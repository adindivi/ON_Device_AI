package com.example.backend

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/**
 * [DtcRgatEngine]
 * Relational Graph Attention Network (RGAT) 기반 차량 전장 DTC-하네스 커넥터 근본 원인 추론 엔진
 * 
 * Clean Code & SRP 리팩토링:
 * 1. Single Responsibility Principle:
 *    - 데이터 적재(GraphLoader), 점수 산출(ScoringPipeline), 시각화 그래프 구성(VisGraphBuilder)으로 기능 분리
 * 2. Type Safety:
 *    - `KnowledgeGraphNode` 인터페이스를 통한 타입 안전성 보장 (Any 캐스팅 제거)
 * 3. 상수화 & 가독성:
 *    - 엔진 하이퍼파라미터 및 가중치 매직 넘버 상수화
 *    - 4자리 소수점 반올림 및 정규화 확장 함수 도입
 */
class DtcRgatEngine private constructor(private val context: Context) {

    companion object {
        private const val TAG = "DtcRgatEngine"
        private const val GRAPH_ASSET_PATH = "DB/dtc_knowledge_graph.json"
        private const val EMBED_ASSET_PATH = "DB/rgat_embeddings.bin"

        // 하이퍼파라미터 및 기본 가중치
        const val DEFAULT_ALPHA = 0.7  // 2-Hop 물리 배선 구조 점수 가중치
        const val DEFAULT_BETA = 0.3   // 64차원 RGAT 시맨틱 유사도 가중치
        const val EMBEDDING_DIM = 64   // 사전 학습 노드 임베딩 차원
        const val VERIFIED_SCORE = 2.0 // 마스터 직결 검증(HW_MAP) 고정 점수
        private const val MASK_PENALTY = -1e9 // 비도달 노드 마스킹 페널티
        private const val MIN_EPSILON = 1e-9

        @Volatile
        private var instance: DtcRgatEngine? = null

        fun getInstance(context: Context): DtcRgatEngine {
            return instance ?: synchronized(this) {
                instance ?: DtcRgatEngine(context.applicationContext).also {
                    it.initialize()
                    instance = it
                }
            }
        }
    }

    // ── 도메인 노드 타입 계층 (Type Safety) ──────────────────────────
    sealed interface KnowledgeGraphNode {
        val id: String
    }

    data class DtcNode(
        override val id: String,
        val code: String,
        val ecuName: String,
        val description: String,
        val faultCategory: String,
        val systemCode: String,
        val systemName: String,
        val mfrSpecific: String,
        val subtypeCode: String,
        val subtypeName: String,
        val position: String,
        val color: String
    ) : KnowledgeGraphNode

    data class EcuNode(
        override val id: String,
        val name: String,
        val ecuInfo: String,
        val color: String
    ) : KnowledgeGraphNode

    data class ConnectorNode(
        override val id: String,
        val name: String,
        val location: String,
        val color: String
    ) : KnowledgeGraphNode

    // ── 분석 결과 모델 ──────────────────────────────────────────
    data class RgatConnectorRank(
        val rank: Int,
        val connId: String,
        val name: String,
        val location: String,
        val finalScore: Double,
        val structScore: Double,
        val rgatScore: Double,
        val nHit: Int,
        val nValid: Int,
        val hitCodes: List<String>,
        val connEcus: List<String>,
        val verified: Boolean,
        val isReachable: Boolean
    )

    data class RgatVisNode(
        val id: String,
        val label: String,
        val group: String, // "dtc_input", "ecu", "conn_top1", "conn_top", "conn"
        val title: String,
        val size: Int,
        val level: Int // 0: DTC, 1: ECU, 2: Connector
    )

    data class RgatVisEdge(
        val from: String,
        val to: String,
        val color: String,
        val title: String,
        val width: Float,
        val dashes: Boolean
    )

    data class RgatDtcSummary(
        val code: String,
        val desc: String,
        val cat: String,
        val ecuNames: List<String>,
        val subtypeCode: String,
        val subtypeName: String
    )

    data class RgatAnalysisResult(
        val inputCodes: List<String>,
        val unknownCodes: List<String>,
        val dtcInfo: List<RgatDtcSummary>,
        val results: List<RgatConnectorRank>,
        val visNodes: List<RgatVisNode>,
        val visEdges: List<RgatVisEdge>,
        val topologyMaskApplied: Boolean,
        val reachableConnsCount: Int,
        val error: String? = null
    )

    // ── 인메모리 지식 그래프 색인 저장소 ──────────────────────────────
    private var isInitialized = false
    private val dtcNodes = HashMap<String, DtcNode>()
    private val ecuNodes = HashMap<String, EcuNode>()
    private val connNodes = HashMap<String, ConnectorNode>()
    private val nodeIdToIdx = HashMap<String, Int>()

    // 인접 관계 인덱스
    private val dtcToEcus = HashMap<String, MutableSet<String>>()
    private val dtcToConns = HashMap<String, MutableSet<String>>()
    private val ecuToConns = HashMap<String, MutableSet<String>>()
    private val connToEcus = HashMap<String, MutableSet<String>>()
    private val codeToNids = HashMap<String, MutableList<String>>()

    private val connIds = ArrayList<String>()
    private val connIdToIdx = HashMap<String, Int>()
    private var embeddings: Array<FloatArray> = emptyArray()

    val isReady: Boolean get() = isInitialized

    // ── 초기화 파이프라인 (SRP: Asset Loading) ────────────────────────
    @Synchronized
    fun initialize() {
        if (isInitialized) return
        val startTime = System.currentTimeMillis()
        try {
            loadEmbeddings()
            loadKnowledgeGraph()

            isInitialized = true
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "✅ DtcRgatEngine initialized in ${elapsed}ms. " +
                    "DTCs: ${dtcNodes.size}, ECUs: ${ecuNodes.size}, Conns: ${connIds.size}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize DtcRgatEngine: ${e.message}", e)
        }
    }

    private fun loadEmbeddings() {
        context.assets.open(EMBED_ASSET_PATH).use { input ->
            val bytes = input.readBytes()
            val floatBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
            val totalFloats = floatBuffer.remaining()
            val totalNodes = totalFloats / EMBEDDING_DIM
            embeddings = Array(totalNodes) { FloatArray(EMBEDDING_DIM) }
            for (i in 0 until totalNodes) {
                floatBuffer.get(embeddings[i])
            }
            Log.d(TAG, "Loaded embeddings: $totalNodes nodes × $EMBEDDING_DIM dim (${bytes.size / 1024} KB)")
        }
    }

    private fun loadKnowledgeGraph() {
        val jsonString = context.assets.open(GRAPH_ASSET_PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(jsonString)
        parseNodes(root.getJSONArray("nodes"))
        parseEdges(root.getJSONArray("edges"))
    }

    private fun parseNodes(nodesArray: JSONArray) {
        for (i in 0 until nodesArray.length()) {
            val n = nodesArray.getJSONObject(i)
            val id = n.getString("id")
            nodeIdToIdx[id] = i

            when (n.optString("node_type")) {
                "DTC" -> {
                    val code = n.optString("code").uppercase().trim()
                    val dtc = DtcNode(
                        id = id,
                        code = code,
                        ecuName = n.optString("ecu_name"),
                        description = n.optString("description"),
                        faultCategory = n.optString("fault_category"),
                        systemCode = n.optString("system_code"),
                        systemName = n.optString("system_name"),
                        mfrSpecific = n.optString("mfr_specific"),
                        subtypeCode = n.optString("subtype_code"),
                        subtypeName = n.optString("subtype_name"),
                        position = n.optString("position"),
                        color = n.optString("color", "#4A90D9")
                    )
                    dtcNodes[id] = dtc
                    if (code.isNotEmpty()) {
                        codeToNids.getOrPut(code) { ArrayList() }.add(id)
                    }
                }
                "ECU" -> {
                    val ecu = EcuNode(
                        id = id,
                        name = n.optString("name"),
                        ecuInfo = n.optString("ecu_info"),
                        color = n.optString("color", "#E67E22")
                    )
                    ecuNodes[id] = ecu
                }
                "Connector" -> {
                    val conn = ConnectorNode(
                        id = id,
                        name = n.optString("name", id),
                        location = n.optString("location"),
                        color = n.optString("color", "#27AE60")
                    )
                    connNodes[id] = conn
                    connIds.add(id)
                    connIdToIdx[id] = connIds.size - 1
                }
            }
        }
    }

    private fun parseEdges(edgesArray: JSONArray) {
        for (i in 0 until edgesArray.length()) {
            val e = edgesArray.getJSONObject(i)
            val rel = e.optString("rel")
            val src = e.optString("source")
            val dst = e.optString("target")

            when (rel) {
                "SW_IN", "SW_LOGIC" -> dtcToEcus.getOrPut(src) { HashSet() }.add(dst)
                "HW_MAP" -> dtcToConns.getOrPut(src) { HashSet() }.add(dst)
                "HW_WIRE" -> {
                    ecuToConns.getOrPut(src) { HashSet() }.add(dst)
                    ecuNodes[src]?.let { ecu ->
                        connToEcus.getOrPut(dst) { HashSet() }.add(ecu.name)
                    }
                }
            }
        }
    }

    // ── 분석 파이프라인 (SRP: Analysis Pipeline) ─────────────────────
    fun analyze(
        inputCodes: List<String>,
        topologyMask: Boolean = true,
        topK: Int = 10,
        alpha: Double = DEFAULT_ALPHA,
        beta: Double = DEFAULT_BETA
    ): RgatAnalysisResult {
        if (!isInitialized) initialize()
        if (!isInitialized) {
            return createErrorResult(inputCodes, "RGAT 엔진 초기화 실패")
        }

        // 1. 입력 DTC 검증 및 토폴로지/임베딩 추출
        val (validDtcList, unknownCodes) = resolveDtcInputs(inputCodes)
        val nValid = validDtcList.size
        if (nValid == 0) {
            return createErrorResult(inputCodes, "유효한 DTC 코드가 없습니다.", unknownCodes)
        }

        // 2. 검증 직결 HW_MAP 매핑 집계
        val verifiedConnCodes = aggregateVerifiedMappings(validDtcList)

        // 3. 구조 점수 (2-Hop 배선 도달 비율) 계산
        val (structScores, structHits, allReachableConns) = computeStructuralScores(validDtcList, nValid)

        // 4. RGAT 임베딩 내적 시맨틱 유사도 계산
        val rgatScores = computeRgatScores(validDtcList)

        // 5. 하이브리드 점수 산출 및 토폴로지 마스킹
        val effectiveScores = calculateHybridScores(structScores, rgatScores, allReachableConns, topologyMask, alpha, beta)

        // 6. 최우선 검증 커넥터 + 랭킹 커넥터 조립
        val results = assembleConnectorRankings(
            verifiedConnCodes = verifiedConnCodes,
            effectiveScores = effectiveScores,
            structScores = structScores,
            rgatScores = rgatScores,
            structHits = structHits,
            allReachableConns = allReachableConns,
            nValid = nValid,
            topK = topK,
            topologyMask = topologyMask
        )

        // 7. 3계층 시각화 지식 그래프 생성
        val (visNodes, visEdges) = buildVisualizationGraph(validDtcList, results.take(5), nValid)

        val dtcSummaryList = validDtcList.map { it.toSummary() }
        val maskApplied = topologyMask && allReachableConns.isNotEmpty()

        return RgatAnalysisResult(
            inputCodes = inputCodes,
            unknownCodes = unknownCodes,
            dtcInfo = dtcSummaryList,
            results = results,
            visNodes = visNodes,
            visEdges = visEdges,
            topologyMaskApplied = maskApplied,
            reachableConnsCount = allReachableConns.size
        )
    }

    // ── 세부 분석 단계별 SRP 헬퍼 함수들 ─────────────────────────────

    private data class InternalDtcInfo(
        val code: String,
        val nids: List<String>,
        val ecuNames: List<String>,
        val structConns: Set<String>,
        val meanEmb: FloatArray,
        val desc: String,
        val cat: String,
        val subtypeCode: String,
        val subtypeName: String
    ) {
        fun toSummary(): RgatDtcSummary = RgatDtcSummary(
            code = code,
            desc = desc,
            cat = cat,
            ecuNames = ecuNames,
            subtypeCode = subtypeCode,
            subtypeName = subtypeName
        )
    }

    private fun resolveDtcInputs(inputCodes: List<String>): Pair<List<InternalDtcInfo>, List<String>> {
        val unknown = ArrayList<String>()
        val validList = ArrayList<InternalDtcInfo>()

        for (rawCode in inputCodes) {
            val codeUpper = rawCode.uppercase().trim()
            val nids = codeToNids[codeUpper]
            if (nids.isNullOrEmpty()) {
                unknown.add(codeUpper)
                continue
            }

            val reachedEcus = HashSet<String>()
            val reachedConns = HashSet<String>()
            for (nid in nids) {
                dtcToEcus[nid]?.let { eids ->
                    for (eid in eids) {
                        reachedEcus.add(eid)
                        ecuToConns[eid]?.let { reachedConns.addAll(it) }
                    }
                }
                dtcToConns[nid]?.let { reachedConns.addAll(it) }
            }

            val meanEmb = computeMeanEmbedding(nids)
            val firstDtc = dtcNodes[nids[0]]
            val ecuNames = reachedEcus.mapNotNull { ecuNodes[it]?.name }.sorted()

            validList.add(
                InternalDtcInfo(
                    code = codeUpper,
                    nids = nids,
                    ecuNames = ecuNames,
                    structConns = reachedConns,
                    meanEmb = meanEmb,
                    desc = firstDtc?.description?.take(70) ?: "",
                    cat = firstDtc?.faultCategory ?: "",
                    subtypeCode = firstDtc?.subtypeCode ?: "",
                    subtypeName = firstDtc?.subtypeName ?: ""
                )
            )
        }
        return Pair(validList, unknown)
    }

    private fun computeMeanEmbedding(nids: List<String>): FloatArray {
        val meanEmb = FloatArray(EMBEDDING_DIM)
        var embCount = 0
        for (nid in nids) {
            val idx = nodeIdToIdx[nid] ?: continue
            if (idx < embeddings.size) {
                val emb = embeddings[idx]
                for (k in 0 until EMBEDDING_DIM) {
                    meanEmb[k] += emb[k]
                }
                embCount++
            }
        }
        if (embCount > 0) {
            for (k in 0 until EMBEDDING_DIM) {
                meanEmb[k] /= embCount.toFloat()
            }
        }
        return meanEmb
    }

    private fun aggregateVerifiedMappings(dtcList: List<InternalDtcInfo>): Map<String, Set<String>> {
        val verifiedConnCodes = HashMap<String, MutableSet<String>>()
        for (info in dtcList) {
            for (nid in info.nids) {
                dtcToConns[nid]?.let { cids ->
                    for (cid in cids) {
                        verifiedConnCodes.getOrPut(cid) { HashSet() }.add(info.code)
                    }
                }
            }
        }
        return verifiedConnCodes
    }

    private data class StructuralAnalysisResult(
        val structScores: DoubleArray,
        val structHits: Map<String, Set<String>>,
        val allReachableConns: Set<String>
    )

    private fun computeStructuralScores(
        dtcList: List<InternalDtcInfo>,
        nValid: Int
    ): StructuralAnalysisResult {
        val structHits = HashMap<String, MutableSet<String>>()
        val allReachableConns = HashSet<String>()

        for (info in dtcList) {
            for (cid in info.structConns) {
                structHits.getOrPut(cid) { HashSet() }.add(info.code)
                allReachableConns.add(cid)
            }
        }

        val structScores = DoubleArray(connIds.size) { i ->
            val cid = connIds[i]
            val hits = structHits[cid]?.size ?: 0
            hits.toDouble() / nValid
        }
        return StructuralAnalysisResult(structScores, structHits, allReachableConns)
    }

    private fun computeRgatScores(dtcList: List<InternalDtcInfo>): DoubleArray {
        val sumEmb = FloatArray(EMBEDDING_DIM)
        for (info in dtcList) {
            for (k in 0 until EMBEDDING_DIM) {
                sumEmb[k] += info.meanEmb[k]
            }
        }

        val rgatRaw = DoubleArray(connIds.size)
        var minVal = Double.MAX_VALUE
        var maxVal = -Double.MAX_VALUE

        for (i in connIds.indices) {
            val cid = connIds[i]
            val nodeIdx = nodeIdToIdx[cid] ?: 0
            val cEmb = if (nodeIdx < embeddings.size) embeddings[nodeIdx] else FloatArray(EMBEDDING_DIM)
            var dot = 0.0
            for (k in 0 until EMBEDDING_DIM) {
                dot += cEmb[k] * sumEmb[k]
            }
            rgatRaw[i] = dot
            if (dot < minVal) minVal = dot
            if (dot > maxVal) maxVal = dot
        }

        val diff = (maxVal - minVal).coerceAtLeast(MIN_EPSILON)
        return DoubleArray(connIds.size) { i ->
            (rgatRaw[i] - minVal) / diff
        }
    }

    private fun calculateHybridScores(
        structScores: DoubleArray,
        rgatScores: DoubleArray,
        allReachableConns: Set<String>,
        topologyMask: Boolean,
        alpha: Double,
        beta: Double
    ): DoubleArray {
        val effective = DoubleArray(connIds.size) { i ->
            alpha * structScores[i] + beta * rgatScores[i]
        }
        if (topologyMask && allReachableConns.isNotEmpty()) {
            for (i in connIds.indices) {
                if (!allReachableConns.contains(connIds[i])) {
                    effective[i] = MASK_PENALTY
                }
            }
        }
        return effective
    }

    private fun assembleConnectorRankings(
        verifiedConnCodes: Map<String, Set<String>>,
        effectiveScores: DoubleArray,
        structScores: DoubleArray,
        rgatScores: DoubleArray,
        structHits: Map<String, Set<String>>,
        allReachableConns: Set<String>,
        nValid: Int,
        topK: Int,
        topologyMask: Boolean
    ): List<RgatConnectorRank> {
        val results = ArrayList<RgatConnectorRank>()
        val verifiedAdded = HashSet<String>()

        // 1순위: 마스터 직결 검증 커넥터 (Score 2.0)
        val sortedVerified = verifiedConnCodes.keys.sortedByDescending { verifiedConnCodes[it]?.size ?: 0 }
        for (connId in sortedVerified) {
            if (results.size >= topK) break
            val connNode = connNodes[connId]
            val hitCodes = verifiedConnCodes[connId]?.toList()?.sorted() ?: emptyList()
            val connIdx = connIdToIdx[connId] ?: -1
            results.add(
                RgatConnectorRank(
                    rank = results.size + 1,
                    connId = connId,
                    name = connNode?.name ?: connId,
                    location = connNode?.location ?: "",
                    finalScore = VERIFIED_SCORE,
                    structScore = VERIFIED_SCORE,
                    rgatScore = if (connIdx >= 0) rgatScores[connIdx].roundScore() else 0.0,
                    nHit = hitCodes.size,
                    nValid = nValid,
                    hitCodes = hitCodes,
                    connEcus = connToEcus[connId]?.toList()?.sorted() ?: emptyList(),
                    verified = true,
                    isReachable = true
                )
            )
            verifiedAdded.add(connId)
        }

        // 2순위: 하이브리드 점수 랭킹
        val rankedIndices = effectiveScores.indices.sortedByDescending { effectiveScores[it] }
        val maskActive = topologyMask && allReachableConns.isNotEmpty()

        for (connIdx in rankedIndices) {
            if (results.size >= topK) break
            if (maskActive && effectiveScores[connIdx] < -1e8) break

            val connId = connIds[connIdx]
            if (verifiedAdded.contains(connId)) continue

            val connNode = connNodes[connId]
            val hitCodes = structHits[connId]?.toList()?.sorted() ?: emptyList()
            val rawFinalScore = effectiveScores[connIdx]

            results.add(
                RgatConnectorRank(
                    rank = results.size + 1,
                    connId = connId,
                    name = connNode?.name ?: connId,
                    location = connNode?.location ?: "",
                    finalScore = rawFinalScore.roundScore(),
                    structScore = structScores[connIdx].roundScore(),
                    rgatScore = rgatScores[connIdx].roundScore(),
                    nHit = hitCodes.size,
                    nValid = nValid,
                    hitCodes = hitCodes,
                    connEcus = connToEcus[connId]?.toList()?.sorted() ?: emptyList(),
                    verified = false,
                    isReachable = allReachableConns.contains(connId)
                )
            )
        }
        return results
    }

    // ── 3계층 시각화 그래프 빌더 (SRP: VisGraphBuilder) ───────────────
    private fun buildVisualizationGraph(
        dtcList: List<InternalDtcInfo>,
        top5Results: List<RgatConnectorRank>,
        nValid: Int
    ): Pair<List<RgatVisNode>, List<RgatVisEdge>> {
        val visNodes = ArrayList<RgatVisNode>()
        val visEdges = ArrayList<RgatVisEdge>()
        val addedNodeIds = HashSet<String>()
        val addedEdgeKeys = HashSet<String>()

        fun addNode(id: String, label: String, group: String, title: String, size: Int, level: Int) {
            if (addedNodeIds.add(id)) {
                visNodes.add(RgatVisNode(id, label, group, title, size, level))
            }
        }

        fun addEdge(from: String, to: String, color: String, title: String, width: Float, dashes: Boolean) {
            val key = "$from->$to:$title"
            if (addedEdgeKeys.add(key)) {
                visEdges.add(RgatVisEdge(from, to, color, title, width, dashes))
            }
        }

        // 1. Level 2: 커넥터 노드 (우측 종단)
        val top5ConnIds = top5Results.map { it.connId }.toSet()
        for (res in top5Results) {
            val group = when (res.rank) {
                1 -> "conn_top1"
                in 2..3 -> "conn_top"
                else -> "conn"
            }
            val size = when (res.rank) {
                1 -> 32
                in 2..3 -> 24
                else -> 18
            }
            val shortName = if (res.name.contains("/")) res.name.split("/")[0].trim() else res.name
            val displayName = if (shortName.length > 18) shortName.take(18) + "…" else shortName
            val verifiedTag = if (res.verified) "[검증] " else ""

            addNode(
                id = res.connId,
                label = displayName,
                group = group,
                title = "${verifiedTag}커넥터: ${res.name}\n연결 DTC: ${res.nHit}/${nValid}개\n순위: #${res.rank}\n점수: ${res.finalScore}",
                size = size,
                level = 2
            )
        }

        // 2. Level 0 (DTC) -> Level 1 (ECU) -> Level 2 (Connector)
        for (item in dtcList) {
            val dtcVisId = "VIS_DTC::${item.code}"
            addNode(
                id = dtcVisId,
                label = item.code,
                group = "dtc_input",
                title = "DTC: ${item.code}\n카테고리: ${item.cat}\n${item.desc}",
                size = 22,
                level = 0
            )

            val allEcus = ArrayList<String>()
            for (nid in item.nids) {
                dtcToEcus[nid]?.let { eids ->
                    for (eid in eids) {
                        if (!allEcus.contains(eid)) allEcus.add(eid)
                    }
                }
            }

            // Top 5 커넥터와 물리 배선이 닿는 ECU 최우선 선택
            val connectedEcus = allEcus.filter { (ecuToConns[it] ?: emptySet()).any { c -> top5ConnIds.contains(c) } }
            val targetEcus = if (connectedEcus.isNotEmpty()) connectedEcus else allEcus.take(1)

            for (ecuId in targetEcus) {
                val ecuNode = ecuNodes[ecuId]
                val ecuName = ecuNode?.name ?: ecuId
                addNode(
                    id = ecuId,
                    label = ecuName,
                    group = "ecu",
                    title = "ECU: $ecuName",
                    size = 20,
                    level = 1
                )
                // DTC -> ECU (SW_LOGIC)
                addEdge(
                    from = dtcVisId,
                    to = ecuId,
                    color = "#64748b",
                    title = "SW_LOGIC (로직관계)",
                    width = 1.5f,
                    dashes = false
                )
                // ECU -> Connector (HW_WIRE)
                val connectingConns = (ecuToConns[ecuId] ?: emptySet()).filter { top5ConnIds.contains(it) }
                for (cid in connectingConns) {
                    addEdge(
                        from = ecuId,
                        to = cid,
                        color = "#3b82f6",
                        title = "HW_WIRE (물리관계)",
                        width = 2.5f,
                        dashes = false
                    )
                }
            }

            // DTC -> Connector (HW_MAP 직접 검증 링크)
            for (nid in item.nids) {
                val directConns = (dtcToConns[nid] ?: emptySet()).filter { top5ConnIds.contains(it) }
                for (cid in directConns) {
                    addEdge(
                        from = dtcVisId,
                        to = cid,
                        color = "#2563eb",
                        title = "HW_MAP (검증매핑)",
                        width = 2.5f,
                        dashes = false
                    )
                }
            }
        }

        // 3. AI 가상 추천 배선 (Case C-1, Case C-2)
        buildAiFallbackEdges(dtcList, top5Results, visNodes, visEdges, ::addEdge)

        return Pair(visNodes, visEdges)
    }

    private fun buildAiFallbackEdges(
        dtcList: List<InternalDtcInfo>,
        top5Results: List<RgatConnectorRank>,
        visNodes: List<RgatVisNode>,
        visEdges: List<RgatVisEdge>,
        addEdge: (from: String, to: String, color: String, title: String, width: Float, dashes: Boolean) -> Unit
    ) {
        val visNodeIds = visNodes.map { it.id }.toSet()

        // Case C-1: 회로도상 물리 배선이 확인되지 않는 비도달 커넥터
        for (res in top5Results) {
            if (!res.isReachable) {
                var linked = false
                for (item in dtcList) {
                    val nids = codeToNids[item.code] ?: emptyList()
                    for (nid in nids) {
                        val ecus = dtcToEcus[nid] ?: emptySet()
                        for (eid in ecus) {
                            if (visNodeIds.contains(eid)) {
                                addEdge(
                                    eid,
                                    res.connId,
                                    "#93c5fd",
                                    "AI_HW_WIRE (추론물리관계)",
                                    2.0f,
                                    true
                                )
                                linked = true
                                break
                            }
                        }
                        if (linked) break
                    }
                    if (linked) break
                }
            }
        }

        // Case C-2: 활성화된 제어기 중 Top 5 커넥터와 물리 배선이 하나도 없는 제어기
        val ecusWithHw = visEdges.filter { it.title.contains("HW_WIRE") && !it.dashes }.map { it.from }.toSet()
        val visEcus = visNodes.filter { it.group == "ecu" }.map { it.id }
        val unlinkedEcus = visEcus.filter { !ecusWithHw.contains(it) }

        if (top5Results.isNotEmpty()) {
            val targetConns = top5Results.take(2).map { it.connId }
            for (ecuId in unlinkedEcus) {
                for (targetConnId in targetConns) {
                    addEdge(
                        ecuId,
                        targetConnId,
                        "#93c5fd",
                        "AI_HW_WIRE (추론물리관계)",
                        2.0f,
                        true
                    )
                }
            }
        }
    }

    private fun createErrorResult(
        inputCodes: List<String>,
        errorMessage: String,
        unknownCodes: List<String> = inputCodes
    ): RgatAnalysisResult = RgatAnalysisResult(
        inputCodes = inputCodes,
        unknownCodes = unknownCodes,
        dtcInfo = emptyList(),
        results = emptyList(),
        visNodes = emptyList(),
        visEdges = emptyList(),
        topologyMaskApplied = false,
        reachableConnsCount = 0,
        error = errorMessage
    )

    // 소수점 4자리 반올림 유틸리티
    private fun Double.roundScore(): Double = String.format(Locale.US, "%.4f", this).toDouble()
}
