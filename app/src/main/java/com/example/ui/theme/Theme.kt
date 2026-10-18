package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color(0xFF042F2E),
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = Color(0xFFA7F3D0),
    secondary = OxfordGold,
    onSecondary = Color(0xFF271300),
    secondaryContainer = OxfordGoldSoft,
    onSecondaryContainer = OxfordGoldLight,
    tertiary = IeltsTeal,
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = IeltsTealSoft,
    onTertiaryContainer = IeltsTealLight,
    background = ObsidianBackground,
    onBackground = TextPrimaryDark,
    surface = ObsidianSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = ObsidianBorder,
    error = PhonemeRed
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF064E3B),
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = OxfordGoldLight,
    onSecondaryContainer = Color(0xFF78350F),
    tertiary = Color(0xFF0D9488),
    onTertiary = Color.White,
    tertiaryContainer = IeltsTealLight,
    onTertiaryContainer = Color(0xFF134E4A),
    background = SandBackground,
    onBackground = TextPrimaryLight,
    surface = SandSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SandCard,
    onSurfaceVariant = TextSecondaryLight,
    outline = SandBorder,
    error = PhonemeRed
)

@Composable
fun LinguaBridgeTheme(
    darkTheme: Boolean = true,
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    LinguaBridgeTheme(darkTheme = darkTheme, isRtl = true, content = content)
}
