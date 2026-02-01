package com.example.messaging.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives SMS_DELIVER broadcasts when this app is the default SMS app.
 *
 * This receiver MUST be in the app module (not upstream) because:
 * 1. It must be registered in the app's manifest
 * 2. It dispatches to core-sms interfaces, not upstream internals
 */
class SmsDeliverReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // TODO: Extract SMS from intent and dispatch to SmsReceiveListener
        // Use core-sms interfaces, not upstream classes directly
    }
}
