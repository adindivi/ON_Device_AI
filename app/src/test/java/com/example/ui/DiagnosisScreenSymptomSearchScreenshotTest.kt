package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.backend.SearchResult
import com.example.backend.VectorDbMetadata
import com.example.data.local.DiagnosticHistory
import com.example.data.local.RagDocument
import com.example.ui.components.HeaderBar
import com.example.ui.screens.DiagnosisScreen
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * [DiagnosisScreenSymptomSearchScreenshotTest]
 * 증상 검색 및 AI 진단 결과 표출 딥검증 스크린샷 테스트
 *
 * 1. 스마트폰 화면: "C120601 시동 지연, 계기판 경고등 점등" 복합 검색 결과
 * 2. Samsung DeX 화면: 1280x800dp 2분할(42:58) 대화면에서의 정비 진단서 및 기술문서 레이아웃
 * 3. 갤럭시 S24 화면: "에어컨 냉방 불량, 송풍팬 소음" 순수 자연어 증상 검색 결과
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DiagnosisScreenSymptomSearchScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleHistory = DiagnosticHistory(
        id = 1L,
        title = "DTC C120601 코드 및 시동 지연 증상 AI 분석",
        dtcCode = "C120601",
        symptomText = "시동 지연, 계기판 경고등 점등",
        summary = "Top 3 추천 원인: 1순위: 휠속도센서 | 2순위: ABS 모듈 | 3순위: 배터리 단자",
        fullAnalysis = "스마트 정비 진단서 내용",
        checksListJson = "1순위(휠속도센서): 뒤좌측 배선 및 핀 텐션 점검|2순위(ABS 제어기): 전원 전압 및 접지선 단선 점검|3순위(배터리 전원): 충전 상태 및 암전류 누설 점검",
        solutionText = "⚡ [온디바이스 RAG 진단] Ko-SBERT 768차원 벡터 검색 기반 핵심 점검 가이드입니다.\n\n" +
                "• 1순위 추천 정비: [휠속도센서] 위치: 뒤좌측 휠 허브 부근\n" +
                "• 권장 점검: 커넥터 단자 핀 부식 및 하네스 단선 여부를 멀티미터로 측정하세요.",
        warningText = "DTC C120601 고장 방치 시 연관 ESC 제어기 및 차체 구동계 2차 손상의 원인이 됩니다.",
        timestamp = System.currentTimeMillis(),
        statusType = "WARNING"
    )

    private val sampleMatches = listOf(
        SearchResult(
            id = "DOC-C120601",
            text = "[증상] 브레이크 경고등 점등 및 시동 지연 [조치] 뒤좌측 휠속도센서 배선 점검",
            score = 10.5f,
            recommendations = 42,
            metadata = VectorDbMetadata(
                dtcCode = "C120601",
                component = "뒤좌측 휠속도센서",
                connectorLocation = "뒤좌측 휠 허브 하네스 커넥터",
                dtcs = listOf("C120601")
            ),
            confidencePercent = 98.8f
        ),
        SearchResult(
            id = "DOC-ABS-02",
            text = "[증상] ABS 경고등 및 시동 시 전압 강하 [조치] ABS 모듈 전원단 퓨즈 점검",
            score = 7.2f,
            recommendations = 18,
            metadata = VectorDbMetadata(
                dtcCode = "C120602",
                component = "ABS 모듈",
                connectorLocation = "엔진룸 메인 정션박스",
                dtcs = listOf("C120602")
            ),
            confidencePercent = 88.5f
        ),
        SearchResult(
            id = "DOC-BATT-01",
            text = "[증상] 배터리 충전 불량 및 시동 지연 [조치] 알터네이터 B+ 단자 전압 측정",
            score = 5.1f,
            recommendations = 25,
            metadata = VectorDbMetadata(
                dtcCode = "",
                component = "배터리 충전 시스템",
                connectorLocation = "배터리 음극 터미널 센서",
                dtcs = emptyList()
            ),
            confidencePercent = 78.0f
        )
    )

    private val sampleRagDocs = listOf(
        RagDocument(
            id = 101L,
            docCode = "C120601",
            category = "정비 지침서",
            title = "뒤좌측 휠속도센서 커넥터 핀 점검 지침",
            snippet = "1. 점화 스위치 OFF 후 센서 커넥터를 분리합니다. 2. 내부 핀의 부식 및 변형 점검.",
            fullContent = "1. 점화 스위치 OFF 후 센서 커넥터를 분리합니다.\n2. 내부 핀의 부식 및 변형 여부를 점검합니다.\n3. 규격 저항값(1.0~1.3 kΩ)을 확인합니다.",
            dtcCode = "C120601",
            component = "뒤좌측 휠속도센서",
            connectorLocation = "뒤좌측 휠 허브 하네스",
            recommendationCount = 42
        ),
        RagDocument(
            id = 102L,
            docCode = "C120602",
            category = "회로 점검",
            title = "ABS ECU 전원 회로 단선/단락 점검 절차",
            snippet = "1. 메인 퓨즈(40A) 도통 상태를 확인합니다. 2. 제어기 입력 전압 측정.",
            fullContent = "1. 메인 퓨즈(40A) 도통 상태를 확인합니다.\n2. 제어기 입력 전원 전압이 12V 이상인지 측정합니다.",
            dtcCode = "C120602",
            component = "ABS 모듈",
            connectorLocation = "엔진룸 메인 퓨즈박스",
            recommendationCount = 18
        )
    )

    @Test
    @Config(qualifiers = "w390dp-h1200dp", sdk = [36])
    fun captureSymptomSearch_Phone() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HeaderBar(
                            onTextDecrease = {},
                            onTextIncrease = {},
                            onOpenContribution = {}
                        )
                        DiagnosisScreen(
                            dtcInput = "C120601",
                            symptomInput = "시동 지연, 계기판 경고등 점등",
                            isDiagnosing = false,
                            diagnosisStep = 4,
                            activeResult = sampleHistory,
                            ragDocuments = sampleRagDocs,
                            activeResultMatches = sampleMatches,
                            isGuideExpanded = true,
                            textSizeScale = 1.0f,
                            isExpandedScreen = false,
                            modifier = Modifier.weight(1f),
                            onDtcChange = {},
                            onSymptomChange = {},
                            onAddChip = {},
                            onStartDiagnosis = {},
                            onToggleGuide = {},
                            onOpenScanner = {},
                            onRecommendDocument = { _, _ -> },
                            onOpenAddRemedy = {},
                            onOpenRgatAnalysis = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/symptom_search_diagnosis_phone.png"
        )
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land", sdk = [36])
    fun captureSymptomSearch_DeX() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HeaderBar(
                            onTextDecrease = {},
                            onTextIncrease = {},
                            onOpenContribution = {}
                        )
                        DiagnosisScreen(
                            dtcInput = "C120601",
                            symptomInput = "시동 지연, 계기판 경고등 점등",
                            isDiagnosing = false,
                            diagnosisStep = 4,
                            activeResult = sampleHistory,
                            ragDocuments = sampleRagDocs,
                            activeResultMatches = sampleMatches,
                            isGuideExpanded = true,
                            textSizeScale = 1.0f,
                            isExpandedScreen = true,
                            modifier = Modifier.weight(1f),
                            onDtcChange = {},
                            onSymptomChange = {},
                            onAddChip = {},
                            onStartDiagnosis = {},
                            onToggleGuide = {},
                            onOpenScanner = {},
                            onRecommendDocument = { _, _ -> },
                            onOpenAddRemedy = {},
                            onOpenRgatAnalysis = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/symptom_search_diagnosis_dex.png"
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h1000dp", sdk = [36])
    fun capturePureSymptomSearch_GalaxyS24() {
        val pureSymptomHistory = DiagnosticHistory(
            id = 2L,
            title = "차량 증상 AI 점검 결과",
            dtcCode = "DIAG",
            symptomText = "에어컨 냉방 불량, 송풍팬 소음",
            summary = "Top 3 추천 원인: 1순위: 에어컨 블로워 모터 | 2순위: 냉매 압력 센서 | 3순위: 컴프레서 마그네틱 클러치",
            fullAnalysis = "자연어 질의 기반 AI 솔루션",
            checksListJson = "1순위(블로워 모터): 브러시 마모 및 내부 이물질 확인|2순위(냉매 압력): 저압/고압 게이지 압력 측정|3순위(컴프레서): 마그네틱 클러치 간극 및 릴레이 점검",
            solutionText = "⚡ [온디바이스 RAG 자연어 진단] 입력된 증상 텍스트를 기반으로 도출된 핵심 점검 가이드입니다.\n\n" +
                    "• 1순위 추천 정비: [에어컨 블로워 모터] 위치: 글로브 박스 하단\n" +
                    "• 권장 점검: 블로워 레지스터 및 모터 회전 저항을 확인하세요.",
            warningText = "블로워 모터 과부하 방치 시 공조 회로 화재 및 퓨즈 단락의 원인이 됩니다.",
            timestamp = System.currentTimeMillis(),
            statusType = "WARNING"
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        HeaderBar(
                            onTextDecrease = {},
                            onTextIncrease = {},
                            onOpenContribution = {}
                        )
                        DiagnosisScreen(
                            dtcInput = "",
                            symptomInput = "에어컨 냉방 불량, 송풍팬 소음",
                            isDiagnosing = false,
                            diagnosisStep = 4,
                            activeResult = pureSymptomHistory,
                            ragDocuments = sampleRagDocs,
                            activeResultMatches = sampleMatches,
                            isGuideExpanded = true,
                            textSizeScale = 1.0f,
                            isExpandedScreen = false,
                            modifier = Modifier.weight(1f),
                            onDtcChange = {},
                            onSymptomChange = {},
                            onAddChip = {},
                            onStartDiagnosis = {},
                            onToggleGuide = {},
                            onOpenScanner = {},
                            onRecommendDocument = { _, _ -> },
                            onOpenAddRemedy = {},
                            onOpenRgatAnalysis = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/symptom_search_pure_symptom.png"
        )
    }
}
