package com.cyberlog69.passgen.domain

import java.security.SecureRandom
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToLong

object SecureRandomGenerator {

    private val secureRandom: SecureRandom = SecureRandom()

    const val UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    const val LOWERCASE = "abcdefghijklmnopqrstuvwxyz"
    const val NUMBERS = "0123456789"
    const val DEFAULT_SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    val AMBIGUOUS_SET: Set<Char> = setOf('l', '1', 'I', 'O', '0', 'o', 'Q', 'S', '5', 'Z', '2')

    /**
     * Rejection sampling for uniform random integer in [0, max) without modulo bias.
     */
    fun secureRandomInt(max: Int): Int {
        if (max <= 0) return 0
        val limit = (0x7FFFFFFFL / max) * max
        var sample: Long
        do {
            sample = secureRandom.nextInt().toLong() and 0x7FFFFFFFL
        } while (sample >= limit)
        return (sample % max).toInt()
    }

    private fun filterAmbiguous(str: String): String {
        return str.filter { it !in AMBIGUOUS_SET }
    }

    fun <T> shuffle(list: MutableList<T>): List<T> {
        for (i in list.size - 1 downTo 1) {
            val j = secureRandomInt(i + 1)
            val temp = list[i]
            list[i] = list[j]
            list[j] = temp
        }
        return list
    }

    fun generatePassword(
        length: Int,
        includeUpper: Boolean,
        includeLower: Boolean,
        includeNumbers: Boolean,
        includeSymbols: Boolean,
        excludeAmbiguous: Boolean,
        customSymbols: String = DEFAULT_SYMBOLS
    ): String {
        val activeSets = mutableListOf<String>()

        if (includeUpper) {
            val s = if (excludeAmbiguous) filterAmbiguous(UPPERCASE) else UPPERCASE
            if (s.isNotEmpty()) activeSets.add(s)
        }
        if (includeLower) {
            val s = if (excludeAmbiguous) filterAmbiguous(LOWERCASE) else LOWERCASE
            if (s.isNotEmpty()) activeSets.add(s)
        }
        if (includeNumbers) {
            val s = if (excludeAmbiguous) filterAmbiguous(NUMBERS) else NUMBERS
            if (s.isNotEmpty()) activeSets.add(s)
        }
        if (includeSymbols) {
            val rawSym = if (customSymbols.isNotBlank()) customSymbols else DEFAULT_SYMBOLS
            val s = if (excludeAmbiguous) filterAmbiguous(rawSym) else rawSym
            if (s.isNotEmpty()) activeSets.add(s)
        }

        if (activeSets.isEmpty() || length <= 0) return ""

        val resultChars = mutableListOf<Char>()
        // Guarantee at least 1 character from each active charset
        for (set in activeSets) {
            resultChars.add(set[secureRandomInt(set.length)])
        }

        val allPool = activeSets.joinToString("")
        val remaining = (length - resultChars.size).coerceAtLeast(0)
        for (i in 0 until remaining) {
            resultChars.add(allPool[secureRandomInt(allPool.length)])
        }

        return shuffle(resultChars).joinToString("")
    }

    fun generatePassphrase(
        wordCount: Int,
        separator: Separator,
        capitalize: Boolean,
        appendNumber: Boolean
    ): String {
        val wordList = EffWordlist.words
        val count = wordCount.coerceIn(3, 10)
        val selectedWords = (0 until count).map {
            val word = wordList[secureRandomInt(wordList.size)]
            if (capitalize) {
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            } else {
                word
            }
        }.toMutableList()

        if (appendNumber) {
            val idx = secureRandomInt(selectedWords.size)
            selectedWords[idx] = selectedWords[idx] + secureRandomInt(100)
        }

        return selectedWords.joinToString(separator.symbol)
    }

    fun generatePIN(length: Int): String {
        val len = length.coerceIn(4, 16)
        return (0 until len).map {
            NUMBERS[secureRandomInt(NUMBERS.length)]
        }.joinToString("")
    }

    fun calculateEntropy(
        mode: GeneratorMode,
        length: Int,
        includeUpper: Boolean = true,
        includeLower: Boolean = true,
        includeNumbers: Boolean = true,
        includeSymbols: Boolean = true,
        excludeAmbiguous: Boolean = false,
        customSymbols: String = DEFAULT_SYMBOLS,
        wordCount: Int = 4,
        includeNumber: Boolean = false
    ): Int {
        return when (mode) {
            GeneratorMode.PASSWORD -> {
                var poolSize = 0
                if (includeUpper) poolSize += (if (excludeAmbiguous) filterAmbiguous(UPPERCASE) else UPPERCASE).length
                if (includeLower) poolSize += (if (excludeAmbiguous) filterAmbiguous(LOWERCASE) else LOWERCASE).length
                if (includeNumbers) poolSize += (if (excludeAmbiguous) filterAmbiguous(NUMBERS) else NUMBERS).length
                if (includeSymbols) {
                    val rawSym = if (customSymbols.isNotBlank()) customSymbols else DEFAULT_SYMBOLS
                    poolSize += (if (excludeAmbiguous) filterAmbiguous(rawSym) else rawSym).length
                }
                if (poolSize <= 0 || length <= 0) 0
                else floor(length * log2(poolSize.toDouble())).toInt()
            }
            GeneratorMode.PASSPHRASE -> {
                val poolSize = EffWordlist.words.size
                var bits = wordCount * log2(poolSize.toDouble())
                if (includeNumber) bits += log2(100.0)
                floor(bits).toInt()
            }
            GeneratorMode.PIN -> {
                if (length <= 0) 0
                else floor(length * log2(10.0)).toInt()
            }
        }
    }

    fun estimateCrackTime(bits: Int): String {
        if (bits <= 0) return "Instant"
        val combinations = 2.0.pow(bits.toDouble())
        val seconds = combinations / 1e11 // 100 Billion guesses/sec (modern GPU cluster)

        return when {
            seconds < 1 -> "Instant"
            seconds < 60 -> "${seconds.roundToLong()} seconds"
            seconds < 3600 -> "${(seconds / 60).roundToLong()} minutes"
            seconds < 86400 -> "${(seconds / 3600).roundToLong()} hours"
            seconds < 31536000 -> "${(seconds / 86400).roundToLong()} days"
            seconds < 31536000L * 100 -> "${(seconds / 31536000).roundToLong()} years"
            seconds < 31536000L * 1_000_000 -> "${"%.1f".format(seconds / (31536000 * 1_000))}k years"
            else -> "${"%.1f".format(seconds / (31536000 * 1_000_000))}M years"
        }
    }

    fun getStrengthLevel(bits: Int): StrengthLevel {
        return when {
            bits < 40 -> StrengthLevel.WEAK
            bits < 60 -> StrengthLevel.FAIR
            bits < 80 -> StrengthLevel.STRONG
            else -> StrengthLevel.VERY_STRONG
        }
    }

    fun getGuidance(
        mode: GeneratorMode,
        bits: Int,
        length: Int,
        hasSymbols: Boolean = true,
        wordCount: Int = 4
    ): StrengthGuidance {
        return when (mode) {
            GeneratorMode.PIN -> {
                when {
                    length < 6 -> StrengthGuidance(
                        GuidanceStatus.ALERT,
                        "⚠️",
                        "Short PIN (only 10k combinations). Ensure rate-limiting on target lock."
                    )
                    length >= 8 -> StrengthGuidance(
                        GuidanceStatus.GOOD,
                        "🛡️",
                        "High security PIN with over 100 million combinations."
                    )
                    else -> StrengthGuidance(
                        GuidanceStatus.WARN,
                        "💡",
                        "Standard 6-digit PIN. Well balanced for phone lock screens."
                    )
                }
            }
            GeneratorMode.PASSPHRASE -> {
                when {
                    wordCount < 4 -> StrengthGuidance(
                        GuidanceStatus.WARN,
                        "💡",
                        "Tip: 4 or more words are recommended for robust dictionary attack resistance."
                    )
                    wordCount >= 6 -> StrengthGuidance(
                        GuidanceStatus.GOOD,
                        "🔒",
                        "Massive entropy passphrase! Highly memorable and practically uncrackable."
                    )
                    else -> StrengthGuidance(
                        GuidanceStatus.GOOD,
                        "🛡️",
                        "Strong passphrase. Easy to type, hard to crack."
                    )
                }
            }
            GeneratorMode.PASSWORD -> {
                when {
                    length < 8 -> StrengthGuidance(
                        GuidanceStatus.ALERT,
                        "⚠️",
                        "Extremely short! Use at least 12–16 characters for critical logins."
                    )
                    !hasSymbols -> StrengthGuidance(
                        GuidanceStatus.WARN,
                        "💡",
                        "Tip: Enabling symbols significantly increases resistance to brute-force attacks."
                    )
                    bits < 40 -> StrengthGuidance(
                        GuidanceStatus.ALERT,
                        "⚠️",
                        "Weak password. Increase length and enable more character sets."
                    )
                    bits < 60 -> StrengthGuidance(
                        GuidanceStatus.WARN,
                        "💡",
                        "Fair password. Good for basic accounts, but 16+ chars recommended."
                    )
                    bits >= 80 -> StrengthGuidance(
                        GuidanceStatus.GOOD,
                        "🔒",
                        "Excellent! Highly resistant to offline GPU cluster attacks."
                    )
                    else -> StrengthGuidance(
                        GuidanceStatus.GOOD,
                        "🛡️",
                        "Strong password with solid entropy."
                    )
                }
            }
        }
    }
}
