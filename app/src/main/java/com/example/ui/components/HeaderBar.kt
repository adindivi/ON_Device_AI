package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray500
import com.example.ui.theme.TossGradientBlue
import com.example.ui.theme.TossGradientPink
import com.example.ui.theme.TossGradientPurple
import com.example.ui.theme.TossWhite

@Composable
fun HeaderBar(
    onTextDecrease: () -> Unit,
    onTextIncrease: () -> Unit,
    onOpenContribution: () -> Unit,
    onAdminLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Animated gradient offset for shimmer effect
    val infiniteTransition = rememberInfiniteTransition(label = "header_gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_anim"
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(TossGradientPurple, TossGradientBlue, TossGradientPink),
        start = Offset(gradientOffset * 600f, 0f),
        end = Offset(gradientOffset * 600f + 600f, 200f)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(gradientBrush)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Left: Logo + App Name ─────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Logo box: white bg + black border + code icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(TossWhite)
                        .border(2.dp, TossBlack, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onAdminLongPress() })
                        }
                        .testTag("btn_car_logo"),
                    contentAlignment = Alignment.Center
                ) {
                    // Code icon: </> rendered as Text for crisp look
                    Text(
                        text = "</>",
                        fontSize = 7.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TossBlack,
                        letterSpacing = (-0.5).sp
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "On-Device AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TossBlack,
                            fontSize = 17.sp,
                            letterSpacing = (-0.3).sp
                        )
                    )
                    Text(
                        text = "Ko-SBERT RAG Engine",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TossGray500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // ── Right: A- / A+ buttons + Avatar ──────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 가 - button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { onTextDecrease() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("btn_text_decrease"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가 -",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                // 가 + button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50.dp))
                        .background(Color(0xFFF1F5F9))
                        .clickable { onTextIncrease() }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("btn_text_increase"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가 +",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // Avatar: black circle + person icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TossBlack)
                        .clickable { onOpenContribution() }
                        .testTag("btn_header_contribution"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User / Contribution",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
