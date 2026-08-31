package com.example.engine

import android.app.ActivityManager
import android.content.Context
import android.net.Uri
import com.example.model.GgufMetadata
import com.example.model.GgufQuantizationType
import com.example.model.MemoryPolicy
import com.example.model.ProfilingLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Manages GGUF model files in the isolated App Sandbox directory.
 * Provides file copying, integrity validation, and profiling log persistence.
 */
class SandboxModelManager(private val context: Context) {

    private val sandboxDir: File
        get() {
            val dir = File(context.filesDir, "sandbox_models")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val profilingLogFile: File
        get() = File(context.filesDir, "profiling_log.json")

    /**
     * Copy user-selected GGUF file from Uri to app sandbox
     */
    suspend fun importGgufToSandbox(
        uri: Uri,
        fileName: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val targetFile = File(sandboxDir, safeName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                val totalBytes = runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                }.getOrNull() ?: -1L

                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        if (totalBytes > 0) {
                            onProgress(bytesCopied.toFloat() / totalBytes.toFloat())
                        }
                    }
                    output.flush()
                }
            } ?: return@withContext Result.failure(Exception("URI에서 스트림을 열 수 없습니다"))

            // Verify file copied successfully and is non-empty
            if (!targetFile.exists() || targetFile.length() < 64) {
                targetFile.delete()
                return@withContext Result.failure(Exception("파일 복사 실패: 빈 파일이거나 복사 중 중단되었습니다"))
            }

            Result.success(targetFile)
        } catch (e: Exception) {
            Result.failure(Exception("샌드박스 복사 실패: ${e.localizedMessage ?: "알 수 없는 I/O 오류"}"))
        }
    }

    /**
     * Preload or generate an initial Qwen2.5 GGUF container in the sandbox for instant testing
     */
    suspend fun createPresetQwenModel(presetName: String = "qwen2.5-0.5b-instruct-q4_k_m.gguf"): File = withContext(Dispatchers.IO) {
        val file = File(sandboxDir, presetName)
        if (!file.exists() || file.length() < 100) {
            FileOutputStream(file).use { fos ->
                // Write standard GGUF v3 header
                val bb = ByteBuffer.allocate(1024).order(ByteOrder.LITTLE_ENDIAN)
                // Magic: GGUF
                bb.put(byteArrayOf(0x47, 0x47, 0x55, 0x46))
                // Version 3
                bb.putInt(3)
                // Tensor count
                bb.putLong(196L)
                // KV count
                bb.putLong(14L)

                // Write standard Qwen key values
                writeGgufKvString(bb, "general.architecture", "qwen2")
                writeGgufKvString(bb, "general.name", "Qwen2.5-0.5B-Instruct-Mobile")
                writeGgufKvInt32(bb, "qwen2.context_length", 4096)
                writeGgufKvInt32(bb, "qwen2.embedding_length", 896)
                writeGgufKvInt32(bb, "qwen2.block_count", 24)
                writeGgufKvInt32(bb, "qwen2.attention.head_count", 14)
                writeGgufKvInt32(bb, "qwen2.attention.head_count_kv", 2)
                writeGgufKvInt32(bb, "qwen2.vocab_size", 151936)
                writeGgufKvString(bb, "general.quantization_version", "Q4_K_M")

                fos.write(bb.array(), 0, bb.position())
                // Write dummy weight buffer
                val dummyWeights = ByteArray(32 * 1024)
                fos.write(dummyWeights)
                fos.flush()
            }
        }
        file
    }

    private fun writeGgufKvString(bb: ByteBuffer, key: String, value: String) {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        bb.putLong(keyBytes.size.toLong())
        bb.put(keyBytes)
        bb.putInt(8) // Type STRING
        val valBytes = value.toByteArray(Charsets.UTF_8)
        bb.putLong(valBytes.size.toLong())
        bb.put(valBytes)
    }

    private fun writeGgufKvInt32(bb: ByteBuffer, key: String, value: Int) {
        val keyBytes = key.toByteArray(Charsets.UTF_8)
        bb.putLong(keyBytes.size.toLong())
        bb.put(keyBytes)
        bb.putInt(4) // Type UINT32/INT32
        bb.putInt(value)
    }

    /**
     * List all GGUF models stored in sandbox
     */
    fun listSandboxModels(): List<File> {
        return sandboxDir.listFiles { file ->
            file.isFile && (file.extension.equals("gguf", ignoreCase = true) || file.name.contains(".gguf", ignoreCase = true))
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    /**
     * Delete model from sandbox
     */
    fun deleteModel(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    /**
     * Get device memory information
     */
    fun getDeviceMemoryPolicy(quantType: GgufQuantizationType): MemoryPolicy {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = (memInfo.totalMem / (1024 * 1024))
        val availRamMb = (memInfo.availMem / (1024 * 1024))

        return MemoryPolicy.createDefault(totalRamMb, availRamMb, quantType)
    }

    /**
     * Append profiling record to profiling_log.json
     */
    suspend fun saveProfilingLog(log: ProfilingLog) = withContext(Dispatchers.IO) {
        try {
            val logsArray = if (profilingLogFile.exists() && profilingLogFile.length() > 0) {
                try {
                    val content = profilingLogFile.readText()
                    JSONArray(content)
                } catch (e: Exception) {
                    JSONArray()
                }
            } else {
                JSONArray()
            }

            logsArray.put(log.toJson())
            profilingLogFile.writeText(logsArray.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Read all profiling logs from profiling_log.json
     */
    suspend fun readProfilingLogs(): List<ProfilingLog> = withContext(Dispatchers.IO) {
        if (!profilingLogFile.exists() || profilingLogFile.length() == 0L) {
            return@withContext emptyList()
        }
        try {
            val content = profilingLogFile.readText()
            val array = JSONArray(content)
            val list = mutableListOf<ProfilingLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(ProfilingLog.fromJson(obj))
            }
            list.reversed()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun clearProfilingLogs() = withContext(Dispatchers.IO) {
        if (profilingLogFile.exists()) {
            profilingLogFile.delete()
        }
    }
}
