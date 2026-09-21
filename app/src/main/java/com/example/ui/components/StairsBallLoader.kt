package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Uiverse.io by Nawsome - Bouncing Ball on Steps Loader
 * Compose 구현체: 5개의 계단 바를 공이 통통 튀어오르고 내려가는 무한 로딩 애니메이션
 */
private data class BallKeyframe(val p: Float, val x: Float, val y: Float)

private val ballKeyframes = listOf(
    BallKeyframe(0.00f, 0f, 0f),
    BallKeyframe(0.05f, 8f, -14f),
    BallKeyframe(0.10f, 15f, -10f),
    BallKeyframe(0.17f, 23f, -24f),
    BallKeyframe(0.20f, 30f, -20f),
    BallKeyframe(0.27f, 38f, -34f),
    BallKeyframe(0.30f, 45f, -30f),
    BallKeyframe(0.37f, 53f, -44f),
    BallKeyframe(0.40f, 60f, -40f),
    BallKeyframe(0.50f, 60f, 0f),
    BallKeyframe(0.57f, 53f, -14f),
    BallKeyframe(0.60f, 45f, -10f),
    BallKeyframe(0.67f, 37f, -24f),
    BallKeyframe(0.70f, 30f, -20f),
    BallKeyframe(0.77f, 22f, -34f),
    BallKeyframe(0.80f, 15f, -30f),
    BallKeyframe(0.87f, 7f, -44f),
    BallKeyframe(0.90f, 0f, -40f),
    BallKeyframe(1.00f, 0f, 0f)
)

private fun getBarScale(barIndex: Int, t: Float): Float {
    return when (barIndex) {
        0 -> when {
            t < 0.40f -> 0.2f
            t < 0.50f -> 0.2f + (1.0f - 0.2f) * ((t - 0.40f) / 0.10f)
            t < 0.90f -> 1.0f
            else      -> 1.0f - (1.0f - 0.2f) * ((t - 0.90f) / 0.10f)
        }
        1 -> when {
            t < 0.40f -> 0.4f
            t < 0.50f -> 0.4f + (0.8f - 0.4f) * ((t - 0.40f) / 0.10f)
            t < 0.90f -> 0.8f
            else      -> 0.8f - (0.8f - 0.4f) * ((t - 0.90f) / 0.10f)
        }
        2 -> 0.6f
        3 -> when {
            t < 0.40f -> 0.8f
            t < 0.50f -> 0.8f - (0.8f - 0.4f) * ((t - 0.40f) / 0.10f)
            t < 0.90f -> 0.4f
            else      -> 0.4f + (0.8f - 0.4f) * ((t - 0.90f) / 0.10f)
        }
        4 -> when {
            t < 0.40f -> 1.0f
            t < 0.50f -> 1.0f - (1.0f - 0.2f) * ((t - 0.40f) / 0.10f)
            t < 0.90f -> 0.2f
            else      -> 0.2f + (1.0f - 0.2f) * ((t - 0.90f) / 0.10f)
        }
        else -> 0.6f
    }
}

private fun interpolateBallPosition(t: Float): Pair<Float, Float> {
    for (i in 0 until ballKeyframes.size - 1) {
        val curr = ballKeyframes[i]
        val next = ballKeyframes[i + 1]
        if (t in curr.p..next.p) {
            val fraction = if (next.p > curr.p) (t - curr.p) / (next.p - curr.p) else 0f
            val x = curr.x + (next.x - curr.x) * fraction
            val y = curr.y + (next.y - curr.y) * fraction
            return Pair(x, y)
        }
    }
    return Pair(0f, 0f)
}

@Composable
fun StairsBallLoader(
    modifier: Modifier = Modifier.size(width = 36.dp, height = 24.dp),
    barColor: Color = Color.White.copy(alpha = 0.85f),
    ballColor: Color = Color(0xFF2C8FFF), // #2C8FFF (Uiverse 원본 블루)
    durationMillis: Int = 3600
) {
    val transition = rememberInfiniteTransition(label = "stairs_ball_transition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing)
        ),
        label = "stairs_ball_progress"
    )

    Canvas(modifier = modifier) {
        // 기준 디자인 좌표계: 너비 70px, 높이 65px (바 바닥은 y=65)
        val designWidth = 70f
        val designHeight = 65f
        val scale = min(size.width / designWidth, size.height / designHeight)

        val offsetX = (size.width - designWidth * scale) / 2f
        val offsetY = (size.height - designHeight * scale) / 2f

        val baseY = designHeight * scale + offsetY
        val maxBarHeight = 50f * scale
        val barWidth = 10f * scale
        val barStepX = 15f * scale
        val cornerRad = CornerRadius(2f * scale, 2f * scale)

        // 1. 5개 계단 바 그리기
        for (i in 0 until 5) {
            val barScale = getBarScale(i, progress)
            val barHeight = maxBarHeight * barScale
            val barLeft = offsetX + (i * barStepX)
            val barTop = baseY - barHeight

            drawRoundRect(
                color = barColor,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRad
            )
        }

        // 2. 통통 튀는 공 그리기
        val (ballX, ballY) = interpolateBallPosition(progress)
        val ballRadius = 5f * scale
        // CSS에서 ball의 기본 bottom은 10px (baseY - 10px*scale)
        // translate(x, y)에서 y는 음수일수록 위로 올라감 (-ballY)
        val ballCenterX = offsetX + (ballX * scale) + ballRadius
        val ballCenterY = baseY - (10f * scale) + (ballY * scale) - ballRadius

        drawCircle(
            color = ballColor,
            radius = ballRadius,
            center = Offset(ballCenterX, ballCenterY)
        )
    }
}
