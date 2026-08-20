package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.ExtractedMetadata

@Composable
fun AddRemedyDialog(
    onDismiss: () -> Unit,
    onAnalyzeMetadata: (String) -> ExtractedMetadata,
    onSaveRemedy: (String, String?, String?, String?) -> Unit
) {
    var solutionText by remember { mutableStateOf("") }
    var dtcCode by remember { mutableStateOf("") }
    var component by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var hasExtracted by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "신규 조치 방안 추가",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "새로운 해결책이나 정비 노하우를 공유해 주세요.",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF64748B))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Input Remedy Text (Notion / Apple Luxury Note Style)
                OutlinedTextField(
                    value = solutionText,
                    onValueChange = { solutionText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .testTag("input_new_solution_text"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF1E293B), fontSize = 13.sp),
                    placeholder = {
                        Text(
                            text = "정비 노하우를 자유롭게 입력하세요. 온디바이스 AI가 부품명과 고장코드를 자동 감지합니다.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF94A3B8)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedTextColor = Color(0xFF1E293B),
                        unfocusedTextColor = Color(0xFF1E293B),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF94A3B8),
                        focusedBorderColor = Color(0xFF004AC6),
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Metadata Extraction Button
                Button(
                    onClick = {
                        if (solutionText.isNotBlank()) {
                            val extracted = onAnalyzeMetadata(solutionText)
                            dtcCode = extracted.dtcCode ?: ""
                            component = extracted.component ?: ""
                            location = extracted.location ?: ""
                            hasExtracted = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_analyze_metadata"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF334155),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Extract",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "메타데이터 자동 추출",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (hasExtracted || dtcCode.isNotBlank() || component.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "추출된 메타데이터 검토",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = dtcCode,
                        onValueChange = { dtcCode = it },
                        label = { Text("DTC 코드 (옵션)", color = Color(0xFF475569)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_meta_dtc"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontSize = 13.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = component,
                        onValueChange = { component = it },
                        label = { Text("관련 부품 (옵션)", color = Color(0xFF475569)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_meta_component"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontSize = 13.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("커넥터 위치 (옵션)", color = Color(0xFF475569)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_meta_location"),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.Black, fontSize = 13.sp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedLabelColor = Color(0xFF10B981),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("취소", color = Color(0xFF64748B))
                    }

                    Button(
                        onClick = {
                            if (solutionText.isNotBlank()) {
                                onSaveRemedy(
                                    solutionText,
                                    dtcCode.ifBlank { null },
                                    component.ifBlank { null },
                                    location.ifBlank { null }
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_add_remedy"),
                        enabled = solutionText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Text("DB에 최종 등록", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
