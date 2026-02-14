package com.technicallyrural.junction.core.transport

import android.net.Uri

/**
 * Observer interface for outbound message send status callbacks.
 *
 * AOSP's SendStatusReceiver calls this observer after processing
 * MESSAGE_SENT_ACTION and MESSAGE_DELIVERED_ACTION broadcasts.
 *
 * This enables app-layer bridging (e.g., to Matrix) without modifying
 * AOSP's core send/receive flow.
 *
 * Architecture:
 * - Defined in core-sms (interface layer)
 * - Called by sms-upstream SendStatusReceiver (minimal AOSP patch)
 * - Implemented by app module (Matrix bridging logic)
 * - Registered via CoreSmsRegistry
 */
interface OutboundMessageObserver {

    /**
     * Called when an SMS message send completes (success or failure).
     *
     * This is invoked AFTER AOSP has processed the send result and
     * updated the telephony database.
     *
     * @param messageUri URI of the sent message (content://sms/<id>)
     * @param resultCode Result from SmsManager (Activity.RESULT_OK or error)
     * @param isSms true for SMS, false for MMS
     */
    fun onMessageSent(
        messageUri: Uri,
        resultCode: Int,
        isSms: Boolean
    )

    /**
     * Called when a delivery report is received for a sent message.
     *
     * This is invoked AFTER AOSP has processed the delivery report.
     *
     * @param messageUri URI of the message (content://sms/<id>)
     * @param status Delivery status (Sms.STATUS_COMPLETE, STATUS_PENDING, STATUS_FAILED)
     */
    fun onDeliveryReportReceived(
        messageUri: Uri,
        status: Int
    )
}
