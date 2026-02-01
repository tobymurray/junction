package com.example.messaging.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives SMS_RECEIVED broadcasts (fallback when not default SMS app).
 */
class SmsReceivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Handle SMS_RECEIVED (lower priority than SMS_DELIVER)
    }
}
