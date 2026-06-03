package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ThriftCoral,
    secondary = ThriftGold,
    tertiary = MintSuccess,
    background = DeepCharcoal,
    surface = DarkCardBg,
    onPrimary = Color.White,
    onSecondary = DeepCharcoal,
    onTertiary = Color.White,
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFE5E7EB)
)

private val LightColorScheme = lightColorScheme(
    primary = ThriftCoral,
    secondary = ThriftGold,
    tertiary = MintSuccess,
    background = Color.White,
    surface = LightCardBg,
    onPrimary = Color.White,
    onSecondary = DeepCharcoal,
    onTertiary = Color.White,
    onBackground = DeepCharcoal,
    onSurface = Color(0xFF1F2937)
)

@Composable
fun ThriftSixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
