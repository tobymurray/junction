package com.technicallyrural.junction.core.transport

import android.app.PendingIntent

/**
 * Interface for SMS/MMS transport operations.
 *
 * This interface abstracts the underlying SMS implementation (AOSP Messaging)
 * from the app layer. The app should NEVER directly use SmsManager or any
 * upstream AOSP classes.
 *
 * Implementation is provided by sms-upstream module.
 */
interface SmsTransport {

    /**
     * Send an SMS message.
     *
     * @param destinationAddress The phone number to send to
     * @param message The text message content
     * @param subscriptionId The SIM subscription ID (-1 for default)
     * @param sentCallback Called when message is sent to carrier
     * @param deliveryCallback Called when delivery report received (optional)
     * @return A SendResult indicating immediate success/failure
     */
    fun sendSms(
        destinationAddress: String,
        message: String,
        subscriptionId: Int = DEFAULT_SUBSCRIPTION,
        sentCallback: SmsCallback? = null,
        deliveryCallback: SmsCallback? = null
    ): SendResult

    /**
     * Send a multipart SMS message (for messages > 160 chars).
     */
    fun sendMultipartSms(
        destinationAddress: String,
        parts: List<String>,
        subscriptionId: Int = DEFAULT_SUBSCRIPTION,
        sentCallbacks: List<SmsCallback>? = null,
        deliveryCallbacks: List<SmsCallback>? = null
    ): SendResult

    /**
     * Send an MMS message.
     *
     * @param destinationAddresses List of recipient phone numbers
     * @param subject MMS subject (optional)
     * @param parts List of MMS parts (text, images, etc.)
     * @param subscriptionId The SIM subscription ID
     * @return A SendResult indicating immediate success/failure
     */
    fun sendMms(
        destinationAddresses: List<String>,
        subject: String?,
        parts: List<MmsPart>,
        subscriptionId: Int = DEFAULT_SUBSCRIPTION
    ): SendResult

    /**
     * Get available subscription IDs (SIM cards).
     */
    fun getAvailableSubscriptions(): List<SubscriptionInfo>

    /**
     * Get the default subscription ID for SMS.
     */
    fun getDefaultSmsSubscription(): Int

    /**
     * Check if device can send SMS.
     */
    fun canSendSms(): Boolean

    /**
     * Check if device can send MMS.
     */
    fun canSendMms(): Boolean

    companion object {
        const val DEFAULT_SUBSCRIPTION = -1
    }
}

/**
 * Result of a send operation.
 */
sealed class SendResult {
    /** Message accepted for sending */
    data class Success(val messageId: Long) : SendResult()

    /** Message rejected immediately */
    data class Failure(val error: SendError) : SendResult()
}

/**
 * Errors that can occur when sending.
 */
enum class SendError {
    NO_SERVICE,
    INVALID_DESTINATION,
    MESSAGE_TOO_LONG,
    NO_DEFAULT_SMS_APP,
    PERMISSION_DENIED,
    RADIO_OFF,
    UNKNOWN
}

/**
 * Callback for SMS send/delivery status.
 */
interface SmsCallback {
    fun onSuccess()
    fun onFailure(error: SendError)
}

/**
 * Information about a SIM subscription.
 */
data class SubscriptionInfo(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String,
    val phoneNumber: String?,
    val slotIndex: Int
)

/**
 * A part of an MMS message.
 */
sealed class MmsPart {
    data class Text(val text: String, val contentType: String = "text/plain") : MmsPart()
    data class Image(val uri: String, val contentType: String) : MmsPart()
    data class Video(val uri: String, val contentType: String) : MmsPart()
    data class Audio(val uri: String, val contentType: String) : MmsPart()
}
