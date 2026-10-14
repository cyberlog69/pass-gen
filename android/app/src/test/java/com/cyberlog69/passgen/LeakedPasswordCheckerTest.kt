package com.cyberlog69.passgen

import com.cyberlog69.passgen.domain.LeakedPasswordChecker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeakedPasswordCheckerTest {

    @Test
    fun testDetectsCommonBreachedPasswords() {
        val breached = listOf("password", "123456", "admin123", "qwerty", "iloveyou", "dragon")
        for (pass in breached) {
            val res = LeakedPasswordChecker.check(pass)
            assertTrue("Expected '$pass' to be flagged as leaked", res.isLeaked)
        }
    }

    @Test
    fun testDetectsRepeatedCharacters() {
        val repeated = listOf("aaaaaa", "111111", "ZZZZZZ")
        for (pass in repeated) {
            val res = LeakedPasswordChecker.check(pass)
            assertTrue("Expected repeated '$pass' to be flagged", res.isLeaked)
        }
    }

    @Test
    fun testDetectsKeyboardWalks() {
        val walks = listOf("myqwertytest", "asdfgh123", "myzxcvbnpass")
        for (pass in walks) {
            val res = LeakedPasswordChecker.check(pass)
            assertTrue("Expected walk '$pass' to be flagged", res.isLeaked)
        }
    }

    @Test
    fun testAllowsStrongRandomPasswords() {
        val strong = listOf("k9#Xm!8Lz@2Wq", "Tr0ub4dor&3", "correct-horse-battery-staple-99")
        for (pass in strong) {
            val res = LeakedPasswordChecker.check(pass)
            assertFalse("Expected strong '$pass' not to be flagged as leaked", res.isLeaked)
        }
    }
}
