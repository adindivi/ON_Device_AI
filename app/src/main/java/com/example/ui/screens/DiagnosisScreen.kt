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
                Toast.makeText(context, "음성 입력 완료", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isListeningVoice = true
        } else {
            isListeningVoice = false
            Toast.makeText(context, "마이크 권한 필요", Toast.LENGTH_SHORT).show()
        }
    }

    fun startGoogleVoiceInput() {
        val hasAudioPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            isListeningVoice = true
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
            com.example.ui.components.ActiveDiagnosticGuideCard(
                result = result,
                activeResultMatches = activeResultMatches,
                isGuideExpanded = isGuideExpanded,
                isDiagnosing = isDiagnosing,
                textSizeScale = textSizeScale,
                onToggleGuide = onToggleGuide
            )
        }

        // Technical Reference Documents Section (RAG)
        com.example.ui.components.TechnicalDocumentsSection(
            isTechDocsExpanded = isTechDocsExpanded,
            ragDocuments = ragDocuments,
            activeResultMatches = activeResultMatches,
            textSizeScale = textSizeScale,
            onToggleTechDocs = { isTechDocsExpanded = !isTechDocsExpanded },
            onOpenAddRemedy = onOpenAddRemedy,
            onDocumentSelect = { doc -> selectedDocForDetail = doc },
            onRecommendDocument = onRecommendDocument
        )
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

    if (isListeningVoice) {
        com.example.ui.components.VoiceRecognitionBottomSheet(
            onDismiss = { isListeningVoice = false },
            onResult = { spokenText ->
                isListeningVoice = false
                if (spokenText.isNotBlank()) {
                    val updated = if (symptomInput.isBlank()) spokenText else "$symptomInput, $spokenText"
                    onSymptomChange(updated)
                    Toast.makeText(context, "음성 입력 반영됨", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

