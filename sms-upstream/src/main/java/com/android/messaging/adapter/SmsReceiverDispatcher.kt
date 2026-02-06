/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.telephony.SmsMessage
import com.technicallyrural.junction.core.transport.ReceivedMms
import com.technicallyrural.junction.core.transport.ReceivedMmsPart
import com.technicallyrural.junction.core.transport.ReceivedSms
import com.technicallyrural.junction.core.transport.SmsReceiverRegistry

/**
 * Dispatcher that bridges AOSP Messaging's broadcast receivers to the
 * [SmsReceiverRegistry] interface.
 *
 * Call these methods from the AOSP broadcast receivers (SmsReceiver, MmsReceiver, etc.)
 * to notify registered listeners of incoming messages.
 *
 * This allows the app module to register a listener and receive message events
 * without depending on AOSP internals.
 */
object SmsReceiverDispatcher {

    /**
     * Dispatch a received SMS to registered listeners.
     *
     * @param messages Array of SmsMessage PDUs from the broadcast
     * @param subscriptionId The SIM subscription ID
     */
    fun dispatchSmsReceived(messages: Array<SmsMessage>, subscriptionId: Int) {
        val listener = SmsReceiverRegistry.getListener() ?: return

        // Combine multipart messages into one
        val originatingAddress = messages.firstOrNull()?.originatingAddress ?: return
        val messageBody = messages.joinToString("") { it.messageBody ?: "" }
        val timestamp = messages.firstOrNull()?.timestampMillis ?: System.currentTimeMillis()
        val serviceCenterAddress = messages.firstOrNull()?.serviceCenterAddress

        val receivedSms = ReceivedSms(
            originatingAddress = originatingAddress,
            messageBody = messageBody,
            timestamp = timestamp,
            subscriptionId = subscriptionId,
            serviceCenterAddress = serviceCenterAddress
        )

        listener.onSmsReceived(receivedSms)
    }

    /**
     * Dispatch a received MMS to registered listeners.
     *
     * @param from Sender address
     * @param to List of recipient addresses
     * @param subject MMS subject
     * @param timestamp Message timestamp
     * @param subscriptionId The SIM subscription ID
     * @param parts List of MMS parts
     */
    fun dispatchMmsReceived(
        from: String,
        to: List<String>,
        subject: String?,
        timestamp: Long,
        subscriptionId: Int,
        parts: List<ReceivedMmsPart>
    ) {
        val listener = SmsReceiverRegistry.getListener() ?: return

        val receivedMms = ReceivedMms(
            from = from,
            to = to,
            subject = subject,
            timestamp = timestamp,
            subscriptionId = subscriptionId,
            parts = parts
        )

        listener.onMmsReceived(receivedMms)
    }

    /**
     * Dispatch a WAP push message to registered listeners.
     *
     * @param pdu The WAP push PDU data
     * @param subscriptionId The SIM subscription ID
     */
    fun dispatchWapPushReceived(pdu: ByteArray, subscriptionId: Int) {
        val listener = SmsReceiverRegistry.getListener() ?: return
        listener.onWapPushReceived(pdu, subscriptionId)
    }

    /**
     * Check if any listener is registered.
     *
     * @return true if a listener is registered
     */
    fun hasListener(): Boolean = SmsReceiverRegistry.getListener() != null
}
