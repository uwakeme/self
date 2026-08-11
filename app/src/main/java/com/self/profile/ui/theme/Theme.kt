package com.self.profile.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 字体大小档位（US-014）
 * - SMALL: 0.9x
 * - STANDARD: 1.0x
 * - LARGE: 1.15x
 * - XLARGE: 1.3x
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

/** 深色 / 浅色 切换的过渡时长。PRD AC-015-2 要求 300ms。 */
private const val THEME_TRANSITION_MS = 300

/**
 * 主题入口。从设置读取 darkMode / fontScale。
 *
 * 切换深色 / 浅色时，核心颜色用 [animateColorAsState] 做 300ms 过渡，避免硬切。
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

    val targetColorScheme = if (useDark) {
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

    // 主题切换动画：对用户能看到的主要颜色做 300ms 过渡
    val colorScheme = animateColorScheme(targetColorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bg = colorScheme.background.toArgb()
            window.statusBarColor = bg
            window.navigationBarColor = bg
            val insets = WindowCompat.getInsetsController(window, view)
            insets.isAppearanceLightStatusBars = !useDark
            insets.isAppearanceLightNavigationBars = !useDark
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

/**
 * 对 [target] 的核心色做 [THEME_TRANSITION_MS] ms 过渡，返回新的 ColorScheme。
 *
 * 覆盖：background / onBackground / surface / onSurface / surfaceVariant / onSurfaceVariant /
 * primary / onPrimary / outline / outlineVariant / error。
 * 其它（secondary / onError 等）保持原色 —— 视觉上影响小，少一组动画开销。
 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = THEME_TRANSITION_MS)
    val background by animateColorAsState(target.background, spec, label = "bg")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBg")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceV")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceV")
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "outlineV")
    val error by animateColorAsState(target.error, spec, label = "error")
    return target.copy(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        primary = primary,
        onPrimary = onPrimary,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error
    )
}
