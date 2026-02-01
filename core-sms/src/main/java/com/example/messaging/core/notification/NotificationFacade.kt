package com.example.messaging.core.notification

import com.example.messaging.core.store.Conversation
import com.example.messaging.core.store.Message

/**
 * Interface for displaying message notifications.
 *
 * This abstracts notification display from the upstream implementation,
 * allowing the app to customize notification appearance and behavior.
 *
 * Implementation can be in app module (preferred for customization)
 * or in sms-upstream (for upstream defaults).
 */
interface NotificationFacade {

    /**
     * Show notification for a new incoming message.
     *
     * @param message The received message
     * @param conversation The conversation it belongs to
     */
    fun showNewMessageNotification(message: Message, conversation: Conversation)

    /**
     * Show notification for multiple new messages (summary).
     *
     * @param messages List of new messages
     */
    fun showMultipleMessagesNotification(messages: List<Message>)

    /**
     * Show notification for a failed send.
     *
     * @param message The message that failed
     * @param conversation The conversation
     */
    fun showSendFailedNotification(message: Message, conversation: Conversation)

    /**
     * Cancel notifications for a conversation.
     * Call this when user opens the conversation.
     *
     * @param conversationId The conversation ID
     */
    fun cancelConversationNotifications(conversationId: Long)

    /**
     * Cancel all message notifications.
     */
    fun cancelAllNotifications()

    /**
     * Update notification channels.
     * Call this on app startup and when settings change.
     */
    fun createNotificationChannels()

    /**
     * Check if notifications are enabled.
     */
    fun areNotificationsEnabled(): Boolean
}

/**
 * Configuration for notification behavior.
 */
data class NotificationConfig(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val ledEnabled: Boolean = true,
    val showPreview: Boolean = true,     // Show message content in notification
    val showSender: Boolean = true,      // Show sender name
    val groupByConversation: Boolean = true
)
