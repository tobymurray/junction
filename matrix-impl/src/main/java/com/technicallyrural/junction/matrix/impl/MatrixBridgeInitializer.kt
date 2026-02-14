package com.technicallyrural.junction.matrix.impl

import android.content.Context
import com.technicallyrural.junction.matrix.MatrixBridge
import com.technicallyrural.junction.matrix.MatrixPresenceService
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.MatrixRoomMapper

/**
 * Initializer for Matrix bridge components.
 *
 * Call this from the app module when Matrix is enabled to set up all
 * Matrix bridge implementations and register them with MatrixRegistry.
 */
object MatrixBridgeInitializer {

    private var clientManager: TrixnityClientManager? = null
    private var bridge: MatrixBridge? = null
    private var roomMapper: MatrixRoomMapper? = null
    private var presenceService: MatrixPresenceService? = null

    /**
     * Initialize Matrix bridge from stored configuration.
     *
     * @param context Android context
     * @param serverUrl Homeserver URL
     * @param userId Matrix user ID
     * @param deviceId Device ID
     * @param accessToken Access token
     * @param homeserverDomain Domain for room alias creation (e.g., "matrix.org")
     * @param enableServiceGrouping Whether to group short codes by service (default: true)
     * @return true if initialization succeeded
     */
    suspend fun initialize(
        context: Context,
        serverUrl: String,
        userId: String,
        deviceId: String,
        accessToken: String,
        homeserverDomain: String,
        enableServiceGrouping: Boolean = true
    ): Boolean {
        // Create client manager
        val manager = TrixnityClientManager(context)
        val success = manager.initializeFromStore(
            serverUrl = serverUrl,
            userId = userId,
            deviceId = deviceId,
            accessToken = accessToken
        )

        if (!success) {
            return false
        }

        // Create room mapper
        val mapper = SimpleRoomMapper(
            context = context,
            clientManager = manager,
            homeserverDomain = homeserverDomain,
            enableServiceGrouping = enableServiceGrouping
        )

        // Create bridge
        val matrixBridge = TrixnityMatrixBridge(
            context = context,
            clientManager = manager,
            roomMapper = mapper
        )

        // Create presence service (stub for now)
        val presence = StubPresenceService()

        // Register with MatrixRegistry
        MatrixRegistry.initialize(
            matrixBridge = matrixBridge,
            roomMapper = mapper,
            presenceService = presence
        )

        // Store references
        clientManager = manager
        bridge = matrixBridge
        roomMapper = mapper
        presenceService = presence

        return true
    }

    /**
     * Login with username and password.
     *
     * @param enableServiceGrouping Whether to group short codes by service (default: true)
     * @return LoginResult with credentials or error
     */
    suspend fun login(
        context: Context,
        serverUrl: String,
        username: String,
        password: String,
        homeserverDomain: String,
        enableServiceGrouping: Boolean = true
    ): TrixnityClientManager.LoginResult {
        val manager = TrixnityClientManager(context)
        val result = manager.login(serverUrl, username, password)

        if (result is TrixnityClientManager.LoginResult.Success) {
            // Initialize bridge after successful login
            val mapper = SimpleRoomMapper(
                context = context,
                clientManager = manager,
                homeserverDomain = homeserverDomain,
                enableServiceGrouping = enableServiceGrouping
            )

            val matrixBridge = TrixnityMatrixBridge(
                context = context,
                clientManager = manager,
                roomMapper = mapper
            )

            val presence = StubPresenceService()

            MatrixRegistry.initialize(
                matrixBridge = matrixBridge,
                roomMapper = mapper,
                presenceService = presence
            )

            clientManager = manager
            bridge = matrixBridge
            roomMapper = mapper
            presenceService = presence
        }

        return result
    }

    /**
     * Shutdown and clear all Matrix components.
     */
    suspend fun shutdown() {
        clientManager?.stopSync()
        clientManager = null

        MatrixRegistry.clear()

        bridge = null
        roomMapper = null
        presenceService = null
    }

    /**
     * Get the client manager for direct access (e.g., from a Service).
     */
    fun getClientManager(): TrixnityClientManager? = clientManager
}

/**
 * Stub implementation of MatrixPresenceService.
 * TODO: Implement real presence tracking.
 */
private class StubPresenceService : MatrixPresenceService {

    private val _deviceStatus = kotlinx.coroutines.flow.MutableStateFlow(
        com.technicallyrural.junction.matrix.DeviceStatus(
            dataConnected = false,
            connectionType = com.technicallyrural.junction.matrix.ConnectionType.NONE,
            cellSignal = 0,
            wifiConnected = false,
            lastUpdate = 0L
        )
    )

    override val deviceStatus: kotlinx.coroutines.flow.Flow<com.technicallyrural.junction.matrix.DeviceStatus>
        get() = _deviceStatus

    override suspend fun startMonitoring() {
        // Stub - real implementation would register connectivity listeners
    }

    override suspend fun stopMonitoring() {
        // Stub
    }

    override suspend fun sendImmediateUpdate() {
        // Stub - use MatrixBridge.updatePresence() for custom state events
    }

    override suspend fun getOrCreateControlRoom(): String? {
        // Stub - returns null for now
        return null
    }

    override fun isMonitoring(): Boolean {
        return false
    }
}
