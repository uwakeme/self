package com.self.profile.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 字体大小档位（US-014）
 * - STANDARD: 1.0x
 * - LARGE: 1.15x
 */
enum class FontScale(val multiplier: Float) {
    SMALL(0.9f),
    STANDARD(1.0f),
    LARGE(1.15f),
    XLARGE(1.3f)
}

/**
 * 用户设置驱动的暗色模式状态。
 * off / on / system
 */
enum class DarkModeOption { Off, On, System }

/**
 * 主题入口。从设置读取 darkMode / fontScale。
 */
@Composable
fun SelfTheme(
    darkMode: DarkModeOption = DarkModeOption.System,
    fontScale: FontScale = FontScale.STANDARD,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = when (darkMode) {
        DarkModeOption.Off -> false
        DarkModeOption.On -> true
        DarkModeOption.System -> systemDark
    }

    val colorScheme = if (useDark) {
        darkColorScheme(
            primary = WarmJournalColorsDark.Primary,
            onPrimary = WarmJournalColorsDark.TextInverse,
            primaryContainer = WarmJournalColorsDark.PrimaryLightest,
            onPrimaryContainer = WarmJournalColorsDark.PrimaryLighter,
            secondary = WarmJournalColorsDark.PrimaryLighter,
            onSecondary = WarmJournalColorsDark.TextInverse,
            background = WarmJournalColorsDark.Bg,
            onBackground = WarmJournalColorsDark.TextPrimary,
            surface = WarmJournalColorsDark.BgCard,
            onSurface = WarmJournalColorsDark.TextPrimary,
            surfaceVariant = WarmJournalColorsDark.BgSurface,
            onSurfaceVariant = WarmJournalColorsDark.TextSecondary,
            outline = WarmJournalColorsDark.Border,
            outlineVariant = WarmJournalColorsDark.Divider,
            error = WarmJournalColorsDark.Error,
            onError = WarmJournalColorsDark.TextInverse
        )
    } else {
        lightColorScheme(
            primary = WarmJournalColors.Primary,
            onPrimary = WarmJournalColors.TextInverse,
            primaryContainer = WarmJournalColors.PrimaryLightest,
            onPrimaryContainer = WarmJournalColors.PrimaryDarker,
            secondary = WarmJournalColors.PrimaryLighter,
            onSecondary = WarmJournalColors.TextPrimary,
            background = WarmJournalColors.Bg,
            onBackground = WarmJournalColors.TextPrimary,
            surface = WarmJournalColors.BgCard,
            onSurface = WarmJournalColors.TextPrimary,
            surfaceVariant = WarmJournalColors.BgSurface,
            onSurfaceVariant = WarmJournalColors.TextSecondary,
            outline = WarmJournalColors.Border,
            outlineVariant = WarmJournalColors.Divider,
            error = WarmJournalColors.Error,
            onError = WarmJournalColors.TextInverse
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !useDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !useDark
        }
    }

    // 字体大小缩放：覆盖 LocalDensity 的 fontScale 字段
    val baseDensity = LocalDensity.current
    val scaledDensity = if (fontScale == FontScale.STANDARD) {
        baseDensity
    } else {
        androidx.compose.ui.unit.Density(
            density = baseDensity.density,
            fontScale = baseDensity.fontScale * fontScale.multiplier
        )
    }

    CompositionLocalProvider(
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WarmJournalTypography,
            shapes = WarmJournalShapes,
            content = content
        )
    }
}
