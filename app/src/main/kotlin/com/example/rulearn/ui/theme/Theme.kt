package com.example.rulearn.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Material You 主题。
 *
 * - Android 12 及以上：从壁纸取色（dynamicDarkColorScheme / dynamicLightColorScheme），
 *   整支 App 的配色随系统主题色变化，这是 Material You 的核心特征。
 * - 以下版本回退到品牌配色：同样按 M3 的色调表配齐全部 container / on 角色，
 *   保证任何组件在任何页面上的对比度都正确。
 *
 * 所有 surface* 角色都是不透明色，背景不再依赖底层渐变透出。
 */

/** 品牌色（动态取色不可用时的回退方案）。 */
private val BrandLight = lightColorScheme(
    primary = Color(0xFF00639B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCBE6FF),
    onPrimaryContainer = Color(0xFF001D2E),
    secondary = Color(0xFF526070),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4F6),
    onSecondaryContainer = Color(0xFF0F1D2B),
    tertiary = Color(0xFF6C5689),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF2DAFF),
    onTertiaryContainer = Color(0xFF251340),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFCFCFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F9FC),
    surfaceContainer = Color(0xFFF1F4F9),
    surfaceContainerHigh = Color(0xFFEBEFF5),
    surfaceContainerHighest = Color(0xFFE5E9F0),
    outline = Color(0xFF72787F),
    outlineVariant = Color(0xFFC1C7D0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2F3134),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFF9BCAFF),
    surfaceTint = Color(0xFF00639B)
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFF9BCAFF),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF004A77),
    onPrimaryContainer = Color(0xFFCBE6FF),
    secondary = Color(0xFFBAC8DB),
    onSecondary = Color(0xFF253242),
    secondaryContainer = Color(0xFF3B4859),
    onSecondaryContainer = Color(0xFFD6E4F6),
    tertiary = Color(0xFFD6BCFA),
    onTertiary = Color(0xFF3B2757),
    tertiaryContainer = Color(0xFF533D74),
    onTertiaryContainer = Color(0xFFF2DAFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC1C7D0),
    surfaceContainerLowest = Color(0xFF0F1113),
    surfaceContainerLow = Color(0xFF222427),
    surfaceContainer = Color(0xFF26282B),
    surfaceContainerHigh = Color(0xFF313336),
    surfaceContainerHighest = Color(0xFF3C3E41),
    outline = Color(0xFF8B9198),
    outlineVariant = Color(0xFF42474E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E2E6),
    inverseOnSurface = Color(0xFF2F3134),
    inversePrimary = Color(0xFF00639B),
    surfaceTint = Color(0xFF9BCAFF)
)

/**
 * Material 3 Expressive 形状：容器用大圆角、强调 modernity。
 * 按钮是全圆角的胶囊形，卡片和底部抽屉是超大圆角。
 */
val RuLearnShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun RuLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Android 12 起才支持动态取色；测试或另类需求可关掉
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> BrandDark
        else -> BrandLight
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = RuLearnShapes,
        typography = Typography(),
        content = content
    )
}
