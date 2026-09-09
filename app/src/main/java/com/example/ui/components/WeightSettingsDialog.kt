package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.backend.ScoringWeights
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossGray100
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray400
import com.example.ui.theme.TossGray500
import com.example.ui.theme.TossGray600
import com.example.ui.theme.TossInputBg
import com.example.ui.theme.TossOutline
import com.example.ui.theme.TossWhite
import com.example.ui.theme.CarPrimary
import java.util.Locale

// ── Toss B&W 슬라이더 테마 색상 ────────────────────────────────────────────────
private val SliderActive  = Color(0xFF000000)   // 검정 트랙 + 썸
private val SliderInactive = Color(0xFFE5E5EA)  // 연회색 비활성 트랙
private val ValueBadgeBg  = Color(0xFFF2F2F7)   // 값 배지 배경
private val ValueBadgeFg  = Color(0xFF000000)   // 값 배지 텍스트

/**
 * 비밀번호 입력 인증 다이얼로그 (1234) — Toss B&W 스타일
 */
@Composable
fun PasswordVerificationDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    var passwordInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TossWhite,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 자물쇠 아이콘 박스 (흰 배경 + 검정 테두리)
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(TossGray100)
                        .border(1.5.dp, TossGray200, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = TossBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "관리자 가중치 설정 인증",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TossBlack,
                        fontSize = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "추천 알고리즘 점수 수치를 변경하려면\n비밀번호(1234)를 입력하십시오.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TossGray500,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = {
                        if (it.length <= 8) {
                            passwordInput = it
                            isError = false
                        }
                    },
                    label = {
                        Text("비밀번호 입력", color = TossGray400, fontSize = 12.sp)
                    },
                    singleLine = true,
                    isError = isError,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TossInputBg,
                        unfocusedContainerColor = TossInputBg,
                        focusedBorderColor = CarPrimary,
                        unfocusedBorderColor = TossOutline,
                        focusedTextColor = TossBlack,
                        unfocusedTextColor = TossBlack,
                        errorBorderColor = Color(0xFFEF4444),
                        errorContainerColor = Color(0xFFFFF0F0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isError) {
                    Text(
                        text = "비밀번호가 올바르지 않습니다.",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 취소 버튼 (회색 테두리)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TossGray200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TossGray600
                        )
                    ) {
                        Text("취소", fontWeight = FontWeight.SemiBold)
                    }

                    // 확인 버튼 (검정)
                    Button(
                        onClick = {
                            if (passwordInput == "1234") {
                                onVerify(passwordInput)
                            } else {
                                isError = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TossBlack,
                            contentColor = TossWhite
                        )
                    ) {
                        Text("확인", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 모바일 최적화 RAG 추천 가중치 조절 다이얼로그 — Toss B&W 스타일
 */
@Composable
fun WeightSettingsDialog(
    initialWeights: ScoringWeights,
    onDismiss: () -> Unit,
    onSave: (ScoringWeights) -> Unit,
    onReset: () -> Unit
) {
    var dtcBoost    by remember { mutableFloatStateOf(initialWeights.dtcExactBoost) }
    var compBoost   by remember { mutableFloatStateOf(initialWeights.compMatchBoost) }
    var textBoost   by remember { mutableFloatStateOf(initialWeights.textOverlapBoost) }
    var upvoteScale by remember { mutableFloatStateOf(initialWeights.upvoteBonusScale) }
    var aiWeight    by remember { mutableFloatStateOf(initialWeights.aiTrackWeight) }

    val scrollState = rememberScrollState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = TossWhite,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(scrollState)
            ) {
                // ── 헤더 ─────────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 설정 아이콘 박스 (검정 테두리, 흰 배경)
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TossGray100)
                                .border(1.5.dp, TossGray200, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TossBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "추천방안 가중치 조절",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TossBlack,
                                fontSize = 17.sp
                            )
                        )
                    }

                    // 초기화 버튼
                    IconButton(
                        onClick = {
                            val defaultWeights = ScoringWeights()
                            dtcBoost    = defaultWeights.dtcExactBoost
                            compBoost   = defaultWeights.compMatchBoost
                            textBoost   = defaultWeights.textOverlapBoost
                            upvoteScale = defaultWeights.upvoteBonusScale
                            aiWeight    = defaultWeights.aiTrackWeight
                            onReset()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TossGray100)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = TossGray500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "슬라이더를 조절하여 하이브리드 RRF 추천 알고리즘 가중치를 커스텀하십시오.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TossGray500,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    ),
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                HorizontalDivider(color = TossGray200, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // ── [섹션 1] 트랙 1: 키워드 & 정비 데이터 가중치 ─────────────────────
                Text(
                    text = "트랙 1: 키워드 & 정비 데이터 가중치",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TossGray600,
                        fontSize = 12.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))

                WeightSliderItem(
                    emoji = "🥇",
                    number = 1,
                    title = "DTC 고장코드 매칭 점수",
                    value = dtcBoost,
                    valueRange = 0.0f..30.0f,
                    description = "DTC 고장 코드가 같을 때 최우선 부여되는 점수 (기본 15.0점)",
                    onValueChange = { dtcBoost = (it * 2).toInt() / 2.0f }
                )

                WeightSliderItem(
                    emoji = "🔩",
                    number = 2,
                    title = "부품명 직접 매칭 점수",
                    value = compBoost,
                    valueRange = 0.0f..5.0f,
                    description = "입력 문장에 부품명이 직접 일치할 때 부여 점수 (기본 2.0점)",
                    onValueChange = { compBoost = (it * 10).toInt() / 10.0f }
                )

                WeightSliderItem(
                    emoji = "💬",
                    number = 3,
                    title = "증상 단어 일치 점수",
                    value = textBoost,
                    valueRange = 0.0f..2.0f,
                    description = "입력한 증상 키워드 1개당 일치 보너스 (기본 1.0점)",
                    onValueChange = { textBoost = (it * 10).toInt() / 10.0f }
                )

                WeightSliderItem(
                    emoji = "👍",
                    number = 4,
                    title = "현장 추천 보너스 점수",
                    value = upvoteScale,
                    valueRange = 0.0f..3.0f,
                    description = "정비사 도움됨 추천 누적 1회당 적용되는 가산점 (기본 1.0점)",
                    onValueChange = { upvoteScale = (it * 10).toInt() / 10.0f }
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = TossGray200, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // ── [섹션 2] 트랙 2: 표준 RRF AI 문맥 반영 배율 (하단 직관적 분리 카드) ──
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEEF2FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧠", fontSize = 13.sp)
                            }
                            Text(
                                text = "트랙 2: 표준 RRF AI 문맥 가중치",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    fontSize = 13.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "최종 순위 산출 시, 키워드(1.0x) 대비 온디바이스 Ko-SBERT AI의 의미 분석을 몇 배로 반영할지 결정합니다.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = TossGray500,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        WeightSliderItem(
                            emoji = "⚡",
                            number = 5,
                            title = "AI 문맥 종합 반영 배율",
                            value = aiWeight,
                            valueRange = 0.0f..3.0f,
                            unit = "x",
                            description = when {
                                aiWeight == 0.0f -> "AI 문맥 무시 (키워드 100% 전용 검색)"
                                aiWeight < 1.0f  -> "키워드 우선 모드 (AI ${String.format(Locale.getDefault(), "%.1f", aiWeight)}x 반영)"
                                aiWeight == 1.0f -> "표준 균형 모드 (키워드 1.0x : AI 1.0x 동등)"
                                aiWeight <= 2.0f -> "AI 문맥 우대 모드 (AI ${String.format(Locale.getDefault(), "%.1f", aiWeight)}x 강력 반영)"
                                else             -> "AI 문맥 최우선 모드 (AI ${String.format(Locale.getDefault(), "%.1f", aiWeight)}x 초강력 반영)"
                            },
                            onValueChange = { aiWeight = (it * 10).toInt() / 10.0f }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 하단 버튼 ─────────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 취소 버튼 (회색 테두리)
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, TossGray200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TossGray600
                        )
                    ) {
                        Text(
                            "취소",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }

                    // 저장 및 적용 버튼 (검정)
                    Button(
                        onClick = {
                            val updatedWeights = ScoringWeights(
                                dtcExactBoost    = dtcBoost,
                                compMatchBoost   = compBoost,
                                textOverlapBoost = textBoost,
                                upvoteBonusScale = upvoteScale,
                                aiTrackWeight    = aiWeight,
                                aiAmpScale       = 4.0f
                            )
                            onSave(updatedWeights)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TossBlack,
                            contentColor = TossWhite
                        )
                    ) {
                        Text(
                            "저장 및 적용",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

// ── 가중치 슬라이더 항목 컴포넌트 (Toss B&W 스타일) ─────────────────────────────
@Composable
private fun WeightSliderItem(
    emoji: String,
    number: Int,
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    description: String,
    unit: String = "점",
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // ── 제목 행: 번호+제목 / 점수 배지 ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$emoji $number. $title",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = TossBlack,
                    fontSize = 13.5.sp
                ),
                modifier = Modifier.weight(1f)
            )

            // 현재 값 배지 (회색 pill)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(ValueBadgeBg)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", value)}$unit",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = ValueBadgeFg,
                        fontSize = 12.sp
                    )
                )
            }
        }

        // ── 설명 텍스트 ──────────────────────────────────────────────────────
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TossGray500,
                fontSize = 11.sp,
                lineHeight = 16.sp
            ),
            modifier = Modifier.padding(top = 3.dp)
        )

        // ── 슬라이더 (검정 트랙) ─────────────────────────────────────────────
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = SliderActive,
                activeTrackColor = SliderActive,
                inactiveTrackColor = SliderInactive,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        )
    }
}
