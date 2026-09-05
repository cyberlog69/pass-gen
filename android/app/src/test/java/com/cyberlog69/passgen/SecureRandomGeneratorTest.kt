package com.cyberlog69.passgen

import com.cyberlog69.passgen.domain.EffWordlist
import com.cyberlog69.passgen.domain.GeneratorMode
import com.cyberlog69.passgen.domain.QrCodeEngine
import com.cyberlog69.passgen.domain.SecureRandomGenerator
import com.cyberlog69.passgen.domain.Separator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureRandomGeneratorTest {

    @Test
    fun testWordlistLoaded() {
        assertTrue("EFF Wordlist should contain over 500 words", EffWordlist.words.size > 500)
        assertFalse("EFF Wordlist words should not be empty", EffWordlist.words.any { it.isBlank() })
    }

    @Test
    fun testSecureRandomBounds() {
        for (max in 1..50) {
            for (i in 0..20) {
                val r = SecureRandomGenerator.secureRandomInt(max)
                assertTrue("Random int $r must be in [0, $max)", r in 0 until max)
            }
        }
    }

    @Test
    fun testPasswordGeneration() {
        val pwd = SecureRandomGenerator.generatePassword(
            length = 20,
            includeUpper = true,
            includeLower = true,
            includeNumbers = true,
            includeSymbols = true,
            excludeAmbiguous = false
        )
        assertEquals(20, pwd.length)
        assertTrue("Should contain uppercase", pwd.any { it in SecureRandomGenerator.UPPERCASE })
        assertTrue("Should contain lowercase", pwd.any { it in SecureRandomGenerator.LOWERCASE })
        assertTrue("Should contain digits", pwd.any { it in SecureRandomGenerator.NUMBERS })
        assertTrue("Should contain symbols", pwd.any { it in SecureRandomGenerator.DEFAULT_SYMBOLS })
    }

    @Test
    fun testAmbiguousExclusion() {
        val pwd = SecureRandomGenerator.generatePassword(
            length = 50,
            includeUpper = true,
            includeLower = true,
            includeNumbers = true,
            includeSymbols = true,
            excludeAmbiguous = true
        )
        for (ch in SecureRandomGenerator.AMBIGUOUS_SET) {
            assertFalse("Ambiguous character '$ch' should be excluded", pwd.contains(ch))
        }
    }

    @Test
    fun testPassphraseGeneration() {
        val phrase = SecureRandomGenerator.generatePassphrase(
            wordCount = 5,
            separator = Separator.PERIOD,
            capitalize = true,
            appendNumber = true
        )
        val parts = phrase.split(".")
        assertEquals(5, parts.size)
        assertTrue("Words should be capitalized", parts.all { it.first().isUpperCase() })
        assertTrue("Passphrase should contain numbers when requested", phrase.any { it.isDigit() })
    }

    @Test
    fun testPinGeneration() {
        for (len in listOf(4, 6, 8, 12, 16)) {
            val pin = SecureRandomGenerator.generatePIN(len)
            assertEquals(len, pin.length)
            assertTrue("PIN must be digits only: $pin", pin.all { it.isDigit() })
        }
    }

    @Test
    fun testEntropyCalculations() {
        val passBits = SecureRandomGenerator.calculateEntropy(
            mode = GeneratorMode.PASSWORD,
            length = 16
        )
        assertTrue("16-char password should have high entropy: $passBits", passBits > 90)

        val pinBits = SecureRandomGenerator.calculateEntropy(
            mode = GeneratorMode.PIN,
            length = 6
        )
        assertEquals("6-digit PIN should have 19 bits entropy", 19, pinBits)
    }

    @Test
    fun testQrMatrixGeneration() {
        val matrix = QrCodeEngine.generateMatrix("SecurePassword123")
        assertTrue("QR matrix should be at least 21x21", matrix.size >= 21)
        assertEquals(matrix.size, matrix[0].size)
    }
}
