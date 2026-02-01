package com.example.messaging.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Headless service for sending SMS from other applications.
 */
class HeadlessSmsSendService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Handle headless SMS send
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
