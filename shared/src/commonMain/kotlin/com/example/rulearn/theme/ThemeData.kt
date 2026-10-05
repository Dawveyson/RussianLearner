package com.example.rulearn.theme

import androidx.compose.ui.graphics.Color

/**
 * 品牌配色（纯数据），桌面与 Android 共用。Android 端若支持动态取色可在此基础上叠加。
 */
object BrandColors {
    val LightPrimary = Color(0xFF1565C0)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightBackground = Color(0xFFF6F8FB)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSecondary = Color(0xFFE3F2FD)

    val DarkPrimary = Color(0xFF90CAF9)
    val DarkOnPrimary = Color(0xFF0D1B2A)
    val DarkBackground = Color(0xFF0D1B2A)
    val DarkSurface = Color(0xFF15212E)
    val DarkSecondary = Color(0xFF1E2D3D)
}
