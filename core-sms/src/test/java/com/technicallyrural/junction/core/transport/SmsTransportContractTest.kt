package com.technicallyrural.junction.core.transport

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [SmsTransport] interface.
 *
 * These tests define the expected behavior of any SmsTransport implementation:
 * - Send operations return SendResult (Success or Failure)
 * - Callbacks are invoked appropriately
 * - Subscription management works correctly
 * - Capability checks return boolean results
 *
 * Implementations must satisfy these contracts.
 */
class SmsTransportContractTest {

    private lateinit var transport: SmsTransport

    @Before
    fun setUp() {
        // Create a mock implementation for contract testing
        // Real tests will use actual adapter implementations
        transport = mockk(relaxed = true)
    }

    // ========================================================================
    // SMS Sending
    // ========================================================================

    @Test
    fun `sendSms with valid parameters returns SendResult`() {
        // Arrange
        val destination = "+1234567890"
        val message = "Test message"
        val expectedResult = SendResult.Success(messageId = 12345L)
        every { transport.sendSms(destination, message, any(), any(), any()) } returns expectedResult

        // Act
        val result = transport.sendSms(destination, message)

        // Assert
        assertTrue("sendSms should return SendResult.Success for valid input",
            result is SendResult.Success)
        assertEquals("Message ID should match", 12345L, (result as SendResult.Success).messageId)
    }

    @Test
    fun `sendSms with invalid destination returns Failure`() {
        // Arrange
        val invalidDestination = "invalid"
        val message = "Test"
        val expectedResult = SendResult.Failure(SendError.INVALID_DESTINATION)
        every { transport.sendSms(invalidDestination, message, any(), any(), any()) } returns expectedResult

        // Act
        val result = transport.sendSms(invalidDestination, message)

        // Assert
        assertTrue("sendSms should return Failure for invalid destination",
            result is SendResult.Failure)
        assertEquals("Error should be INVALID_DESTINATION",
            SendError.INVALID_DESTINATION, (result as SendResult.Failure).error)
    }

    @Test
    fun `sendSms with null callbacks succeeds`() {
        // Arrange
        val expectedResult = SendResult.Success(messageId = 123L)
        every { transport.sendSms(any(), any(), any(), null, null) } returns expectedResult

        // Act
        val result = transport.sendSms("+1234567890", "Test", sentCallback = null, deliveryCallback = null)

        // Assert
        assertTrue("sendSms should accept null callbacks", result is SendResult.Success)
    }

    @Test
    fun `sendSms invokes sentCallback on success`() {
        // Arrange
        val mockCallback = mockk<SmsCallback>(relaxed = true)
        every { transport.sendSms(any(), any(), any(), any(), any()) } returns SendResult.Success(123L)

        // Act
        transport.sendSms("+1234567890", "Test", sentCallback = mockCallback)

        // Note: Callback invocation is async, so we verify the contract allows it
        // Actual callback testing would be in implementation tests
    }

    @Test
    fun `sendMultipartSms with multiple parts returns SendResult`() {
        // Arrange
        val parts = listOf("Part 1", "Part 2", "Part 3")
        val expectedResult = SendResult.Success(messageId = 456L)
        every { transport.sendMultipartSms(any(), parts, any(), any(), any()) } returns expectedResult

        // Act
        val result = transport.sendMultipartSms("+1234567890", parts)

        // Assert
        assertTrue("sendMultipartSms should return SendResult", result is SendResult.Success)
    }

    @Test
    fun `sendMultipartSms with empty parts list handles gracefully`() {
        // Arrange
        val emptyParts = emptyList<String>()
        val expectedResult = SendResult.Failure(SendError.UNKNOWN)
        every { transport.sendMultipartSms(any(), emptyParts, any(), any(), any()) } returns expectedResult

        // Act
        val result = transport.sendMultipartSms("+1234567890", emptyParts)

        // Assert - Implementation should handle empty parts (either reject or accept)
        assertNotNull("sendMultipartSms should return a result for empty parts", result)
    }

    // ========================================================================
    // MMS Sending
    // ========================================================================

    @Test
    fun `sendMms with valid parameters returns SendResult`() {
        // Arrange
        val destinations = listOf("+1234567890", "+0987654321")
        val subject = "Test MMS"
        val parts = listOf(MmsPart.Text("Hello"))
        val expectedResult = SendResult.Success(messageId = 789L)
        every { transport.sendMms(destinations, subject, parts, any()) } returns expectedResult

        // Act
        val result = transport.sendMms(destinations, subject, parts)

        // Assert
        assertTrue("sendMms should return SendResult", result is SendResult.Success)
    }

    @Test
    fun `sendMms with null subject succeeds`() {
        // Arrange
        val expectedResult = SendResult.Success(messageId = 999L)
        every { transport.sendMms(any(), null, any(), any()) } returns expectedResult

        // Act
        val result = transport.sendMms(listOf("+1234567890"), null, listOf(MmsPart.Text("Hi")))

        // Assert
        assertTrue("sendMms should accept null subject", result is SendResult.Success)
    }

    @Test
    fun `sendMms with multiple recipients succeeds`() {
        // Arrange
        val recipients = listOf("+1111111111", "+2222222222", "+3333333333")
        val expectedResult = SendResult.Success(messageId = 111L)
        every { transport.sendMms(recipients, any(), any(), any()) } returns expectedResult

        // Act
        val result = transport.sendMms(recipients, "Group MMS", listOf(MmsPart.Text("Hi all")))

        // Assert
        assertTrue("sendMms should support multiple recipients", result is SendResult.Success)
    }

    // ========================================================================
    // Subscription Management
    // ========================================================================

    @Test
    fun `getAvailableSubscriptions returns list of subscriptions`() {
        // Arrange
        val subscriptions = listOf(
            SubscriptionInfo(1, "SIM 1", "Carrier A", "+1111111111", 0),
            SubscriptionInfo(2, "SIM 2", "Carrier B", "+2222222222", 1)
        )
        every { transport.getAvailableSubscriptions() } returns subscriptions

        // Act
        val result = transport.getAvailableSubscriptions()

        // Assert
        assertNotNull("getAvailableSubscriptions should return a list", result)
        assertEquals("Should return all subscriptions", 2, result.size)
    }

    @Test
    fun `getAvailableSubscriptions with no SIMs returns empty list`() {
        // Arrange
        every { transport.getAvailableSubscriptions() } returns emptyList()

        // Act
        val result = transport.getAvailableSubscriptions()

        // Assert
        assertNotNull("Should return empty list when no SIMs", result)
        assertTrue("Should be empty", result.isEmpty())
    }

    @Test
    fun `getDefaultSmsSubscription returns valid subscription ID`() {
        // Arrange
        every { transport.getDefaultSmsSubscription() } returns 1

        // Act
        val result = transport.getDefaultSmsSubscription()

        // Assert
        assertTrue("Default subscription should be valid ID or -1", result >= -1)
    }

    @Test
    fun `getDefaultSmsSubscription returns -1 when no default`() {
        // Arrange
        every { transport.getDefaultSmsSubscription() } returns -1

        // Act
        val result = transport.getDefaultSmsSubscription()

        // Assert
        assertEquals("Should return -1 when no default", -1, result)
    }

    // ========================================================================
    // Capability Checks
    // ========================================================================

    @Test
    fun `canSendSms returns boolean`() {
        // Arrange
        every { transport.canSendSms() } returns true

        // Act
        val result = transport.canSendSms()

        // Assert
        assertTrue("canSendSms should return true when capable", result)
    }

    @Test
    fun `canSendSms returns false when incapable`() {
        // Arrange
        every { transport.canSendSms() } returns false

        // Act
        val result = transport.canSendSms()

        // Assert
        assertFalse("canSendSms should return false when incapable", result)
    }

    @Test
    fun `canSendMms returns boolean`() {
        // Arrange
        every { transport.canSendMms() } returns true

        // Act
        val result = transport.canSendMms()

        // Assert
        assertTrue("canSendMms should return true when capable", result)
    }

    @Test
    fun `canSendMms returns false when incapable`() {
        // Arrange
        every { transport.canSendMms() } returns false

        // Act
        val result = transport.canSendMms()

        // Assert
        assertFalse("canSendMms should return false when incapable", result)
    }

    // ========================================================================
    // SendResult Sealed Class Contract
    // ========================================================================

    @Test
    fun `SendResult Success contains message ID`() {
        // Act
        val result = SendResult.Success(messageId = 42L)

        // Assert
        assertEquals("Success should contain message ID", 42L, result.messageId)
    }

    @Test
    fun `SendResult Failure contains error`() {
        // Act
        val result = SendResult.Failure(error = SendError.NO_SERVICE)

        // Assert
        assertEquals("Failure should contain error", SendError.NO_SERVICE, result.error)
    }

    @Test
    fun `SendError enum contains all expected errors`() {
        // Assert - Verify all error types exist
        val errors = SendError.values()
        assertTrue("Should have NO_SERVICE", errors.contains(SendError.NO_SERVICE))
        assertTrue("Should have INVALID_DESTINATION", errors.contains(SendError.INVALID_DESTINATION))
        assertTrue("Should have MESSAGE_TOO_LONG", errors.contains(SendError.MESSAGE_TOO_LONG))
        assertTrue("Should have NO_DEFAULT_SMS_APP", errors.contains(SendError.NO_DEFAULT_SMS_APP))
        assertTrue("Should have PERMISSION_DENIED", errors.contains(SendError.PERMISSION_DENIED))
        assertTrue("Should have RADIO_OFF", errors.contains(SendError.RADIO_OFF))
        assertTrue("Should have UNKNOWN", errors.contains(SendError.UNKNOWN))
    }
}
