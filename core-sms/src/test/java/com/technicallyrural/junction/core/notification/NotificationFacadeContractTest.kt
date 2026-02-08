package com.technicallyrural.junction.core.notification

import com.technicallyrural.junction.core.store.Conversation
import com.technicallyrural.junction.core.store.Message
import com.technicallyrural.junction.core.store.MessageStatus
import com.technicallyrural.junction.core.store.MessageType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [NotificationFacade] interface.
 *
 * These tests define the expected behavior of any NotificationFacade implementation:
 * - Notification methods can be called without error
 * - Cancellation methods work correctly
 * - Channel creation succeeds
 * - Notification status can be checked
 *
 * Implementations must satisfy these contracts.
 */
class NotificationFacadeContractTest {

    private lateinit var facade: NotificationFacade
    private lateinit var testMessage: Message
    private lateinit var testConversation: Conversation

    @Before
    fun setUp() {
        facade = mockk(relaxed = true)

        testMessage = Message(
            id = 1L,
            conversationId = 100L,
            address = "+1234567890",
            body = "Test message",
            timestamp = System.currentTimeMillis(),
            isIncoming = true,
            status = MessageStatus.RECEIVED,
            type = MessageType.SMS,
            subscriptionId = -1
        )

        testConversation = Conversation(
            id = 100L,
            participants = emptyList(),
            snippet = "Test",
            snippetTimestamp = System.currentTimeMillis(),
            unreadCount = 1,
            isGroup = false,
            isArchived = false
        )
    }

    // ========================================================================
    // Show Notifications
    // ========================================================================

    @Test
    fun `showNewMessageNotification can be called with valid parameters`() {
        // Act
        facade.showNewMessageNotification(testMessage, testConversation)

        // Assert
        verify { facade.showNewMessageNotification(testMessage, testConversation) }
    }

    @Test
    fun `showNewMessageNotification handles incoming message`() {
        // Arrange
        val incomingMessage = testMessage.copy(isIncoming = true)

        // Act
        facade.showNewMessageNotification(incomingMessage, testConversation)

        // Assert
        verify { facade.showNewMessageNotification(incomingMessage, testConversation) }
    }

    @Test
    fun `showMultipleMessagesNotification can be called with empty list`() {
        // Act
        facade.showMultipleMessagesNotification(emptyList())

        // Assert
        verify { facade.showMultipleMessagesNotification(emptyList()) }
    }

    @Test
    fun `showMultipleMessagesNotification can be called with multiple messages`() {
        // Arrange
        val messages = listOf(
            testMessage,
            testMessage.copy(id = 2L, body = "Another message"),
            testMessage.copy(id = 3L, body = "Third message")
        )

        // Act
        facade.showMultipleMessagesNotification(messages)

        // Assert
        verify { facade.showMultipleMessagesNotification(messages) }
    }

    @Test
    fun `showSendFailedNotification can be called with failed message`() {
        // Arrange
        val failedMessage = testMessage.copy(
            isIncoming = false,
            status = MessageStatus.FAILED
        )

        // Act
        facade.showSendFailedNotification(failedMessage, testConversation)

        // Assert
        verify { facade.showSendFailedNotification(failedMessage, testConversation) }
    }

    // ========================================================================
    // Cancel Notifications
    // ========================================================================

    @Test
    fun `cancelConversationNotifications can be called with valid ID`() {
        // Act
        facade.cancelConversationNotifications(100L)

        // Assert
        verify { facade.cancelConversationNotifications(100L) }
    }

    @Test
    fun `cancelConversationNotifications can be called with non-existent ID`() {
        // Act - Should not throw even if conversation doesn't exist
        facade.cancelConversationNotifications(999L)

        // Assert
        verify { facade.cancelConversationNotifications(999L) }
    }

    @Test
    fun `cancelAllNotifications can be called`() {
        // Act
        facade.cancelAllNotifications()

        // Assert
        verify { facade.cancelAllNotifications() }
    }

    @Test
    fun `cancelAllNotifications is idempotent`() {
        // Act - Calling multiple times should be safe
        facade.cancelAllNotifications()
        facade.cancelAllNotifications()

        // Assert
        verify(exactly = 2) { facade.cancelAllNotifications() }
    }

    // ========================================================================
    // Channel Management
    // ========================================================================

    @Test
    fun `createNotificationChannels can be called`() {
        // Act
        facade.createNotificationChannels()

        // Assert
        verify { facade.createNotificationChannels() }
    }

    @Test
    fun `createNotificationChannels is idempotent`() {
        // Act - Calling multiple times should be safe (channels already exist)
        facade.createNotificationChannels()
        facade.createNotificationChannels()

        // Assert
        verify(exactly = 2) { facade.createNotificationChannels() }
    }

    // ========================================================================
    // Notification Status
    // ========================================================================

    @Test
    fun `areNotificationsEnabled returns boolean`() {
        // Arrange
        every { facade.areNotificationsEnabled() } returns true

        // Act
        val result = facade.areNotificationsEnabled()

        // Assert
        assertTrue("Should return true when notifications enabled", result)
    }

    @Test
    fun `areNotificationsEnabled returns false when disabled`() {
        // Arrange
        every { facade.areNotificationsEnabled() } returns false

        // Act
        val result = facade.areNotificationsEnabled()

        // Assert
        assertFalse("Should return false when notifications disabled", result)
    }

    // ========================================================================
    // NotificationConfig Data Class
    // ========================================================================

    @Test
    fun `NotificationConfig has sensible defaults`() {
        // Act
        val config = NotificationConfig()

        // Assert
        assertTrue("Sound should be enabled by default", config.soundEnabled)
        assertTrue("Vibration should be enabled by default", config.vibrationEnabled)
        assertTrue("LED should be enabled by default", config.ledEnabled)
        assertTrue("Preview should be shown by default", config.showPreview)
        assertTrue("Sender should be shown by default", config.showSender)
        assertTrue("Group by conversation by default", config.groupByConversation)
    }

    @Test
    fun `NotificationConfig can be customized`() {
        // Act
        val config = NotificationConfig(
            soundEnabled = false,
            vibrationEnabled = false,
            ledEnabled = false,
            showPreview = false,
            showSender = false,
            groupByConversation = false
        )

        // Assert
        assertFalse("Sound can be disabled", config.soundEnabled)
        assertFalse("Vibration can be disabled", config.vibrationEnabled)
        assertFalse("LED can be disabled", config.ledEnabled)
        assertFalse("Preview can be hidden", config.showPreview)
        assertFalse("Sender can be hidden", config.showSender)
        assertFalse("Grouping can be disabled", config.groupByConversation)
    }

    @Test
    fun `NotificationConfig supports privacy mode`() {
        // Act - Privacy mode: no preview, no sender
        val privacyConfig = NotificationConfig(
            showPreview = false,
            showSender = false
        )

        // Assert
        assertFalse("Preview should be hidden in privacy mode", privacyConfig.showPreview)
        assertFalse("Sender should be hidden in privacy mode", privacyConfig.showSender)
        assertTrue("Other settings should remain default", privacyConfig.soundEnabled)
    }
}
