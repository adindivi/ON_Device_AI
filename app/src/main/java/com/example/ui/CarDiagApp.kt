package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AddRemedyDialog
import com.example.ui.components.ContributionDashboardDialog
import com.example.ui.components.DetailReportDialog
import com.example.ui.components.EditRemedyDialog
import com.example.ui.components.ErrorAlertDialog
import com.example.ui.components.HeaderBar
import com.example.ui.components.PasswordVerificationDialog
import com.example.ui.components.RootCauseAnalysisDialog
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

// HTML 디자인 기준: 진단 / 데이터 관리 두 탭 (아이콘 및 테스트 태그 캡슐화)
enum class AppTab(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    DIAGNOSIS("진단", Icons.AutoMirrored.Filled.Assignment, "tab_diagnosis"),
    DATA_MANAGEMENT("데이터 관리", Icons.Default.Storage, "tab_data_management")
}

/**
 * [CarDiagApp]
 * 차량 진단 앱 루트 컴포저블
 *
 * - Root Recomposition 최적화: 최상위 Scaffold에서 미사용 StateFlow(vehicle, historyList) 수집을 전면 제거하고,
 *   각 탭 화면에 필요한 StateFlow를 하위 컴포저블(DiagnosisTabContent, DataManagementTabContent)로 격리 스코핑.
 * - 텍스트 입력 및 진단 상태 변경 시 최상위 Scaffold, HeaderBar, NavigationBar의 불필요한 리컴포지션을 원천 방지.
 */
@Composable
fun CarDiagApp(viewModel: CarDiagViewModel) {
    var selectedTab by remember { mutableStateOf(AppTab.DIAGNOSIS) }

    val configuration = LocalConfiguration.current
    val isExpandedScreen = configuration.screenWidthDp >= 600

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
            if (!isExpandedScreen) {
                AppBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
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
            if (isExpandedScreen) {
                // DeX / 가로 대화면: 좌측 NavigationRail 사이드바 + 우측 메인 영역
                Row(modifier = Modifier.fillMaxSize()) {
                    AppNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (selectedTab) {
                            AppTab.DIAGNOSIS -> DiagnosisTabContent(
                                viewModel = viewModel,
                                isExpandedScreen = true
                            )
                            AppTab.DATA_MANAGEMENT -> DataManagementTabContent(
                                viewModel = viewModel
                            )
                        }
                    }
                }
            } else {
                when (selectedTab) {
                    AppTab.DIAGNOSIS -> DiagnosisTabContent(
                        viewModel = viewModel,
                        isExpandedScreen = false
                    )
                    AppTab.DATA_MANAGEMENT -> DataManagementTabContent(
                        viewModel = viewModel
                    )
                }
            }

            // ── Dialog Overlays (단일 책임 격리 컴포넌트) ────────────────────────
            CarDiagDialogOverlays(viewModel = viewModel)
        }
    }
}

/**
 * [DiagnosisTabContent]
 * 진단 탭 전용 상태 구독 및 화면 매핑 컴포저블 (Root Recomposition 격리)
 */
@Composable
private fun DiagnosisTabContent(
    viewModel: CarDiagViewModel,
    isExpandedScreen: Boolean
) {
    val dtcInput by viewModel.dtcInput.collectAsStateWithLifecycle()
    val symptomInput by viewModel.symptomInput.collectAsStateWithLifecycle()
    val isDiagnosing by viewModel.isDiagnosing.collectAsStateWithLifecycle()
    val diagnosisStep by viewModel.diagnosisStep.collectAsStateWithLifecycle()
    val activeResult by viewModel.activeResult.collectAsStateWithLifecycle()
    val activeResultMatches by viewModel.activeResultMatches.collectAsStateWithLifecycle()
    val isGuideExpanded by viewModel.isGuideExpanded.collectAsStateWithLifecycle()
    val ragDocuments by viewModel.ragDocuments.collectAsStateWithLifecycle()
    val textSizeScale by viewModel.textSizeScale.collectAsStateWithLifecycle()

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
        isExpandedScreen = isExpandedScreen,
        onDtcChange = { viewModel.updateDtcInput(it) },
        onSymptomChange = { viewModel.updateSymptomInput(it) },
        onAddChip = { viewModel.addSymptomChip(it) },
        onStartDiagnosis = { viewModel.startDiagnosis() },
        onToggleGuide = { viewModel.toggleGuideExpanded() },
        onOpenScanner = { viewModel.setScannerModalVisible(true) },
        onRecommendDocument = { id, code -> viewModel.incrementRecommendation(id, code) },
        onOpenAddRemedy = { viewModel.setAddRemedyModalVisible(true) },
        onOpenRgatAnalysis = { codes -> viewModel.runRgatAnalysis(codes) }
    )
}

/**
 * [DataManagementTabContent]
 * 데이터 관리 탭 전용 상태 구독 및 화면 매핑 컴포저블 (Root Recomposition 격리)
 */
@Composable
private fun DataManagementTabContent(
    viewModel: CarDiagViewModel
) {
    val userManualDocuments by viewModel.userManualDocuments.collectAsStateWithLifecycle()
    val backendDbStatus by viewModel.backendDbStatus.collectAsStateWithLifecycle()
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

/**
 * [AppBottomNavigationBar]
 * 스마트폰 세로 모드 전용 토스 스타일 단색 하단 네비게이션 바
 */
@Composable
private fun AppBottomNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .background(Color.White),
        containerColor = Color.White,
        contentColor = TossBlack,
        tonalElevation = 0.dp
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TossBlack,
                    selectedTextColor = TossBlack,
                    unselectedIconColor = TossGray400,
                    unselectedTextColor = TossGray400,
                    indicatorColor = TossGray100
                ),
                modifier = Modifier.testTag(tab.testTag)
            )
        }
    }
}

/**
 * [AppNavigationRail]
 * Samsung DeX 및 가로 대화면(>=600dp) 전용 토스 스타일 좌측 네비게이션 레일 사이드바
 */
@Composable
private fun AppNavigationRail(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.White),
        containerColor = Color.White,
        contentColor = TossBlack
    ) {
        AppTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = TossBlack,
                    selectedTextColor = TossBlack,
                    unselectedIconColor = TossGray400,
                    unselectedTextColor = TossGray400,
                    indicatorColor = TossGray100
                ),
                modifier = Modifier.testTag("${tab.testTag}_rail")
            )
        }
    }
}

/**
 * [CarDiagDialogOverlays]
 * 앱 전역 모달 다이얼로그 및 토스트를 캡슐화한 오버레이 컴포넌트
 */
@Composable
private fun CarDiagDialogOverlays(viewModel: CarDiagViewModel) {
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
    val showRgatModal by viewModel.showRgatModal.collectAsStateWithLifecycle()
    val rgatAnalysisResult by viewModel.rgatAnalysisResult.collectAsStateWithLifecycle()
    val userManualDocuments by viewModel.userManualDocuments.collectAsStateWithLifecycle()

    if (showScannerModal) {
        ScannerOcrDialog(
            onDismiss = { viewModel.setScannerModalVisible(false) },
            onSelectCode = { viewModel.applyOcrCode(it) },
            onAnalyzeRgat = { codes ->
                viewModel.setScannerModalVisible(false)
                viewModel.runRgatAnalysis(codes)
            }
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

    val currentRgatResult = rgatAnalysisResult
    if (showRgatModal && currentRgatResult != null) {
        RootCauseAnalysisDialog(
            result = currentRgatResult,
            onDismiss = { viewModel.closeRgatModal() }
        )
    }

    TopFloatingToast(
        message = toastMessage,
        onDismiss = { viewModel.clearToast() }
    )
}
