package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DiagnosticHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecordsScreen(
    historyList: List<DiagnosticHistory>,
    onSelectHistory: (DiagnosticHistory) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "WARNING", "NORMAL"

    val filteredList = historyList.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.dtcCode.contains(searchQuery, ignoreCase = true) ||
                item.symptomText.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            "WARNING" -> item.statusType == "WARNING"
            "NORMAL" -> item.statusType == "NORMAL"
            else -> true
        }

        matchesSearch && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        // Title Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = "Records",
                tint = Color(0xFF004AC6),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "진단 이력 및 라이브러리",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_history_input"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontSize = 13.sp),
            placeholder = { Text("DTC 코드 또는 증상 검색", fontSize = 13.sp, color = Color(0xFF64748B)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF64748B)
                )
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedPlaceholderColor = Color(0xFF64748B),
                unfocusedPlaceholderColor = Color(0xFF64748B),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFE2E8F0)
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("전체 (${historyList.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDBE1FF),
                    selectedLabelColor = Color(0xFF00174B)
                )
            )
            FilterChip(
                selected = selectedFilter == "WARNING",
                onClick = { selectedFilter = "WARNING" },
                label = { Text("경고/점검") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFFFDAD6),
                    selectedLabelColor = Color(0xFF93000A)
                )
            )
            FilterChip(
                selected = selectedFilter == "NORMAL",
                onClick = { selectedFilter = "NORMAL" },
                label = { Text("정상") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFFDCFCE7),
                    selectedLabelColor = Color(0xFF15803D)
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "검색된 진단 이력이 없습니다.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF94A3B8))
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredList, key = { it.id }) { item ->
                    val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
                    val dateStr = dateFormat.format(Date(item.timestamp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectHistory(item) }
                            .testTag("records_item_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val iconBg = if (item.statusType == "WARNING") Color(0xFFFEE2E2) else Color(0xFFDBE1FF)
                                    val iconTint = if (item.statusType == "WARNING") Color(0xFFDC2626) else Color(0xFF2563EB)
                                    val iconSym = if (item.statusType == "WARNING") Icons.Default.Warning else Icons.Default.AcUnit

                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(iconBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = iconSym,
                                            contentDescription = "Status",
                                            tint = iconTint,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B)
                                            )
                                        )
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color(0xFF94A3B8),
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Open",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = item.summary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF475569),
                                    fontSize = 12.sp
                                ),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}
