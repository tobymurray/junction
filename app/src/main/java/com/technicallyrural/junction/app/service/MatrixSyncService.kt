package com.technicallyrural.junction.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.technicallyrural.junction.app.R
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.app.ui.MatrixConfigActivity
import com.technicallyrural.junction.matrix.MatrixRegistry
import com.technicallyrural.junction.matrix.impl.SimpleRoomMapper
import com.technicallyrural.junction.matrix.impl.TrixnityClientManager
import com.technicallyrural.junction.matrix.impl.TrixnityClientManagerSingleton
import com.technicallyrural.junction.matrix.impl.TrixnityMatrixBridge
import com.technicallyrural.junction.core.CoreSmsRegistry
import com.technicallyrural.junction.core.transport.SendResult
import com.technicallyrural.junction.persistence.repository.MessageRepository
import com.technicallyrural.junction.persistence.repository.RoomMappingRepository
import com.technicallyrural.junction.persistence.util.AospThreadIdExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that maintains Matrix sync connection.
 *
 * Responsibilities:
 * - Keep Matrix sync loop running in background
 * - Bridge incoming Matrix messages to SMS
 * - Send periodic presence/status updates
 * - Maintain persistent notification
 *
 * Lifecycle:
 * - Started when Matrix is enabled in settings
 * - Stopped when Matrix is disabled or user logs out
 * - Auto-restarts via START_STICKY if killed by system
 */
class MatrixSyncService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncJob: Job? = null
    private var presenceJob: Job? = null

    private lateinit var clientManager: TrixnityClientManager
    private var bridge: TrixnityMatrixBridge? = null
    private lateinit var configRepository: MatrixConfigRepository

    companion object {
        private const val TAG = "MatrixSyncService"
        private const val CHANNEL_ID = "matrix_sync"
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START_SYNC = "com.technicallyrural.junction.START_MATRIX_SYNC"
        const val ACTION_STOP_SYNC = "com.technicallyrural.junction.STOP_MATRIX_SYNC"

        /**
         * Start Matrix sync service.
         */
        fun start(context: Context) {
            val intent = Intent(context, MatrixSyncService::class.java).apply {
                action = ACTION_START_SYNC
            }
            context.startForegroundService(intent)
        }

        /**
         * Stop Matrix sync service.
         */
        fun stop(context: Context) {
            val intent = Intent(context, MatrixSyncService::class.java).apply {
                action = ACTION_STOP_SYNC
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MatrixSyncService created")

        configRepository = MatrixConfigRepository.getInstance(this)
        clientManager = TrixnityClientManagerSingleton.getInstance(this)

        // Create notification channel
        createNotificationChannel()

        // Start foreground with notification
        startForeground(NOTIFICATION_ID, createNotification("Connecting..."))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "MatrixSyncService started")

        when (intent?.action) {
            ACTION_START_SYNC -> startMatrixSync()
            ACTION_STOP_SYNC -> stopSelf()
            else -> startMatrixSync() // Default action
        }

        // START_STICKY ensures service restarts if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "MatrixSyncService destroyed")
        stopMatrixSync()
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Initialize Matrix client and start sync loop.
     */
    private fun startMatrixSync() {
        syncJob?.cancel()

        syncJob = scope.launch {
            try {
                val config = configRepository.loadConfig()

                // Initialize client from stored session
                val initialized = clientManager.initializeFromStore(
                    serverUrl = config.serverUrl,
                    userId = config.userId,
                    deviceId = config.deviceId,
                    accessToken = config.accessToken
                )

                if (!initialized) {
                    Log.e(TAG, "Failed to initialize Matrix client from store")
                    updateNotification("Connection failed - not logged in")
                    stopSelf()
                    return@launch
                }

                // Initialize bridge
                val serverDomain = config.serverUrl
                    .removePrefix("https://")
                    .removePrefix("http://")
                    .substringBefore(":")
                    .substringBefore("/")

                val roomMapper = SimpleRoomMapper(
                    context = applicationContext,
                    clientManager = clientManager,
                    homeserverDomain = serverDomain
                )

                val matrixBridge = TrixnityMatrixBridge(
                    context = applicationContext,
                    clientManager = clientManager,
                    roomMapper = roomMapper
                )

                // Store bridge instance
                bridge = matrixBridge

                // Register bridge in MatrixRegistry for app-wide access
                // Use bridge for presence service (it has updatePresence method)
                val presenceService = BridgePresenceServiceAdapter(matrixBridge)
                MatrixRegistry.initialize(
                    matrixBridge = matrixBridge,
                    roomMapper = roomMapper,
                    presenceService = presenceService
                )

                Log.d(TAG, "MatrixRegistry initialized")

                // Start sync
                updateNotification("Connected to ${config.serverUrl}")
                Log.d(TAG, "Starting Matrix sync...")

                bridge?.startSync()

                // Subscribe to incoming messages
                subscribeToMatrixMessages()

                // Start presence updates
                startPresenceUpdates()

            } catch (e: Exception) {
                Log.e(TAG, "Error starting Matrix sync", e)
                updateNotification("Sync error: ${e.message}")
                stopSelf()
            }
        }
    }

    /**
     * Subscribe to incoming Matrix messages and bridge to SMS with persistent deduplication.
     */
    private fun subscribeToMatrixMessages() {
        val bridgeInstance = bridge ?: return

        scope.launch {
            bridgeInstance.observeMatrixMessages().collect { matrixMessage ->
                try {
                    Log.d(TAG, "Matrix message from ${matrixMessage.sender}, eventId=${matrixMessage.eventId}")

                    // Get conversation ID from room mapping
                    val roomRepo = RoomMappingRepository.getInstance(applicationContext)
                    val conversationId = roomRepo.getConversationForRoom(matrixMessage.roomId)
                    if (conversationId == null) {
                        Log.w(TAG, "No conversation mapping for room ${matrixMessage.roomId}")
                        return@collect
                    }

                    // Get participants for this conversation
                    val participants = roomRepo.getParticipants(conversationId)
                    if (participants.isNullOrEmpty()) {
                        Log.w(TAG, "No participants for conversation $conversationId")
                        return@collect
                    }

                    // Get our phone number
                    val ownNumber = AospThreadIdExtractor.getOwnPhoneNumber(applicationContext) ?: "unknown"

                    // Initialize repository
                    val messageRepo = MessageRepository.getInstance(applicationContext)

                    // Record send attempt (with deduplication by event ID)
                    val record = messageRepo.recordMatrixToSmsSend(
                        matrixEventId = matrixMessage.eventId,
                        matrixRoomId = matrixMessage.roomId,
                        conversationId = conversationId,
                        senderAddress = matrixMessage.sender,
                        recipientAddresses = participants.filter { it != ownNumber },
                        body = matrixMessage.body,
                        timestamp = matrixMessage.timestamp,
                        isGroup = participants.size > 1
                    )

                    if (record == null) {
                        Log.w(TAG, "Duplicate Matrix event detected (id=${matrixMessage.eventId}), skipping SMS send")
                        return@collect
                    }

                    // For now, send to first participant (1:1 SMS)
                    // TODO: Group SMS support in Phase 1
                    val recipient = participants.firstOrNull() ?: return@collect

                    Log.d(TAG, "Matrix → SMS: $recipient (eventId=${matrixMessage.eventId})")

                    // Send SMS via CoreSmsRegistry
                    if (!CoreSmsRegistry.isInitialized) {
                        Log.e(TAG, "CoreSmsRegistry not initialized, cannot send SMS")
                        messageRepo.recordSmsSendFailure(
                            matrixEventId = matrixMessage.eventId,
                            failureReason = "CoreSmsRegistry not initialized"
                        )
                        return@collect
                    }

                    val result = CoreSmsRegistry.smsTransport.sendSms(
                        destinationAddress = recipient,
                        message = matrixMessage.body
                    )

                    when (result) {
                        is SendResult.Success -> {
                            Log.d(TAG, "Matrix message bridged to SMS: $recipient, eventId=${matrixMessage.eventId}")
                            // Confirm send
                            messageRepo.confirmSmsSend(
                                matrixEventId = matrixMessage.eventId,
                                smsMessageId = result.messageId ?: 0L
                            )
                        }
                        is SendResult.Failure -> {
                            Log.e(TAG, "Failed to send SMS for eventId=${matrixMessage.eventId}: ${result.error}")
                            // Record failure
                            messageRepo.recordSmsSendFailure(
                                matrixEventId = matrixMessage.eventId,
                                failureReason = result.error.name
                            )
                        }
                    }

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Matrix message eventId=${matrixMessage.eventId}", e)
                }
            }
        }
    }

    /**
     * Send periodic presence updates to control room.
     */
    private fun startPresenceUpdates() {
        val bridgeInstance = bridge ?: return

        presenceJob?.cancel()

        presenceJob = scope.launch {
            while (isActive) {
                try {
                    // TODO: Get actual connectivity status
                    val dataConnected = true // Placeholder
                    val cellSignal = 4 // Placeholder (0-4 bars)

                    bridgeInstance.updatePresence(dataConnected, cellSignal)
                    Log.d(TAG, "Sent presence update")

                } catch (e: Exception) {
                    Log.e(TAG, "Error sending presence update", e)
                }

                // Send updates every 5 minutes
                delay(5 * 60 * 1000L)
            }
        }
    }

    /**
     * Stop Matrix sync and cleanup.
     */
    private fun stopMatrixSync() {
        syncJob?.cancel()
        presenceJob?.cancel()

        scope.launch {
            try {
                bridge?.stopSync()
                MatrixRegistry.clear()
                Log.d(TAG, "Matrix sync stopped, registry cleared")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping sync", e)
            }
        }
    }

    /**
     * Create notification channel for service.
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Matrix Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Maintains connection to Matrix homeserver"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create foreground service notification.
     */
    private fun createNotification(statusText: String): Notification {
        val intent = Intent(this, MatrixConfigActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Matrix Bridge Active")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_notification) // TODO: Create icon
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Update notification text.
     */
    private fun updateNotification(statusText: String) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification(statusText))
    }
}

/**
 * Adapter that wraps MatrixBridge to provide MatrixPresenceService interface.
 * Since MatrixBridge has updatePresence method, we delegate to it.
 */
private class BridgePresenceServiceAdapter(
    private val bridge: com.technicallyrural.junction.matrix.MatrixBridge
) : com.technicallyrural.junction.matrix.MatrixPresenceService {

    override val deviceStatus: kotlinx.coroutines.flow.Flow<com.technicallyrural.junction.matrix.DeviceStatus>
        get() = kotlinx.coroutines.flow.flowOf(
            com.technicallyrural.junction.matrix.DeviceStatus(
                dataConnected = false,
                connectionType = com.technicallyrural.junction.matrix.ConnectionType.NONE,
                cellSignal = 0,
                wifiConnected = false,
                lastUpdate = 0L
            )
        )

    override suspend fun sendImmediateUpdate() {
        // Delegate to bridge's updatePresence
        bridge.updatePresence(dataConnected = true, cellSignal = 4)
    }

    override suspend fun getOrCreateControlRoom(): String? {
        return bridge.getControlRoomId()
    }

    override fun isMonitoring(): Boolean {
        return bridge.isSyncing()
    }

    override suspend fun startMonitoring() {
        // No-op: monitoring is handled by sync service
    }

    override suspend fun stopMonitoring() {
        // No-op: monitoring is handled by sync service
    }
}
