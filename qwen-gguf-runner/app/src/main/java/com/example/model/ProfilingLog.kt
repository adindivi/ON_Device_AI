package com.example.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * System profiling record stored in profiling_log.json
 */
data class ProfilingLog(
    val timestamp: Long = System.currentTimeMillis(),
    val modelName: String,
    val modelPath: String,
    val quantization: String,
    val loadTimeMs: Long,
    val inferenceTimeMs: Long,
    val tokensGenerated: Int,
    val tokensPerSecond: Double,
    val memoryUsageMb: Double,
    val vramUsageMb: Double,
    val gpuUtilizationPercent: Double,
    val cpuThreadsUsed: Int,
    val accelerationBackend: String,
    val isSuccess: Boolean,
    val statusMessage: String,
    val extraDetails: Map<String, Any> = emptyMap()
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("timestamp", timestamp)
        json.put("model_name", modelName)
        json.put("model_path", modelPath)
        json.put("quantization", quantization)
        json.put("load_time_ms", loadTimeMs)
        json.put("inference_time_ms", inferenceTimeMs)
        json.put("tokens_generated", tokensGenerated)
        json.put("tokens_per_second", String.format("%.2f", tokensPerSecond).toDoubleOrNull() ?: tokensPerSecond)
        json.put("memory_usage_mb", String.format("%.1f", memoryUsageMb).toDoubleOrNull() ?: memoryUsageMb)
        json.put("vram_usage_mb", String.format("%.1f", vramUsageMb).toDoubleOrNull() ?: vramUsageMb)
        json.put("gpu_utilization_percent", String.format("%.1f", gpuUtilizationPercent).toDoubleOrNull() ?: gpuUtilizationPercent)
        json.put("cpu_threads_used", cpuThreadsUsed)
        json.put("acceleration_backend", accelerationBackend)
        json.put("is_success", isSuccess)
        json.put("status_message", statusMessage)

        val detailsJson = JSONObject()
        extraDetails.forEach { (k, v) -> detailsJson.put(k, v) }
        json.put("system_metrics", detailsJson)

        return json
    }

    fun toFormattedJsonString(): String {
        return toJson().toString(2)
    }

    companion object {
        fun fromJson(json: JSONObject): ProfilingLog {
            return ProfilingLog(
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                modelName = json.optString("model_name", "Unknown"),
                modelPath = json.optString("model_path", ""),
                quantization = json.optString("quantization", "Unknown"),
                loadTimeMs = json.optLong("load_time_ms", 0L),
                inferenceTimeMs = json.optLong("inference_time_ms", 0L),
                tokensGenerated = json.optInt("tokens_generated", 0),
                tokensPerSecond = json.optDouble("tokens_per_second", 0.0),
                memoryUsageMb = json.optDouble("memory_usage_mb", 0.0),
                vramUsageMb = json.optDouble("vram_usage_mb", 0.0),
                gpuUtilizationPercent = json.optDouble("gpu_utilization_percent", 0.0),
                cpuThreadsUsed = json.optInt("cpu_threads_used", 4),
                accelerationBackend = json.optString("acceleration_backend", "vulkan"),
                isSuccess = json.optBoolean("is_success", true),
                statusMessage = json.optString("status_message", "OK")
            )
        }
    }
}
