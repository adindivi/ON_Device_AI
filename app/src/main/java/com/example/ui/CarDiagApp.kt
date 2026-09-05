package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddRemedyDialog
import com.example.ui.components.ContributionDashboardDialog
import com.example.ui.components.DetailReportDialog
import com.example.ui.components.EditRemedyDialog
import com.example.ui.components.ErrorAlertDialog
import com.example.ui.components.HeaderBar
import com.example.ui.components.PasswordVerificationDialog
import com.example.ui.components.ScannerOcrDialog
import com.example.ui.components.TopFloatingToast
import com.example.ui.components.WeightSettingsDialog
import com.example.ui.screens.ContributionSettingsScreen
import com.example.ui.screens.DiagnosisScreen
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossGray100
import com.example.ui.theme.TossGray400
import com.example.ui.theme.TossWhite
import com.example.ui.viewmodel.CarDiagViewModel

// HTML 디자인 기준: 진단 / 데이터 관리 두 탭
enum class AppTab(val title: String) {
    DIAGNOSIS("진단"),
    DATA_MANAGEMENT("데이터 관리")
}

@Composable
fun CarDiagApp(viewModel: CarDiagViewModel) {
    var selectedTab by remember { mutableStateOf(AppTab.DIAGNOSIS) }

    val vehicle by viewModel.vehicle.collectAsStateWithLifecycle()
    val textSizeScale by viewModel.textSizeScale.collectAsStateWithLifecycle()

    val dtcInput by viewModel.dtcInput.collectAsStateWithLifecycle()
    val symptomInput by viewModel.symptomInput.collectAsStateWithLifecycle()
    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val diagnosisStep by viewModel.diagnosisStep.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResult.collectAsStateWithLifecycle()
    val activeResultMatches by viewModel.activeResultMatches.collectAsStateWithLifecycle()
    val isGuideExpanded by viewModel.isGuideExpanded.collectAsStateWithLifecycle()

    val historyList by viewModel.historyList.collectAsStateWithLifecycle()
    val ragDocuments by viewModel.ragDocuments.collectAsStateWithLifecycle()
    val userManualDocuments by viewModel.userManualDocuments.collectAsStateWithLifecycle()
    val backendDbStatus by viewModel.backendDbStatus.collectAsStateWithLifecycle()

    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    val showScannerModal by viewModel.showScannerModal.collectAsStateWithLifecycle()
    val showAddRemedyModal by viewModel.showAddRemedyModal.collectAsStateWithLifecycle()
    val showContributionDashboardModal by viewModel.showContributionDashboardModal.collectAsStateWithLifecycle()
    val editingDocument by viewModel.editingDocument.collectAsStateWithLifecycle()
    val selectedDetailHistory by viewModel.selectedDetailHistory.collectAsStateWithLifecycle()

    val showPasswordModal by viewModel.showPasswordModal.collectAsStateWithLifecycle()
    val showWeightSettingsModal by viewModel.showWeightSettingsModal.collectAsStateWithLifecycle()
    val scoringWeights by viewModel.scoringWeights.collectAsStateWithLifecycle()
    val errorDialogState by viewModel.errorDialogState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HeaderBar(
                onTextDecrease = { viewModel.changeTextSize(-0.1f) },
                onTextIncrease = { viewModel.changeTextSize(0.1f) },
                onOpenContribution = { viewModel.setContributionDashboardModalVisible(true) },
                onAdminLongPress = { viewModel.openPasswordModal() }
            )
        },
        bottomBar = {
            // ── Toss-style B&W Bottom Navigation ─────────────────────────────
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_navigation_bar"),
                containerColor = TossWhite,
                tonalElevation = 0.dp
            ) {
                // 진단 탭
                NavigationBarItem(
                    selected = selectedTab == AppTab.DIAGNOSIS,
                    onClick = { selectedTab = AppTab.DIAGNOSIS },
                    icon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Assignment,
                            contentDescription = "Diagnosis"
                        )
                    },
                    label = {
                        Text(
                            "진단",
                            fontSize = 10.sp,
                            fontWeight = if (selectedTab == AppTab.DIAGNOSIS) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TossBlack,
                        selectedTextColor = TossBlack,
                        unselectedIconColor = TossGray400,
                        unselectedTextColor = TossGray400,
                        indicatorColor = TossGray100
                    ),
                    modifier = Modifier.testTag("tab_diagnosis")
                )

                // 데이터 관리 탭 (기존 기록+설정 통합)
                NavigationBarItem(
                    selected = selectedTab == AppTab.DATA_MANAGEMENT,
                    onClick = { selectedTab = AppTab.DATA_MANAGEMENT },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Data Management"
                        )
                    },
                    label = {
                        Text(
                            "데이터 관리",
                            fontSize = 10.sp,
                            fontWeight = if (selectedTab == AppTab.DATA_MANAGEMENT) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TossBlack,
                        selectedTextColor = TossBlack,
                        unselectedIconColor = TossGray400,
                        unselectedTextColor = TossGray400,
                        indicatorColor = TossGray100
                    ),
                    modifier = Modifier.testTag("tab_data_management")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TossWhite)
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.DIAGNOSIS -> {
                    DiagnosisScreen(
                        dtcInput = dtcInput,
                        symptomInput = symptomInput,
                        isDiagnosing = isDiagnosing,
                        diagnosisStep = diagnosisStep,
                        activeResult = activeResult,
                        ragDocuments = ragDocuments,
                        activeResultMatches = activeResultMatches,
                        isGuideExpanded = isGuideExpanded,
                        textSizeScale = textSizeScale,
                        onDtcChange = { viewModel.updateDtcInput(it) },
                        onSymptomChange = { viewModel.updateSymptomInput(it) },
                        onAddChip = { viewModel.addSymptomChip(it) },
                        onStartDiagnosis = { viewModel.startDiagnosis() },
                        onToggleGuide = { viewModel.toggleGuideExpanded() },
                        onOpenScanner = { viewModel.setScannerModalVisible(true) },
                        onRecommendDocument = { id, code -> viewModel.incrementRecommendation(id, code) },
                        onOpenAddRemedy = { viewModel.setAddRemedyModalVisible(true) }
                    )
                }

                // 데이터 관리: 기록 + 설정을 모두 포함 (ContributionSettingsScreen 사용)
                AppTab.DATA_MANAGEMENT -> {
                    val isQwenLoading by viewModel.isQwenLoading.collectAsStateWithLifecycle()
                    val isQwenAnswerEnabled by viewModel.isQwenAnswerEnabled.collectAsStateWithLifecycle()
                    ContributionSettingsScreen(
                        userDocuments = userManualDocuments,
                        backendStatus = backendDbStatus,
                        isQwenLoading = isQwenLoading,
                        isQwenAnswerEnabled = isQwenAnswerEnabled,
                        onToggleQwenAnswer = { viewModel.toggleQwenAnswer(it) },
                        onOpenAddRemedy = { viewModel.setAddRemedyModalVisible(true) },
                        onEditDocument = { viewModel.setEditingDocument(it) },
                        onDeleteDocument = { viewModel.deleteRemedy(it) },
                        onRefreshStatus = { viewModel.refreshBackendStatus() },
                        onQwenModelSelected = { viewModel.onQwenModelSelected(it) }
                    )
                }
            }

            // ── Dialog Overlays ───────────────────────────────────────────────
            if (showScannerModal) {
                ScannerOcrDialog(
                    onDismiss = { viewModel.setScannerModalVisible(false) },
                    onSelectCode = { viewModel.applyOcrCode(it) }
                )
            }

            if (showAddRemedyModal) {
                AddRemedyDialog(
                    onDismiss = { viewModel.setAddRemedyModalVisible(false) },
                    onAnalyzeMetadata = { viewModel.extractMetadata(it) },
                    onSaveRemedy = { text, dtc, comp, loc ->
                        viewModel.addNewRemedy(text, dtc, comp, loc)
                    }
                )
            }

            if (showContributionDashboardModal) {
                ContributionDashboardDialog(
                    userDocuments = userManualDocuments,
                    onDismiss = { viewModel.setContributionDashboardModalVisible(false) },
                    onOpenAddRemedy = { viewModel.setAddRemedyModalVisible(true) },
                    onEditDocument = { viewModel.setEditingDocument(it) },
                    onDeleteDocument = { viewModel.deleteRemedy(it) }
                )
            }

            editingDocument?.let { doc ->
                EditRemedyDialog(
                    doc = doc,
                    onDismiss = { viewModel.setEditingDocument(null) },
                    onSaveEdit = { viewModel.updateRemedy(it) }
                )
            }

            selectedDetailHistory?.let { detail ->
                DetailReportDialog(
                    history = detail,
                    onDismiss = { viewModel.setSelectedDetailHistory(null) }
                )
            }

            if (showPasswordModal) {
                PasswordVerificationDialog(
                    onDismiss = { viewModel.closePasswordModal() },
                    onVerify = { viewModel.verifyPassword(it) }
                )
            }

            if (showWeightSettingsModal) {
                WeightSettingsDialog(
                    initialWeights = scoringWeights,
                    onDismiss = { viewModel.closeWeightSettingsModal() },
                    onSave = { viewModel.updateScoringWeights(it) },
                    onReset = { viewModel.resetScoringWeights() }
                )
            }

            if (errorDialogState.isVisible) {
                ErrorAlertDialog(
                    state = errorDialogState,
                    onDismiss = { viewModel.dismissErrorDialog() },
                    onRetry = {
                        viewModel.dismissErrorDialog()
                        viewModel.startDiagnosis()
                    }
                )
            }

            // ── Toss-Style Top Floating Toast ───────────────────────────────
            TopFloatingToast(
                message = toastMessage,
                onDismiss = { viewModel.clearToast() }
            )
        }
    }
}
