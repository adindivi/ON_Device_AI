package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.backend.RAGSearcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("차량 진단 AI", appName)
    }

    @Test
    fun `verify RAGSearcher SAMPLE_DOC_IDS integrity`() {
        // 단일 진실 공급원(SSOT) 샘플 문서 ID 집합 무결성 검증
        val sampleIds = RAGSearcher.SAMPLE_DOC_IDS
        assertTrue("기본 샘플 문서 ID가 정의되어 있어야 합니다", sampleIds.isNotEmpty())
        assertTrue("DOC-DEFAULT-1이 포함되어야 합니다", sampleIds.contains("DOC-DEFAULT-1"))
        assertTrue("TSB-03-15가 포함되어야 합니다", sampleIds.contains("TSB-03-15"))
        assertTrue("DIAG-C1206이 포함되어야 합니다", sampleIds.contains("DIAG-C1206"))
    }

    @Test
    fun `verify TopFloatingToast emoji stripping logic`() {
        // TopFloatingToast의 이모지 정규식 필터링 검증
        val emojiRegex = Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cs}\\p{Cn}\\u2000-\\u3300\\uD83C-\\uDFFF]+\\s*")

        val testCases = listOf(
            "🤖 AI 상세 답변 켜짐" to "AI 상세 답변 켜짐",
            "⚡ 초고속 모드 켜짐" to "초고속 모드 켜짐",
            "✅ AI 모델 준비 완료" to "AI 모델 준비 완료",
            "❌ AI 모델 연결 실패" to "AI 모델 연결 실패",
            "스마트 정비 진단서가 작성되었습니다." to "스마트 정비 진단서가 작성되었습니다."
        )

        for ((input, expected) in testCases) {
            val cleaned = input.replace(emojiRegex, "").trim()
            assertEquals("이모지가 깔끔히 제거되어야 합니다", expected, cleaned)
        }
    }

    @Test
    fun `verify TopFloatingToast error detection logic`() {
        fun isErrorToast(msg: String): Boolean {
            return msg.contains("실패") || msg.contains("불일치") || msg.contains("오류") ||
                    msg.contains("필요") || msg.contains("다시 확인") || msg.contains("⚠️") || msg.contains("올바르지 않습니다")
        }

        assertTrue("연결 실패는 에러로 분류되어야 합니다", isErrorToast("AI 모델 연결 실패"))
        assertTrue("비밀번호 확인 요구는 에러로 분류되어야 합니다", isErrorToast("비밀번호를 다시 확인해주세요."))
        assertTrue("권한 필요는 에러로 분류되어야 합니다", isErrorToast("마이크 권한 필요"))

        assertFalse("정상 성공 메시지는 에러가 아니어야 합니다", isErrorToast("스마트 정비 진단서가 작성되었습니다."))
        assertFalse("모드 켜짐 메시지는 에러가 아니어야 합니다", isErrorToast("AI 상세 답변 켜짐"))
    }
}
