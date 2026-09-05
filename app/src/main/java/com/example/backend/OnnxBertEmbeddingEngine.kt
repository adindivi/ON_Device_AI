package com.example.backend

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log
import java.io.File
import java.nio.LongBuffer
import kotlin.math.sqrt

class OnnxBertEmbeddingEngine(
    private val modelFile: File,
    private val vocabFile: File? = null
) {
    private var env: OrtEnvironment? = null
    private var session: OrtSession? = null
    private var tokenizer: BertTokenizer = BertTokenizer()

    var isReady: Boolean = false
        private set

    init {
        initEngine()
    }

    fun initEngine(): Boolean {
        Log.d("OnnxBert", "initEngine: path=${modelFile.absolutePath}, exists=${modelFile.exists()}, size=${modelFile.length()}B")
        if (!modelFile.exists() || modelFile.length() == 0L) {
            Log.w("OnnxBert", "⚠️ ONNX model not found or empty → isReady=false, will use hash fallback")
            isReady = false
            return false
        }

        return try {
            env = OrtEnvironment.getEnvironment()
            session = env?.createSession(modelFile.absolutePath, OrtSession.SessionOptions())

            if (vocabFile != null && vocabFile.exists()) {
                tokenizer = BertTokenizer.loadFromFile(vocabFile)
                Log.d("OnnxBert", "Vocab loaded: ${vocabFile.absolutePath}, size=${vocabFile.length()}B")
            } else {
                Log.w("OnnxBert", "Vocab file missing, using default tokenizer")
            }

            isReady = true
            Log.i("OnnxBert", "✅ ONNX KoSBERT loaded successfully! isReady=true, model=${modelFile.length() / 1024 / 1024}MB")
            true
        } catch (e: Exception) {
            Log.e("OnnxBert", "❌ ONNX load failed: ${e.message}", e)
            isReady = false
            false
        }
    }

    fun getEmbedding(text: String, isQuery: Boolean = false): List<Float>? {
        if (!isReady || session == null || env == null) {
            Log.w("OnnxBertEngine", "[Ko-SBERT 임베딩 불가] isReady=$isReady, session=${session != null}, env=${env != null}")
            return null
        }

        val startTime = System.currentTimeMillis()
        var inputIdsTensor: OnnxTensor? = null
        var attentionMaskTensor: OnnxTensor? = null
        var results: OrtSession.Result? = null

        return try {
            val encoded = tokenizer.encode(text, isQuery = isQuery)
            val maxLen = encoded.inputIds.size.toLong()
            val shape = longArrayOf(1, maxLen)

            inputIdsTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.inputIds), shape)
            attentionMaskTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(encoded.attentionMask), shape)

            val inputs = mapOf(
                "input_ids" to inputIdsTensor,
                "attention_mask" to attentionMaskTensor
            )

            results = session?.run(inputs)
            val outputTensor = results?.get(0)?.value

            val embeddingVec = processOutputTensor(outputTensor, encoded.attentionMask)
            val elapsedMs = System.currentTimeMillis() - startTime
            Log.d("OnnxBertEngine", "[Ko-SBERT 임베딩 성공] 소요시간=${elapsedMs}ms, 토큰수=$maxLen, 차원=${embeddingVec.size}")
            embeddingVec
        } catch (e: Exception) {
            Log.e("OnnxBertEngine", "❌ [Ko-SBERT 임베딩 추론 실패] text='${text.take(50)}', isReady=$isReady, 예외: ${e.message}", e)
            null
        } finally {
            try { inputIdsTensor?.close() } catch (_: Exception) {}
            try { attentionMaskTensor?.close() } catch (_: Exception) {}
            try { results?.close() } catch (_: Exception) {}
        }
    }

    private fun processOutputTensor(output: Any?, attentionMask: LongArray): List<Float> {
        if (output == null) return emptyList()

        // Handle [1, seqLen, hiddenDim] tensor (last_hidden_state)
        if (output is Array<*> && output.isArrayOf<Array<FloatArray>>()) {
            @Suppress("UNCHECKED_CAST")
            val hiddenStates = (output as Array<Array<FloatArray>>)[0] // [seqLen, hiddenDim]
            val seqLen = hiddenStates.size
            if (seqLen == 0) return emptyList()
            val hiddenDim = hiddenStates[0].size

            val meanVec = FloatArray(hiddenDim)
            var validTokenCount = 0

            for (i in 0 until seqLen) {
                if (i < attentionMask.size && attentionMask[i] == 1L) {
                    validTokenCount++
                    for (d in 0 until hiddenDim) {
                        meanVec[d] += hiddenStates[i][d]
                    }
                }
            }

            val denom = if (validTokenCount > 0) validTokenCount.toFloat() else 1.0f
            for (d in 0 until hiddenDim) {
                meanVec[d] /= denom
            }

            return normalizeVector(meanVec)
        }

        // Handle [1, hiddenDim] pooled output tensor
        if (output is Array<*> && output.isArrayOf<FloatArray>()) {
            @Suppress("UNCHECKED_CAST")
            val pooledVec = (output as Array<FloatArray>)[0]
            return normalizeVector(pooledVec)
        }

        return emptyList()
    }

    private fun normalizeVector(vec: FloatArray): List<Float> {
        var sumSq = 0.0f
        for (v in vec) {
            sumSq += v * v
        }
        val norm = sqrt(sumSq.toDouble()).toFloat().coerceAtLeast(1e-6f)
        return vec.map { it / norm }
    }

    fun close() {
        try {
            session?.close()
            env?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isReady = false
        }
    }
}
