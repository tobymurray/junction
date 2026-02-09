package com.technicallyrural.junction.matrix

/**
 * Unified registry for all core-matrix interface implementations.
 *
 * The matrix-impl module registers implementations during app startup.
 * The app module can then access them via property accessors without
 * importing any Matrix SDK classes.
 *
 * This follows the same pattern as CoreSmsRegistry.
 */
object MatrixRegistry {

    private var _matrixBridge: MatrixBridge? = null
    private var _roomMapper: MatrixRoomMapper? = null
    private var _presenceService: MatrixPresenceService? = null

    val isInitialized: Boolean
        get() = _matrixBridge != null

    val matrixBridge: MatrixBridge
        get() = _matrixBridge
            ?: throw IllegalStateException("MatrixRegistry not initialized. Call initialize() first.")

    val roomMapper: MatrixRoomMapper
        get() = _roomMapper
            ?: throw IllegalStateException("MatrixRegistry not initialized. Call initialize() first.")

    val presenceService: MatrixPresenceService
        get() = _presenceService
            ?: throw IllegalStateException("MatrixRegistry not initialized. Call initialize() first.")

    /**
     * Initialize the registry with Matrix implementations.
     * Called once during app startup when Matrix is enabled.
     */
    fun initialize(
        matrixBridge: MatrixBridge,
        roomMapper: MatrixRoomMapper,
        presenceService: MatrixPresenceService
    ) {
        _matrixBridge = matrixBridge
        _roomMapper = roomMapper
        _presenceService = presenceService
    }

    /**
     * Clear all registered implementations.
     * Called when Matrix bridge is disabled or user logs out.
     */
    fun clear() {
        _matrixBridge = null
        _roomMapper = null
        _presenceService = null
    }

    /**
     * Check if Matrix bridge is currently active.
     * Active means initialized AND the bridge is syncing.
     */
    fun isActive(): Boolean {
        return isInitialized && _matrixBridge?.isSyncing() == true
    }
}
