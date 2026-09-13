package com.example.backend

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log

@RunWith(AndroidJUnit4::class)
class RootCauseTest {

    @Test
    fun testRootCauseGraphGeneration() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val engine = OnDeviceBackendEngine(context)

        // Ensure model is ready (might take a second to load)
        var attempts = 0
        while (!engine.qwenLlm.isReady && attempts < 10) {
            kotlinx.coroutines.delay(1000)
            attempts++
        }

        val query = "ISO-ROOT: P053001, C120601"
        Log.d("RootCauseTest", "Starting test with query: \$query")

        val flow = engine.diagnoseStream(query)
        val results = flow.toList()

        assertTrue("Should have emitted some results", results.isNotEmpty())
        
        val finalResult = results.last().qwenAnswer
        Log.d("RootCauseTest", "Final Result: \$finalResult")

        assertTrue("Result should start with [JSON_GRAPH_START]", finalResult.startsWith("[JSON_GRAPH_START]"))
    }
}
