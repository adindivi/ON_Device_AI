package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ── Toss-Inspired Minimal B&W Light Scheme (전역 단일 고정 라이트 테마) ───────
private val LightColorScheme = lightColorScheme(
    primary            = TossBlack,
    onPrimary          = TossWhite,
    primaryContainer   = TossGray100,
    onPrimaryContainer = TossBlack,
    secondary          = TossGray600,
    onSecondary        = TossWhite,
    secondaryContainer = TossGray100,
    onSecondaryContainer = TossGray600,
    tertiary           = CarTertiary,
    tertiaryContainer  = CarTertiaryContainer,
    onTertiaryContainer = CarTertiary,
    background         = TossWhite,
    onBackground       = TossBlack,
    surface            = TossWhite,
    onSurface          = TossBlack,
    surfaceVariant     = TossGray100,
    onSurfaceVariant   = TossGray600,
    outline            = TossOutline,
    error              = CarError,
    errorContainer     = CarErrorContainer,
    onErrorContainer   = CarOnErrorContainer
)

/**
 * [MyApplicationTheme]
 * 토스 인스파이어드 미니멀 B&W 단일 라이트 테마
 * - 다크 모드 분기를 완전히 제거하고 시스템 설정과 무관하게 항상 일관된 고시인성 화이트/블랙 핀테크 테마를 제공합니다.
 */
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
