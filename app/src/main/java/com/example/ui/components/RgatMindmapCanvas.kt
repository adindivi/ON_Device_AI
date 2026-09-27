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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
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
    // 1. 기본 분위기 (Apple Reference: Fog Canvas & Subtle Hairline)
    val CanvasBg = Color(0xFFF5F5F7)        // 애플 시그니처 포그 캔버스 (Fog Gray)
    val CanvasBorder = Color(0xFFE5E5EA)    // 정제된 헤어라인 테두리 (Apple System Gray 5)

    // 2. 조작/상단 태그 (Apple Reference: Unified Minimal Pill)
    val CapsuleBg = Color(0xFFFFFFFF)       // 퓨어 화이트 캡슐 배경
    val CapsuleBorder = Color(0xFFE5E5EA)   // 은은한 헤어라인 테두리
    val CapsuleText = Color(0xFF1D1D1F)     // 애플 시그니처 포그라운드 텍스트

    // 3. 배선 연결선 (Apple Reference: 1순위 시그니처 블루 + 일반 배선 연회색 위계)
    val EdgeTop1Wire = Color(0xFF0071E3)    // 1순위 핵심 배선 (애플 시그니처 블루)
    val EdgeHwWire = Color(0xFFB0B8C1)      // 일반 물리 배선 (차분한 스틸 쿨 그레이)
    val EdgeLogic = Color(0xFFD2D2D7)       // 소프트웨어 논리 관계 (애플 라이트 그레이)

    // 선택 하이라이트
    val SelectedBorder = Color(0xFF0071E3)  // 애플 블루

    // 노드 스타일 사양 (기존 유지, 1순위는 Apple Blue #0071E3로 시각적 통일)
    data class NodeStyle(val bg: Color, val border: Color, val text: Color)

    fun resolveNodeStyle(node: DtcRgatEngine.RgatVisNode): NodeStyle = when {
        // 1위 최우선 원인 커넥터: 선명한 애플 블루 솔리드 카드
        node.group == "conn_top1" -> NodeStyle(Color(0xFF0071E3), Color(0xFF0056B3), Color.White)
        // 기타 추천 커넥터: 산뜻한 민트/에메랄드 카드
        node.group == "conn_top" || node.level == 2 -> NodeStyle(Color(0xFFF0FDF4), Color(0xFF86EFAC), Color(0xFF166534))
        // 제어기 (ECU): 부드러운 웜 앰버 카드
        node.level == 1 -> NodeStyle(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFF92400E))
        // 진단 코드 (DTC): 청량한 소프트 블루 카드
        else -> NodeStyle(Color(0xFFEFF6FF), Color(0xFFBFDBFE), Color(0xFF1E40AF))
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
    modifier: Modifier = Modifier,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null
) {
    if (visNodes.isEmpty()) {
        EmptyMindmapPlaceholder(modifier = modifier)
        return
    }

    var scale by remember { mutableFloatStateOf(1.0f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize(800, 600)) }
    var selectedNode by remember { mutableStateOf<DtcRgatEngine.RgatVisNode?>(null) }

    val top1NodeId = remember(visNodes) { visNodes.find { it.group == "conn_top1" }?.id }

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
    val topMarginPx = with(density) { (if (isFullscreen) 66.dp else 64.dp).toPx() }
    val bottomMarginPx = with(density) { (if (isFullscreen) 96.dp else 64.dp).toPx() }

    // 노드별 화면 2D 좌표 계산
    val nodePositions = remember(canvasSize, visNodes, topMarginPx, bottomMarginPx) {
        calculateNodePositions(visNodes, canvasSize, topMarginPx, bottomMarginPx)
    }

    val canvasShape = if (isFullscreen) RoundedCornerShape(0.dp) else RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .clip(canvasShape)
            .background(MindmapTokens.CanvasBg)
            .then(
                if (isFullscreen) Modifier
                else Modifier.border(1.dp, MindmapTokens.CanvasBorder, canvasShape)
            )
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1.0f, 2.5f)
                    val maxPanX = ((canvasSize.width * (newScale - 1.0f)) / 2f).coerceAtLeast(0f)
                    val maxPanY = ((canvasSize.height * (newScale - 1.0f)) / 2f).coerceAtLeast(0f)

                    val newOffsetX = if (newScale <= 1.02f) 0f else (offset.x + pan.x * 0.85f).coerceIn(-maxPanX, maxPanX)
                    val newOffsetY = if (newScale <= 1.02f) 0f else (offset.y + pan.y * 0.85f).coerceIn(-maxPanY, maxPanY)

                    scale = newScale
                    offset = Offset(newOffsetX, newOffsetY)
                }
            }
    ) {
        // 1. 변환 레이어 (Canvas 엣지 + Composable 노드)
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
                nodePositions = nodePositions,
                top1NodeId = top1NodeId
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

        // 2. 상단 3계층 컬럼 안내 헤더
        MindmapColumnHeader()

        // 3. 플로팅 컨트롤 버튼 (전체화면 토글 + 줌 리셋)
        MindmapControlButtons(
            isFullscreen = isFullscreen,
            onToggleFullscreen = onToggleFullscreen,
            onResetZoom = {
                scale = 1.0f
                offset = Offset.Zero
            },
            modifier = Modifier.align(Alignment.BottomEnd)
        )

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
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ColumnTitleTag(title = "진단 코드 (DTC)")
        ColumnTitleTag(title = "제어기 (ECU)")
        ColumnTitleTag(title = "하네스 커넥터")
    }
}

@Composable
private fun ColumnTitleTag(title: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MindmapTokens.CapsuleBg)
            .border(1.dp, MindmapTokens.CapsuleBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MindmapTokens.CapsuleText,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MindmapControlButtons(
    isFullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 12.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, MindmapTokens.CanvasBorder, CircleShape)
                .clickable(onClick = onResetZoom),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "화면 비율 리셋",
                tint = Color(0xFF1D1D1F),
                modifier = Modifier.size(15.dp)
            )
        }
        if (onToggleFullscreen != null) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, MindmapTokens.CanvasBorder, CircleShape)
                .clickable(onClick = onToggleFullscreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (isFullscreen) "전체화면 종료" else "전체화면",
                    tint = Color(0xFF1D1D1F),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MindmapEdgesCanvas(
    visEdges: List<DtcRgatEngine.RgatVisEdge>,
    nodePositions: Map<String, Offset>,
    top1NodeId: String?
) {
    val (top1Edges, normalEdges) = remember(visEdges, top1NodeId) {
        visEdges.partition { edge ->
            edge.to == top1NodeId || edge.from == top1NodeId || edge.title.contains("HW_MAP")
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        // 1. 일반 배선 (배경 계층 - 차분한 애플 라이트/스틸 그레이)
        for (edge in normalEdges) {
            val p1 = nodePositions[edge.from] ?: continue
            val p2 = nodePositions[edge.to] ?: continue
            drawBezierEdge(edge, p1, p2, isTop1 = false)
        }
        // 2. 1순위 핵심 배선 (전경 계층 - 선명한 애플 시그니처 블루)
        for (edge in top1Edges) {
            val p1 = nodePositions[edge.from] ?: continue
            val p2 = nodePositions[edge.to] ?: continue
            drawBezierEdge(edge, p1, p2, isTop1 = true)
        }
    }
}

private fun DrawScope.drawBezierEdge(
    edge: DtcRgatEngine.RgatVisEdge,
    p1: Offset,
    p2: Offset,
    isTop1: Boolean
) {
    val (edgeColor, strokeWidth) = when {
        isTop1 -> Pair(MindmapTokens.EdgeTop1Wire, 2.5f)
        edge.title.contains("HW_WIRE") || edge.title.contains("AI_HW_WIRE") -> Pair(MindmapTokens.EdgeHwWire, 1.8f)
        else -> Pair(MindmapTokens.EdgeLogic, 1.2f)
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
            width = strokeWidth.dp.toPx(),
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
                .clip(RoundedCornerShape(6.dp))
                .background(style.bg)
                .border(
                    width = if (isSelected) 2.dp else if (isTop1) 1.5.dp else 1.dp,
                    color = if (isSelected) MindmapTokens.SelectedBorder else style.border,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable { onNodeClick(node) }
                .padding(horizontal = 7.dp, vertical = 2.5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isTop1) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Top 1 Verified",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = node.label,
                    color = style.text,
                    fontSize = if (isTop1) 10.5.sp else 9.5.sp,
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
            .padding(start = 14.dp, end = 90.dp, bottom = 14.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, MindmapTokens.CanvasBorder, RoundedCornerShape(8.dp))
            .clickable { onDismiss() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Detail",
                tint = MindmapTokens.EdgeTop1Wire,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = node.title.replace("\n", "  |  "),
                color = Color(0xFF1D1D1F),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3열(DTC, ECU, Connector) 노드 2D 좌표 배치 계산 함수 (충돌 방지 가변 여백 적용)
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
        if (count == 0) return
        if (count == 1) {
            map[nodes[0].id] = Offset(x, topMarginPx + usableHeight / 2f)
            return
        }
        if (count == 2) {
            val centerY = topMarginPx + usableHeight / 2f
            val span = (usableHeight * 0.28f).coerceAtLeast(36f)
            map[nodes[0].id] = Offset(x, centerY - span)
            map[nodes[1].id] = Offset(x, centerY + span)
            return
        }
        if (count == 3) {
            val centerY = topMarginPx + usableHeight / 2f
            val span = (usableHeight * 0.38f).coerceAtLeast(48f)
            map[nodes[0].id] = Offset(x, centerY - span)
            map[nodes[1].id] = Offset(x, centerY)
            map[nodes[2].id] = Offset(x, centerY + span)
            return
        }
        // count >= 4 (4개 DTC 코드 또는 5개 커넥터 노드 전 구역 균등 분배)
        val startY = topMarginPx + 4f
        val endY = height - bottomMarginPx - 4f
        val stepY = (endY - startY) / (count - 1)
        nodes.forEachIndexed { idx, node ->
            map[node.id] = Offset(x, startY + idx * stepY)
        }
    }

    layoutColumn(level0, width * 0.16f)
    layoutColumn(level1, width * 0.47f)
    layoutColumn(level2, width * 0.77f)

    return map
}
