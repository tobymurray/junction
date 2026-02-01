package com.example.messaging.core.transport

/**
 * Interface for receiving incoming SMS/MMS messages.
 *
 * The app module should register an implementation of this interface
 * to be notified when messages arrive. This decouples the BroadcastReceiver
 * (which must be in the app module for manifest registration) from the
 * message handling logic.
 */
interface SmsReceiveListener {

    /**
     * Called when an SMS message is received.
     *
     * @param message The received message data
     */
    fun onSmsReceived(message: ReceivedSms)

    /**
     * Called when an MMS message is received.
     *
     * @param message The received MMS data
     */
    fun onMmsReceived(message: ReceivedMms)

    /**
     * Called when a WAP push message is received (for MMS notification).
     *
     * @param pdu The WAP push PDU
     * @param subscriptionId The SIM subscription ID
     */
    fun onWapPushReceived(pdu: ByteArray, subscriptionId: Int)
}

/**
 * Data for a received SMS.
 */
data class ReceivedSms(
    val originatingAddress: String,
    val messageBody: String,
    val timestamp: Long,
    val subscriptionId: Int,
    val serviceCenterAddress: String?
)

/**
 * Data for a received MMS.
 */
data class ReceivedMms(
    val from: String,
    val to: List<String>,
    val subject: String?,
    val timestamp: Long,
    val subscriptionId: Int,
    val parts: List<ReceivedMmsPart>
)

/**
 * A part of a received MMS.
 */
data class ReceivedMmsPart(
    val contentType: String,
    val data: ByteArray?,
    val text: String?,
    val contentId: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ReceivedMmsPart
        return contentType == other.contentType &&
                data.contentEquals(other.data) &&
                text == other.text &&
                contentId == other.contentId
    }

    override fun hashCode(): Int {
        var result = contentType.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (contentId?.hashCode() ?: 0)
        return result
    }
}

/**
 * Registry for SMS receive listeners.
 *
 * The upstream module calls this to dispatch received messages.
 * The app module registers its listener here.
 */
object SmsReceiverRegistry {
    private var listener: SmsReceiveListener? = null

    fun registerListener(listener: SmsReceiveListener) {
        this.listener = listener
    }

    fun unregisterListener() {
        this.listener = null
    }

    internal fun getListener(): SmsReceiveListener? = listener
}
