package com.example.backend

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.Closeable
import java.io.File
import androidx.core.content.FileProvider

/**
 * Bridge to llamacpp-kotlin native inference engine.
 * Bypasses the restrictive LlamaHelper and directly controls LlamaAndroid
 * to apply custom parameters (temperature, max tokens, etc).
 */
class LlamaCppBridge(private val context: Context) : Closeable {

    private var llamaAndroidInstance: Any? = null
    private var currentContextId: Int? = null
    private var isModelLoaded: Boolean = false
    private var modelPath: String = ""

    val isLoaded: Boolean
        get() = isModelLoaded && currentContextId != null && llamaAndroidInstance != null
    
    // Callback to receive JSON events from native
    private var nativeCallback: ((String) -> Unit)? = null

    // Inference parameters
    var nThreads: Int = 4
    var nGpuLayers: Int = 0
    var contextSize: Int = 2048
    var temperature: Float = 0.7f
    var topP: Float = 0.9f
    var topK: Int = 40
    var repeatPenalty: Float = 1.1f
    var maxTokens: Int = 1024
    var useMlock: Boolean = false

    companion object {
        private const val TAG = "LlamaCppBridge"
        private var llamaAndroidClass: Class<*>? = null
        
        init {
            try {
                llamaAndroidClass = Class.forName("org.nehuatl.llamacpp.LlamaAndroid")
                Log.i(TAG, "LlamaAndroid class found directly")
            } catch (e: Exception) {
                Log.e(TAG, "LlamaAndroid class not found", e)
            }
        }
        
        fun isNativeAvailable(): Boolean = llamaAndroidClass != null
    }

    init {
        try {
            if (llamaAndroidClass != null) {
                val constructor = llamaAndroidClass!!.getDeclaredConstructor(android.content.ContentResolver::class.java)
                llamaAndroidInstance = constructor.newInstance(context.contentResolver)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init LlamaAndroid", e)
        }
    }

    suspend fun loadModel(modelFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        if (!modelFile.exists()) {
            return@withContext Result.failure(Exception("모델 파일이 존재하지 않습니다: ${modelFile.name}"))
        }

        if (llamaAndroidInstance == null) {
            return@withContext Result.failure(Exception("엔진 초기화 실패"))
        }

        try {
            closeNativeModel()

            val fileUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                modelFile
            )

            // Open FileDescriptor
            val pfd = context.contentResolver.openFileDescriptor(fileUri, "r")
                ?: throw Exception("Failed to open FileDescriptor")
            val fd = pfd.detachFd()
            pfd.close()

            // Prepare config map for startEngine
            val configMap = mutableMapOf<String, Any>(
                "model" to fileUri.toString(),
                "model_fd" to fd,
                "n_ctx" to contextSize,
                "n_threads" to if (nThreads > 0) nThreads else 0,
                "n_gpu_layers" to if (nGpuLayers > 0) nGpuLayers else 0,
                "use_mmap" to !useMlock,
                "use_mlock" to useMlock,
                "embedding" to false,
                "n_batch" to 512,
                "vocab_only" to false
            )

            Log.i(TAG, "Calling startEngine with config: $configMap")

            val startEngineMethod = llamaAndroidClass!!.getMethod(
                "startEngine", 
                java.util.Map::class.java, 
                kotlin.jvm.functions.Function1::class.java
            )
            
            // JNI Event Callback
            val callback: (String) -> Unit = { jsonStr ->
                nativeCallback?.invoke(jsonStr)
            }
            
            val result = startEngineMethod.invoke(llamaAndroidInstance, configMap, callback) as? Map<*, *>
            val contextId = result?.get("contextId") as? Int
            
            if (contextId != null) {
                currentContextId = contextId
                isModelLoaded = true
                modelPath = modelFile.absolutePath
                Log.i(TAG, "Model loaded successfully. contextId=$contextId")
                Result.success(Unit)
            } else {
                Result.failure(Exception("Context ID was null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model", e)
            isModelLoaded = false
            currentContextId = null
            Result.failure(Exception("모델 로드 실패: ${e.cause?.message ?: e.message}"))
        }
    }

    fun streamInference(prompt: String): Flow<String> = callbackFlow {
        if (!isModelLoaded || currentContextId == null || llamaAndroidInstance == null) {
            throw java.lang.IllegalStateException("모델이 로드되지 않았습니다")
        }

        nativeCallback = { tokenStr ->
            if (tokenStr.isNotEmpty()) {
                trySend(tokenStr)
            }
        }

        try {
            val params = mutableMapOf<String, Any>(
                "prompt" to prompt,
                "emit_partial_completion" to true,
                "temperature" to temperature.toDouble(),
                "top_p" to topP.toDouble(),
                "top_k" to topK,
                "penalty_repeat" to repeatPenalty.toDouble(),
                "n_predict" to maxTokens
            )

            Log.i(TAG, "Calling launchCompletion with params: $params")

            val launchMethod = llamaAndroidClass!!.getMethod(
                "launchCompletion", 
                Int::class.javaPrimitiveType, 
                java.util.Map::class.java
            )
            
            // Using Kotlin Coroutines instead of unmanaged raw Thread
            launch(Dispatchers.IO) {
                try {
                    launchMethod.invoke(llamaAndroidInstance, currentContextId!!, params)
                    // When invoke returns, generation is complete
                    close()
                } catch (e: Exception) {
                    close(Exception("추론 중 예외 발생: ${e.cause?.message ?: e.message}"))
                }
            }

        } catch (e: Exception) {
            close(Exception("추론 시작 실패: ${e.cause?.message ?: e.message}"))
        }

        awaitClose { 
            nativeCallback = null
        }
    }.flowOn(Dispatchers.IO)

    fun stopInference() {
        if (currentContextId != null && llamaAndroidInstance != null) {
            try {
                val stopMethod = llamaAndroidClass!!.getMethod("stopCompletion", Int::class.javaPrimitiveType)
                stopMethod.invoke(llamaAndroidInstance, currentContextId!!)
                Log.i(TAG, "Native JNI inference stopped successfully for contextId: $currentContextId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to call native stopCompletion: ${e.message}")
            }
        }
    }

    private fun closeNativeModel() {
        nativeCallback = null
        if (isModelLoaded && currentContextId != null && llamaAndroidInstance != null) {
            try {
                val releaseMethod = llamaAndroidClass!!.getMethod("releaseContext", Int::class.javaPrimitiveType)
                releaseMethod.invoke(llamaAndroidInstance, currentContextId!!)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release context", e)
            }
        }
        isModelLoaded = false
        currentContextId = null
    }

    override fun close() {
        closeNativeModel()
    }
}
