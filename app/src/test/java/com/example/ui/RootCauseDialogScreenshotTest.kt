package com.example.ui

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
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
}
