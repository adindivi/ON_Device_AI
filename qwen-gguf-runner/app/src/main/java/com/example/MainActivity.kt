package com.example

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.ui.GradioChatList
import com.example.ui.GradioHeader
import com.example.ui.GradioInputRow
import com.example.ui.GradioModelBar
import com.example.ui.GradioSettingsAccordion
import com.example.ui.ProfilingJsonDialog
import com.example.ui.QwenViewModel
import com.example.ui.theme.HighDensityBg
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Rose500

class MainActivity : ComponentActivity() {

    private val viewModel: QwenViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val context = LocalContext.current

                // File picker launcher for .gguf files
                val ggufPickerLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    if (uri != null) {
                        var fileName = "model.gguf"
                        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                fileName = cursor.getString(nameIndex) ?: "model.gguf"
                            }
                        }
                        viewModel.importGgufUri(uri, fileName)
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .testTag("main_scaffold"),
                    bottomBar = {
                        GradioInputRow(
                            isStreaming = uiState.isStreaming,
                            onSendMessage = { prompt -> viewModel.sendMessage(prompt) },
                            onStopStreaming = { viewModel.stopStreaming() }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Top Gradio Header
                        GradioHeader(
                            uiState = uiState,
                            onOpenProfiling = { viewModel.setProfilingModalVisible(true) },
                            onToggleSettings = { viewModel.toggleSettingsAccordion() },
                            onClearChat = { viewModel.clearChat() }
                        )

                        // Model bar & Sandbox selector
                        GradioModelBar(
                            uiState = uiState,
                            onImportClick = { ggufPickerLauncher.launch("*/*") },
                            onSelectSandboxModel = { file -> viewModel.loadModelFromFile(file) },
                            onDeleteModel = { file -> viewModel.deleteSandboxModel(file) }
                        )

                        // Settings Accordion
                        GradioSettingsAccordion(
                            uiState = uiState,
                            onUpdateParameters = { temp, topP, maxTok, threads, backend, layers, sysPrompt ->
                                viewModel.updateParameters(temp, topP, maxTok, threads, backend, layers, sysPrompt)
                            }
                        )

                        // Chat History Area
                        GradioChatList(
                            messages = uiState.messages,
                            onSuggestionClick = { suggestion -> viewModel.sendMessage(suggestion) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Profiling JSON Modal
                    if (uiState.showProfilingModal) {
                        ProfilingJsonDialog(
                            logs = uiState.profilingLogs,
                            onDismiss = { viewModel.setProfilingModalVisible(false) },
                            onClearLogs = { viewModel.clearProfilingLogs() }
                        )
                    }

                    // Strict Failure Alert Dialog (No Fallback)
                    if (uiState.errorMessage != null && uiState.isErrorState) {
                        AlertDialog(
                            onDismissRequest = { viewModel.dismissErrorMessage() },
                            modifier = Modifier.testTag("dialog_error_failure"),
                            title = {
                                Text(text = "❌ 모델 로딩/추론 실패", color = Rose500)
                            },
                            text = {
                                Text(text = uiState.errorMessage ?: "실패: 처리를 완료할 수 없습니다 (폴백 없음)")
                            },
                            confirmButton = {
                                Button(
                                    onClick = { viewModel.dismissErrorMessage() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                                ) {
                                    Text("확인")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
