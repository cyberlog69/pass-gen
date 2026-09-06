package com.cyberlog69.passgen.domain

import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class GeneratorMode(val title: String) {
    PASSWORD("Password"),
    PASSPHRASE("Passphrase"),
    PIN("PIN")
}

enum class StrengthLevel(val label: String, val color: Color, val segments: Int) {
    WEAK("Weak", Color(0xFFEF4444), 1),
    FAIR("Fair", Color(0xFFF97316), 2),
    STRONG("Strong", Color(0xFFEAB308), 3),
    VERY_STRONG("Very Strong", Color(0xFF22C55E), 4)
}

enum class GuidanceStatus {
    GOOD, WARN, ALERT
}

data class StrengthGuidance(
    val status: GuidanceStatus,
    val icon: String,
    val text: String
)

enum class Separator(val symbol: String, val displayName: String) {
    HYPHEN("-", "Hyphen (-)"),
    SPACE(" ", "Space ( )"),
    PERIOD(".", "Period (.)"),
    UNDERSCORE("_", "Underscore (_)")
}

enum class ThemePreset(val displayName: String, val accentColor: Color) {
    VIOLET("Neon Violet", Color(0xFF7C3AED)),
    EMERALD("Emerald Matrix", Color(0xFF059669)),
    SAPPHIRE("Sapphire Glass", Color(0xFF2563EB)),
    CYBERPUNK("Cyberpunk Amber", Color(0xFFD946EF)),
    LIGHT("Light Mode", Color(0xFF6D28D9))
}

data class HistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isMasked: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class GenerationResult(
    val text: String,
    val entropyBits: Int,
    val crackTime: String,
    val guidance: StrengthGuidance,
    val strengthLevel: StrengthLevel
)

data class VaultItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val password: String,
    val isMasked: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
