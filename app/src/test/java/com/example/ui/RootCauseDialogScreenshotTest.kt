package com.example.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.backend.DtcRgatEngine
import com.example.ui.components.RootCauseAnalysisDialog
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class RootCauseDialogScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var analysisResult: DtcRgatEngine.RgatAnalysisResult

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DtcRgatEngine.getInstance(context)
        engine.initialize()
        analysisResult = engine.analyze(listOf("C181787", "C183186"))
    }

    @Test
    fun `captureTab0_Mindmap`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = analysisResult,
                    onDismiss = {},
                    initialTab = 0
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/tab_0_mindmap.png"
        )
    }

    @Test
    fun `captureTab1_Ranking`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = analysisResult,
                    onDismiss = {},
                    initialTab = 1
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/tab_1_ranking.png"
        )
    }

    @Test
    fun `captureTab2_DtcDetail`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = analysisResult,
                    onDismiss = {},
                    initialTab = 2
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/tab_2_dtc_detail.png"
        )
    }

    @Test
    fun `captureUserScenario_4DTCs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DtcRgatEngine.getInstance(context)
        val userResult = engine.analyze(listOf("B16C500", "B16C600", "B186D16", "B187C88"))

        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = userResult,
                    onDismiss = {},
                    initialTab = 0
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/user_scenario_4dtcs_mindmap.png"
        )
    }

    @Test
    fun `captureFullscreen_Mindmap`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DtcRgatEngine.getInstance(context)
        val userResult = engine.analyze(listOf("B16C500", "B16C600", "B186D16", "B187C88"))

        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = userResult,
                    onDismiss = {},
                    initialTab = 0,
                    initialFullscreenMindmap = true
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/mindmap_fullscreen_view.png"
        )
    }

    @Test
    fun `testFullscreen_NodeInspectionTooltip_andCapture`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = DtcRgatEngine.getInstance(context)
        val userResult = engine.analyze(listOf("B16C500", "B16C600", "B186D16", "B187C88"))

        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = userResult,
                    onDismiss = {},
                    initialTab = 0,
                    initialFullscreenMindmap = true
                )
            }
        }

        // 1위 추천 커넥터 노드 클릭
        composeTestRule.onNodeWithText("FRNT_MAIN11").performClick()
        composeTestRule.waitForIdle()

        // 툴팁 텍스트 노출 검증 (고유 텍스트 '연결 DTC')
        composeTestRule.onNodeWithText("연결 DTC:", substring = true).assertIsDisplayed()

        // 툴팁 스크린샷 캡처 및 검증
        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/mindmap_fullscreen_tooltip.png"
        )
    }

    @Test
    fun `testFullscreenTransition_toggleOpenAndClose`() {
        var dismissed = false
        composeTestRule.setContent {
            MyApplicationTheme {
                RootCauseAnalysisDialog(
                    result = analysisResult,
                    onDismiss = { dismissed = true },
                    initialTab = 0,
                    initialFullscreenMindmap = false
                )
            }
        }

        // 기본 다이얼로그 확인
        composeTestRule.onNodeWithText("동시 고장 배선 진단").assertIsDisplayed()

        // 전체화면 버튼 클릭 -> 전체화면 진입
        composeTestRule.onNodeWithContentDescription("전체화면").performClick()
        composeTestRule.onNodeWithText("배선 연결망 한눈에 보기").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("전체화면 닫기").assertIsDisplayed()

        // 상단 전체화면 닫기 버튼 클릭 -> 기본 다이얼로그 복귀
        composeTestRule.onNodeWithContentDescription("전체화면 닫기").performClick()
        composeTestRule.onNodeWithText("동시 고장 배선 진단").assertIsDisplayed()
    }
}
