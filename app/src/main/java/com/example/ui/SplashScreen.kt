package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

/**
 * 스플래시 화면 (크래시 버그 수정 완료)
 * - AdaptiveIconDrawable 호환을 위해 R.drawable.splash_icon 비트맵 리소스 사용
 * - 중앙: 사용자가 등록한 C 심볼 아이콘
 * - 하단: "On-Device AI" 브랜드 텍스트
 */
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }
    val brandAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 아이콘 페이드인 및 스케일 애니메이션
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
        )

        // 하단 브랜드 문구 페이드인
        delay(250)
        brandAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500)
        )

        // 1.5초 후 메인 앱 진입
        delay(1250)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // ── 중앙: C 심볼 앱 아이콘 (PNG 안전 비트맵) ──────────────────────────
        Box(
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.splash_icon),
                contentDescription = "App Icon Symbol",
                modifier = Modifier
                    .size(130.dp)
                    .clip(RoundedCornerShape(26.dp))
            )
        }

        // ── 하단: "On-Device AI" 문구 ──────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 70.dp)
                .alpha(brandAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "On-Device AI",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-0.5).sp
            )
        }
    }
}
