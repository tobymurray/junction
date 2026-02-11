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
import de.connect2x.trixnity.client.MatrixClient
import de.connect2x.trixnity.client.create
import de.connect2x.trixnity.client.CryptoDriverModule
import de.connect2x.trixnity.client.MediaStoreModule
import de.connect2x.trixnity.client.RepositoriesModule
import de.connect2x.trixnity.client.media.inMemory
import de.connect2x.trixnity.client.store.repository.inMemory
import de.connect2x.trixnity.clientserverapi.client.MatrixClientAuthProviderData
import de.connect2x.trixnity.clientserverapi.client.classicLoginWithPassword
import de.connect2x.trixnity.clientserverapi.model.authentication.IdentifierType
import de.connect2x.trixnity.crypto.driver.CryptoDriver
import de.connect2x.trixnity.crypto.driver.vodozemac.VodozemacCryptoDriver
import org.koin.dsl.module

/**
 * Real implementation of Matrix client manager using Trixnity SDK 5.x.
 *
 * This replaces StubMatrixClientManager with actual Trixnity integration.
 * Handles client lifecycle, authentication, and sync management.
 *
 * **Changes in 5.x:**
 * - Package renamed from net.folivo to de.connect2x
 * - New authentication system with MatrixClientAuthProviderData
 * - MatrixClient.create() replaces login() and fromStore()
 * - CryptoDriverModule is now required
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

    // Reusable modules - using new 5.x factory pattern
    private val repositoriesModule = RepositoriesModule.inMemory()
    private val mediaStoreModule = MediaStoreModule.inMemory()
    private val cryptoDriverModule = CryptoDriverModule {
        module { single<CryptoDriver> { VodozemacCryptoDriver } }
    }

    /**
     * Initialize MatrixClient from stored credentials.
     *
     * In Trixnity 5.x, this uses MatrixClient.create() without authProviderData.
     * The client will attempt to restore from stored session data.
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
            // In 5.x, MatrixClient.create() with authProviderData=null will restore from store
            val result = MatrixClient.create(
                repositoriesModule = repositoriesModule,
                mediaStoreModule = mediaStoreModule,
                cryptoDriverModule = cryptoDriverModule,
                authProviderData = null, // null = restore from store
                coroutineContext = Dispatchers.IO
            )

            result.fold(
                onSuccess = { matrixClient ->
                    _client = matrixClient
                    _isInitialized.value = true
                    true
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
     * In Trixnity 5.x, this uses a two-step process:
     * 1. MatrixClientAuthProviderData.classicLoginWithPassword() to get auth data
     * 2. MatrixClient.create() with the auth data
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
            // Step 1: Perform classic password login to get auth provider data
            val authProviderData = MatrixClientAuthProviderData.classicLoginWithPassword(
                baseUrl = Url(serverUrl),
                identifier = IdentifierType.User(username),
                password = password,
                initialDeviceDisplayName = "Junction SMS Bridge"
            ).getOrElse { error ->
                return LoginResult.Error(error.message ?: "Login failed")
            }

            // Step 2: Create MatrixClient with the auth provider data
            val matrixClient = MatrixClient.create(
                repositoriesModule = repositoriesModule,
                mediaStoreModule = mediaStoreModule,
                cryptoDriverModule = cryptoDriverModule,
                authProviderData = authProviderData,
                coroutineContext = Dispatchers.IO
            ).getOrElse { error ->
                return LoginResult.Error(error.message ?: "Client creation failed")
            }

            _client = matrixClient
            _isInitialized.value = true

            LoginResult.Success(
                userId = matrixClient.userId.full,
                accessToken = authProviderData.accessToken,
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
        scope.launch {
            _client?.stopSync()
            _isSyncing.value = false
        }
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
