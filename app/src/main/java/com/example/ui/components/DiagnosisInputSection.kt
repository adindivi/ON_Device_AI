package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossChipGreenBg
import com.example.ui.theme.TossChipGreenFg
import com.example.ui.theme.TossChipPurpleBg
import com.example.ui.theme.TossChipPurpleFg
import com.example.ui.theme.TossChipRedBg
import com.example.ui.theme.TossChipRedFg
import com.example.ui.theme.TossGray100
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray400
import com.example.ui.theme.TossGray500
import com.example.ui.theme.TossInputBg
import com.example.ui.theme.TossOutline
import com.example.ui.theme.TossWhite
import com.example.ui.theme.CarPrimary

@Composable
fun DiagnosisInputSection(
    dtcInput: String,
    symptomInput: String,
    isDiagnosing: Boolean,
    hasActiveResult: Boolean,
    textSizeScale: Float,
    onDtcChange: (String) -> Unit,
    onSymptomChange: (String) -> Unit,
    onAddChip: (String) -> Unit,
    onStartDiagnosis: () -> Unit,
    onOpenScanner: () -> Unit,
    onStartVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFF4FF))          // surface-container-low tint
            .border(1.dp, Color(0xFFE8ECFA), RoundedCornerShape(16.dp))
            .shadow(elevation = 0.dp, shape = RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Section Header & 2D/3D AR Launch Buttons ──────────────────────
            val context = LocalContext.current
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Diagnosis",
                        tint = TossBlack,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "진단 데이터 입력",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TossBlack,
                            fontSize = (17 * textSizeScale).sp
                        )
                    )
                }

                // ── 2D/3D AR 연동 슬림 캡슐 세그먼트 ([ 2D │ 3D ]) ─────────────
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(20.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // [2D] 세그먼트 -> com.smartarecumap.kyh
                    Box(
                        modifier = Modifier
                            .clickable {
                                launchExternalApp(
                                    context = context,
                                    packageName = "com.smartarecumap.kyh",
                                    appName = "SmartAR ECU MAP(2D)"
                                )
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                            .testTag("btn_ar_2d"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "2D",
                            fontSize = (12 * textSizeScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }

                    // 수직 구분선
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color(0xFFE2E8F0))
                    )

                    // [3D] 세그먼트 -> com.smartpinch3d.kyh
                    Box(
                        modifier = Modifier
                            .clickable {
                                launchExternalApp(
                                    context = context,
                                    packageName = "com.smartpinch3d.kyh",
                                    appName = "SmartPinch 3D"
                                )
                            }
                            .padding(horizontal = 11.dp, vertical = 5.dp)
                            .testTag("btn_ar_3d"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3D",
                            fontSize = (12 * textSizeScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── DTC Code Field ─────────────────────────────────────────────────
            Text(
                text = "DTC 코드",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TossBlack,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (13 * textSizeScale).sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = dtcInput,
                onValueChange = onDtcChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_dtc_code"),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = (16 * textSizeScale).sp,
                    color = TossBlack
                ),
                singleLine = true,
                placeholder = {
                    Text(
                        "예: B24BC96",
                        fontSize = 14.sp,
                        color = TossGray400
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = TossInputBg,
                    unfocusedContainerColor = TossInputBg,
                    focusedTextColor = TossBlack,
                    unfocusedTextColor = TossBlack,
                    focusedPlaceholderColor = TossGray400,
                    unfocusedPlaceholderColor = TossGray400,
                    focusedBorderColor = CarPrimary,
                    unfocusedBorderColor = TossOutline
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Symptom Field ──────────────────────────────────────────────────
            Text(
                text = "발생 증상",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TossBlack,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (13 * textSizeScale).sp
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = symptomInput,
                    onValueChange = onSymptomChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .testTag("input_symptom_text"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (14 * textSizeScale).sp,
                        color = TossBlack
                    ),
                    placeholder = {
                        Text(
                            "차량의 증상을 입력해주세요",
                            fontSize = 14.sp,
                            color = TossGray400
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = TossInputBg,
                        unfocusedContainerColor = TossInputBg,
                        focusedTextColor = TossBlack,
                        unfocusedTextColor = TossBlack,
                        focusedPlaceholderColor = TossGray400,
                        unfocusedPlaceholderColor = TossGray400,
                        focusedBorderColor = CarPrimary,
                        unfocusedBorderColor = TossOutline
                    )
                )

                // Camera OCR & Voice buttons (bottom-right overlay)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Camera / OCR button
                    Surface(
                        shape = CircleShape,
                        color = TossWhite,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onOpenScanner() }
                            .testTag("btn_camera_ocr")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Camera OCR",
                                tint = TossBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Mic / Voice button
                    Surface(
                        shape = CircleShape,
                        color = TossWhite,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .size(34.dp)
                            .clickable { onStartVoiceInput() }
                            .testTag("btn_mic_voice")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Mic Voice",
                                tint = TossBlack,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Quick Symptom Chips ────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 시동 지연 chip (monochrome)
                QuickChip(
                    label = "시동 지연",
                    bgColor = TossInputBg,
                    textColor = TossBlack,
                    onClick = { onAddChip("시동 지연") },
                    testTag = "chip_delay_start"
                )

                // 엔진 경고 chip (monochrome)
                QuickChip(
                    label = "엔진 경고",
                    bgColor = TossInputBg,
                    textColor = TossBlack,
                    onClick = { onAddChip("엔진 경고") },
                    testTag = "chip_engine_warning"
                )

                // 에어컨 불량 chip (monochrome)
                QuickChip(
                    label = "에어컨 불량",
                    bgColor = TossInputBg,
                    textColor = TossBlack,
                    onClick = { onAddChip("에어컨 불량") },
                    testTag = "chip_ac_fault"
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── AI 진단 시작 Button (Pure Black #000000) ────────────────────────
            Button(
                onClick = onStartDiagnosis,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_run_ai_diagnosis"),
                enabled = !isDiagnosing,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF000000),
                    contentColor = TossWhite,
                    disabledContainerColor = TossGray200,
                    disabledContentColor = TossGray500
                )
            ) {
                if (isDiagnosing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TossWhite,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "AI 추론 진행 중...",
                        fontWeight = FontWeight.Bold,
                        fontSize = (15 * textSizeScale).sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Run",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasActiveResult) "AI 진단 재실행" else "AI 진단 시작",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (16 * textSizeScale).sp
                        )
                    )
                }
            }
        }
    }
}

// ── Reusable Quick Chip (Toss Deep Blue Ripple Animation) ───────────────────
@Composable
private fun QuickChip(
    label: String,
    bgColor: Color,
    textColor: Color,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(50.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}

/**
 * 2D/3D 외부 AR 어플리케이션(SmartAR ECU MAP / SmartPinch 3D) 호출 헬퍼 함수
 * 
 * - 안드로이드 시스템 Intent를 사용하여 외부 어플을 엽니다.
 * - 타깃 어플이 미설치된 경우 비정상 종료(Crash)를 방지하고 Toast 알림으로 안전하게 안내합니다.
 * 
 * @param context 안드로이드 컨텍스트
 * @param packageName 호출 대상 패키지명 (2D: com.smartarecumap.kyh, 3D: com.smartpinch3d.kyh)
 * @param appName 토스트 알림에 표시할 어플리케이션 명칭
 */
private fun launchExternalApp(
    context: Context,
    packageName: String,
    appName: String
) {
    try {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(
                context,
                "[$appName] 어플이 설치되어 있지 않습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "어플 실행 중 오류가 발생했습니다: ${e.message}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

