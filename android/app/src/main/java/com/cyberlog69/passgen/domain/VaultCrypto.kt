package com.cyberlog69.passgen.domain

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONArray
import org.json.JSONObject

object VaultCrypto {

    private const val PBKDF2_ITERATIONS = 100000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16
    private const val IV_LENGTH_BYTES = 12
    private const val TAG_LENGTH_BITS = 128

    fun generatePepper(): String {
        val rand = SecureRandom()
        val bytes = ByteArray(32)
        rand.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(pepper: String, password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$pepper:$password".toByteArray(StandardCharsets.UTF_8)
        val digest = md.digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    data class EncryptedBundle(
        val salt: String,
        val iv: String,
        val ciphertext: String
    ) {
        fun toJson(): String {
            val obj = JSONObject()
            obj.put("salt", salt)
            obj.put("iv", iv)
            obj.put("ciphertext", ciphertext)
            return obj.toString()
        }

        companion object {
            fun fromJson(jsonStr: String): EncryptedBundle? {
                return try {
                    val obj = JSONObject(jsonStr)
                    val salt = when {
                        obj.has("salt") -> obj.getString("salt")
                        obj.has("kdf") -> obj.getJSONObject("kdf").optString("salt")
                        else -> null
                    }
                    val iv = when {
                        obj.has("iv") -> obj.getString("iv")
                        obj.has("cipher") -> obj.getJSONObject("cipher").optString("iv")
                        else -> null
                    }
                    val ciphertext = if (obj.has("ciphertext")) obj.getString("ciphertext") else null

                    if (!salt.isNullOrBlank() && !iv.isNullOrBlank() && !ciphertext.isNullOrBlank()) {
                        EncryptedBundle(salt = salt, iv = iv, ciphertext = ciphertext)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                }
            }
        }
    }

    fun decodeBytes(str: String): ByteArray {
        val clean = str.trim()
        return if (clean.matches(Regex("^[0-9a-fA-F]+$")) && clean.length % 2 == 0) {
            val bytes = ByteArray(clean.length / 2)
            for (i in clean.indices step 2) {
                bytes[i / 2] = clean.substring(i, i + 2).toInt(16).toByte()
            }
            bytes
        } else {
            Base64.getDecoder().decode(clean)
        }
    }

    fun encryptItems(items: List<VaultItem>, masterPasscode: String): EncryptedBundle {
        val jsonArray = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("password", item.password)
            obj.put("createdAt", item.createdAt)
            jsonArray.put(obj)
        }
        val plainBytes = jsonArray.toString().toByteArray(StandardCharsets.UTF_8)

        val rand = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTES)
        val iv = ByteArray(IV_LENGTH_BYTES)
        rand.nextBytes(salt)
        rand.nextBytes(iv)

        val keySpec = PBEKeySpec(masterPasscode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val cipherBytes = cipher.doFinal(plainBytes)

        val encoder = Base64.getEncoder()
        return EncryptedBundle(
            salt = encoder.encodeToString(salt),
            iv = encoder.encodeToString(iv),
            ciphertext = encoder.encodeToString(cipherBytes)
        )
    }

    fun decryptItems(bundle: EncryptedBundle, masterPasscode: String): List<VaultItem> {
        val salt = decodeBytes(bundle.salt)
        val iv = decodeBytes(bundle.iv)
        val cipherBytes = decodeBytes(bundle.ciphertext)

        val keySpec = PBEKeySpec(masterPasscode.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val plainBytes = cipher.doFinal(cipherBytes)
        val jsonStr = String(plainBytes, StandardCharsets.UTF_8)
        val jsonArray = JSONArray(jsonStr)

        val result = mutableListOf<VaultItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            result.add(
                VaultItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    title = obj.optString("title", "Untitled"),
                    password = obj.optString("password", ""),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        return result
    }

    fun exportBackup(items: List<VaultItem>, masterPasscode: String): String {
        val bundle = encryptItems(items, masterPasscode)
        val root = JSONObject()
        root.put("format", "passgen-vault-backup")
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val kdf = JSONObject()
        kdf.put("algorithm", "PBKDF2-HMAC-SHA256")
        kdf.put("iterations", PBKDF2_ITERATIONS)
        kdf.put("salt", bundle.salt)
        root.put("kdf", kdf)

        val cipher = JSONObject()
        cipher.put("algorithm", "AES-256-GCM")
        cipher.put("iv", bundle.iv)
        cipher.put("tagLength", TAG_LENGTH_BITS)
        root.put("cipher", cipher)

        root.put("ciphertext", bundle.ciphertext)
        root.put("itemCount", items.size)
        return root.toString(2)
    }

    fun importBackup(jsonStr: String, masterPasscode: String): List<VaultItem> {
        val bundle = EncryptedBundle.fromJson(jsonStr)
            ?: throw IllegalArgumentException("Invalid backup format or corrupted JSON")
        return decryptItems(bundle, masterPasscode)
    }
}
