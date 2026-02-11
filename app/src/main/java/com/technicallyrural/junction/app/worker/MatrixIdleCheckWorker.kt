package com.technicallyrural.junction.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.technicallyrural.junction.matrix.MatrixRegistry
import kotlinx.coroutines.delay

/**
 * WorkManager worker for periodic Matrix sync checks during idle mode.
 *
 * When the MatrixSyncService enters idle mode (screen off + 15 min inactivity),
 * it stops continuous sync to save battery. This worker performs periodic
 * one-time sync checks to ensure messages aren't missed.
 *
 * Frequency: Every 15 minutes during idle mode
 * Duration: Quick sync (~5-10 seconds)
 */
class MatrixIdleCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "MatrixIdleCheckWorker"
        const val WORK_NAME = "matrix_idle_check"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting idle mode Matrix check")

        return try {
            val bridge = MatrixRegistry.matrixBridge
            if (bridge == null) {
                Log.w(TAG, "Matrix bridge not initialized, skipping check")
                return Result.success()
            }

            // Perform a quick sync check
            // The Matrix SDK will process any pending events then return
            // Note: startSync() is continuous, but we'll stop it after a short delay
            bridge.startSync()

            // Wait for sync to process pending events (max 10 seconds)
            delay(10_000)

            // Stop sync to return to idle mode
            bridge.stopSync()

            Log.d(TAG, "Idle check completed")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error during idle check", e)
            // Retry on failure
            Result.retry()
        }
    }
}
