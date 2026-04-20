package com.gcancino.levelingup.data.local.datastore

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class DataStoreCryptoManager {

    private val TAG = "DataStoreCryptoManager"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getSecretKey(): SecretKey {
        val alias = "leveling_up_datastore_enc_key"
        val existingKey = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: createSecretKey(alias)
    }

    private fun createSecretKey(alias: String): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(value: String?): String? {
        if (value == null) return null
        return try {
            val bytes = value.toByteArray()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(bytes)
            
            // Combine IV and Encrypted payload
            val combined = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error encrypting value")
            null
        }
    }

    fun decrypt(encryptedString: String?): String? {
        if (encryptedString.isNullOrBlank()) return null
        return try {
            val decoded = Base64.decode(encryptedString, Base64.NO_WRAP)
            if (decoded.size < 12) return null // GCM IV is 12 bytes
            
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = decoded.copyOfRange(0, 12)
            val encryptedContent = decoded.copyOfRange(12, decoded.size)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            String(cipher.doFinal(encryptedContent))
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Error decrypting value")
            null
        }
    }

    fun encryptInt(value: Int): String? = encrypt(value.toString())
    fun decryptInt(encryptedString: String?): Int? = decrypt(encryptedString)?.toIntOrNull()
}
