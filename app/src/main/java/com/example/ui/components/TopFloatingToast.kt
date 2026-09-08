package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 토스(Toss) 스타일 상단 미니 캡슐 알림 (Top Floating Toast)
 */
@Composable
fun TopFloatingToast(
    message: String?,
    onDismiss: () -> Unit,
    durationMs: Long = 2400L
) {
    var visible by remember { mutableStateOf(false) }
    var currentMsg by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (!message.isNullOrBlank()) {
            currentMsg = message
            visible = true
            delay(durationMs)
            visible = false
            delay(300L)
            onDismiss()
        } else {
            visible = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 20.dp, end = 20.dp)
            .testTag("top_floating_toast_container"),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
        ) {
            val isError = currentMsg.contains("실패") || currentMsg.contains("불일치") || currentMsg.contains("오류") || currentMsg.contains("필요") || currentMsg.contains("다시 확인") || currentMsg.contains("⚠️") || currentMsg.contains("올바르지 않습니다")
            // 이모지 프리픽스 제거 (컴팩트 벡터 아이콘과 중복 방지)
            val displayMsg = currentMsg.replace(Regex("^[\\p{So}\\p{Sk}\\p{Sm}\\p{Sc}\\p{Cs}\\p{Cn}\\u2000-\\u3300\\uD83C-\\uDFFF]+\\s*"), "").trim()

            Surface(
                modifier = Modifier
                    .shadow(4.dp, shape = RoundedCornerShape(50.dp))
                    .border(0.75.dp, Color(0xFFE2E8F0), RoundedCornerShape(50.dp)),
                shape = RoundedCornerShape(50.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (!isError) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (!isError) Color(0xFF007AFF) else Color(0xFFDC2626),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayMsg,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF1D1D1F)
                    )
                }
            }
        }
    }
}
