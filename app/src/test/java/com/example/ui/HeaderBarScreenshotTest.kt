package com.example.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.HeaderBar
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeaderBarScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h780dp", sdk = [36])
    fun captureHeaderBar_GalaxyS24() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HeaderBar(
                    onTextDecrease = {},
                    onTextIncrease = {},
                    onOpenContribution = {},
                    onAdminLongPress = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/header_bar_galaxy_s24.png"
        )
    }

    @Test
    @Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
    fun captureHeaderBar_Phone() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HeaderBar(
                    onTextDecrease = {},
                    onTextIncrease = {},
                    onOpenContribution = {},
                    onAdminLongPress = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/header_bar_phone.png"
        )
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land", sdk = [36])
    fun captureHeaderBar_DeX() {
        composeTestRule.setContent {
            MyApplicationTheme {
                HeaderBar(
                    onTextDecrease = {},
                    onTextIncrease = {},
                    onOpenContribution = {},
                    onAdminLongPress = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(
            filePath = "src/test/screenshots/header_bar_dex.png"
        )
    }
}
