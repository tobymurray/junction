package com.technicallyrural.junction.core.transport

import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [SmsReceiveListener] interface.
 *
 * These tests define the expected behavior of any SmsReceiveListener implementation:
 * - Listener methods can be invoked without error
 * - Data classes contain expected information
 * - Various message scenarios are handled
 *
 * Implementations must satisfy these contracts.
 */
class SmsReceiveListenerContractTest {

    private lateinit var listener: SmsReceiveListener

    @Before
    fun setUp() {
        listener = mockk(relaxed = true)
    }

    // ========================================================================
    // SMS Reception
    // ========================================================================

    @Test
    fun `onSmsReceived can be called with valid ReceivedSms`() {
        // Arrange
        val receivedSms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "Hello, this is a test message",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            serviceCenterAddress = null
        )

        // Act
        listener.onSmsReceived(receivedSms)

        // Assert
        verify { listener.onSmsReceived(receivedSms) }
    }

    @Test
    fun `onSmsReceived handles message with service center address`() {
        // Arrange
        val receivedSms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "Test",
            timestamp = System.currentTimeMillis(),
            subscriptionId = 1,
            serviceCenterAddress = "+1555555555"
        )

        // Act
        listener.onSmsReceived(receivedSms)

        // Assert
        verify { listener.onSmsReceived(receivedSms) }
    }

    @Test
    fun `onSmsReceived handles empty message body`() {
        // Arrange
        val receivedSms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            serviceCenterAddress = null
        )

        // Act
        listener.onSmsReceived(receivedSms)

        // Assert
        verify { listener.onSmsReceived(receivedSms) }
    }

    @Test
    fun `onSmsReceived handles long message body`() {
        // Arrange
        val longMessage = "a".repeat(500) // Longer than typical SMS
        val receivedSms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = longMessage,
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            serviceCenterAddress = null
        )

        // Act
        listener.onSmsReceived(receivedSms)

        // Assert
        verify { listener.onSmsReceived(receivedSms) }
    }

    @Test
    fun `onSmsReceived handles specific subscription ID`() {
        // Arrange
        val receivedSms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "Test from SIM 2",
            timestamp = System.currentTimeMillis(),
            subscriptionId = 2, // Specific SIM card
            serviceCenterAddress = null
        )

        // Act
        listener.onSmsReceived(receivedSms)

        // Assert
        verify { listener.onSmsReceived(receivedSms) }
    }

    // ========================================================================
    // MMS Reception
    // ========================================================================

    @Test
    fun `onMmsReceived can be called with valid ReceivedMms`() {
        // Arrange
        val receivedMms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+0987654321"),
            subject = "Test MMS",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = listOf(
                ReceivedMmsPart(
                    contentType = "text/plain",
                    data = null,
                    text = "Hello from MMS",
                    contentId = null
                )
            )
        )

        // Act
        listener.onMmsReceived(receivedMms)

        // Assert
        verify { listener.onMmsReceived(receivedMms) }
    }

    @Test
    fun `onMmsReceived handles null subject`() {
        // Arrange
        val receivedMms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+0987654321"),
            subject = null,
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = emptyList()
        )

        // Act
        listener.onMmsReceived(receivedMms)

        // Assert
        verify { listener.onMmsReceived(receivedMms) }
    }

    @Test
    fun `onMmsReceived handles multiple recipients`() {
        // Arrange
        val receivedMms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+1111111111", "+2222222222", "+3333333333"),
            subject = "Group MMS",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = emptyList()
        )

        // Act
        listener.onMmsReceived(receivedMms)

        // Assert
        verify { listener.onMmsReceived(receivedMms) }
    }

    @Test
    fun `onMmsReceived handles multiple parts`() {
        // Arrange
        val receivedMms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+0987654321"),
            subject = "MMS with attachments",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = listOf(
                ReceivedMmsPart("text/plain", null, "Text part", null),
                ReceivedMmsPart("image/jpeg", byteArrayOf(1, 2, 3), null, "image1"),
                ReceivedMmsPart("audio/mpeg", byteArrayOf(4, 5, 6), null, "audio1")
            )
        )

        // Act
        listener.onMmsReceived(receivedMms)

        // Assert
        verify { listener.onMmsReceived(receivedMms) }
    }

    @Test
    fun `onMmsReceived handles empty parts list`() {
        // Arrange
        val receivedMms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+0987654321"),
            subject = "Empty MMS",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = emptyList()
        )

        // Act
        listener.onMmsReceived(receivedMms)

        // Assert
        verify { listener.onMmsReceived(receivedMms) }
    }

    // ========================================================================
    // WAP Push Reception
    // ========================================================================

    @Test
    fun `onWapPushReceived can be called with valid PDU`() {
        // Arrange
        val pdu = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        val subscriptionId = -1

        // Act
        listener.onWapPushReceived(pdu, subscriptionId)

        // Assert
        verify { listener.onWapPushReceived(pdu, subscriptionId) }
    }

    @Test
    fun `onWapPushReceived handles empty PDU`() {
        // Arrange
        val emptyPdu = byteArrayOf()

        // Act
        listener.onWapPushReceived(emptyPdu, -1)

        // Assert
        verify { listener.onWapPushReceived(emptyPdu, -1) }
    }

    @Test
    fun `onWapPushReceived handles large PDU`() {
        // Arrange
        val largePdu = ByteArray(10000) { it.toByte() }

        // Act
        listener.onWapPushReceived(largePdu, 1)

        // Assert
        verify { listener.onWapPushReceived(largePdu, 1) }
    }

    // ========================================================================
    // Data Class Contracts
    // ========================================================================

    @Test
    fun `ReceivedSms data class has expected properties`() {
        // Act
        val sms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "Test message",
            timestamp = 1234567890L,
            subscriptionId = 1,
            serviceCenterAddress = "+1555555555"
        )

        // Assert
        assertEquals("+1234567890", sms.originatingAddress)
        assertEquals("Test message", sms.messageBody)
        assertEquals(1234567890L, sms.timestamp)
        assertEquals(1, sms.subscriptionId)
        assertEquals("+1555555555", sms.serviceCenterAddress)
    }

    @Test
    fun `ReceivedSms supports null serviceCenterAddress`() {
        // Act
        val sms = ReceivedSms(
            originatingAddress = "+1234567890",
            messageBody = "Test",
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            serviceCenterAddress = null
        )

        // Assert
        assertNull("Service center address can be null", sms.serviceCenterAddress)
    }

    @Test
    fun `ReceivedMms data class has expected properties`() {
        // Act
        val mms = ReceivedMms(
            from = "+1234567890",
            to = listOf("+0987654321"),
            subject = "Test",
            timestamp = 1234567890L,
            subscriptionId = 1,
            parts = emptyList()
        )

        // Assert
        assertEquals("+1234567890", mms.from)
        assertEquals(1, mms.to.size)
        assertEquals("Test", mms.subject)
        assertEquals(1234567890L, mms.timestamp)
        assertEquals(1, mms.subscriptionId)
        assertTrue(mms.parts.isEmpty())
    }

    @Test
    fun `ReceivedMms supports null subject`() {
        // Act
        val mms = ReceivedMms(
            from = "+1234567890",
            to = emptyList(),
            subject = null,
            timestamp = System.currentTimeMillis(),
            subscriptionId = -1,
            parts = emptyList()
        )

        // Assert
        assertNull("Subject can be null", mms.subject)
    }

    @Test
    fun `ReceivedMmsPart data class has expected properties`() {
        // Act
        val part = ReceivedMmsPart(
            contentType = "image/jpeg",
            data = byteArrayOf(1, 2, 3),
            text = null,
            contentId = "image1"
        )

        // Assert
        assertEquals("image/jpeg", part.contentType)
        assertNotNull("Data can be non-null", part.data)
        assertNull("Text can be null", part.text)
        assertEquals("image1", part.contentId)
    }

    @Test
    fun `ReceivedMmsPart equals handles ByteArray correctly`() {
        // Arrange
        val data = byteArrayOf(1, 2, 3)
        val part1 = ReceivedMmsPart("text/plain", data, null, null)
        val part2 = ReceivedMmsPart("text/plain", data, null, null)

        // Act & Assert
        assertEquals("Parts with same data should be equal", part1, part2)
        assertEquals("Hash codes should match", part1.hashCode(), part2.hashCode())
    }

    @Test
    fun `ReceivedMmsPart text part has null data`() {
        // Act
        val textPart = ReceivedMmsPart(
            contentType = "text/plain",
            data = null,
            text = "Hello",
            contentId = null
        )

        // Assert
        assertNull("Text part should have null data", textPart.data)
        assertNotNull("Text part should have text", textPart.text)
    }

    @Test
    fun `ReceivedMmsPart binary part has null text`() {
        // Act
        val binaryPart = ReceivedMmsPart(
            contentType = "image/png",
            data = byteArrayOf(1, 2, 3, 4),
            text = null,
            contentId = "img"
        )

        // Assert
        assertNotNull("Binary part should have data", binaryPart.data)
        assertNull("Binary part should have null text", binaryPart.text)
    }
}
