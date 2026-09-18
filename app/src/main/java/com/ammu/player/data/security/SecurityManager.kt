package com.ammu.player.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages encrypted application preferences and security credentials using Jetpack Security.
 */
class SecurityManager(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ammu_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getSavedMasterKey(): String? = securePrefs.getString(KEY_MASTER, null)
    fun setSavedMasterKey(key: String) = securePrefs.edit().putString(KEY_MASTER, key).apply()

    fun getSavedAesKey(): String? = securePrefs.getString(KEY_AES, null)
    fun setSavedAesKey(key: String) = securePrefs.edit().putString(KEY_AES, key).apply()

    fun getSavedCreatorPasskey(): String? = securePrefs.getString(KEY_CREATOR, null)
    fun setSavedCreatorPasskey(key: String) = securePrefs.edit().putString(KEY_CREATOR, key).apply()

    fun getSavedDownloadKey(): String? = securePrefs.getString(KEY_DOWNLOAD, null)
    fun setSavedDownloadKey(key: String) = securePrefs.edit().putString(KEY_DOWNLOAD, key).apply()

    companion object {
        private const val KEY_MASTER = "sec_master_key"
        private const val KEY_AES = "sec_aes_key"
        private const val KEY_CREATOR = "sec_creator_passkey"
        private const val KEY_DOWNLOAD = "sec_download_key"
    }
}
