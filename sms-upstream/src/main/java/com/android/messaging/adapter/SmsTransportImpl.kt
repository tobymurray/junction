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

import android.content.Context
import android.net.Uri
import android.telephony.SubscriptionInfo as AndroidSubscriptionInfo
import com.android.messaging.sms.MmsConfig
import com.android.messaging.sms.SmsSender
import com.android.messaging.util.PhoneUtils
import com.technicallyrural.junction.core.transport.MmsPart
import com.technicallyrural.junction.core.transport.SendError
import com.technicallyrural.junction.core.transport.SendResult
import com.technicallyrural.junction.core.transport.SmsCallback
import com.technicallyrural.junction.core.transport.SmsTransport
import com.technicallyrural.junction.core.transport.SubscriptionInfo
import java.util.concurrent.atomic.AtomicLong

/**
 * Implementation of [SmsTransport] that bridges to AOSP Messaging's SMS/MMS sending infrastructure.
 *
 * This adapter wraps:
 * - [SmsSender] for SMS sending
 * - [PhoneUtils] for subscription management
 * - [MmsConfig] for capability checks
 *
 * @param context Application context for SMS operations
 */
class SmsTransportImpl(private val context: Context) : SmsTransport {

    private val messageIdCounter = AtomicLong(System.currentTimeMillis())

    override fun sendSms(
        destinationAddress: String,
        message: String,
        subscriptionId: Int,
        sentCallback: SmsCallback?,
        deliveryCallback: SmsCallback?
    ): SendResult {
        val effectiveSubId = getEffectiveSubscriptionId(subscriptionId)
        val messageUri = generateMessageUri()

        return try {
            val result = SmsSender.sendMessage(
                context,
                effectiveSubId,
                destinationAddress,
                message,
                null, // serviceCenter - let system determine
                deliveryCallback != null, // requireDeliveryReport
                messageUri
            )

            handleSmsSendResult(result, messageUri, sentCallback, deliveryCallback)
        } catch (e: Exception) {
            sentCallback?.onFailure(SendError.UNKNOWN)
            SendResult.Failure(mapException(e))
        }
    }

    override fun sendMultipartSms(
        destinationAddress: String,
        parts: List<String>,
        subscriptionId: Int,
        sentCallbacks: List<SmsCallback>?,
        deliveryCallbacks: List<SmsCallback>?
    ): SendResult {
        // Join parts and send as single message - SmsSender handles multipart internally
        val fullMessage = parts.joinToString("")
        return sendSms(
            destinationAddress,
            fullMessage,
            subscriptionId,
            sentCallbacks?.firstOrNull(),
            deliveryCallbacks?.lastOrNull()
        )
    }

    override fun sendMms(
        destinationAddresses: List<String>,
        subject: String?,
        parts: List<MmsPart>,
        subscriptionId: Int
    ): SendResult {
        // MMS sending requires building a SendReq PDU and using MmsSender.
        // This is more complex and will be implemented in a future iteration.
        // For now, return a failure indicating MMS is not yet supported via this interface.
        return SendResult.Failure(SendError.UNKNOWN)
    }

    override fun getAvailableSubscriptions(): List<SubscriptionInfo> {
        val phoneUtils = PhoneUtils.getDefault()

        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            phoneUtils.toLMr1().activeSubscriptionInfoList.map { it.toSubscriptionInfo() }
        } else {
            // Pre-LMR1: single default subscription
            listOf(createDefaultSubscriptionInfo(phoneUtils))
        }
    }

    override fun getDefaultSmsSubscription(): Int {
        return PhoneUtils.getDefault().defaultSmsSubscriptionId
    }

    override fun canSendSms(): Boolean {
        val phoneUtils = PhoneUtils.getDefault()
        return phoneUtils.isSmsCapable && phoneUtils.hasSim()
    }

    override fun canSendMms(): Boolean {
        val subId = getDefaultSmsSubscription()
        return try {
            // MmsConfig doesn't expose a direct "isMmsEnabled" - use GroupMmsEnabled as proxy
            // and check that we have data capability
            MmsConfig.get(subId).groupMmsEnabled
        } catch (e: Exception) {
            false
        }
    }

    // --- Private helper methods ---

    private fun getEffectiveSubscriptionId(subscriptionId: Int): Int {
        return if (subscriptionId == SmsTransport.DEFAULT_SUBSCRIPTION) {
            PhoneUtils.getDefault().defaultSmsSubscriptionId
        } else {
            subscriptionId
        }
    }

    private fun generateMessageUri(): Uri {
        val messageId = messageIdCounter.incrementAndGet()
        return Uri.parse("content://mms-sms/pending/$messageId")
    }

    private fun handleSmsSendResult(
        result: SmsSender.SendResult,
        messageUri: Uri,
        sentCallback: SmsCallback?,
        deliveryCallback: SmsCallback?
    ): SendResult {
        val messageId = messageUri.lastPathSegment?.toLongOrNull() ?: 0L

        return when (result.highestFailureLevel) {
            SmsSender.SendResult.FAILURE_LEVEL_NONE -> {
                sentCallback?.onSuccess()
                SendResult.Success(messageId)
            }
            SmsSender.SendResult.FAILURE_LEVEL_TEMPORARY -> {
                sentCallback?.onFailure(SendError.NO_SERVICE)
                SendResult.Failure(SendError.NO_SERVICE)
            }
            SmsSender.SendResult.FAILURE_LEVEL_PERMANENT -> {
                sentCallback?.onFailure(SendError.UNKNOWN)
                SendResult.Failure(SendError.UNKNOWN)
            }
            else -> {
                sentCallback?.onFailure(SendError.UNKNOWN)
                SendResult.Failure(SendError.UNKNOWN)
            }
        }
    }

    private fun mapException(e: Exception): SendError {
        val message = e.message ?: ""
        return when {
            message.contains("empty destination", ignoreCase = true) -> SendError.INVALID_DESTINATION
            message.contains("empty text", ignoreCase = true) -> SendError.MESSAGE_TOO_LONG
            else -> SendError.UNKNOWN
        }
    }

    private fun AndroidSubscriptionInfo.toSubscriptionInfo(): SubscriptionInfo {
        return SubscriptionInfo(
            subscriptionId = this.subscriptionId,
            displayName = this.displayName?.toString() ?: "",
            carrierName = this.carrierName?.toString() ?: "",
            phoneNumber = this.number,
            slotIndex = this.simSlotIndex
        )
    }

    private fun createDefaultSubscriptionInfo(phoneUtils: PhoneUtils): SubscriptionInfo {
        return SubscriptionInfo(
            subscriptionId = SmsTransport.DEFAULT_SUBSCRIPTION,
            displayName = phoneUtils.carrierName ?: "Default",
            carrierName = phoneUtils.carrierName ?: "",
            phoneNumber = try { phoneUtils.getSelfRawNumber(true) } catch (e: Exception) { null },
            slotIndex = 0
        )
    }
}
