package com.technicallyrural.junction.matrix.impl

import android.content.Context

/**
 * Singleton wrapper for TrixnityClientManager.
 *
 * This ensures the same client manager instance (and its in-memory repositories)
 * is shared across the app (Activity and Service).
 *
 * This is a temporary workaround until we implement persistent Room-based repositories.
 *
 * Usage:
 * ```
 * val clientManager = TrixnityClientManagerSingleton.getInstance(context)
 * ```
 */
object TrixnityClientManagerSingleton {

    @Volatile
    private var instance: TrixnityClientManager? = null

    fun getInstance(context: Context): TrixnityClientManager {
        return instance ?: synchronized(this) {
            instance ?: TrixnityClientManager(context.applicationContext).also {
                instance = it
            }
        }
    }

    fun clearInstance() {
        synchronized(this) {
            instance = null
        }
    }
}
