package com.technicallyrural.junction.matrix.impl

import android.content.Context
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.MatrixClient
import net.folivo.trixnity.client.fromStore
import net.folivo.trixnity.client.login
import net.folivo.trixnity.client.media.createInMemoryMediaStoreModule
import net.folivo.trixnity.client.store.repository.createInMemoryRepositoriesModule
import net.folivo.trixnity.clientserverapi.model.authentication.IdentifierType

/**
 * Real implementation of Matrix client manager using Trixnity SDK.
 *
 * This replaces StubMatrixClientManager with actual Trixnity integration.
 * Handles client lifecycle, authentication, and sync management.
 */
class TrixnityClientManager(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var _client: MatrixClient? = null
    val client: MatrixClient? get() = _client

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: Flow<Boolean> = _isInitialized.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: Flow<Boolean> = _isSyncing.asStateFlow()

    /**
     * Initialize MatrixClient from stored credentials.
     *
     * Trixnity stores session data in its repositories automatically after login.
     * This method attempts to restore the session from those stored credentials.
     *
     * Note: The parameters are kept for interface compatibility but not used directly.
     * Trixnity loads credentials from its internal AccountStore.
     *
     * @return true if initialization succeeded, false if no stored session exists
     */
    suspend fun initializeFromStore(
        serverUrl: String,
        userId: String,
        deviceId: String,
        accessToken: String
    ): Boolean {
        return try {
            // Attempt to restore client from Trixnity's internal storage
            val result = MatrixClient.fromStore(
                repositoriesModule = createRepositoriesModule(),
                mediaStoreModule = createInMemoryMediaStoreModule()
            )

            result.fold(
                onSuccess = { matrixClient ->
                    if (matrixClient != null) {
                        _client = matrixClient
                        _isInitialized.value = true
                        true
                    } else {
                        // No stored session found
                        _isInitialized.value = false
                        false
                    }
                },
                onFailure = { error ->
                    error.printStackTrace()
                    _isInitialized.value = false
                    false
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _isInitialized.value = false
            false
        }
    }

    /**
     * Login with username and password.
     *
     * @param serverUrl Homeserver URL
     * @param username Username (without @ or :server)
     * @param password User password
     * @return LoginResult with credentials or error
     */
    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): LoginResult {
        return try {
            // Create Matrix client with login credentials using v4.22.7 API
            val matrixClient = MatrixClient.login(
                baseUrl = Url(serverUrl),
                identifier = IdentifierType.User(username),
                password = password,
                initialDeviceDisplayName = "Junction SMS Bridge",
                repositoriesModule = createRepositoriesModule(),
                mediaStoreModule = createInMemoryMediaStoreModule()
            ).getOrElse { error ->
                return LoginResult.Error(error.message ?: "Login failed")
            }

            _client = matrixClient
            _isInitialized.value = true

            LoginResult.Success(
                userId = matrixClient.userId.full,
                accessToken = "", // Note: Not directly exposed in v4.22.7 interface
                deviceId = matrixClient.deviceId
            )
        } catch (e: Exception) {
            e.printStackTrace()
            LoginResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * Start the Matrix sync loop.
     * This is a blocking call that should run in a background service.
     */
    fun startSync() {
        val matrixClient = _client ?: return

        scope.launch {
            try {
                _isSyncing.value = true
                matrixClient.startSync() // Blocking call - runs until stopped
            } catch (e: Exception) {
                e.printStackTrace()
                _isSyncing.value = false
            }
        }
    }

    /**
     * Stop the Matrix sync loop.
     */
    fun stopSync() {
        _client?.close() // Closes the client and stops sync
        _isSyncing.value = false
    }


    /**
     * Create repositories module for storing Matrix state.
     *
     * Currently using in-memory repositories. For production, should use
     * trixnity-client-repository-room for persistent storage.
     */
    private fun createRepositoriesModule() = createInMemoryRepositoriesModule()

    /**
     * Extract domain from server URL.
     */
    private fun extractDomain(serverUrl: String): String {
        return serverUrl
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore(":")
            .substringBefore("/")
    }

    sealed class LoginResult {
        data class Success(
            val userId: String,
            val accessToken: String,
            val deviceId: String
        ) : LoginResult()

        data class Error(val message: String) : LoginResult()
    }
}
