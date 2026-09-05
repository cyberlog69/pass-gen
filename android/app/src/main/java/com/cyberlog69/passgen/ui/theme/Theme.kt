package com.cyberlog69.passgen.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.cyberlog69.passgen.domain.ThemePreset

@Composable
fun PassGenTheme(
    preset: ThemePreset = ThemePreset.VIOLET,
    content: @Composable () -> Unit
) {
    val colorScheme = when (preset) {
        ThemePreset.LIGHT -> lightColorScheme(
            primary = LightAccent,
            secondary = LightAccent2,
            background = LightBg,
            surface = LightSurface,
            surfaceVariant = LightSurface2,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = LightText,
            onSurface = LightText,
            outline = LightBorder
        )
        ThemePreset.EMERALD -> darkColorScheme(
            primary = EmeraldAccent,
            secondary = EmeraldAccent2,
            background = Color(0xFF04130C),
            surface = Color(0xFF092015),
            surfaceVariant = Color(0xFF0E2E1F),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText,
            outline = Color(0xFF1B4D36)
        )
        ThemePreset.SAPPHIRE -> darkColorScheme(
            primary = SapphireAccent,
            secondary = SapphireAccent2,
            background = Color(0xFF06111E),
            surface = Color(0xFF0B1E33),
            surfaceVariant = Color(0xFF112A47),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText,
            outline = Color(0xFF1E426D)
        )
        ThemePreset.CYBERPUNK -> darkColorScheme(
            primary = CyberpunkAccent,
            secondary = CyberpunkAccent2,
            background = Color(0xFF160814),
            surface = Color(0xFF261023),
            surfaceVariant = Color(0xFF381834),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText,
            outline = Color(0xFF592453)
        )
        ThemePreset.VIOLET -> darkColorScheme(
            primary = VioletAccent,
            secondary = VioletAccent2,
            background = DarkBg,
            surface = DarkSurface,
            surfaceVariant = DarkSurface2,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = DarkText,
            onSurface = DarkText,
            outline = DarkBorder
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
