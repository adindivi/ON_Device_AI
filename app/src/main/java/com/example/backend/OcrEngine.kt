package com.example.backend

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class OcrResult(
    val success: Boolean,
    val rawText: String,
    val dtcCodes: List<String>
)

/**
 * Google ML Kit Text Recognition 온디바이스 딥러닝 AI 비전 엔진 (BACKUP 버그 수정 완료)
 * - 자동차 DTC 표준 규격: 첫 글자 [PCBU] + 두 번째 문자는 반드시 숫자 [0-9] (BACKUP, BLOCK 등 일반 단어 100% 원천 차단)
 */
object OcrEngine {

    // 자동차 DTC 고장코드 정밀 패턴: 첫글자 P,C,B,U + 두번째는 무조건 숫자 [0-9] + 뒤이어 3~6자리 헥사코드
    private const val DTC_STRICT_PATTERN = "(?i)\\b[PCBU][0-9][0-9A-Z]{3,6}\\b"

    private val NOISE_WORDS = setOf(
        "TROUBLE", "CODES", "DETECTED", "SCANNER", "DIAGNOSIS",
        "CTROUBLE", "CODESDET", "SYSTEM", "ENGINE", "CHECK", "BACKUP", "BLOCK"
    )

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * 구글 ML Kit AI 비전 모델로 비트맵 엔진에 한글/영어 섞인 이미지를 투입하여 DTC 고장코드 파싱
     */
    suspend fun analyzeBitmapImage(bitmap: Bitmap): OcrResult = suspendCoroutine { continuation ->
        var enhancedBitmap: Bitmap? = null
        try {
            enhancedBitmap = enhanceBitmapForOcr(bitmap)
            val image = InputImage.fromBitmap(enhancedBitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    try {
                        val fullRecognizedText = visionText.text
                        val parsedResult = processOcrText(fullRecognizedText)
                        continuation.resume(parsedResult)
                    } finally {
                        recycleSafely(enhancedBitmap)
                    }
                }
                .addOnFailureListener {
                    try {
                        continuation.resume(
                            OcrResult(success = false, rawText = "", dtcCodes = emptyList())
                        )
                    } finally {
                        recycleSafely(enhancedBitmap)
                    }
                }
        } catch (e: Exception) {
            recycleSafely(enhancedBitmap)
            continuation.resume(
                OcrResult(success = false, rawText = "", dtcCodes = emptyList())
            )
        }
    }

    /**
     * [recycleSafely]
     * - Bitmap 자원을 안전하게 반환하여 연속 스캔 시 네이티브 메모리(OOM) 누수를 방지하는 헬퍼 함수
     */
    private fun recycleSafely(target: Bitmap?) {
        if (target != null && !target.isRecycled) {
            target.recycle()
        }
    }

    /**
     * [enhanceBitmapForOcr] - 안드로이드 순정 ColorMatrix 필터 전처리
     * - OpenCV 같은 대용량 라이브러리 추가 없이(앱 용량 0MB 증가) 흑백 변환 및 대비 1.5배 강조
     * - 어두운 지하 주차장이나 빛 반사 계기판 화면에서도 텍스트 외곽선을 뚜렷하게 보정합니다.
     */
    fun enhanceBitmapForOcr(original: Bitmap): Bitmap {
        val config = original.config ?: Bitmap.Config.ARGB_8888
        val resultBitmap = Bitmap.createBitmap(original.width, original.height, config)
        val canvas = android.graphics.Canvas(resultBitmap)
        val paint = android.graphics.Paint()

        val grayMatrix = android.graphics.ColorMatrix()
        grayMatrix.setSaturation(0f)

        val contrast = 1.5f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = android.graphics.ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        val finalMatrix = android.graphics.ColorMatrix()
        finalMatrix.setConcat(contrastMatrix, grayMatrix)

        paint.colorFilter = android.graphics.ColorMatrixColorFilter(finalMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)

        return resultBitmap
    }

    /**
     * 추출된 텍스트에서 DTC 고장코드 정밀 추출
     */
    fun processOcrText(inputText: String): OcrResult {
        val dtcCodes = mutableSetOf<String>()

        val pattern = Pattern.compile(DTC_STRICT_PATTERN)
        val matcher = pattern.matcher(inputText)
        while (matcher.find()) {
            val code = matcher.group().uppercase()
            if (isValidDtcCode(code)) {
                dtcCodes.add(code)
            }
        }

        val tokens = inputText.split("\\s+".toRegex())
        for (token in tokens) {
            val cleanToken = token.replace("[^a-zA-Z0-9]".toRegex(), "")
            val tokenMatcher = pattern.matcher(cleanToken)
            while (tokenMatcher.find()) {
                val code = tokenMatcher.group().uppercase()
                if (isValidDtcCode(code)) {
                    dtcCodes.add(code)
                }
            }
        }

        return OcrResult(
            success = true,
            rawText = inputText,
            dtcCodes = dtcCodes.toList()
        )
    }

    /**
     * 유효한 자동차 DTC 고장코드인지 엄격 검증
     * - BACKUP 등 영문 단어 제외
     */
    private fun isValidDtcCode(code: String): Boolean {
        if (code.length < 5 || code.length > 8) return false
        val upper = code.uppercase()
        if (upper in NOISE_WORDS) return false
        if (upper.startsWith("BACKUP") || upper.startsWith("CTROUBLE")) return false

        // 1. 접두사는 P, C, B, U 로 시작해야 함
        val firstChar = upper.first()
        if (firstChar !in listOf('P', 'C', 'B', 'U')) return false

        // 2. 두 번째 문자는 반드시 숫자(0~9)여야 함 (SAE/ISO 고장코드 규격)
        val secondChar = upper[1]
        return secondChar.isDigit()
    }
}
