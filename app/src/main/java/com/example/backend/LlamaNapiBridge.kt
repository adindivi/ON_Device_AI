package com.example.backend

import android.util.Log

interface LlamaCallback {
    fun onToken(token: String)
}

/**
 * Qwen2.5-1.5B 8-bit GGUF LLM Native C++ JNI Bridge
 */
class LlamaNapiBridge {

    companion object {
        private const val TAG = "LlamaNapiBridge"

        init {
            try {
                System.loadLibrary("ondevice_ai_engine")
                Log.i(TAG, "Native library 'ondevice_ai_engine' loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }
        }
    }

    external fun nativeInitModel(modelPath: String): Boolean
    external fun nativeGenerateResponseStream(promptText: String, maxTokens: Int, callback: LlamaCallback)
    external fun nativeReleaseModel()

    fun initModel(modelPath: String): Boolean {
        return try {
            nativeInitModel(modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing native LLM model: ${e.message}")
            false
        }
    }

    fun generateResponseStream(prompt: String, maxTokens: Int = 512, callback: LlamaCallback) {
        try {
            nativeGenerateResponseStream(prompt, maxTokens, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating LLM streaming response: ${e.message}")
            callback.onToken("\n[에러: 온디바이스 LLM 추론 중 예외가 발생했습니다.]")
        }
    }

    fun release() {
        try {
            nativeReleaseModel()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing LLM model: ${e.message}")
        }
    }
}
