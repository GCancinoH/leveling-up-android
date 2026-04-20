package com.gcancino.levelingup.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

object SecurityUtils {
    private const val PREFS_FILE = "levelingup_secure_prefs"
    private const val DB_KEY_ALIAS = "db_encryption_key"

    fun getOrCreateDatabaseKey(context: Context): ByteArray {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        val storedKey = encryptedPrefs.getString(DB_KEY_ALIAS, null)
        if (storedKey != null) {
            return android.util.Base64.decode(storedKey, android.util.Base64.NO_WRAP)
        }

        val newKey = ByteArray(32)
        SecureRandom().nextBytes(newKey)

        encryptedPrefs.edit()
            .putString(DB_KEY_ALIAS, android.util.Base64.encodeToString(newKey, android.util.Base64.NO_WRAP))
            .apply()

        return newKey
    }

    @Deprecated("Use getOrCreateDatabaseKey(context: Context) instead", ReplaceWith("getOrCreateDatabaseKey(context)"))
    fun getOrCreateDatabaseKey(): ByteArray {
        throw UnsupportedOperationException("Context required for secure key storage")
    }
}
