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
import com.technicallyrural.junction.matrix.impl.SimpleRoomMapper
import com.technicallyrural.junction.matrix.impl.TrixnityClientManager
import com.technicallyrural.junction.matrix.impl.TrixnityMatrixBridge
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
    private lateinit var bridge: TrixnityMatrixBridge
    private lateinit var configRepository: MatrixConfigRepository

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "MatrixSyncService created")

        configRepository = MatrixConfigRepository.getInstance(this)
        clientManager = TrixnityClientManager(applicationContext)

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

                bridge = TrixnityMatrixBridge(
                    context = applicationContext,
                    clientManager = clientManager,
                    roomMapper = roomMapper
                )

                // Start sync
                updateNotification("Connected to ${config.serverUrl}")
                Log.d(TAG, "Starting Matrix sync...")

                bridge.startSync()

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
     * Subscribe to incoming Matrix messages and bridge to SMS.
     */
    private fun subscribeToMatrixMessages() {
        scope.launch {
            bridge.observeMatrixMessages().collect { matrixMessage ->
                try {
                    Log.d(TAG, "Received Matrix message: ${matrixMessage.body} from ${matrixMessage.sender}")

                    // TODO: Bridge to SMS
                    // 1. Extract phone number from room mapping
                    // 2. Use SmsTransport to send SMS
                    // 3. Handle send result

                    // For now, just log
                    Log.d(TAG, "Matrix → SMS bridge not yet implemented")

                } catch (e: Exception) {
                    Log.e(TAG, "Error processing Matrix message", e)
                }
            }
        }
    }

    /**
     * Send periodic presence updates to control room.
     */
    private fun startPresenceUpdates() {
        presenceJob?.cancel()

        presenceJob = scope.launch {
            while (isActive) {
                try {
                    // TODO: Get actual connectivity status
                    val dataConnected = true // Placeholder
                    val cellSignal = 4 // Placeholder (0-4 bars)

                    bridge.updatePresence(dataConnected, cellSignal)
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
                bridge.stopSync()
                Log.d(TAG, "Matrix sync stopped")
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
}
