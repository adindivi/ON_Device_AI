package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.RagDocument
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

/**
 * 해결방안 기여 대시보드 다이얼로그 — Toss B&W 모바일 UI 최적화 (기여 건수 강조형 배지 적용)
 */
@Composable
fun ContributionDashboardDialog(
    userDocuments: List<RagDocument>,
    onDismiss: () -> Unit,
    onOpenAddRemedy: () -> Unit,
    onEditDocument: (RagDocument) -> Unit,
    onDeleteDocument: (Long) -> Unit
) {
    val count = userDocuments.size

    // [추천 3] 기여 건수 강조형 배지 등급 설정
    val (gradeTag, gradeColor) = when {
        count >= 10 -> "🥇 마스터 (기여 ${count}건)" to Color(0xFFD97706)
        count >= 5 -> "🥈 실버 (기여 ${count}건)" to Color(0xFF475569)
        else -> "🥉 브론즈 (기여 ${count}건)" to Color(0xFFB45309)
    }

    val progressFraction = (count / 10f).coerceIn(0f, 1f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("dialog_contribution_dashboard"),
            shape = RoundedCornerShape(20.dp),
            color = TossWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // ── 상단 헤더 ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TossBlack),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = "Dashboard",
                                tint = TossWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "해결방안 기여 대시보드",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TossBlack,
                                    fontSize = 16.sp
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "등록된 수리 조치 방안 및 기여 현황",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TossGray500,
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_contribution_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TossGray500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 기여도 요약 카드 ───────────────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = TossWhite),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TossOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "등록한 수리 조치",
                                    fontSize = 11.sp,
                                    color = TossGray500
                                )
                                Text(
                                    text = "$count 건",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TossBlack
                                )
                            }

                            // 모바일 핏 컴팩트 배지 [추천 3 적용]
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(TossGray100)
                                    .border(1.dp, TossGray200, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = gradeTag,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = gradeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = TossBlack,
                            trackColor = TossGray100
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("초급 정비사", fontSize = 10.sp, color = TossGray400)
                            Text("마스터 (10건)", fontSize = 10.sp, color = TossGray400)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── 목록 타이틀 & 등록 버튼 ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "등록된 해결방안 (${userDocuments.size})",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TossBlack,
                            fontSize = 14.sp
                        )
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TossBlack)
                            .clickable {
                                onDismiss()
                                onOpenAddRemedy()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("btn_add_remedy_dialog_action")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Add",
                                tint = TossWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "해결방안 등록",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TossWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // ── 목록 ────────────────────────────────────────────────────────
                if (userDocuments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.5.dp, TossOutline, RoundedCornerShape(12.dp))
                            .background(TossWhite)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 등록한 수리 조치 방안이 없습니다.\n'해결방안 등록' 버튼을 눌러 추가해 보세요!",
                            color = TossGray400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(userDocuments, key = { it.id }) { doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = TossWhite),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, TossOutline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            doc.dtcCode?.let { dtc ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(TossBadgeRedBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = dtc,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TossBadgeRedFg
                                                    )
                                                }
                                            }

                                            doc.component?.let { comp ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(TossBadgeBlueBg)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = comp,
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TossBadgeBlueFg
                                                    )
                                                }
                                            }

                                            doc.connectorLocation?.let { loc ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.LocationOn,
                                                        contentDescription = null,
                                                        tint = TossGray400,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = loc,
                                                        fontSize = 9.5.sp,
                                                        color = TossGray500
                                                    )
                                                }
                                            }
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            IconButton(
                                                onClick = { onEditDocument(doc) },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("btn_dialog_edit_doc_${doc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Edit",
                                                    tint = TossGray600,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteDocument(doc.id) },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("btn_dialog_delete_doc_${doc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = doc.fullContent,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = TossBlack,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "도움됨 추천수: ${doc.recommendationCount}회",
                                        fontSize = 10.sp,
                                        color = TossGray400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
