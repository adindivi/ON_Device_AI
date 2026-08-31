package com.example.model

import org.json.JSONObject

/**
 * GGUF Quantization types supported by llama.cpp and Qwen
 */
enum class GgufQuantizationType(val label: String, val bitsPerWeight: Float, val vramMultiplier: Float) {
    Q4_0("Q4_0", 4.5f, 0.56f),
    Q4_K_M("Q4_K_M", 4.85f, 0.60f),
    Q5_K_M("Q5_K_M", 5.5f, 0.69f),
    Q8_0("Q8_0", 8.5f, 1.06f),
    F16("F16", 16.0f, 2.00f),
    F32("F32", 32.0f, 4.00f),
    UNKNOWN("Unknown", 4.5f, 0.56f);

    companion object {
        fun fromString(name: String): GgufQuantizationType {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) || it.label.equals(name, ignoreCase = true) }
                ?: if (name.contains("Q4", ignoreCase = true)) Q4_K_M
                else if (name.contains("Q8", ignoreCase = true)) Q8_0
                else if (name.contains("F16", ignoreCase = true)) F16
                else UNKNOWN
        }
    }
}

/**
 * GPU Acceleration backend options
 */
enum class GpuAccelerationBackend(val displayName: String, val apiName: String) {
    VULKAN("Vulkan GPU", "vulkan"),
    OPENCL("OpenCL GPU", "opencl"),
    NNAPI("Android NNAPI", "nnapi"),
    CPU_ONLY("CPU Multithreading", "cpu")
}

/**
 * Parsed GGUF metadata information
 */
data class GgufMetadata(
    val magic: String,
    val version: Int,
    val tensorCount: Long,
    val kvCount: Long,
    val architecture: String, // e.g. "qwen2", "llama"
    val modelName: String,
    val contextLength: Int,
    val embeddingLength: Int,
    val blockCount: Int,
    val headCount: Int,
    val headCountKv: Int,
    val vocabSize: Int,
    val quantizationType: GgufQuantizationType,
    val fileSizeMb: Double,
    val estimatedVramMb: Double,
    val estimatedRamMb: Double,
    val rawMetadata: Map<String, Any> = emptyMap()
) {
    fun toSummary(): String {
        return "$architecture • $quantizationType • ctx:$contextLength • ${String.format("%.1f", fileSizeMb)}MB"
    }
}

/**
 * Memory footprint and VRAM allocation policy
 */
data class MemoryPolicy(
    val totalDeviceRamMb: Long,
    val availableDeviceRamMb: Long,
    val heapMaxMb: Long,
    val isLowRamDevice: Boolean,
    val maxRecommendedContext: Int,
    val gpuLayersToOffload: Int,
    val kvCacheCompressionEnabled: Boolean
) {
    companion object {
        fun createDefault(totalRamMb: Long, availRamMb: Long, quant: GgufQuantizationType): MemoryPolicy {
            val lowRam = totalRamMb < 4000
            val maxCtx = when {
                lowRam -> 1024
                totalRamMb < 6000 -> 2048
                else -> 4096
            }
            val gpuLayers = when (quant) {
                GgufQuantizationType.Q4_0, GgufQuantizationType.Q4_K_M -> if (lowRam) 16 else 32
                GgufQuantizationType.Q8_0 -> if (lowRam) 8 else 20
                else -> if (lowRam) 4 else 12
            }
            return MemoryPolicy(
                totalDeviceRamMb = totalRamMb,
                availableDeviceRamMb = availRamMb,
                heapMaxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024),
                isLowRamDevice = lowRam,
                maxRecommendedContext = maxCtx,
                gpuLayersToOffload = gpuLayers,
                kvCacheCompressionEnabled = lowRam
            )
        }
    }
}

/**
 * Chat Message Model
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val tokensGenerated: Int = 0,
    val tokensPerSecond: Double = 0.0,
    val isFailure: Boolean = false,
    val errorMessage: String? = null
)

enum class MessageSender {
    USER, ASSISTANT, SYSTEM
}
