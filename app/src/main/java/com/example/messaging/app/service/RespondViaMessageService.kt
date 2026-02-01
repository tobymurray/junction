package com.example.messaging.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Service for handling RESPOND_VIA_MESSAGE intents.
 *
 * This allows quick replies from the call screen when declining a call.
 */
class RespondViaMessageService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: Handle respond via message
        stopSelf(startId)
        return START_NOT_STICKY
    }
}
