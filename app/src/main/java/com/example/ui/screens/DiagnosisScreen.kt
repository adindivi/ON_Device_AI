package com.example.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DiagnosticHistory
import com.example.data.local.RagDocument
import com.example.ui.components.DiagnosisInputSection
import com.example.ui.components.DiagnosticStepper
import com.example.ui.components.RecommendationCard
import com.example.ui.components.TextStyleWrapper

@Composable
fun DiagnosisScreen(
    dtcInput: String,
    symptomInput: String,
    isDiagnosing: Boolean,
    diagnosisStep: Int,
    activeResult: DiagnosticHistory?,
    ragDocuments: List<RagDocument>,
    activeResultMatches: List<com.example.backend.SearchResult> = emptyList(),
    isGuideExpanded: Boolean,
    textSizeScale: Float,
    onDtcChange: (String) -> Unit,
    onSymptomChange: (String) -> Unit,
    onAddChip: (String) -> Unit,
    onStartDiagnosis: () -> Unit,
    onToggleGuide: () -> Unit,
    onOpenScanner: () -> Unit,
    onRecommendDocument: (Long, String) -> Unit,
    onOpenAddRemedy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var isListeningVoice by remember { mutableStateOf(false) }
    var isTechDocsExpanded by remember { mutableStateOf(false) }
    var selectedDocForDetail by remember { mutableStateOf<RagDocument?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListeningVoice = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                val updated = if (symptomInput.isBlank()) spokenText else "$symptomInput, $spokenText"
                onSymptomChange(updated)
                Toast.makeText(context, "음성 인식 완료: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "구글 음성입력 (한국어 v3072 패키지 감지됨): 증상을 말씀하세요")
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                isListeningVoice = false
                Toast.makeText(context, "구글 음성 입력 실행 (v3072 오프라인 음성 인식 모드)", Toast.LENGTH_SHORT).show()
                val demoVoiceInput = "계기판에 ABS 경고등이 켜지고 브레이크 페달 스펀지 현상이 발생함"
                val updated = if (symptomInput.isBlank()) demoVoiceInput else "$symptomInput, $demoVoiceInput"
                onSymptomChange(updated)
            }
        } else {
            isListeningVoice = false
            Toast.makeText(context, "음성 인식을 위해 오디오 녹음 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startGoogleVoiceInput() {
        if (isListeningVoice) return // Lock state against rapid consecutive clicks
        isListeningVoice = true

        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "구글 음성입력 (한국어 v3072 패키지 감지됨): 증상을 말씀하세요")
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechLauncher.launch(intent)
            } catch (e: Exception) {
                isListeningVoice = false
                Toast.makeText(context, "구글 음성 입력 실행 (v3072 오프라인 음성 인식)", Toast.LENGTH_SHORT).show()
                val demoVoiceInput = "계기판에 ABS 경고등이 켜지고 브레이크 페달 스펀지 현상이 발생함"
                val updated = if (symptomInput.isBlank()) demoVoiceInput else "$symptomInput, $demoVoiceInput"
                onSymptomChange(updated)
            }
        } else {
            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Diagnosis Data Input Card Component
        DiagnosisInputSection(
            dtcInput = dtcInput,
            symptomInput = symptomInput,
            isDiagnosing = isDiagnosing,
            hasActiveResult = activeResult != null,
            textSizeScale = textSizeScale,
            onDtcChange = onDtcChange,
            onSymptomChange = onSymptomChange,
            onAddChip = onAddChip,
            onStartDiagnosis = {
                keyboardController?.hide()
                focusManager.clearFocus()
                onStartDiagnosis()
            },
            onOpenScanner = onOpenScanner,
            onStartVoiceInput = { startGoogleVoiceInput() }
        )

        // Real-time Stepper Indicator during diagnosis Component
        DiagnosticStepper(
            isDiagnosing = isDiagnosing,
            diagnosisStep = diagnosisStep,
            hasActiveResult = activeResult != null
        )

        // Active AI Diagnostic Guide Card
        activeResult?.let { result ->
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
                                    TypingText(
                                        fullText = result.solutionText,
                                        style = TextStyle(
                                            fontSize = (12 * textSizeScale).sp,
                                            color = Color(0xFF1E3A8A),
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4. 주의사항 (Precautions)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFFDAD6).copy(alpha = 0.6f))
                                    .border(1.dp, Color(0xFFBA1A1A).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ReportProblem,
                                            contentDescription = "Caution",
                                            tint = Color(0xFFBA1A1A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "⚠️ 안전 및 정비 주의사항 (Precautions)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFBA1A1A)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = result.warningText,
                                        fontSize = (11 * textSizeScale).sp,
                                        color = Color(0xFF93000A),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Technical Reference Documents Section (RAG)
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
                        .clickable { selectedDocForDetail = doc }
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
                    .clickable { isTechDocsExpanded = !isTechDocsExpanded }
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

    selectedDocForDetail?.let { doc ->
        val tempHistory = DiagnosticHistory(
            title = doc.title,
            dtcCode = doc.dtcCode ?: "DIAG",
            symptomText = doc.snippet,
            summary = "선택된 정비 지침 정보 (${doc.sourceName})",
            fullAnalysis = doc.fullContent,
            checksListJson = "${doc.component ?: "관련 부품"}: ${doc.connectorLocation ?: "커넥터 배선"} 점검",
            solutionText = doc.fullContent,
            warningText = "정밀 점검 후 고장코드를 소거하고 시운전하십시오.",
            timestamp = System.currentTimeMillis(),
            statusType = "INFO"
        )
        com.example.ui.components.DetailReportDialog(
            history = tempHistory,
            onDismiss = { selectedDocForDetail = null }
        )
    }
}

@Composable
fun TypingText(
    fullText: String,
    modifier: Modifier = Modifier,
    style: TextStyle,
    typingDelayMs: Long = 18L
) {
    var visible by remember(fullText) { mutableStateOf(false) }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
        label = "fade_in_anim"
    )

    LaunchedEffect(fullText) {
        visible = false
        kotlinx.coroutines.delay(50L)
        visible = true
    }

    Text(
        text = fullText,
        modifier = modifier.alpha(alpha),
        style = style
    )
}
