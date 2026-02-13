package com.technicallyrural.junction.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.matrix.impl.TrixnityClientManagerSingleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Matrix connection status indicator.
 *
 * Provides real-time connection state derived from:
 * - MatrixSyncService sync state (via TrixnityClientManager)
 * - Matrix configuration (enabled/disabled)
 * - Last message timestamp (from sync events)
 *
 * States:
 * - NOT_CONFIGURED: Matrix credentials not set up
 * - CONFIGURED_NOT_RUNNING: Credentials exist but service not started (bug indicator)
 * - CONNECTING: Service starting, client initializing
 * - CONNECTED: Actively syncing with Matrix server
 * - DISCONNECTED: Service stopped or network error
 */
class MatrixStatusViewModel(application: Application) : AndroidViewModel(application) {

    private val configRepository = MatrixConfigRepository.getInstance(application)
    private val clientManager = TrixnityClientManagerSingleton.getInstance(application)

    private val _status = MutableStateFlow<MatrixStatus>(MatrixStatus.Loading)
    val status: StateFlow<MatrixStatus> = _status.asStateFlow()

    init {
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            // Combine config state and sync state
            val config = configRepository.loadConfig()

            if (!config.enabled || !config.isAuthenticated()) {
                _status.value = MatrixStatus.NotConfigured
                return@launch
            }

            // Observe client initialization state
            launch {
                clientManager.isInitialized.collect { isInitialized ->
                    if (!isInitialized) {
                        _status.value = MatrixStatus.ConfiguredNotRunning(config.serverUrl)
                    }
                }
            }

            // Observe client sync state
            clientManager.isSyncing.collect { isSyncing: Boolean ->
                _status.value = when {
                    isSyncing -> MatrixStatus.Connected(
                        serverUrl = config.serverUrl,
                        lastMessageTime = System.currentTimeMillis() // TODO: Track actual last message
                    )
                    else -> MatrixStatus.Disconnected(config.serverUrl)
                }
            }
        }
    }

    /**
     * Force refresh the connection status.
     */
    fun refresh() {
        observeConnectionState()
    }
}

/**
 * Matrix connection status states.
 */
sealed class MatrixStatus {
    /** Loading initial state */
    object Loading : MatrixStatus()

    /** Matrix credentials not configured */
    object NotConfigured : MatrixStatus()

    /** Configured but service not running (indicates bug) */
    data class ConfiguredNotRunning(val serverUrl: String) : MatrixStatus()

    /** Connected and syncing */
    data class Connected(
        val serverUrl: String,
        val lastMessageTime: Long
    ) : MatrixStatus()

    /** Disconnected or error */
    data class Disconnected(val serverUrl: String) : MatrixStatus()
}
