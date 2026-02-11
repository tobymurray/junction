package com.technicallyrural.junction.matrix

import kotlinx.coroutines.flow.Flow

/**
 * Interface for Matrix bridge operations.
 *
 * This interface abstracts the Matrix SDK implementation from the app layer.
 * The app should NEVER directly use Matrix SDK classes.
 *
 * Implementation is provided by matrix-impl module.
 */
interface MatrixBridge {

    /**
     * Current connection state of the Matrix client.
     */
    val connectionState: Flow<ConnectionState>

    /**
     * Send an SMS message to Matrix.
     *
     * This creates or finds the appropriate Matrix room for the phone number,
     * then sends the message content as a Matrix message.
     *
     * @param phoneNumber E.164 formatted phone number
     * @param messageBody SMS message content
     * @param timestamp Message timestamp (milliseconds since epoch)
     * @param isGroup Whether this is a group message
     * @return Result indicating success or failure
     */
    suspend fun sendToMatrix(
        phoneNumber: String,
        messageBody: String,
        timestamp: Long,
        isGroup: Boolean = false
    ): MatrixSendResult

    /**
     * Send an MMS message to Matrix.
     *
     * @param phoneNumber E.164 formatted phone number
     * @param messageBody Optional text content
     * @param attachments List of media attachments
     * @param timestamp Message timestamp
     * @return Result indicating success or failure
     */
    suspend fun sendMmsToMatrix(
        phoneNumber: String,
        messageBody: String?,
        attachments: List<MatrixAttachment>,
        timestamp: Long
    ): MatrixSendResult

    /**
     * Observe incoming Matrix messages that should be bridged to SMS.
     *
     * This flow emits messages from Matrix rooms that are mapped to phone numbers.
     * The app layer is responsible for sending these as actual SMS via SmsTransport.
     */
    fun observeMatrixMessages(): Flow<MatrixInboundMessage>

    /**
     * Update device presence/status in Matrix.
     *
     * Sends a custom state event to the control room with connectivity info.
     *
     * @param dataConnected Whether mobile data/WiFi is connected
     * @param cellSignal Cell signal strength (0-4)
     */
    suspend fun updatePresence(dataConnected: Boolean, cellSignal: Int)

    /**
     * Get the current control room ID.
     * The control room is where status updates are sent.
     */
    suspend fun getControlRoomId(): String?

    /**
     * Start the Matrix sync loop.
     * This should be called when the service starts.
     */
    suspend fun startSync()

    /**
     * Stop the Matrix sync loop.
     * This should be called when the service stops.
     */
    suspend fun stopSync()

    /**
     * Check if the client is currently syncing.
     */
    fun isSyncing(): Boolean
}

/**
 * Connection state of the Matrix client.
 */
sealed class ConnectionState {
    /** Not connected */
    data object Disconnected : ConnectionState()

    /** Connecting to homeserver */
    data object Connecting : ConnectionState()

    /** Connected and syncing */
    data object Connected : ConnectionState()

    /** Connection error */
    data class Error(val message: String, val retryable: Boolean) : ConnectionState()
}

/**
 * Result of sending a message to Matrix.
 */
sealed class MatrixSendResult {
    /** Message sent successfully */
    data class Success(
        val eventId: String,
        val roomId: String? = null
    ) : MatrixSendResult()

    /** Message send failed */
    data class Failure(val error: MatrixSendError) : MatrixSendResult()
}

/**
 * Errors that can occur when sending to Matrix.
 */
enum class MatrixSendError {
    NOT_CONNECTED,
    ROOM_NOT_FOUND,
    ROOM_CREATION_FAILED,
    SEND_FAILED,
    INVALID_PHONE_NUMBER,
    UNAUTHORIZED,
    UNKNOWN
}

/**
 * An incoming message from Matrix that should be bridged to SMS.
 */
data class MatrixInboundMessage(
    val roomId: String,
    val eventId: String,
    val sender: String,
    val body: String,
    val timestamp: Long,
    val messageType: MatrixMessageType
)

/**
 * Type of Matrix message.
 */
enum class MatrixMessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    EMOTE,
    NOTICE
}

/**
 * Attachment for MMS messages sent to Matrix.
 */
data class MatrixAttachment(
    val uri: String,
    val mimeType: String,
    val filename: String?,
    val size: Long
)
