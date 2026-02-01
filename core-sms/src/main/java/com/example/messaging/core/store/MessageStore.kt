package com.example.messaging.core.store

import kotlinx.coroutines.flow.Flow

/**
 * Interface for message and conversation storage operations.
 *
 * This interface abstracts the underlying storage (AOSP Messaging database,
 * system SMS provider, etc.) from the app layer.
 *
 * Implementation is provided by sms-upstream module.
 */
interface MessageStore {

    // ========================================================================
    // Conversations
    // ========================================================================

    /**
     * Get all conversations, ordered by most recent first.
     */
    fun getConversations(): Flow<List<Conversation>>

    /**
     * Get a single conversation by ID.
     */
    suspend fun getConversation(conversationId: Long): Conversation?

    /**
     * Get conversation for a specific phone number/address.
     * Creates new conversation if none exists.
     */
    suspend fun getOrCreateConversation(address: String): Conversation

    /**
     * Delete a conversation and all its messages.
     */
    suspend fun deleteConversation(conversationId: Long): Boolean

    /**
     * Mark all messages in conversation as read.
     */
    suspend fun markConversationRead(conversationId: Long)

    // ========================================================================
    // Messages
    // ========================================================================

    /**
     * Get messages in a conversation, ordered by timestamp.
     */
    fun getMessages(conversationId: Long): Flow<List<Message>>

    /**
     * Get a single message by ID.
     */
    suspend fun getMessage(messageId: Long): Message?

    /**
     * Insert a new message (used when receiving).
     */
    suspend fun insertMessage(message: Message): Long

    /**
     * Update message status (sent, delivered, failed, etc.).
     */
    suspend fun updateMessageStatus(messageId: Long, status: MessageStatus)

    /**
     * Delete a single message.
     */
    suspend fun deleteMessage(messageId: Long): Boolean

    // ========================================================================
    // Search
    // ========================================================================

    /**
     * Search messages by content.
     */
    fun searchMessages(query: String): Flow<List<Message>>

    // ========================================================================
    // Sync
    // ========================================================================

    /**
     * Sync with system SMS provider.
     * Call this to import messages from system or export to system.
     */
    suspend fun syncWithSystemProvider()
}

/**
 * Represents a conversation (thread).
 */
data class Conversation(
    val id: Long,
    val participants: List<Participant>,
    val snippet: String?,
    val snippetTimestamp: Long,
    val unreadCount: Int,
    val isGroup: Boolean,
    val isArchived: Boolean
)

/**
 * A participant in a conversation.
 */
data class Participant(
    val address: String,          // Phone number
    val displayName: String?,     // Contact name if resolved
    val photoUri: String?,        // Contact photo if available
    val isContact: Boolean        // Whether this is a known contact
)

/**
 * Represents a single message.
 */
data class Message(
    val id: Long,
    val conversationId: Long,
    val address: String,           // Sender (incoming) or recipient (outgoing)
    val body: String?,
    val timestamp: Long,
    val isIncoming: Boolean,
    val status: MessageStatus,
    val type: MessageType,
    val subscriptionId: Int,
    val mmsSubject: String? = null,
    val mmsParts: List<MmsPartInfo>? = null
)

/**
 * Status of a message.
 */
enum class MessageStatus {
    DRAFT,
    PENDING,        // Queued for sending
    SENDING,        // Currently being sent
    SENT,           // Sent to carrier
    DELIVERED,      // Delivery confirmed
    FAILED,         // Send failed
    RECEIVED        // Received message (incoming)
}

/**
 * Type of message.
 */
enum class MessageType {
    SMS,
    MMS
}

/**
 * Information about an MMS part (attachment).
 */
data class MmsPartInfo(
    val contentType: String,
    val uri: String?,
    val text: String?
)
