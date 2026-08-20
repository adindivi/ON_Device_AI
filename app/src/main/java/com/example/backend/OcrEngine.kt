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
     * 구글 ML Kit AI 비전 모델로 비트맵 사진 속 글자를 딥러닝 인지하여 DTC 고장코드 파싱
     */
    suspend fun analyzeBitmapImage(bitmap: Bitmap): OcrResult = suspendCoroutine { continuation ->
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullRecognizedText = visionText.text
                    val parsedResult = processOcrText(fullRecognizedText)
                    continuation.resume(parsedResult)
                }
                .addOnFailureListener {
                    continuation.resume(
                        OcrResult(success = false, rawText = "", dtcCodes = emptyList())
                    )
                }
        } catch (e: Exception) {
            continuation.resume(
                OcrResult(success = false, rawText = "", dtcCodes = emptyList())
            )
        }
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
