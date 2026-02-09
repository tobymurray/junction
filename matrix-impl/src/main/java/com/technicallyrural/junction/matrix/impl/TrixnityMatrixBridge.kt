package com.technicallyrural.junction.matrix.impl

import android.content.Context
import com.technicallyrural.junction.matrix.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import net.folivo.trixnity.client.room
import net.folivo.trixnity.client.room.message.text
import net.folivo.trixnity.core.model.RoomId

/**
 * Real implementation of MatrixBridge using Trixnity SDK.
 *
 * Handles bidirectional SMS↔Matrix message bridging:
 * - SMS → Matrix: Create/find room for phone number, send message
 * - Matrix → SMS: Subscribe to timeline events, emit for SMS sending
 */
class TrixnityMatrixBridge(
    private val context: Context,
    private val clientManager: TrixnityClientManager,
    private val roomMapper: MatrixRoomMapper
) : MatrixBridge {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: Flow<ConnectionState> = _connectionState.asStateFlow()

    private val _inboundMessages = MutableSharedFlow<MatrixInboundMessage>(
        extraBufferCapacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )

    private var controlRoomIdCached: String? = null

    init {
        // Subscribe to sync state changes
        scope.launch {
            clientManager.isSyncing.collect { syncing ->
                _connectionState.value = if (syncing) {
                    ConnectionState.Connected
                } else {
                    ConnectionState.Disconnected
                }
            }
        }

        // Subscribe to incoming messages when client is ready
        scope.launch {
            clientManager.isInitialized.collect { initialized ->
                if (initialized) {
                    subscribeToRoomMessages()
                }
            }
        }
    }

    override suspend fun sendToMatrix(
        phoneNumber: String,
        messageBody: String,
        timestamp: Long,
        isGroup: Boolean
    ): MatrixSendResult {
        val client = clientManager.client
            ?: return MatrixSendResult.Failure(MatrixSendError.NOT_CONNECTED)

        // Get or create room for this contact
        val roomIdStr = roomMapper.getRoomForContact(phoneNumber)
            ?: return MatrixSendResult.Failure(MatrixSendError.ROOM_CREATION_FAILED)

        return try {
            // Send text message using Trixnity v4.22.7 API
            // Returns transaction ID (String), not EventId
            val transactionId = client.room.sendMessage(RoomId(roomIdStr)) {
                text(messageBody)
            }

            MatrixSendResult.Success(transactionId)
        } catch (e: Exception) {
            e.printStackTrace()
            MatrixSendResult.Failure(MatrixSendError.SEND_FAILED)
        }
    }

    override suspend fun sendMmsToMatrix(
        phoneNumber: String,
        messageBody: String?,
        attachments: List<MatrixAttachment>,
        timestamp: Long
    ): MatrixSendResult {
        val client = clientManager.client
            ?: return MatrixSendResult.Failure(MatrixSendError.NOT_CONNECTED)

        val roomIdStr = roomMapper.getRoomForContact(phoneNumber)
            ?: return MatrixSendResult.Failure(MatrixSendError.ROOM_CREATION_FAILED)

        return try {
            // TODO: Implement MMS with media upload
            // 1. Upload media files: client.media.upload(file)
            // 2. Send messages with media refs

            MatrixSendResult.Failure(MatrixSendError.SEND_FAILED)
        } catch (e: Exception) {
            e.printStackTrace()
            MatrixSendResult.Failure(MatrixSendError.SEND_FAILED)
        }
    }

    override fun observeMatrixMessages(): Flow<MatrixInboundMessage> {
        return _inboundMessages.asSharedFlow()
    }

    override suspend fun updatePresence(dataConnected: Boolean, cellSignal: Int) {
        val client = clientManager.client ?: return
        val controlRoomId = controlRoomIdCached ?: return

        try {
            // TODO: Send custom state event to control room
            // Expected: client.room.sendStateEvent(roomId, type, key, content)
            // Needs API verification
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getControlRoomId(): String? {
        if (controlRoomIdCached != null) {
            return controlRoomIdCached
        }

        val client = clientManager.client ?: return null

        // TODO: Create or find control room with alias #junction_control:<server>
        // For now, return null (control room creation is optional)
        return null
    }

    override suspend fun startSync() {
        clientManager.startSync()
    }

    override suspend fun stopSync() {
        clientManager.stopSync()
    }

    override fun isSyncing(): Boolean {
        return (_connectionState.value is ConnectionState.Connected)
    }

    /**
     * Subscribe to incoming Matrix messages from all rooms.
     *
     * TODO: Implement with verified Trixnity 4.22.7 API
     * Expected flow:
     * 1. Subscribe to timeline events: client.room.getTimeline() or similar
     * 2. Filter for text messages in mapped rooms
     * 3. Skip messages from bridge user
     * 4. Emit for SMS sending
     */
    private suspend fun subscribeToRoomMessages() {
        val client = clientManager.client ?: return

        scope.launch {
            try {
                // TODO: Implement timeline subscription
                // Trixnity API verification needed
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.Error(
                    message = e.message ?: "Timeline subscription failed",
                    retryable = true
                )
            }
        }
    }
}
