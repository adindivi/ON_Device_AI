package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossWhite

/**
 * [DiagnosticStepper]
 * 핀테크 뉴트럴 미니멀 실시간 진단 프로세스 스텝퍼
 * - 원색(파랑/초록) 난립을 배제하고 단색 차콜 블랙(#0F172A)과 뉴트럴 그레이(#94A3B8)로 정돈
 * - 삼성 One UI 폰트 베이스라인 교정(PlatformTextStyle includeFontPadding = false) 적용
 */
@Composable
fun DiagnosticStepper(
    isDiagnosing: Boolean,
    diagnosisStep: Int,
    hasActiveResult: Boolean,
    modifier: Modifier = Modifier
) {
    if (isDiagnosing || diagnosisStep > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TossWhite),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                Text(
                    text = "실시간 진단 프로세스",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val steps = listOf("계획 수립", "데이터 검색", "원인 분석", "보고서 생성")
                    steps.forEachIndexed { index, title ->
                        val stepNum = index + 1
                        val isCompleted = diagnosisStep > stepNum || (!isDiagnosing && hasActiveResult)
                        val isCurrent = isDiagnosing && diagnosisStep == stepNum

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (!isCompleted && !isCurrent) {
                                            Modifier.border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                        } else Modifier
                                    )
                                    .background(
                                        when {
                                            isCompleted -> TossBlack
                                            isCurrent -> TossBlack
                                            else -> Color(0xFFF8FAFC)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "완료",
                                        tint = TossWhite,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$stepNum",
                                        color = if (isCurrent) TossWhite else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        style = TextStyle(
                                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCompleted || isCurrent) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                style = TextStyle(
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
