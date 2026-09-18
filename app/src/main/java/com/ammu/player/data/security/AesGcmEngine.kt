package com.ammu.player.data.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Enterprise-grade AES-256-GCM cipher with PBKDF2 key derivation.
 * Provides authenticated encryption preventing bit-flipping and tampering.
 */
object AesGcmEngine {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH_BIT = 128
    private const val IV_LENGTH_BYTE = 12
    private const val SALT_LENGTH_BYTE = 16
    private const val ITERATION_COUNT = 65536
    private const val KEY_LENGTH_BIT = 256

    @Serializable
    data class EncryptedEnvelope(
        val saltBase64: String,
        val ivBase64: String,
        val ciphertextBase64: String,
        val version: Int = 1
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /**
     * Encrypts plaintext string using the provided passphrase with AES-256-GCM.
     * Returns a JSON-serialized envelope encoded in Base64.
     */
    fun encrypt(plainText: String, passKey: String): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH_BYTE).apply { random.nextBytes(this) }
        val iv = ByteArray(IV_LENGTH_BYTE).apply { random.nextBytes(this) }

        val keySpec = PBEKeySpec(passKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(keySpec)
        val key = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val envelope = EncryptedEnvelope(
            saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        )

        val jsonStr = json.encodeToString(envelope)
        return Base64.encodeToString(jsonStr.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    /**
     * Decrypts an encrypted payload using the provided passkey.
     * Throws an exception if authentication fails or passkey is incorrect.
     */
    fun decrypt(encodedEnvelopeString: String, passKey: String): String {
        val envelopeJsonBytes = Base64.decode(encodedEnvelopeString, Base64.NO_WRAP)
        val envelopeJson = String(envelopeJsonBytes, Charsets.UTF_8)
        val envelope = json.decodeFromString<EncryptedEnvelope>(envelopeJson)

        val salt = Base64.decode(envelope.saltBase64, Base64.NO_WRAP)
        val iv = Base64.decode(envelope.ivBase64, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(envelope.ciphertextBase64, Base64.NO_WRAP)

        val keySpec = PBEKeySpec(passKey.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKey = factory.generateSecret(keySpec)
        val key = SecretKeySpec(secretKey.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BIT, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }
}
