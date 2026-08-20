package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = "실시간 진단 프로세스",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CarTextMuted,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompleted -> Color(0xFF004AC6)
                                            isCurrent -> Color(0xFFDBEAFE)
                                            else -> Color(0xFFF1F5F9)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "$stepNum",
                                        color = if (isCurrent) Color(0xFF004AC6) else Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent || isCompleted) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCompleted) Color(0xFF047857) else if (isCurrent) CarPrimaryContainer else CarTextSubtle
                            )
                        }
                    }
                }
            }
        }
    }
}
