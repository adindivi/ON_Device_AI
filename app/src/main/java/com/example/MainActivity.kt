package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.ui.CarDiagApp
import com.example.ui.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.CarDiagViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: CarDiagViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    // 스플래시 화면: 아이콘 중앙 + 하단 브랜드 이미지
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    // 메인 앱
                    CarDiagApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        var symptom = intent?.getStringExtra("symptom")
        val symptomB64 = intent?.getStringExtra("symptom_b64")
        if (!symptomB64.isNullOrBlank()) {
            try {
                val bytes = android.util.Base64.decode(symptomB64, android.util.Base64.DEFAULT)
                symptom = String(bytes, Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        android.util.Log.i("MainActivity", "handleIntent: symptom='$symptom', autoStart=${intent?.getBooleanExtra("auto_start", false)}")
        if (!symptom.isNullOrBlank()) {
            viewModel.updateSymptomInput(symptom)
        }
        val dtc = intent?.getStringExtra("dtc")
        if (!dtc.isNullOrBlank()) {
            viewModel.updateDtcInput(dtc)
        }
        if (intent?.getBooleanExtra("auto_start", false) == true) {
            viewModel.startDiagnosis()
        }
    }
}
