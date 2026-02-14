package com.technicallyrural.junction.app

import android.util.Log
import com.android.messaging.BugleApplication
import com.technicallyrural.junction.app.matrix.MatrixConfigRepository
import com.technicallyrural.junction.app.observer.OutboundMessageObserverImpl
import com.technicallyrural.junction.app.service.MatrixSyncService
import com.technicallyrural.junction.core.CoreSmsRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Junction application class extending AOSP BugleApplication.
 *
 * Adds Matrix bridge initialization on app startup:
 * - Checks for stored Matrix credentials
 * - Auto-starts MatrixSyncService if configured
 * - Ensures SMS ↔ Matrix bridging works after app restart
 *
 * Why this is needed:
 * - BugleApplication (in sms-upstream) cannot have Matrix code per architecture
 * - Without auto-start, Matrix bridging breaks after app process restart
 * - User would need to manually open app UI to restart sync
 */
class JunctionApplication : BugleApplication() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        private const val TAG = "JunctionApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Junction application starting")

        // Register outbound message observer for Matrix bridging
        // (called by AOSP SendStatusReceiver via CoreSmsRegistry)
        CoreSmsRegistry.registerOutboundMessageObserver(
            OutboundMessageObserverImpl(this)
        )
        Log.d(TAG, "Registered outbound message observer for Matrix bridging")

        // Register Matrix status indicator injector
        registerActivityLifecycleCallbacks(com.technicallyrural.junction.app.ui.MatrixStatusInjector())

        // Check for Matrix credentials and auto-start service if configured
        autoStartMatrixService()
    }

    /**
     * Auto-start Matrix sync service if credentials exist.
     *
     * This ensures SMS → Matrix bridging works immediately after:
     * - App process restart (after being killed)
     * - Device boot (if app auto-starts)
     * - App update/reinstall (if credentials preserved)
     *
     * Without this, incoming SMS would be dropped with:
     * "MatrixRegistry not initialized, skipping bridge"
     */
    private fun autoStartMatrixService() {
        scope.launch {
            try {
                val configRepo = MatrixConfigRepository.getInstance(this@JunctionApplication)
                val config = configRepo.loadConfig()

                if (config.enabled && config.isAuthenticated()) {
                    Log.d(TAG, "Matrix credentials found - auto-starting sync service")
                    MatrixSyncService.start(this@JunctionApplication)
                } else {
                    Log.d(TAG, "Matrix not configured or disabled - skipping auto-start")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to auto-start Matrix service", e)
                // Don't crash - just log the error and continue
            }
        }
    }
}
