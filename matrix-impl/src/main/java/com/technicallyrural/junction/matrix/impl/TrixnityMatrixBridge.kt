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
import net.folivo.trixnity.client.store.TimelineEvent
import net.folivo.trixnity.core.model.RoomId
import net.folivo.trixnity.core.model.events.StateEventContent
import net.folivo.trixnity.core.model.events.m.room.RoomMessageEventContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
            // Send text body first if present
            if (!messageBody.isNullOrBlank()) {
                client.room.sendMessage(RoomId(roomIdStr)) {
                    text(messageBody)
                }
            }

            // Upload and send each attachment
            for (attachment in attachments) {
                // Determine type from MIME type
                when {
                    attachment.mimeType.startsWith("image/") -> {
                        // TODO: Implement image upload
                        // Pattern:
                        // 1. Read file content
                        // 2. val cacheUri = client.media.prepareUploadMedia(content, contentType)
                        // 3. val mxcUri = client.media.uploadMedia(cacheUri).getOrThrow()
                        // 4. client.room.sendMessage(roomId) { image(mxcUri, ...) }
                    }
                    attachment.mimeType.startsWith("video/") -> {
                        // TODO: Similar to image but with video() DSL
                    }
                    attachment.mimeType.startsWith("audio/") -> {
                        // TODO: Similar with audio() DSL
                    }
                    else -> {
                        // TODO: Generic file upload with file() DSL
                    }
                }
            }

            // Return success with placeholder transaction ID
            // In production, should return the actual transaction IDs
            MatrixSendResult.Success("mms_${System.currentTimeMillis()}")
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
            // Create custom state event content for device status
            val statusContent = DeviceStatusContent(
                dataConnected = dataConnected,
                cellSignal = cellSignal,
                lastSeen = System.currentTimeMillis(),
                deviceModel = android.os.Build.MODEL,
                appVersion = "1.0.0" // TODO: Get from BuildConfig
            )

            // Send state event to control room
            client.api.room.sendStateEvent(
                roomId = RoomId(controlRoomId),
                eventContent = statusContent,
                stateKey = "device_${client.deviceId}"
            )
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
     * Subscribe to incoming Matrix messages from all rooms using Trixnity API.
     *
     * Listens to timeline events from sync, filters for text messages,
     * and emits them for SMS sending.
     */
    private suspend fun subscribeToRoomMessages() {
        val client = clientManager.client ?: return

        scope.launch {
            try {
                // Subscribe to timeline events from sync
                client.room.getTimelineEventsFromNowOn()
                    .collect { timelineEvent ->
                        try {
                            processTimelineEvent(timelineEvent)
                        } catch (e: Exception) {
                            // Log error but continue processing other events
                            e.printStackTrace()
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.Error(
                    message = e.message ?: "Timeline subscription failed",
                    retryable = true
                )
            }
        }
    }

    /**
     * Process a single timeline event and emit if it's a relevant text message.
     */
    private suspend fun processTimelineEvent(timelineEvent: TimelineEvent) {
        val client = clientManager.client ?: return

        // Skip messages from ourselves
        if (timelineEvent.event.sender == client.userId) return

        // Get phone number for this room (only process mapped rooms)
        val phoneNumber = roomMapper.getContactForRoom(timelineEvent.event.roomId.full) ?: return

        // Extract content from Result
        val content = timelineEvent.content?.getOrNull() ?: return

        // Filter for text messages
        if (content is RoomMessageEventContent.TextBased.Text) {
            _inboundMessages.emit(
                MatrixInboundMessage(
                    roomId = timelineEvent.event.roomId.full,
                    eventId = timelineEvent.event.id.full,
                    sender = timelineEvent.event.sender.full,
                    body = content.body,
                    timestamp = timelineEvent.event.originTimestamp,
                    messageType = MatrixMessageType.TEXT
                )
            )
        }
    }
}

/**
 * Custom state event content for device status updates.
 *
 * Event type: org.technicallyrural.bridge.status
 * State key: device_<deviceId>
 */
@Serializable
data class DeviceStatusContent(
    @SerialName("data_connected")
    val dataConnected: Boolean,

    @SerialName("cell_signal")
    val cellSignal: Int,

    @SerialName("last_seen")
    val lastSeen: Long,

    @SerialName("device_model")
    val deviceModel: String,

    @SerialName("app_version")
    val appVersion: String,

    override val externalUrl: String? = null
) : StateEventContent
