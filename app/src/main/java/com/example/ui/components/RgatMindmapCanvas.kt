package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.DtcRgatEngine
import kotlin.math.roundToInt

/**
 * [MindmapTokens]
 * 3계층 마인드맵 전용 다크 핀테크 색상 및 레이아웃 토큰
 */
private object MindmapTokens {
    val CanvasBg = Color(0xFF191F28)
    val CanvasBorder = Color(0xFF333D4B)
    val CapsuleBg = Color(0xFF262E3D)

    // 열 타이틀 색상
    val TitleDtc = Color(0xFF79B8FF)
    val TitleEcu = Color(0xFFFFA657)
    val TitleConn = Color(0xFF56D364)

    // 엣지 관계선 색상
    val EdgeHwMap = Color(0xFF38BDF8)     // 직결 검증 (스카이블루)
    val EdgeHwWire = Color(0xFF3182F6)    // 물리 배선 (토스 블루)
    val EdgeAiWire = Color(0xFF8B5CF6)    // AI 추론 가상 배선 (보라)
    val EdgeLogic = Color(0xFF4E5968)     // 소프트웨어 논리 관계 (그레이)

    // 선택 하이라이트
    val SelectedBorder = Color(0xFFFFDD00)

    // 노드 스타일 사양
    data class NodeStyle(val bg: Color, val border: Color, val text: Color)

    fun resolveNodeStyle(node: DtcRgatEngine.RgatVisNode): NodeStyle = when {
        node.group == "conn_top1" -> NodeStyle(Color(0xFF1B335A), Color(0xFF3182F6), Color.White)
        node.group == "conn_top" -> NodeStyle(Color(0xFF143022), Color(0xFF34C759), Color(0xFFD4F8DE))
        node.level == 2 -> NodeStyle(Color(0xFF142B1F), Color(0xFF2EA043), Color(0xFFDCFCE7))
        node.level == 1 -> NodeStyle(Color(0xFF2E241E), Color(0xFFFF9E40), Color(0xFFFFE6D0))
        else -> NodeStyle(Color(0xFF192A42), Color(0xFF5B9DFF), Color(0xFFE0EDFF))
    }
}

/**
 * [RgatMindmapCanvas]
 * 3계층 (DTC ➔ ECU ➔ 커넥터) 핀테크 대화형 지식 그래프 캔버스
 * 
 * Clean Code 리팩토링:
 * 1. Single Responsibility: 헤더 태그, 엣지 렌더러, 노드 레이어, 툴팁 분리
 * 2. Visual Tokens: [MindmapTokens] 객체를 통한 색상 중앙화
 * 3. Layout Optimization: 중앙 정렬 알고리즘 및 핀치 줌 상태 관리 정돈
 */
@Composable
fun RgatMindmapCanvas(
    visNodes: List<DtcRgatEngine.RgatVisNode>,
    visEdges: List<DtcRgatEngine.RgatVisEdge>,
    modifier: Modifier = Modifier
) {
    if (visNodes.isEmpty()) {
        EmptyMindmapPlaceholder(modifier = modifier)
        return
    }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }
    var selectedNode by remember { mutableStateOf<DtcRgatEngine.RgatVisNode?>(null) }

    // 1위 추천 커넥터 펄스 애니메이션 (토스 블루 글로우)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_top1")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val density = LocalDensity.current
    val topMarginPx = with(density) { 46.dp.toPx() }
    val bottomMarginPx = with(density) { 24.dp.toPx() }

    // 노드별 화면 2D 좌표 계산
    val nodePositions = remember(canvasSize, visNodes) {
        calculateNodePositions(visNodes, canvasSize, topMarginPx, bottomMarginPx)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MindmapTokens.CanvasBg)
            .border(1.dp, MindmapTokens.CanvasBorder, RoundedCornerShape(14.dp))
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.6f, 2.5f)
                    offset += pan
                }
            }
    ) {
        // 1. 상단 3계층 컬럼 안내 헤더
        MindmapColumnHeader()

        // 2. 줌 리셋 플로팅 버튼
        ZoomResetButton(
            modifier = Modifier.align(Alignment.BottomEnd),
            onReset = {
                scale = 1.0f
                offset = Offset.Zero
            }
        )

        // 3. 변환 레이어 (Canvas 엣지 + Composable 노드)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        ) {
            // (1) 베지어 곡선 연결선 레이어
            MindmapEdgesCanvas(
                visEdges = visEdges,
                nodePositions = nodePositions
            )

            // (2) 노드 뱃지 오버레이 레이어
            MindmapNodesOverlay(
                visNodes = visNodes,
                nodePositions = nodePositions,
                selectedNode = selectedNode,
                pulseScale = pulseScale,
                onNodeClick = { node ->
                    selectedNode = if (selectedNode?.id == node.id) null else node
                }
            )
        }

        // 4. 노드 상세 인스펙션 툴팁
        if (selectedNode != null) {
            MindmapInspectionTooltip(
                node = selectedNode!!,
                modifier = Modifier.align(Alignment.BottomStart),
                onDismiss = { selectedNode = null }
            )
        }
    }
}

// ── 세부 하위 컴포넌트들 (SRP) ──────────────────────────────────────

@Composable
private fun EmptyMindmapPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8F9FA))
            .border(1.dp, Color(0xFFE5E8EB), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "시각화할 배선 네트워크 정보가 없습니다.",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF8B95A1))
        )
    }
}

@Composable
private fun MindmapColumnHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColumnTitleTag(title = "진단 코드 (DTC)", color = MindmapTokens.TitleDtc)
        ColumnTitleTag(title = "제어기 (ECU)", color = MindmapTokens.TitleEcu)
        ColumnTitleTag(title = "하네스 커넥터", color = MindmapTokens.TitleConn)
    }
}

@Composable
private fun ColumnTitleTag(title: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MindmapTokens.CapsuleBg)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ZoomResetButton(modifier: Modifier = Modifier, onReset: () -> Unit) {
    IconButton(
        onClick = onReset,
        modifier = modifier
            .padding(10.dp)
            .size(32.dp)
            .background(MindmapTokens.CanvasBorder.copy(alpha = 0.85f), CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Zoom Reset",
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
private fun MindmapEdgesCanvas(
    visEdges: List<DtcRgatEngine.RgatVisEdge>,
    nodePositions: Map<String, Offset>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        for (edge in visEdges) {
            val p1 = nodePositions[edge.from] ?: continue
            val p2 = nodePositions[edge.to] ?: continue
            drawBezierEdge(edge, p1, p2)
        }
    }
}

private fun DrawScope.drawBezierEdge(
    edge: DtcRgatEngine.RgatVisEdge,
    p1: Offset,
    p2: Offset
) {
    val edgeColor = when {
        edge.title.contains("HW_MAP") -> MindmapTokens.EdgeHwMap
        edge.title.contains("HW_WIRE") -> MindmapTokens.EdgeHwWire
        edge.title.contains("AI_HW_WIRE") -> MindmapTokens.EdgeAiWire
        else -> MindmapTokens.EdgeLogic
    }

    val pathEffect = if (edge.dashes) {
        PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
    } else null

    val path = Path().apply {
        moveTo(p1.x, p1.y)
        val midX = (p1.x + p2.x) / 2f
        cubicTo(midX, p1.y, midX, p2.y, p2.x, p2.y)
    }

    drawPath(
        path = path,
        color = edgeColor,
        style = Stroke(
            width = edge.width.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = pathEffect
        )
    )
}

@Composable
private fun MindmapNodesOverlay(
    visNodes: List<DtcRgatEngine.RgatVisNode>,
    nodePositions: Map<String, Offset>,
    selectedNode: DtcRgatEngine.RgatVisNode?,
    pulseScale: Float,
    onNodeClick: (DtcRgatEngine.RgatVisNode) -> Unit
) {
    visNodes.forEach { node ->
        val pos = nodePositions[node.id] ?: return@forEach
        val isTop1 = node.group == "conn_top1"
        val isSelected = selectedNode?.id == node.id
        val nodeScale = if (isTop1) pulseScale else 1.0f
        val style = MindmapTokens.resolveNodeStyle(node)

        Box(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(
                            (pos.x - placeable.width / 2f).roundToInt(),
                            (pos.y - placeable.height / 2f).roundToInt()
                        )
                    }
                }
                .graphicsLayer(scaleX = nodeScale, scaleY = nodeScale)
                .clip(RoundedCornerShape(8.dp))
                .background(style.bg)
                .border(
                    width = if (isSelected) 2.5.dp else if (isTop1) 2.dp else 1.dp,
                    color = if (isSelected) MindmapTokens.SelectedBorder else style.border,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { onNodeClick(node) }
                .padding(horizontal = 9.dp, vertical = 5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isTop1) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Top 1 Verified",
                        tint = MindmapTokens.EdgeHwWire,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = node.label,
                    color = style.text,
                    fontSize = if (isTop1) 11.sp else 10.sp,
                    fontFamily = if (node.level == 0) FontFamily.Monospace else FontFamily.Default,
                    fontWeight = if (isTop1) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    softWrap = false,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun MindmapInspectionTooltip(
    node: DtcRgatEngine.RgatVisNode,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Box(
        modifier = modifier
            .padding(start = 10.dp, end = 50.dp, bottom = 10.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MindmapTokens.CapsuleBg.copy(alpha = 0.95f))
            .border(1.dp, Color(0xFF4E5968), RoundedCornerShape(8.dp))
            .clickable { onDismiss() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Detail",
                tint = MindmapTokens.EdgeHwMap,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = node.title.replace("\n", "  |  "),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3열(DTC, ECU, Connector) 노드 2D 좌표 배치 계산 함수
 */
private fun calculateNodePositions(
    visNodes: List<DtcRgatEngine.RgatVisNode>,
    canvasSize: IntSize,
    topMarginPx: Float,
    bottomMarginPx: Float
): Map<String, Offset> {
    val map = HashMap<String, Offset>()
    val width = canvasSize.width.toFloat().coerceAtLeast(300f)
    val height = canvasSize.height.toFloat().coerceAtLeast(200f)
    val usableHeight = (height - topMarginPx - bottomMarginPx).coerceAtLeast(100f)

    val level0 = visNodes.filter { it.level == 0 }
    val level1 = visNodes.filter { it.level == 1 }
    val level2 = visNodes.filter { it.level == 2 }

    fun layoutColumn(nodes: List<DtcRgatEngine.RgatVisNode>, x: Float) {
        val count = nodes.size
        nodes.forEachIndexed { idx, node ->
            val y = topMarginPx + (idx + 1) * usableHeight / (count + 1)
            map[node.id] = Offset(x, y)
        }
    }

    layoutColumn(level0, width * 0.16f)
    layoutColumn(level1, width * 0.50f)
    layoutColumn(level2, width * 0.84f)

    return map
}
