package com.technicallyrural.junction.matrix

import kotlinx.coroutines.flow.Flow

/**
 * Interface for Matrix presence and status updates.
 *
 * This service manages:
 * - Device connectivity status (data/WiFi connection)
 * - Cell signal strength
 * - Periodic heartbeat updates to the control room
 *
 * Implementation is provided by matrix-impl module.
 */
interface MatrixPresenceService {

    /**
     * Current device status.
     */
    val deviceStatus: Flow<DeviceStatus>

    /**
     * Start monitoring device status and sending periodic updates.
     *
     * This will:
     * 1. Register connectivity listeners
     * 2. Start periodic heartbeat timer (every 5 minutes)
     * 3. Send immediate status update
     */
    suspend fun startMonitoring()

    /**
     * Stop monitoring and sending updates.
     */
    suspend fun stopMonitoring()

    /**
     * Send an immediate status update to the control room.
     *
     * This bypasses the periodic timer and sends a status event right away.
     * Useful for significant connectivity changes.
     */
    suspend fun sendImmediateUpdate()

    /**
     * Get or create the control room for status updates.
     *
     * The control room is where custom state events are sent.
     * It's typically a private room with the bridge bot user.
     *
     * @return Room ID of the control room, or null if creation failed
     */
    suspend fun getOrCreateControlRoom(): String?

    /**
     * Check if monitoring is currently active.
     */
    fun isMonitoring(): Boolean
}

/**
 * Current device connectivity and signal status.
 */
data class DeviceStatus(
    val dataConnected: Boolean,
    val connectionType: ConnectionType,
    val cellSignal: Int,           // 0-4 signal strength
    val wifiConnected: Boolean,
    val lastUpdate: Long
)

/**
 * Type of data connection.
 */
enum class ConnectionType {
    NONE,
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN
}

/**
 * Status event sent to Matrix control room.
 */
data class BridgeStatusEvent(
    val dataConnected: Boolean,
    val cellSignal: Int,
    val lastSeen: Long,
    val deviceModel: String,
    val appVersion: String,
    val connectionType: String
) {
    companion object {
        /** Custom Matrix event type for bridge status */
        const val EVENT_TYPE = "org.technicallyrural.bridge.status"

        /** State key uses device ID */
        fun stateKey(deviceId: String) = "device_$deviceId"
    }
}
