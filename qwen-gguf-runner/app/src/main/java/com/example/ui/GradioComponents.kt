package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChatMessage
import com.example.model.GpuAccelerationBackend
import com.example.model.MessageSender
import com.example.model.ProfilingLog
import com.example.ui.theme.Blue100
import com.example.ui.theme.Blue700
import com.example.ui.theme.Green100
import com.example.ui.theme.Green500
import com.example.ui.theme.Green700
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.HighDensitySurface
import com.example.ui.theme.HighDensitySurfaceSubtle
import com.example.ui.theme.Indigo100
import com.example.ui.theme.Indigo200
import com.example.ui.theme.Indigo400
import com.example.ui.theme.Indigo50
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Indigo600
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose700
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate300
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate50
import com.example.ui.theme.Slate500
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Top High Density App Header with live 3-column stats grid and status pills
 */
@Composable
fun GradioHeader(
    uiState: QwenUiState,
    onOpenProfiling: () -> Unit,
    onToggleSettings: () -> Unit,
    onClearChat: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gradio_header"),
        color = HighDensitySurface,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Row: App Brand & Status Badges & Quick Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Icon & App Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo600),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isStreaming) {
                            val infiniteTransition = rememberInfiniteTransition(label = "spin_spinner")
                            val rotation by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "rotation"
                            )
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Qwen LLM GGUF",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            fontSize = 17.sp
                        ),
                        color = Slate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Center/Right: Badges & Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // GPU Active / Engine Status Badge
                    val gpuActive = uiState.gpuBackend != GpuAccelerationBackend.CPU_ONLY
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (gpuActive) Green100 else Slate100
                    ) {
                        Text(
                            text = if (gpuActive) "GPU Active" else "CPU MODE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = if (gpuActive) Green700 else Slate600
                            )
                        )
                    }

                    // Quantization Badge
                    val quantLabel = uiState.loadedModel?.quantizationType?.label ?: "Q4_K_M"
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Blue100
                    ) {
                        Text(
                            text = quantLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp,
                                color = Blue700
                            )
                        )
                    }

                    // Action Icons
                    IconButton(
                        onClick = onOpenProfiling,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_profiling_logs")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "프로파일링 로그 JSON",
                            tint = if (uiState.profilingLogs.isNotEmpty()) Indigo600 else Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleSettings,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_toggle_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "엔진 설정",
                            tint = if (uiState.showSettingsAccordion) Indigo600 else Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_clear_chat")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "대화 비우기",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-Column High Density Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Column 1: Memory
                val totalRamMb = uiState.memoryPolicy?.totalDeviceRamMb ?: 4096L
                val availRamMb = uiState.memoryPolicy?.availableDeviceRamMb ?: 2800L
                val usedRamGb = (totalRamMb - availRamMb).coerceAtLeast(1024L) / 1024.0
                val totalRamGb = totalRamMb / 1024.0
                HighDensityStatCard(
                    label = "MEMORY",
                    value = "${String.format(Locale.US, "%.1f", usedRamGb)} / ${String.format(Locale.US, "%.1f", totalRamGb)} GB",
                    modifier = Modifier.weight(1f)
                )

                // Column 2: Inference Speed
                val currentTps = if (uiState.liveTps > 0.0) {
                    uiState.liveTps
                } else {
                    uiState.profilingLogs.firstOrNull()?.tokensPerSecond ?: 18.4
                }
                HighDensityStatCard(
                    label = "INFERENCE",
                    value = "${String.format(Locale.US, "%.1f", currentTps)} t/s",
                    modifier = Modifier.weight(1f)
                )

                // Column 3: Threads
                HighDensityStatCard(
                    label = "THREADS",
                    value = "${uiState.threads} Cores",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Reusable High Density stat tile
 */
@Composable
private fun HighDensityStatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Slate50,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Slate500,
                    letterSpacing = 0.5.sp
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * GGUF Sandbox Model Selector and Import Bar
 */
@Composable
fun GradioModelBar(
    uiState: QwenUiState,
    onImportClick: () -> Unit,
    onSelectSandboxModel: (File) -> Unit,
    onDeleteModel: (File) -> Unit
) {
    var showModelDropdown by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("gradio_model_bar"),
        colors = CardDefaults.cardColors(
            containerColor = HighDensitySurface
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showModelDropdown = !showModelDropdown },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Indigo50),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.loadedModel?.modelName ?: uiState.modelFileName ?: "모델 없음 (GGUF 선택)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (uiState.loadedModel != null) {
                                "${uiState.loadedModel.quantizationType.label} • ${String.format(Locale.US, "%.1f", uiState.loadedModel.fileSizeMb)}MB • ${uiState.gpuBackend.displayName}"
                            } else "탭하여 샌드박스 모델 목록 확인",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Slate500
                        )
                    }
                }

                Button(
                    onClick = onImportClick,
                    modifier = Modifier.testTag("btn_import_gguf"),
                    colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "GGUF 로드", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                }
            }

            if (uiState.isLoadingModel) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (uiState.loadingProgress > 0f) uiState.loadingProgress else 0.5f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Indigo600,
                    trackColor = Indigo100
                )
            }

            // Dropdown list for Sandbox models
            AnimatedVisibility(visible = showModelDropdown) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    HorizontalDivider(color = Slate100)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "앱 샌드박스 내부 GGUF 모델 (${uiState.sandboxModels.size}개)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Indigo600
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (uiState.sandboxModels.isEmpty()) {
                        Text(
                            text = "샌드박스에 로드된 GGUF 파일이 없습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate500,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        uiState.sandboxModels.forEach { file ->
                            val isCurrent = uiState.modelFileName == file.name
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isCurrent) Indigo50 else Color.Transparent)
                                    .clickable {
                                        onSelectSandboxModel(file)
                                        showModelDropdown = false
                                    }
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = if (isCurrent) Indigo600 else Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCurrent) Indigo600 else Slate700
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteModel(file) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "삭제",
                                        tint = Slate400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Expandable Settings Accordion for llama.cpp parameters, GPU Acceleration & VRAM Policy
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradioSettingsAccordion(
    uiState: QwenUiState,
    onUpdateParameters: (Float, Float, Int, Int, GpuAccelerationBackend, Int, String) -> Unit
) {
    var sysPrompt by remember(uiState.systemPrompt) { mutableStateOf(uiState.systemPrompt) }
    var temp by remember(uiState.temperature) { mutableStateOf(uiState.temperature) }
    var topP by remember(uiState.topP) { mutableStateOf(uiState.topP) }
    var maxTok by remember(uiState.maxTokens) { mutableStateOf(uiState.maxTokens.toFloat()) }
    var threads by remember(uiState.threads) { mutableStateOf(uiState.threads.toFloat()) }
    var gpuBackend by remember(uiState.gpuBackend) { mutableStateOf(uiState.gpuBackend) }
    var gpuLayers by remember(uiState.gpuLayers) { mutableStateOf(uiState.gpuLayers.toFloat()) }

    AnimatedVisibility(
        visible = uiState.showSettingsAccordion,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("gradio_settings_accordion"),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "⚙️ 엔진 & 하이퍼파라미터 설정",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Indigo600)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // System Prompt
                Text(text = "System Prompt", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = sysPrompt,
                    onValueChange = {
                        sysPrompt = it
                        onUpdateParameters(temp, topP, maxTok.toInt(), threads.toInt(), gpuBackend, gpuLayers.toInt(), it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_system_prompt"),
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Indigo600,
                        unfocusedBorderColor = Slate200,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Acceleration Backend Selection
                Text(text = "가속 백엔드 (GPU Acceleration)", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Slate700))
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GpuAccelerationBackend.entries.forEach { backend ->
                        val isSelected = gpuBackend == backend
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                gpuBackend = backend
                                onUpdateParameters(temp, topP, maxTok.toInt(), threads.toInt(), backend, gpuLayers.toInt(), sysPrompt)
                            },
                            label = { Text(backend.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Indigo600,
                                selectedLabelColor = Color.White,
                                containerColor = Slate50,
                                labelColor = Slate700
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) Indigo600 else Slate200
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Multi-threading slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "CPU 멀티스레딩 (${threads.toInt()} 코어)", style = MaterialTheme.typography.labelMedium.copy(color = Slate700))
                    Text(text = "Available: ${Runtime.getRuntime().availableProcessors()}", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
                Slider(
                    value = threads,
                    onValueChange = {
                        threads = it
                        onUpdateParameters(temp, topP, maxTok.toInt(), it.toInt(), gpuBackend, gpuLayers.toInt(), sysPrompt)
                    },
                    valueRange = 1f..16f,
                    steps = 14,
                    colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Slate200)
                )

                // GPU Layers slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "GPU 레이어 오프로드 (${gpuLayers.toInt()} Layers)", style = MaterialTheme.typography.labelMedium.copy(color = Slate700))
                    Text(text = "Max: ${uiState.loadedModel?.blockCount ?: 32}", style = MaterialTheme.typography.labelSmall, color = Slate500)
                }
                Slider(
                    value = gpuLayers,
                    onValueChange = {
                        gpuLayers = it
                        onUpdateParameters(temp, topP, maxTok.toInt(), threads.toInt(), gpuBackend, it.toInt(), sysPrompt)
                    },
                    valueRange = 0f..(uiState.loadedModel?.blockCount?.toFloat() ?: 32f),
                    colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Slate200)
                )

                // Temperature
                Text(text = "Temperature: ${String.format(Locale.US, "%.2f", temp)}", style = MaterialTheme.typography.labelMedium.copy(color = Slate700))
                Slider(
                    value = temp,
                    onValueChange = {
                        temp = it
                        onUpdateParameters(it, topP, maxTok.toInt(), threads.toInt(), gpuBackend, gpuLayers.toInt(), sysPrompt)
                    },
                    valueRange = 0.0f..1.5f,
                    colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Slate200)
                )

                // Max Tokens
                Text(text = "Max Tokens: ${maxTok.toInt()}", style = MaterialTheme.typography.labelMedium.copy(color = Slate700))
                Slider(
                    value = maxTok,
                    onValueChange = {
                        maxTok = it
                        onUpdateParameters(temp, topP, it.toInt(), threads.toInt(), gpuBackend, gpuLayers.toInt(), sysPrompt)
                    },
                    valueRange = 64f..2048f,
                    colors = SliderDefaults.colors(thumbColor = Indigo600, activeTrackColor = Indigo600, inactiveTrackColor = Slate200)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // VRAM / RAM Policy Info Box
                uiState.memoryPolicy?.let { policy ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Slate50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "📊 VRAM & 메모리 정책",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Indigo600
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• 기기 RAM: ${policy.totalDeviceRamMb}MB (여유: ${policy.availableDeviceRamMb}MB)\n" +
                                        "• 힙 최대: ${policy.heapMaxMb}MB | 저사양 모드: ${if (policy.isLowRamDevice) "ON" else "OFF"}\n" +
                                        "• 권장 컨텍스트: ${policy.maxRecommendedContext} Tokens | KV 압축: ${policy.kvCacheCompressionEnabled}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Chat Message List with High Density styling and animated tokens
 */
@Composable
fun GradioChatList(
    messages: List<ChatMessage>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (messages.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Indigo50,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Qwen 모바일 추론 준비 완료",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate800)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "GGUF 모델을 로드하고 온디바이스 AI 대화를 시작하세요.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate500,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Suggestion chips
                val suggestions = listOf(
                    "현재 시스템의 추론 성능을 프로파일링해줘.",
                    "GPU 가속 및 스레드 설정 확인",
                    "Qwen 모델 아키텍처 사양"
                )
                suggestions.forEach { prompt ->
                    OutlinedButton(
                        onClick = { onSuggestionClick(prompt) },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = HighDensitySurface
                        )
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate700, fontWeight = FontWeight.Medium)
                        )
                    }
                }
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                HighDensityMessageItem(message = msg)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Individual High Density Chat Message Bubble
 */
@Composable
fun HighDensityMessageItem(message: ChatMessage) {
    val context = LocalContext.current
    val isUser = message.sender == MessageSender.USER
    val isFailure = message.isFailure
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (isUser) {
                // User Bubble: bg-indigo-600 p-3 rounded-2xl rounded-tr-none shadow-md
                Surface(
                    modifier = Modifier
                        .testTag("user_message_card")
                        .shadow(2.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = Indigo600
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                lineHeight = 20.sp,
                                fontSize = 14.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "USER • $formattedTime",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = Indigo200
                            ),
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            } else {
                // Assistant / System Bubble: bg-white p-3 rounded-2xl rounded-tl-none shadow-sm border border-slate-100
                Surface(
                    modifier = Modifier
                        .testTag(if (isFailure) "failure_message_card" else "assistant_message_card")
                        .shadow(1.dp, shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                    color = if (isFailure) Rose100.copy(alpha = 0.4f) else HighDensitySurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFailure) Rose500 else Slate100)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        // Header info & copy button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (message.isStreaming) {
                                // Three animated bouncing dots
                                HighDensityBouncingDots()
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (message.tokensPerSecond > 0.0) {
                                        Text(
                                            text = "${String.format(Locale.US, "%.1f", message.tokensPerSecond)} t/s",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = Green700,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            if (message.content.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Qwen Response", message.content)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "클립보드에 복사되었습니다", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "복사",
                                        tint = Slate400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        if (message.content.isNotBlank() || !message.isStreaming) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Message content (text or JSON formatting)
                        if (message.content.isNotBlank()) {
                            val isJson = message.content.trim().startsWith("{") && message.content.trim().endsWith("}")
                            if (isJson) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate50,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = Slate800,
                                            lineHeight = 18.sp
                                        ),
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = if (isFailure) Rose700 else Slate700,
                                        lineHeight = 20.sp,
                                        fontSize = 14.sp
                                    )
                                )
                            }
                        }

                        // Subtitle: QWEN • STREAMING or SYSTEM • 10:24 AM
                        val senderLabel = when {
                            message.sender == MessageSender.SYSTEM -> "SYSTEM"
                            isFailure -> "ERROR"
                            else -> "QWEN"
                        }
                        val statusLabel = if (message.isStreaming) "STREAMING" else formattedTime

                        Text(
                            text = "$senderLabel • $statusLabel",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = if (isFailure) Rose700 else Slate400,
                                letterSpacing = 0.5.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3-Dot Bouncing Animation Indicator (Indigo400)
 */
@Composable
private fun HighDensityBouncingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots_bounce")

    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Indigo400.copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Indigo400.copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(Indigo400.copy(alpha = dot3Alpha))
        )
    }
}

/**
 * High Density Bottom Input Row with pill field, Send/Stop button, and engine status footer
 */
@Composable
fun GradioInputRow(
    isStreaming: Boolean,
    onSendMessage: (String) -> Unit,
    onStopStreaming: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .testTag("gradio_input_row"),
        color = HighDensitySurface,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Pill input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Slate50)
                    .border(1.dp, Slate200, RoundedCornerShape(24.dp))
                    .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                        .testTag("input_chat_text"),
                    textStyle = TextStyle(
                        color = Slate800,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Indigo600),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isStreaming) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = "메시지를 입력하세요...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Slate400,
                                    fontSize = 14.sp
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.width(6.dp))

                if (isStreaming) {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onStopStreaming() }
                            .testTag("btn_stop_streaming"),
                        color = Rose500,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "중단",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(enabled = inputText.isNotBlank()) {
                                if (inputText.isNotBlank()) {
                                    onSendMessage(inputText)
                                    inputText = ""
                                }
                            }
                            .testTag("btn_send_chat"),
                        color = if (inputText.isNotBlank()) Indigo600 else Slate300,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "전송",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer metadata row: Engine status & VRAM Policy
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_dot")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_pulse"
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Green500.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ENGINE: LLAMA.CPP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Text(
                    text = "VRAM Policy: STRICT_Q4",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Slate400
                    )
                )
            }
        }
    }
}

/**
 * Profiling JSON Logs Dialog (profiling_log.json viewer)
 */
@Composable
fun ProfilingJsonDialog(
    logs: List<ProfilingLog>,
    onDismiss: () -> Unit,
    onClearLogs: () -> Unit
) {
    val context = LocalContext.current
    val latestLog = logs.firstOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("dialog_profiling_logs"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Indigo600)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "profiling_log.json",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Slate900)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "닫기", tint = Slate500)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (latestLog != null) {
                    // Metric Summary Grid
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "최근 실행 메트릭 요약",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Indigo600)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricChip("로딩 시간", "${latestLog.loadTimeMs} ms")
                                MetricChip("추론 시간", "${latestLog.inferenceTimeMs} ms")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricChip("토큰 속도", "${String.format(Locale.US, "%.1f", latestLog.tokensPerSecond)} t/s")
                                MetricChip("RAM 점유", "${String.format(Locale.US, "%.1f", latestLog.memoryUsageMb)} MB")
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                MetricChip("GPU 로드", "${String.format(Locale.US, "%.1f", latestLog.gpuUtilizationPercent)} %")
                                MetricChip("VRAM 할당", "${String.format(Locale.US, "%.1f", latestLog.vramUsageMb)} MB")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = "JSON Raw Output (${logs.size} 레코드):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Slate700)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Raw JSON display box
                val formattedJson = if (latestLog != null) {
                    latestLog.toFormattedJsonString()
                } else {
                    "{\n  \"message\": \"기록된 프로파일링 데이터가 없습니다\"\n}"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Slate900,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                ) {
                    Text(
                        text = formattedJson,
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Slate100
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullJson = if (latestLog != null) latestLog.toFormattedJsonString() else "{}"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Profiling Log JSON", fullJson)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "JSON 로그가 복사되었습니다", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("JSON 복사")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onClearLogs,
                colors = ButtonDefaults.textButtonColors(contentColor = Rose500)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("로그 비우기")
            }
        }
    )
}

@Composable
private fun MetricChip(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = Slate500))
        Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Slate800))
    }
}
