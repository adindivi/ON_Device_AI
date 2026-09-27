package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.backend.DtcRgatEngine
import java.util.Locale

/**
 * [RgatDialogTokens]
 * 핀테크 스타일 색상 및 UI 디자인 토큰 집합
 */
private object RgatDialogTokens {
    // 브랜드 & 상태 색상
    val BrandBlue = Color(0xFF3182F6)
    val BrandBlueLight = Color(0xFFE8F3FF)
    val SuccessGreen = Color(0xFF008A38)
    val SuccessGreenLight = Color(0xFFE8F8EE)
    val WarningAmber = Color(0xFFD9730D)
    val WarningAmberLight = Color(0xFFFFF3E8)

    // 뉴트럴 색상
    val TextPrimary = Color(0xFF191F28)
    val TextSecondary = Color(0xFF4E5968)
    val TextTertiary = Color(0xFF8B95A1)
    val TextMuted = Color(0xFF6B7684)

    val SurfaceBg = Color(0xFFFFFFFF)
    val SurfaceNeutral = Color(0xFFF2F4F6)
    val SurfaceCard = Color(0xFFF8F9FA)
    val BorderNeutral = Color(0xFFE5E8EB)
    val BorderSubtle = Color(0xFFF2F4F6)
    val DotDivider = Color(0xFFB0B8C1)
}

/**
 * [RootCauseAnalysisDialog]
 * 차량 전장 DTC-하네스 커넥터 근본 원인(Root Cause) 핀테크 스타일 분석 팝업창
 * 
 * Clean Code 리팩토링:
 * 1. Single Responsibility Principle (SRP):
 *    - 헤더, DTC 칩, 세그먼트 탭, 각 탭별 컨텐츠, 하단 푸터 액션바를 독립 Composable로 분리
 * 2. Design Tokens:
 *    - 50개 이상의 하드코딩된 색상을 [RgatDialogTokens]로 통합 관리
 * 3. Mobile UI Standard 준수:
 *    - Rule 2 (Anti-Squishing & Two-Row Hierarchy) 적용
 *    - Rule 3 (Segmented Control) 3분할 탭 적용
 *    - Rule 4 & 5 (Alignment & Intuitive Labeling) 적용
 */
@Composable
fun RootCauseAnalysisDialog(
    result: DtcRgatEngine.RgatAnalysisResult,
    onDismiss: () -> Unit,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val topCandidate = result.results.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, RgatDialogTokens.BorderNeutral, RoundedCornerShape(20.dp)),
            color = RgatDialogTokens.SurfaceBg,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // 1. 헤더 (고급 핀테크 타이틀 바 + 닫기 버튼)
                DialogHeader(onDismiss = onDismiss)

                Spacer(modifier = Modifier.height(14.dp))

                // 2. 분석 대상 DTC 태그 칩
                DtcChipsSection(inputCodes = result.inputCodes)

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 토스 스타일 세그먼트 컨트롤 탭 바 (Rule 3)
                SegmentedTabControl(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. 탭별 컨텐츠 영역 (높이 380dp 고정)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                ) {
                    when (selectedTab) {
                        0 -> MindmapTabContent(topCandidate = topCandidate, result = result)
                        1 -> RankingTabContent(rankings = result.results)
                        2 -> DtcDetailTabContent(dtcInfoList = result.dtcInfo)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. 하단 액션 버튼
                DialogActionBar(
                    onCopySummary = { copySummaryToClipboard(context, result, topCandidate) },
                    onDismiss = onDismiss
                )
            }
        }
    }
}

// ── 세부 컴포넌트 (SRP Decomposition) ──────────────────────────────

@Composable
private fun DialogHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(RgatDialogTokens.SurfaceNeutral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = "RGAT",
                    tint = RgatDialogTokens.BrandBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = "하네스 커넥터 근본 원인 분석",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = RgatDialogTokens.TextPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "인과 관계 그래프 및 링크 예측 분석",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = RgatDialogTokens.TextTertiary,
                        fontSize = 11.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = RgatDialogTokens.TextTertiary
            )
        }
    }
}

@Composable
private fun DtcChipsSection(inputCodes: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "분석 코드",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = RgatDialogTokens.TextSecondary,
            maxLines = 1,
            softWrap = false
        )
        inputCodes.forEach { code ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(RgatDialogTokens.SurfaceNeutral)
                    .border(1.dp, RgatDialogTokens.BorderNeutral, RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = code,
                    color = Color(0xFF333D4B),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

private data class TabSpec(val title: String, val icon: ImageVector, val index: Int)

@Composable
private fun SegmentedTabControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = remember {
        listOf(
            TabSpec("배선 마인드맵", Icons.Default.AccountTree, 0),
            TabSpec("추천 순위", Icons.Default.FormatListNumbered, 1),
            TabSpec("DTC 상세", Icons.Default.Info, 2)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(RgatDialogTokens.SurfaceNeutral)
            .padding(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEach { tab ->
                val isSelected = selectedTab == tab.index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) RgatDialogTokens.SurfaceBg else Color.Transparent)
                        .clickable { onTabSelected(tab.index) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) RgatDialogTokens.BrandBlue else RgatDialogTokens.TextTertiary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) RgatDialogTokens.TextPrimary else RgatDialogTokens.TextTertiary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MindmapTabContent(
    topCandidate: DtcRgatEngine.RgatConnectorRank?,
    result: DtcRgatEngine.RgatAnalysisResult
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (topCandidate != null) {
            TopCandidateCard(topCandidate = topCandidate)
            Spacer(modifier = Modifier.height(8.dp))
        }
        RgatMindmapCanvas(
            visNodes = result.visNodes,
            visEdges = result.visEdges,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RankingTabContent(rankings: List<DtcRgatEngine.RgatConnectorRank>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(rankings) { item ->
            ConnectorRankItem(item = item)
        }
    }
}

@Composable
private fun DtcDetailTabContent(dtcInfoList: List<DtcRgatEngine.RgatDtcSummary>) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(dtcInfoList) { dtc ->
            DtcDetailItem(dtc = dtc)
        }
        item {
            DiagnosticInsightCard()
        }
    }
}

@Composable
private fun DiagnosticInsightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = RgatDialogTokens.SurfaceCard),
        border = BorderStroke(1.dp, RgatDialogTokens.BorderNeutral)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = RgatDialogTokens.BrandBlue,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "복수 제어기에서 CAN 통신/센서 이상 동시 발생 시, 상위 1위 배선 커넥터의 공통 접지 및 핀 단선을 우선 점검하세요.",
                fontSize = 11.sp,
                color = RgatDialogTokens.TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun DialogActionBar(
    onCopySummary: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCopySummary,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, RgatDialogTokens.BorderNeutral),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF333D4B))
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("요약 복사", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RgatDialogTokens.BrandBlue)
        ) {
            Text("확인 닫기", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * 최우선 추천 커넥터 요약 카드 (Rule 2 Two-Row Hierarchy 적용)
 */
@Composable
private fun TopCandidateCard(topCandidate: DtcRgatEngine.RgatConnectorRank) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RgatDialogTokens.SurfaceCard),
        border = BorderStroke(1.dp, RgatDialogTokens.BorderNeutral)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1행: 최우선 추천 태그 + 신뢰도 뱃지 (Rule 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Top",
                        tint = RgatDialogTokens.BrandBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "1순위 최우선 원인 커넥터",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = RgatDialogTokens.BrandBlue
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (topCandidate.verified) RgatDialogTokens.SuccessGreenLight else RgatDialogTokens.BrandBlueLight)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    val scoreText = if (topCandidate.verified) {
                        "직결 검증 100%"
                    } else {
                        "일치율 ${String.format(Locale.US, "%.1f", (topCandidate.finalScore * 100).coerceAtMost(100.0))}%"
                    }
                    Text(
                        text = scoreText,
                        color = if (topCandidate.verified) RgatDialogTokens.SuccessGreen else Color(0xFF1B64DA),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2행: 커넥터 명칭 (단독 가로행 할당으로 글자 쪼개짐/생략 방지)
            Text(
                text = topCandidate.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = RgatDialogTokens.TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3행: 부가 위치 및 연계 제어기 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (topCandidate.location.isNotBlank()) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = RgatDialogTokens.TextTertiary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = topCandidate.location,
                        fontSize = 11.sp,
                        color = RgatDialogTokens.TextSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                if (topCandidate.location.isNotBlank() && topCandidate.connEcus.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "·",
                        fontSize = 11.sp,
                        color = RgatDialogTokens.DotDivider,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (topCandidate.connEcus.isNotEmpty()) {
                    Text(
                        text = "연계 제어기: ${topCandidate.connEcus.joinToString(", ")}",
                        fontSize = 11.sp,
                        color = RgatDialogTokens.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

/**
 * 커넥터 순위 리스트 아이템 카드
 */
@Composable
private fun ConnectorRankItem(item: DtcRgatEngine.RgatConnectorRank) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = RgatDialogTokens.SurfaceBg),
        border = BorderStroke(1.dp, RgatDialogTokens.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1행: 순위 + 커넥터명 + 점수 뱃지 (Rule 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (item.rank == 1) RgatDialogTokens.BrandBlue else RgatDialogTokens.SurfaceNeutral),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${item.rank}",
                            color = if (item.rank == 1) Color.White else RgatDialogTokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = RgatDialogTokens.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (item.verified) RgatDialogTokens.SuccessGreenLight else RgatDialogTokens.SurfaceNeutral)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (item.verified) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = RgatDialogTokens.SuccessGreen,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                        Text(
                            text = if (item.verified) "검증 직결" else "Score ${item.finalScore}",
                            color = if (item.verified) RgatDialogTokens.SuccessGreen else RgatDialogTokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // 2행: 위치 및 ECU 정보
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val locPart = if (item.location.isNotBlank()) "위치: ${item.location}" else ""
                val ecuPart = if (item.connEcus.isNotEmpty()) "제어기: ${item.connEcus.joinToString(", ")}" else ""
                val desc = listOf(locPart, ecuPart).filter { it.isNotBlank() }.joinToString(" · ")
                Text(
                    text = if (desc.isNotBlank()) desc else "2-Hop 물리 배선 연결",
                    fontSize = 11.sp,
                    color = RgatDialogTokens.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(RgatDialogTokens.SurfaceNeutral)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "일치 ${item.nHit}/${item.nValid}건",
                        fontSize = 10.sp,
                        color = RgatDialogTokens.TextSecondary,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * DTC 상세 정보 리스트 아이템 카드
 */
@Composable
private fun DtcDetailItem(dtc: DtcRgatEngine.RgatDtcSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = RgatDialogTokens.SurfaceBg),
        border = BorderStroke(1.dp, RgatDialogTokens.BorderSubtle)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 1행: 코드 + 담당 제어기 뱃지 + 카테고리 뱃지 (Rule 2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dtc.code,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = RgatDialogTokens.TextPrimary
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    dtc.ecuNames.forEach { ecuName ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(RgatDialogTokens.WarningAmberLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = ecuName,
                                color = RgatDialogTokens.WarningAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (dtc.cat.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RgatDialogTokens.SurfaceNeutral)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dtc.cat,
                            color = RgatDialogTokens.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2행: ISO 14229 서브타입 명칭
            if (dtc.subtypeName.isNotBlank()) {
                Text(
                    text = "서브타입 (${dtc.subtypeCode}): ${dtc.subtypeName}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333D4B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3행: 제어기 및 설명
            if (dtc.desc.isNotBlank()) {
                Text(
                    text = dtc.desc,
                    fontSize = 11.sp,
                    color = RgatDialogTokens.TextTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 진단 요약 클립보드 복사 헬퍼
 */
private fun copySummaryToClipboard(
    context: Context,
    result: DtcRgatEngine.RgatAnalysisResult,
    topCandidate: DtcRgatEngine.RgatConnectorRank?
) {
    val summary = buildString {
        appendLine("[하네스 커넥터 근본원인 분석 리포트]")
        appendLine("• 입력 DTC: ${result.inputCodes.joinToString(", ")}")
        if (topCandidate != null) {
            appendLine("• 최우선 추천 커넥터: ${topCandidate.name}")
            appendLine("• 점수: ${topCandidate.finalScore} (DTC ${topCandidate.nHit}/${topCandidate.nValid}개 일치)")
            if (topCandidate.location.isNotBlank()) {
                appendLine("• 커넥터 위치: ${topCandidate.location}")
            }
            if (topCandidate.connEcus.isNotEmpty()) {
                appendLine("• 연계 제어기(ECU): ${topCandidate.connEcus.joinToString(", ")}")
            }
        }
    }
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("RGAT Diagnosis", summary))
    Toast.makeText(context, "진단 요약이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
}
