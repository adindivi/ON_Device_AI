package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.backend.BackendDbStatus
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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

/**
 * 데이터 관리 화면 — Toss B&W 모바일 UI 최적화 버전 (기여 건수 강조형 배지 적용)
 */
@Composable
fun ContributionSettingsScreen(
    userDocuments: List<RagDocument>,
    backendStatus: BackendDbStatus? = null,
    isQwenLoading: Boolean = false,
    onOpenAddRemedy: () -> Unit,
    onEditDocument: (RagDocument) -> Unit,
    onDeleteDocument: (Long) -> Unit,
    onRefreshStatus: (() -> Unit)? = null,
    onQwenModelSelected: ((android.net.Uri) -> Unit)? = null,
    isQwenAnswerEnabled: Boolean = true,
    onToggleQwenAnswer: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var refreshKey by remember { mutableIntStateOf(0) }
    val count = userDocuments.size

    val qwenPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onQwenModelSelected?.invoke(it)
        }
    }

    // 애플식 기여도 등급 배지 설정
    val (gradeTag, gradeColor) = when {
        count >= 10 -> "🥇 명장 정비사 (${count}건)" to Color(0xFFD97706)
        count >= 5 -> "🥈 든든한 해결사 (${count}건)" to Color(0xFF475569)
        else -> "🥉 시작하는 정비사 (${count}건)" to Color(0xFFB45309)
    }

    val progressFraction = (count / 10f).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TossWhite)
            .padding(16.dp)
    ) {
        // ── 상단 타이틀 ───────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TossBlack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Data Management",
                    tint = TossWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = "데이터 및 AI 보관함",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TossBlack,
                    fontSize = 16.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // ── 1. 스마트폰 내장 DB 백엔드 연결 상태 카드 ─────────────────────
            backendStatus?.let { status ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = TossBlack)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FolderSpecial,
                                        contentDescription = "DB Folder",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "오프라인 진단 데이터",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0284C7))
                                        .clickable {
                                             refreshKey++
                                             onRefreshStatus?.invoke()
                                             android.widget.Toast.makeText(context, "진단 데이터 최신 상태", android.widget.Toast.LENGTH_SHORT).show()
                                         }
                                         .padding(horizontal = 7.dp, vertical = 3.dp)
                                 ) {
                                     Text(
                                         text = "최신 상태",
                                         fontSize = 10.sp,
                                         fontWeight = FontWeight.Bold,
                                         color = Color.White
                                     )
                                 }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "기기 내 안전하게 저장됨",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // 파일 배치 상세 안내 박스
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF1E293B))
                                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = "📁 기기 내 AI 엔진 구성",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "• 진단 언어 모델: Qwen 1.5B (온디바이스)",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "• 정비 지식 데이터: 263개 핵심 정비 사례",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "• 차량 부품 사전: 실시간 어휘 매핑 활성화",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 연동 항목 상태
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isQwenLoading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(13.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFFFFC107)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "⏳ GGUF 모델 복사 중... 잠시만 기다려주세요",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFFFC107)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Status",
                                            tint = if (status.isQwenModelFound) Color(0xFF10B981) else Color(0xFFF59E0B),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "AI 진단 모델: " +
                                                    if (status.isQwenModelFound) "Qwen 1.5B 준비 완료" else "직접 파일 선택 (탭하여 로드)",
                                            fontSize = 10.5.sp,
                                            color = Color(0xFFE2E8F0),
                                            modifier = Modifier.clickable { qwenPickerLauncher.launch(arrayOf("*/*")) }
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Status",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "정비 지식 데이터: 총 ${status.totalVectorDocuments}건 사례 준비됨",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Status",
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "차량 부품 사전: 실시간 동기화됨",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            androidx.compose.material3.HorizontalDivider(
                                thickness = 0.5.dp,
                                color = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "🤖 AI 종합 진단서 작성",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isQwenAnswerEnabled) "핵심 정비 카드와 함께 AI 심층 분석을 제공합니다" else "빠른 진단을 위해 핵심 정비 카드만 표시합니다",
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = if (isQwenAnswerEnabled) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = isQwenAnswerEnabled,
                                    onCheckedChange = { onToggleQwenAnswer?.invoke(it) },
                                    colors = androidx.compose.material3.SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0284C7),
                                        uncheckedThumbColor = Color(0xFFCBD5E1),
                                        uncheckedTrackColor = Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. 해결방안 기여도 & 등급 (기여 건수 강조형 배지) ─────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TossWhite),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TossOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "내가 남긴 정비 노하우",
                                    fontSize = 12.sp,
                                    color = TossGray500
                                )
                                Text(
                                    text = "$count 건",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TossBlack
                                )
                            }

                            // 모바일 핏 컴팩트 배지
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

                        // 게이지 프로그레스
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

                        val remaining = (10 - count).coerceAtLeast(0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("시작", fontSize = 10.sp, color = TossGray400)
                            Text(
                                if (remaining == 0) "최고 등급 달성" else "명장 정비사까지 ${remaining}건",
                                fontSize = 10.sp,
                                color = TossGray400
                            )
                        }
                    }
                }
            }

            // ── 3. 등록된 해결방안 목록 헤더 ─────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "나의 정비 노트",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TossBlack,
                            fontSize = 15.sp
                        )
                    )

                    // 신규 등록 버튼
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(TossBlack)
                            .clickable { onOpenAddRemedy() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("btn_add_remedy_settings")
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
                                "노하우 추가",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TossWhite
                            )
                        }
                    }
                }
            }

            // 목록 아이템들
            if (userDocuments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, TossOutline, RoundedCornerShape(12.dp))
                            .background(TossWhite)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "아직 저장된 정비 노하우가 없어요.\n나만의 수리 팁을 남겨두면 AI가 진단할 때 함께 활용해요.",
                            color = TossGray400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
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
                                .padding(14.dp)
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

                                // 편집/삭제 액션 버튼
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { onEditDocument(doc) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("btn_edit_doc_${doc.id}")
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
                                            .testTag("btn_delete_doc_${doc.id}")
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

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "도움됨 추천수: ${doc.recommendationCount}회",
                                fontSize = 10.sp,
                                color = TossGray400
                            )
                        }
                    }
                }
            }

            // ── 4. 온디바이스 AI 사양 정보 카드 ───────────────────────────────
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = TossWhite),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TossOutline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "System Info",
                                tint = TossBlack,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "온디바이스 시스템 정보",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TossBlack,
                                    fontSize = 14.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("진단 언어 모델", fontSize = 11.5.sp, color = TossGray500)
                                Text("Qwen 1.5B (기기 내 독립 구동)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TossBlack)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("지식 데이터베이스", fontSize = 11.5.sp, color = TossGray500)
                                Text("기기 내 안전 보관 (Room DB)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TossBlack)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("인터넷 연결", fontSize = 11.5.sp, color = TossGray500)
                                Text("연결 없이 즉시 작동", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }
}
