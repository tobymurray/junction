package com.technicallyrural.junction.app.matrix

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Repository for Matrix configuration persistence.
 *
 * Uses EncryptedSharedPreferences to securely store sensitive data like access tokens.
 */
class MatrixConfigRepository(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to regular SharedPreferences if encryption fails
        // This can happen on some devices or if storage is corrupted
        context.getSharedPreferences(PREFS_NAME_FALLBACK, Context.MODE_PRIVATE)
    }

    /**
     * Load the current Matrix configuration.
     */
    fun loadConfig(): MatrixConfig {
        return MatrixConfig(
            serverUrl = sharedPreferences.getString(KEY_SERVER_URL, "") ?: "",
            userId = sharedPreferences.getString(KEY_USER_ID, "") ?: "",
            username = sharedPreferences.getString(KEY_USERNAME, "") ?: "",
            accessToken = sharedPreferences.getString(KEY_ACCESS_TOKEN, "") ?: "",
            deviceId = sharedPreferences.getString(KEY_DEVICE_ID, "") ?: "",
            enabled = sharedPreferences.getBoolean(KEY_ENABLED, false),
            lastConnectedTimestamp = sharedPreferences.getLong(KEY_LAST_CONNECTED, 0L),
            groupShortCodesByService = sharedPreferences.getBoolean(KEY_GROUP_SHORT_CODES, true)
        )
    }

    /**
     * Save Matrix configuration.
     */
    fun saveConfig(config: MatrixConfig) {
        sharedPreferences.edit().apply {
            putString(KEY_SERVER_URL, config.serverUrl)
            putString(KEY_USER_ID, config.userId)
            putString(KEY_USERNAME, config.username)
            putString(KEY_ACCESS_TOKEN, config.accessToken)
            putString(KEY_DEVICE_ID, config.deviceId)
            putBoolean(KEY_ENABLED, config.enabled)
            putLong(KEY_LAST_CONNECTED, config.lastConnectedTimestamp)
            putBoolean(KEY_GROUP_SHORT_CODES, config.groupShortCodesByService)
            apply()
        }
    }

    /**
     * Clear all Matrix configuration.
     */
    fun clearConfig() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Update last connected timestamp to current time.
     */
    fun updateLastConnected() {
        sharedPreferences.edit()
            .putLong(KEY_LAST_CONNECTED, System.currentTimeMillis())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "matrix_config_secure"
        private const val PREFS_NAME_FALLBACK = "matrix_config"

        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_LAST_CONNECTED = "last_connected"
        private const val KEY_GROUP_SHORT_CODES = "group_short_codes_by_service"

        @Volatile
        private var instance: MatrixConfigRepository? = null

        /**
         * Get singleton instance of the repository.
         */
        fun getInstance(context: Context): MatrixConfigRepository {
            return instance ?: synchronized(this) {
                instance ?: MatrixConfigRepository(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
