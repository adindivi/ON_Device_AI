package com.example.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.SearchResult
import com.example.data.local.RagDocument

@Composable
fun TechnicalDocumentsSection(
    isTechDocsExpanded: Boolean,
    ragDocuments: List<RagDocument>,
    activeResultMatches: List<SearchResult>,
    textSizeScale: Float,
    onToggleTechDocs: () -> Unit,
    onOpenAddRemedy: () -> Unit,
    onDocumentSelect: (RagDocument) -> Unit,
    onRecommendDocument: (Long, String) -> Unit
) {
Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Black icon box with book icon (HTML: w-6 h-6 rounded bg-black)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Docs",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTechDocsExpanded) "참고 문서  (Top 10 펼침)" else "참고 문서  (Top 3 선별)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    )
                }

                // "+ 노하우 추가" button (Toss ice blue)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEFF6FF))
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(8.dp))
                        .clickable { onOpenAddRemedy() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ 해결방안 추가",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF004AC6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val maxDocsCount = if (isTechDocsExpanded) 10 else 3
            val displayDocs = run {
                val matchedDocs = activeResultMatches.mapIndexed { idx, match ->
                    RagDocument(
                        id = (idx + 1).toLong(),
                        docCode = match.id,
                        category = if (match.metadata.dtcCode.isNotBlank()) "DTC Guide" else "Remedy",
                        title = "${idx + 1}순위: ${match.metadata.component.ifBlank { "정비 지침" }}",
                        snippet = match.text.take(80) + "...",
                        fullContent = match.text,
                        dtcCode = match.metadata.dtcCode.ifBlank { null },
                        component = match.metadata.component.ifBlank { null },
                        connectorLocation = match.metadata.connectorLocation.ifBlank { null },
                        sourceName = "RAG Top ${idx + 1}",
                        recommendationCount = match.recommendations,
                        isUserAdded = false,
                        dateString = "실시간 매칭"
                    )
                }
                val existingCodes = matchedDocs.map { it.docCode }.toSet()
                val additionalDocs = ragDocuments.filter { it.docCode !in existingCodes }
                (matchedDocs + additionalDocs).take(maxDocsCount)
            }

            displayDocs.forEach { doc ->
                var localUpvoteCount by remember(doc.id, doc.recommendationCount) { androidx.compose.runtime.mutableIntStateOf(doc.recommendationCount) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onDocumentSelect(doc) }
                        .testTag("rag_card_${doc.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = CardDefaults.outlinedCardBorder()
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
                                            .background(Color(0xFFF1F5F9))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = dtc,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }
                            }

                            Text(text = doc.dateString, fontSize = 9.sp, color = Color(0xFF94A3B8))
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                fontSize = (14 * textSizeScale).sp
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = doc.snippet,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = (11 * textSizeScale).sp,
                                lineHeight = (16 * textSizeScale).sp
                            ),
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = "DB",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = doc.sourceName,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            // Upvote / Recommend Button
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clickable {
                                        localUpvoteCount++
                                        onRecommendDocument(doc.id, doc.docCode)
                                    }
                                    .testTag("btn_upvote_doc_${doc.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ThumbUp,
                                        contentDescription = "Upvote",
                                        tint = Color(0xFF2563EB),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "도움됨",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2563EB)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$localUpvoteCount",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleTechDocs() }
                    .testTag("btn_expand_tech_docs"),
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isTechDocsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isTechDocsExpanded) "간략히 보기" else "관련 정비 지침 더보기",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            fontSize = 12.sp
                        )
                    )
                }
            }
        }
}
