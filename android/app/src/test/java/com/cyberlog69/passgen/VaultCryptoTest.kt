package com.cyberlog69.passgen

import com.cyberlog69.passgen.domain.VaultCrypto
import com.cyberlog69.passgen.domain.VaultItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class VaultCryptoTest {

    @Test
    fun testGeneratePepper() {
        val pepper1 = VaultCrypto.generatePepper()
        val pepper2 = VaultCrypto.generatePepper()
        assertEquals("Pepper must be 64 hex characters", 64, pepper1.length)
        assertEquals("Pepper must be 64 hex characters", 64, pepper2.length)
        assertNotEquals("Peppers must be randomly generated", pepper1, pepper2)
    }

    @Test
    fun testHashPasswordDeterministic() {
        val pepper = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
        val hash1 = VaultCrypto.hashPassword(pepper, "mySecretPassword123")
        val hash2 = VaultCrypto.hashPassword(pepper, "mySecretPassword123")
        val hashOther = VaultCrypto.hashPassword(pepper, "anotherPassword")

        assertEquals("Same password with same pepper must produce same hash", hash1, hash2)
        assertEquals("Hash must be 64 hex chars (SHA-256)", 64, hash1.length)
        assertNotEquals("Different passwords must produce different hashes", hash1, hashOther)
    }

    @Test
    fun testEncryptDecryptRoundtrip() {
        val items = listOf(
            VaultItem(id = "1", title = "Work Account", password = "pass1", isMasked = true, createdAt = 1000L),
            VaultItem(id = "2", title = "Personal Email", password = "pass2", isMasked = true, createdAt = 2000L)
        )
        val masterPasscode = "MasterPasscode#123"

        val bundle = VaultCrypto.encryptItems(items, masterPasscode)
        assertTrue("Salt should not be empty", bundle.salt.isNotBlank())
        assertTrue("IV should not be empty", bundle.iv.isNotBlank())
        assertTrue("Ciphertext should not be empty", bundle.ciphertext.isNotBlank())

        val decrypted = VaultCrypto.decryptItems(bundle, masterPasscode)
        assertEquals(2, decrypted.size)
        assertEquals("Work Account", decrypted[0].title)
        assertEquals("pass1", decrypted[0].password)
        assertEquals("Personal Email", decrypted[1].title)
        assertEquals("pass2", decrypted[1].password)
    }

    @Test
    fun testWrongPasscodeFails() {
        val items = listOf(
            VaultItem(id = "1", title = "Test", password = "secret", isMasked = true, createdAt = 1000L)
        )
        val bundle = VaultCrypto.encryptItems(items, "CorrectPasscode")

        try {
            VaultCrypto.decryptItems(bundle, "WrongPasscode")
            fail("Decryption with wrong passcode should throw an exception")
        } catch (_: Exception) {
            // Expected
        }
    }
}
