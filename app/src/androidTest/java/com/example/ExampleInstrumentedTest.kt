package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test executing on physical/virtual Android device.
 * Verifies target application context, package naming, and essential asset accessibility.
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun verifyAppContextAndPackageConfiguration() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertNotNull("Application target context must not be null", appContext)
        assertEquals(
            "Target application package must match production applicationId",
            "com.aistudio.cardiag.wppgso",
            appContext.packageName
        )
    }

    @Test
    fun verifyAssetManagerAccessibility() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = appContext.assets
        assertNotNull("Asset manager must be accessible for on-device models", assetManager)
        val assets = assetManager.list("") ?: emptyArray()
        assertTrue("Assets list should be queryable", assets.isNotEmpty())
    }
}

