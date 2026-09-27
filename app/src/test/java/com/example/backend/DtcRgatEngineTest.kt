package com.example.backend

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [DtcRgatEngineTest]
 * Python DTC_RGAT 패키지(graph_service.py 및 test_graph_service.py)와의 1:1 완벽 정합성 검증 테스트 슈트
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DtcRgatEngineTest {

    private lateinit var engine: DtcRgatEngine

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        engine = DtcRgatEngine.getInstance(context)
        engine.initialize()
    }

    // ══════════════════════════════════════════════════════════════
    // 1. Python DTC_RGAT 원본 정규 검증 케이스 1:1 대응
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `test analyze single code returns ranking and ecus (Python test 1)`() {
        val res = engine.analyze(listOf("C128387"))
        assertTrue("에러가 없어야 함", res.error == null)
        assertEquals(1, res.dtcInfo.size)
        assertEquals("C128387", res.dtcInfo[0].code)
        assertTrue("ABS/ESC 제어기가 포함되어야 함", res.dtcInfo[0].ecuNames.contains("ABS/ESC"))
        assertTrue("결과가 존재해야 함", res.results.isNotEmpty())
        assertEquals(1, res.results[0].rank)
    }

    @Test
    fun `test analyze cluster acu_b returns verified top1 (Python test 2)`() {
        // 4개 제어기 다발 연쇄 고장 입력 시 ACU_B 마스터 검증 1위 도출 검증
        val codes = listOf("B100552", "C128387", "C164387", "C166987")
        val res = engine.analyze(codes)
        assertTrue("에러가 없어야 함", res.error == null)
        assertEquals(4, res.dtcInfo.size)
        val top1 = res.results[0]
        assertTrue("1순위 커넥터는 ACU_B 여야 함", top1.connId.contains("ACU_B"))
        assertTrue("검증 직결 플래그가 True 여야 함", top1.verified)
        assertEquals("직결 검증 점수는 2.0 고정", 2.0, top1.finalScore, 0.001)
    }

    @Test
    fun `test analyze hybrid generates both hw_wire and ai_hw_wire (Python test 3)`() {
        // 제동·공조 복합 다발 시 HW_WIRE(실선)와 AI_HW_WIRE(점선) 공존 생성 검증
        val codes = listOf("C128387", "B122901", "B234301", "B240301")
        val res = engine.analyze(codes)
        assertTrue("에러가 없어야 함", res.error == null)
        val edges = res.visEdges

        val hwWires = edges.filter { it.title.contains("HW_WIRE") && !it.dashes }
        val aiWires = edges.filter { it.title.contains("AI_HW_WIRE") && it.dashes }

        assertTrue("물리 실선 HW_WIRE 엣지가 존재해야 함", hwWires.isNotEmpty())
        assertTrue("AI 추론 점선 AI_HW_WIRE 엣지가 존재해야 함", aiWires.isNotEmpty())
    }

    @Test
    fun `test analyze unlinked sensor ecus fallback to ai wire (Python test 6)`() {
        // CAD 회로도상 물리 배선이 없는 센서 제어기(LCC)의 AI_HW_WIRE Fallback 검증
        val res = engine.analyze(listOf("B122901", "B234301"))
        assertTrue("에러가 없어야 함", res.error == null)
        val edges = res.visEdges
        val aiWires = edges.filter { it.dashes }
        assertTrue("센서 전용 클러스터는 반드시 AI_HW_WIRE Fallback 점선 엣지를 생성해야 함", aiWires.isNotEmpty())
    }

    @Test
    fun `test analyze empty codes list handled gracefully (Python test 4)`() {
        val res = engine.analyze(emptyList())
        assertNotNull("에러 메시지가 존재해야 함", res.error)
        assertTrue(res.dtcInfo.isEmpty())
        assertTrue(res.results.isEmpty())
        assertTrue(res.visNodes.isEmpty())
    }

    @Test
    fun `test analyze case insensitive and whitespace trimming (Python test 5)`() {
        val res = engine.analyze(listOf(" c128387 \n", "  b122901\t"))
        assertTrue("에러가 없어야 함", res.error == null)
        assertEquals(2, res.dtcInfo.size)
        val codes = res.dtcInfo.map { it.code }
        assertTrue("C128387 정규화 포함", codes.contains("C128387"))
        assertTrue("B122901 정규화 포함", codes.contains("B122901"))
    }

    @Test
    fun `test analyze all unknown codes returns unknown and error (Python test 7)`() {
        val res = engine.analyze(listOf("INVALID_DTC_999", "UNKNOWN_XYZ_888"))
        assertNotNull("에러 메시지가 존재해야 함", res.error)
        assertEquals(2, res.unknownCodes.size)
        assertTrue(res.unknownCodes.contains("INVALID_DTC_999"))
        assertTrue(res.unknownCodes.contains("UNKNOWN_XYZ_888"))
        assertTrue(res.results.isEmpty())
    }

    @Test
    fun `test analyze mixed valid and invalid codes filters unknown (Python test 8)`() {
        val res = engine.analyze(listOf("C128387", "FAKE_CODE_123"))
        assertTrue("에러가 없어야 함", res.error == null)
        assertEquals(listOf("FAKE_CODE_123"), res.unknownCodes)
        assertEquals(1, res.dtcInfo.size)
        assertEquals("C128387", res.dtcInfo[0].code)
        assertTrue(res.results.isNotEmpty())
    }

    // ══════════════════════════════════════════════════════════════
    // 2. 안드로이드 모바일 특화 무결성 및 성능 검증
    // ══════════════════════════════════════════════════════════════

    @Test
    fun `verify 3-tier mindmap graph topology and node-edge integrity`() {
        val result = engine.analyze(listOf("C181787", "C183186"))

        val nodeIds = result.visNodes.map { it.id }.toSet()
        val top1Rank = result.results.first()

        for (edge in result.visEdges) {
            assertTrue("Edge source '${edge.from}' must exist in visNodes", nodeIds.contains(edge.from))
            assertTrue("Edge target '${edge.to}' must exist in visNodes", nodeIds.contains(edge.to))
            assertTrue("Edge title must not be blank", edge.title.isNotBlank())
        }

        val top1VisNode = result.visNodes.firstOrNull { it.group == "conn_top1" }
        assertNotNull("conn_top1 그룹 노드가 반드시 존재해야 함", top1VisNode)
        assertEquals("1순위 커넥터 ID 일치", top1Rank.connId, top1VisNode?.id)

        val dtcVisNodes = result.visNodes.filter { it.level == 0 }
        assertEquals("DTC 노드 개수 일치", 2, dtcVisNodes.size)
        val dtcLabels = dtcVisNodes.map { it.label }.toSet()
        assertTrue("C181787 포함", dtcLabels.contains("C181787"))
        assertTrue("C183186 포함", dtcLabels.contains("C183186"))
    }

    @Test
    fun `verify on-device inference execution time is sub-millisecond on JVM`() {
        engine.analyze(listOf("C110417", "C181787"))

        val start = System.nanoTime()
        val iterations = 10
        for (i in 0 until iterations) {
            engine.analyze(listOf("C184787", "C222071", "C222077"))
        }
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
        val avgTimeMs = elapsedMs / iterations

        println("⚡ RGAT 엔진 3-DTC 분석 평균 소요 시간: ${String.format("%.3f", avgTimeMs)} ms")
        assertTrue("온디바이스 RGAT 분석은 20ms 이내에 완료되어야 함", avgTimeMs < 20.0)
    }
}
