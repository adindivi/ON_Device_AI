package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.SearchResult
import com.example.ui.theme.TossBadgeActionBg
import com.example.ui.theme.TossBadgeActionFg
import com.example.ui.theme.TossBadgeBlueBg
import com.example.ui.theme.TossBadgeBlueFg
import com.example.ui.theme.TossBadgeRedBg
import com.example.ui.theme.TossBadgeRedFg
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossGray100
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray400
import com.example.ui.theme.TossGray500
import com.example.ui.theme.TossGray600
import com.example.ui.theme.TossOutline
import com.example.ui.theme.TossWhite
import com.example.ui.theme.CarRankGold
import com.example.ui.theme.CarRankGoldBg
import com.example.ui.theme.CarRankGoldBorder
import com.example.ui.theme.CarRankSilver
import com.example.ui.theme.CarRankSilverBg
import com.example.ui.theme.CarRankSilverBorder
import com.example.ui.theme.CarRankBronze
import com.example.ui.theme.CarRankBronzeBg
import com.example.ui.theme.CarRankBronzeBorder
import kotlinx.coroutines.delay

@Composable
fun RecommendationCard(
    match: SearchResult,
    rankIndex: Int,
    textSizeScale: Float,
    typingTextComposable: @Composable (String, TextStyleWrapper) -> Unit,
    modifier: Modifier = Modifier
) {
    val rankBadge = when (rankIndex) {
        0 -> "우선 점검 1위"
        1 -> "우선 점검 2위"
        else -> "우선 점검 3위"
    }
    val rankColor = when (rankIndex) {
        0 -> Color(0xFFB45309)
        1 -> Color(0xFF475569)
        else -> Color(0xFF9A3412)
    }
    val rankBg = Color.White
    val rankBorder = when (rankIndex) {
        0 -> Color(0xFFFDE68A)
        1 -> Color(0xFFCBD5E1)
        else -> Color(0xFFFDBA74)
    }

    val compName = match.metadata.component.ifBlank { "관련 부품" }
    val dtcCode = match.metadata.dtcCode.ifBlank { null }
    val connLoc = match.metadata.connectorLocation.trim().ifBlank { null }

    // 1. [증상] 순수 현상 텍스트 (조치 문구 및 고장 원인 배제)
    val symptomText = when {
        match.text.contains("[증상]") ->
            match.text.substringAfter("[증상]").substringBefore("[").replace("\n", " ").trim()
        match.metadata.contextQuery.isNotBlank() ->
            match.metadata.contextQuery.trim()
        else ->
            "$compName 작동 불량 및 경고등 점등"
    }

    // 2. [고장 내용] 순수 고장 원인/명칭 (조치 문장 '~검사하십시오', '~점검' 분리 배제)
    val extractedCause = when {
        match.text.contains("[고장 내용]") ->
            match.text.substringAfter("[고장 내용]").substringBefore("[").replace("\n", " ").trim()
        match.text.contains("[고장내용]") ->
            match.text.substringAfter("[고장내용]").substringBefore("[").replace("\n", " ").trim()
        match.text.contains("[원인]") ->
            match.text.substringAfter("[원인]").substringBefore("[").replace("\n", " ").trim()
        else -> ""
    }

    val causeText = if (extractedCause.isNotBlank() && !extractedCause.contains("하십시오") && !extractedCause.contains("점검")) {
        extractedCause
    } else {
        "$compName 회로 단선/단락 및 제어 모듈 미체결"
    }

    // 3. [점검 조치] 기존 정통 조치 템플릿으로 원복 유지
    val rawAction = when {
        match.text.contains("[조치사항]") ->
            match.text.substringAfter("[조치사항]").substringBefore("[").replace("\n", " ").trim()
        match.text.contains("[조치방안]") ->
            match.text.substringAfter("[조치방안]").substringBefore("[").replace("\n", " ").trim()
        else ->
            "$compName 하네스 커넥터 단자 체결 상태 및 배선 점검"
    }

    val actionText = if (connLoc != null && !rawAction.contains(connLoc)) {
        "[$connLoc 커넥터] $rawAction"
    } else {
        rawAction
    }

    // Helpfulness counter state (local UI state for demonstration)
    var helpfulCount by remember { mutableIntStateOf(match.recommendations) }

    // ── Card Shell ─────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(TossWhite)
            .border(0.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .clickable { /* navigate to detail if needed */ }
            .padding(16.dp)
    ) {
        Column {
            // ── Top Row: Badges + Date ─────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // DTC code monochrome badge (if present)
                    if (dtcCode != null) {
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = dtcCode,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }

                // Date label
                Text(
                    text = "오늘",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TossGray400
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Component Name (Title) ─────────────────────────────────────────
            Text(
                text = compName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TossBlack,
                    fontSize = (17 * textSizeScale).sp
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Symptom + Cause body text ──────────────────────────────────────
            val bodyText = buildString {
                if (symptomText.isNotBlank()) append("[증상] $symptomText\n")
                if (causeText.isNotBlank()) append("[고장 내용] $causeText")
            }.trim()

            if (bodyText.isNotBlank()) {
                Text(
                    text = bodyText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TossGray600,
                        fontSize = (12.5f * textSizeScale).sp,
                        lineHeight = (18 * textSizeScale).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Action text (typing animation box) ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "점검 조치 가이드",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF004AC6),
                            fontSize = 10.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    typingTextComposable(
                        actionText,
                        TextStyleWrapper(
                            fontSize = (12 * textSizeScale).sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // ── Connector location ─────────────────────────────────────────────
            if (connLoc != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFFF0F9FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                ) {
                    Text(
                        text = "커넥터: $connLoc",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Bottom Row: Source + Helpful button ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Source tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = TossGray400,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "오프라인 DB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TossGray400
                    )
                }

                // 도움됨 button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, TossGray200, RoundedCornerShape(50.dp))
                        .background(TossBadgeActionBg)
                        .clickable { helpfulCount++ }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "도움됨",
                            tint = TossBadgeActionFg,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "도움됨 $helpfulCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TossBadgeActionFg
                        )
                    }
                }
            }

            // Rank badge + Confidence chip at bottom
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = rankBg,
                    border = BorderStroke(1.dp, rankBorder)
                ) {
                    Text(
                        text = rankBadge,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = rankColor,
                            fontSize = 10.5.sp
                        )
                    )
                }

                match.confidencePercent?.let { pct ->
                    ConfidenceChip(confidencePercent = pct)
                }
            }
        }
    }
}

data class TextStyleWrapper(
    val fontSize: androidx.compose.ui.unit.TextUnit,
    val color: Color,
    val fontWeight: FontWeight
)

/**
 * 신뢰도 칩 Composable
 * - ONNX 로딩 시에만 표시 (null이면 호출 안 됨)
 * - 딥 차콜 캡슐 (50.dp) + 네온 텍스트 미니멀 테크 스타일
 */
@Composable
fun ConfidenceChip(confidencePercent: Float) {
    val (textColor, borderColor) = when {
        confidencePercent >= 90f -> Color(0xFF059669) to Color(0xFFA7F3D0) // Emerald
        confidencePercent >= 70f -> Color(0xFF0284C7) to Color(0xFFBAE6FD) // Blue
        confidencePercent >= 50f -> Color(0xFFD97706) to Color(0xFFFDE68A) // Amber
        else -> Color(0xFFDC2626) to Color(0xFFFECACA) // Red
    }

    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color.White,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Text(
            text = "일치도 ${String.format(java.util.Locale.US, "%.1f", confidencePercent)}%",
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp
            )
        )
    }
}
