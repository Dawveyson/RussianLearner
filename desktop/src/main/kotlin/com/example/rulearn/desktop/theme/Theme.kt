package com.example.rulearn.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.rulearn.theme.BrandColors

@Composable
fun RuLearnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(BrandColors.DarkPrimary),
            onPrimary = Color(BrandColors.DarkOnPrimary),
            background = Color(BrandColors.DarkBackground),
            surface = Color(BrandColors.DarkSurface),
            secondary = Color(BrandColors.DarkSecondary)
        )
    } else {
        lightColorScheme(
            primary = Color(BrandColors.LightPrimary),
            onPrimary = Color(BrandColors.LightOnPrimary),
            background = Color(BrandColors.LightBackground),
            surface = Color(BrandColors.LightSurface),
            secondary = Color(BrandColors.LightSecondary)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
