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

    @Test
    fun testExportImportBackupRoundtrip() {
        val items = listOf(
            VaultItem(id = "item-101", title = "Cloud Console", password = "super_secret_123", isMasked = true, createdAt = 123456789L)
        )
        val passcode = "BackupMasterPass#99"
        val backupJson = VaultCrypto.exportBackup(items, passcode)

        assertTrue("Backup JSON must specify passgen-vault-backup format", backupJson.contains("passgen-vault-backup"))
        assertTrue("Backup JSON must contain PBKDF2-HMAC-SHA256", backupJson.contains("PBKDF2-HMAC-SHA256"))

        val restored = VaultCrypto.importBackup(backupJson, passcode)
        assertEquals(1, restored.size)
        assertEquals("Cloud Console", restored[0].title)
        assertEquals("super_secret_123", restored[0].password)
    }

    @Test
    fun testDecodeBytesHexAndBase64() {
        val hex = "deadbeef"
        val bytesFromHex = VaultCrypto.decodeBytes(hex)
        assertEquals(4, bytesFromHex.size)

        val b64 = java.util.Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3, 4, 5))
        val bytesFromB64 = VaultCrypto.decodeBytes(b64)
        assertEquals(5, bytesFromB64.size)
    }
}
