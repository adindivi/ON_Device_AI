package com.example.backend

import android.util.Log

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

    private external fun nativeInitModel(modelPath: String): Boolean
    private external fun nativeGenerateResponse(promptText: String, maxTokens: Int): String
    private external fun nativeReleaseModel()

    fun initModel(modelPath: String): Boolean {
        return try {
            nativeInitModel(modelPath)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing native LLM model: ${e.message}")
            false
        }
    }

    fun generateResponse(prompt: String, maxTokens: Int = 512): String {
        return try {
            nativeGenerateResponse(prompt, maxTokens)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating LLM response: ${e.message}")
            "온디바이스 LLM 추론 중 예외가 발생했습니다."
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
