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

    // [추천 3] 기여 건수 강조형 배지 등급 설정
    val (gradeTag, gradeColor) = when {
        count >= 10 -> "🥇 마스터 (기여 ${count}건)" to Color(0xFFD97706)
        count >= 5 -> "🥈 실버 (기여 ${count}건)" to Color(0xFF475569)
        else -> "🥉 브론즈 (기여 ${count}건)" to Color(0xFFB45309)
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
                text = "데이터 관리 및 온디바이스 현황",
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
                                        text = "스마트폰 내장 DB 연결 상태",
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
                                            android.widget.Toast.makeText(context, "🔄 스마트폰 DB 연결 상태가 새로고침되었습니다.", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "정상 연동됨",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "기본 DB 경로: ${status.dbFolderPath}",
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
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "📁 온디바이스 파일 구성 가이드",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                    Text(
                                        text = "• GGUF 모델: qwen2.5-1.5b-instruct-q4_k_m.gguf (4-bit)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "• RAG 벡터 DB: rag_vector_database.json",
                                        fontSize = 10.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "• 어휘 사전: mapping_dictionary.json",
                                        fontSize = 10.sp,
                                        color = Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = "• 정비 문서 폴더: DB/rag_documents/",
                                        fontSize = 10.sp,
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
                                            text = "GGUF LLM 모델: " +
                                                    if (status.isQwenModelFound) "qwen2.5-1.5b-instruct-q4_k_m.gguf 로드 완료" else "qwen2.5-1.5b-instruct-q4_k_m.gguf 파일 직접 선택 (탭하여 로드)",
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
                                        text = "RAG 벡터 DB: 총 ${status.totalVectorDocuments}건 문서 임베딩 연동됨",
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
                                        text = "부품 매핑 사전: 동적 부품/위치 어휘 매핑 활성화",
                                        fontSize = 10.5.sp,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
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
                                    text = "내가 등록한 해결방안",
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("기본 (브론즈)", fontSize = 10.sp, color = TossGray400)
                            Text("목표 (마스터: 10건)", fontSize = 10.sp, color = TossGray400)
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
                        text = "등록된 해결방안 목록",
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
                                "해결방안 등록",
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
                            text = "아직 직접 등록한 수리 조치 방안이 없습니다.\n상단의 [+ 해결방안 등록] 버튼으로 추가해 보세요!",
                            color = TossGray400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium
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
                                text = "온디바이스 AI 시스템 사양",
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
                                Text("AI 모델 엔진", fontSize = 11.5.sp, color = TossGray500)
                                Text("Qwen-2.5-1.5B On-Device", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TossBlack)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RAG 벡터 검색 DB", fontSize = 11.5.sp, color = TossGray500)
                                Text("Local SQLite Room DB", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TossBlack)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("오프라인 추론 지원", fontSize = 11.5.sp, color = TossGray500)
                                Text("완전 지원 (네트워크 불필요)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }
}
