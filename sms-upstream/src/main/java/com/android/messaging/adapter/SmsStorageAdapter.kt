/*
 * Copyright (C) 2026 Junction Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.adapter

import android.content.Context
import android.content.Intent
import com.android.messaging.receiver.SmsReceiver

/**
 * Public adapter for app module to forward SMS/MMS to AOSP storage.
 *
 * This adapter provides a clean interface for the app module to store
 * incoming SMS/MMS messages in the AOSP database without directly
 * importing AOSP receiver classes.
 *
 * Architecture:
 * - App module's SmsDeliverReceiver intercepts SMS_DELIVER
 * - Forwards to Matrix bridge (if enabled)
 * - Calls this adapter to store in AOSP database
 * - AOSP UI displays the message
 */
object SmsStorageAdapter {

    /**
     * Forward an SMS_DELIVER intent to AOSP storage.
     *
     * This method extracts SMS messages from the intent and stores them
     * in the AOSP messaging database, making them visible in the UI.
     *
     * @param context Application context
     * @param intent The SMS_DELIVER intent from system
     */
    fun storeSmsFromIntent(context: Context, intent: Intent) {
        // Forward to AOSP's SmsReceiver for database storage
        SmsReceiver.deliverSmsIntent(context, intent)
    }

    /**
     * Forward a WAP_PUSH_DELIVER intent to AOSP storage.
     *
     * @param context Application context
     * @param intent The WAP_PUSH_DELIVER intent from system
     */
    fun storeMmsFromIntent(context: Context, intent: Intent) {
        // Forward to AOSP's MmsReceiver for database storage
        // Note: Implementation depends on AOSP MMS receiver structure
        // For now, this is a placeholder that can be implemented when
        // MMS → Matrix bridging is added in Phase 1
        TODO("MMS storage adapter not yet implemented - Phase 1 work")
    }
}
