package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.SearchResult
import com.example.data.local.DiagnosticHistory

@Composable
fun ActiveDiagnosticGuideCard(
    result: DiagnosticHistory,
    activeResultMatches: List<SearchResult>,
    isGuideExpanded: Boolean,
    isDiagnosing: Boolean,
    textSizeScale: Float,
    onToggleGuide: () -> Unit
) {
Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_result_display"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F6FD)),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFD0D7F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Bar with Gemini AI Badge and Expand Chevron
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleGuide() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF004AC6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Guide",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "스마트 정비 진단서",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF004AC6)
                                    )
                                )
                                Text(
                                    text = "RAG 768차원 벡터 신경망 AI 솔루션",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF434655),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onToggleGuide,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isGuideExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle",
                                tint = Color(0xFF004AC6)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isGuideExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))

                             // 1. 순위별 개별 추천 방안 카드 (Individual Cards for Top 3 Recommendations)
                             val cardMatches = if (activeResultMatches.isNotEmpty()) activeResultMatches.take(3) else emptyList()

                             if (cardMatches.isNotEmpty()) {
                                 cardMatches.forEachIndexed { idx, match ->
                                     RecommendationCard(
                                         match = match,
                                         rankIndex = idx,
                                         textSizeScale = textSizeScale,
                                         typingTextComposable = { text, styleWrapper ->
                                             TypingText(
                                                 fullText = text,
                                                 style = TextStyle(
                                                     fontSize = styleWrapper.fontSize,
                                                     color = styleWrapper.color,
                                                     fontWeight = styleWrapper.fontWeight
                                                 )
                                             )
                                         }
                                     )
                                     Spacer(modifier = Modifier.height(14.dp))
                                 }
                             } else {
                                 Box(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .clip(RoundedCornerShape(10.dp))
                                         .background(Color.White)
                                         .border(1.dp, Color(0xFFD3D5E7), RoundedCornerShape(10.dp))
                                         .padding(12.dp)
                                 ) {
                                     Text(
                                         text = result.fullAnalysis,
                                         style = MaterialTheme.typography.bodyMedium.copy(
                                             color = Color(0xFF334155),
                                             fontSize = (13 * textSizeScale).sp
                                         )
                                     )
                                 }
                             }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 2. 추정 원인 (Potential Causes)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFD3D5E7), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Build,
                                            contentDescription = "Causes",
                                            tint = Color(0xFF004AC6),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "추정 원인",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF004AC6)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    result.checksListJson.split("|").forEach { cause ->
                                        if (cause.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "• ",
                                                    color = Color(0xFF004AC6),
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = cause,
                                                    fontSize = (12 * textSizeScale).sp,
                                                    color = Color(0xFF334155),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 3. 조치 방안 (Action Steps)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lightbulb,
                                            contentDescription = "Advice",
                                            tint = Color(0xFF1D4ED8),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "조치 방안",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    var fallbackToText by remember { mutableStateOf(false) }
                                    if (result.solutionText.startsWith("[JSON_GRAPH_START]") && !fallbackToText) {
                                        com.example.ui.components.RootCauseGraphView(
                                            jsonGraphString = result.solutionText,
                                            onFallback = { fallbackToText = true }
                                        )
                                    } else {
                                        val displayStr = result.solutionText.removePrefix("[JSON_GRAPH_START]").trim()
                                        TypingText(
                                            fullText = displayStr,
                                            style = TextStyle(
                                                fontSize = (12 * textSizeScale).sp,
                                                color = Color(0xFF1E3A8A),
                                                lineHeight = 18.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
}
