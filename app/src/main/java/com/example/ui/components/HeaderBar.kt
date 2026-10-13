package com.example.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.TossBlack
import com.example.ui.theme.TossGray200
import com.example.ui.theme.TossGray500
import com.example.ui.theme.TossWhite

/**
 * [HeaderBar]
 * 핀테크 뉴트럴 미니멀 상단 헤더바
 *
 * - Anti-Squishing(Rule 2): 좌측 브랜딩 영역에 `weight(1f, fill = false)`와 `TextOverflow.Ellipsis`를 적용하여
 *   갤럭시 S24 등 소형/보통 화면(360~390dp)에서도 우측 프로필 아이콘과 글자 크기 버튼이 절대 화면 밖으로 밀려나지 않도록 보장.
 * - 핀테크 뉴트럴 규격(Rule 6) 및 1px 하단 디바이더 경계선 유지.
 * - 삼성 One UI 폰트 베이스라인 교정(Rule 5): PlatformTextStyle(includeFontPadding = false) 적용.
 */
@Composable
fun HeaderBar(
    onTextDecrease: () -> Unit,
    onTextIncrease: () -> Unit,
    onOpenContribution: () -> Unit,
    onAdminLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val borderColor = TossGray200
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .statusBarsPadding()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = strokeWidth
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // ── Left: Logo + App Name + RAG Engine Pill (가변 영역: weight 1f) ──
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // App Logo
                Image(
                    painter = painterResource(id = R.drawable.splash_icon),
                    contentDescription = "차량 진단 AI 로고 (길게 누르면 관리자 모드)",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, TossGray200, RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = { onAdminLongPress() })
                        }
                        .testTag("btn_car_logo"),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = "On-Device AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor,
                                fontSize = 15.sp,
                                letterSpacing = (-0.3).sp,
                                platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // RAG 뱃지 캡슐
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Ko-SBERT",
                                softWrap = false,
                                maxLines = 1,
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                                )
                            )
                        }
                    }

                    Text(
                        text = "스마트 차량 진단 솔루션",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TossGray500,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // ── Right: A- / A+ buttons + Avatar (고정 크기 조작 영역) ─────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 글자 크기 축소 (가 -)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable { onTextDecrease() }
                        .semantics {
                            role = Role.Button
                            contentDescription = "글자 크기 축소"
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_text_decrease"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가 -",
                        softWrap = false,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                // 글자 크기 확대 (가 +)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(50.dp))
                        .background(Color(0xFFF8FAFC))
                        .clickable { onTextIncrease() }
                        .semantics {
                            role = Role.Button
                            contentDescription = "글자 크기 확대"
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("btn_text_increase"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "가 +",
                        softWrap = false,
                        maxLines = 1,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            platformStyle = @Suppress("DEPRECATION") PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 사용자 프로필 / 기여도 버튼 (Avatar)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TossBlack)
                        .clickable { onOpenContribution() }
                        .semantics {
                            role = Role.Button
                            contentDescription = "사용자 프로필 및 기여도 관리"
                        }
                        .testTag("btn_header_contribution"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TossWhite,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}
