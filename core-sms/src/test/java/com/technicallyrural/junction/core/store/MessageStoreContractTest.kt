package com.technicallyrural.junction.core.store

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [MessageStore] interface.
 *
 * These tests define the expected behavior of any MessageStore implementation:
 * - Conversation CRUD operations work correctly
 * - Message CRUD operations work correctly
 * - Flow-based observables emit updates
 * - Search functionality works
 * - Sync operations complete without error
 *
 * Implementations must satisfy these contracts.
 */
class MessageStoreContractTest {

    private lateinit var store: MessageStore

    @Before
    fun setUp() {
        store = mockk(relaxed = true)
    }

    // ========================================================================
    // Conversation Operations
    // ========================================================================

    @Test
    fun `getConversations returns Flow of conversation list`() = runTest {
        // Arrange
        val conversations = listOf(
            Conversation(1L, emptyList(), "Hello", System.currentTimeMillis(), 0, false, false),
            Conversation(2L, emptyList(), "Hi", System.currentTimeMillis(), 1, false, false)
        )
        every { store.getConversations() } returns flowOf(conversations)

        // Act & Assert
        store.getConversations().test {
            val items = awaitItem()
            assertEquals("Should emit conversation list", 2, items.size)
            awaitComplete()
        }
    }

    @Test
    fun `getConversations emits empty list when no conversations`() = runTest {
        // Arrange
        every { store.getConversations() } returns flowOf(emptyList())

        // Act & Assert
        store.getConversations().test {
            val items = awaitItem()
            assertTrue("Should emit empty list", items.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getConversation returns conversation when found`() = runTest {
        // Arrange
        val conversation = Conversation(1L, emptyList(), "Test", System.currentTimeMillis(), 0, false, false)
        coEvery { store.getConversation(1L) } returns conversation

        // Act
        val result = store.getConversation(1L)

        // Assert
        assertNotNull("Should return conversation when found", result)
        assertEquals("Should return correct conversation", 1L, result?.id)
    }

    @Test
    fun `getConversation returns null when not found`() = runTest {
        // Arrange
        coEvery { store.getConversation(999L) } returns null

        // Act
        val result = store.getConversation(999L)

        // Assert
        assertNull("Should return null when conversation not found", result)
    }

    @Test
    fun `getOrCreateConversation returns existing conversation`() = runTest {
        // Arrange
        val existingConversation = Conversation(5L, emptyList(), "Existing", 0L, 0, false, false)
        coEvery { store.getOrCreateConversation("+1234567890") } returns existingConversation

        // Act
        val result = store.getOrCreateConversation("+1234567890")

        // Assert
        assertNotNull("Should return conversation", result)
        assertEquals("Should return existing conversation", 5L, result.id)
    }

    @Test
    fun `getOrCreateConversation creates new conversation when not found`() = runTest {
        // Arrange
        val newConversation = Conversation(10L, emptyList(), null, 0L, 0, false, false)
        coEvery { store.getOrCreateConversation("+9999999999") } returns newConversation

        // Act
        val result = store.getOrCreateConversation("+9999999999")

        // Assert
        assertNotNull("Should create and return new conversation", result)
        assertEquals("Should return new conversation", 10L, result.id)
    }

    @Test
    fun `deleteConversation returns true when deleted`() = runTest {
        // Arrange
        coEvery { store.deleteConversation(1L) } returns true

        // Act
        val result = store.deleteConversation(1L)

        // Assert
        assertTrue("Should return true when conversation deleted", result)
    }

    @Test
    fun `deleteConversation returns false when not found`() = runTest {
        // Arrange
        coEvery { store.deleteConversation(999L) } returns false

        // Act
        val result = store.deleteConversation(999L)

        // Assert
        assertFalse("Should return false when conversation not found", result)
    }

    @Test
    fun `markConversationRead completes without error`() = runTest {
        // Arrange
        coEvery { store.markConversationRead(1L) } returns Unit

        // Act
        store.markConversationRead(1L)

        // Assert
        coVerify { store.markConversationRead(1L) }
    }

    // ========================================================================
    // Message Operations
    // ========================================================================

    @Test
    fun `getMessages returns Flow of message list`() = runTest {
        // Arrange
        val messages = listOf(
            Message(1L, 1L, "+1234567890", "Hello", System.currentTimeMillis(),
                true, MessageStatus.RECEIVED, MessageType.SMS, -1),
            Message(2L, 1L, "+1234567890", "Hi", System.currentTimeMillis(),
                false, MessageStatus.SENT, MessageType.SMS, -1)
        )
        every { store.getMessages(1L) } returns flowOf(messages)

        // Act & Assert
        store.getMessages(1L).test {
            val items = awaitItem()
            assertEquals("Should emit message list", 2, items.size)
            awaitComplete()
        }
    }

    @Test
    fun `getMessages emits empty list when no messages`() = runTest {
        // Arrange
        every { store.getMessages(999L) } returns flowOf(emptyList())

        // Act & Assert
        store.getMessages(999L).test {
            val items = awaitItem()
            assertTrue("Should emit empty list", items.isEmpty())
            awaitComplete()
        }
    }

    @Test
    fun `getMessage returns message when found`() = runTest {
        // Arrange
        val message = Message(1L, 1L, "+1234567890", "Test", System.currentTimeMillis(),
            true, MessageStatus.RECEIVED, MessageType.SMS, -1)
        coEvery { store.getMessage(1L) } returns message

        // Act
        val result = store.getMessage(1L)

        // Assert
        assertNotNull("Should return message when found", result)
        assertEquals("Should return correct message", 1L, result?.id)
    }

    @Test
    fun `getMessage returns null when not found`() = runTest {
        // Arrange
        coEvery { store.getMessage(999L) } returns null

        // Act
        val result = store.getMessage(999L)

        // Assert
        assertNull("Should return null when message not found", result)
    }

    @Test
    fun `insertMessage returns new message ID`() = runTest {
        // Arrange
        val newMessage = Message(0L, 1L, "+1234567890", "New", System.currentTimeMillis(),
            false, MessageStatus.PENDING, MessageType.SMS, -1)
        coEvery { store.insertMessage(newMessage) } returns 42L

        // Act
        val result = store.insertMessage(newMessage)

        // Assert
        assertTrue("Should return positive message ID", result > 0)
        assertEquals("Should return correct message ID", 42L, result)
    }

    @Test
    fun `updateMessageStatus completes without error`() = runTest {
        // Arrange
        coEvery { store.updateMessageStatus(1L, MessageStatus.SENT) } returns Unit

        // Act
        store.updateMessageStatus(1L, MessageStatus.SENT)

        // Assert
        coVerify { store.updateMessageStatus(1L, MessageStatus.SENT) }
    }

    @Test
    fun `deleteMessage returns true when deleted`() = runTest {
        // Arrange
        coEvery { store.deleteMessage(1L) } returns true

        // Act
        val result = store.deleteMessage(1L)

        // Assert
        assertTrue("Should return true when message deleted", result)
    }

    @Test
    fun `deleteMessage returns false when not found`() = runTest {
        // Arrange
        coEvery { store.deleteMessage(999L) } returns false

        // Act
        val result = store.deleteMessage(999L)

        // Assert
        assertFalse("Should return false when message not found", result)
    }

    // ========================================================================
    // Search
    // ========================================================================

    @Test
    fun `searchMessages returns Flow of matching messages`() = runTest {
        // Arrange
        val searchResults = listOf(
            Message(1L, 1L, "+1234567890", "Hello world", System.currentTimeMillis(),
                true, MessageStatus.RECEIVED, MessageType.SMS, -1)
        )
        every { store.searchMessages("hello") } returns flowOf(searchResults)

        // Act & Assert
        store.searchMessages("hello").test {
            val items = awaitItem()
            assertEquals("Should emit search results", 1, items.size)
            assertTrue("Result should contain search term",
                items[0].body?.contains("Hello", ignoreCase = true) == true)
            awaitComplete()
        }
    }

    @Test
    fun `searchMessages with no matches returns empty Flow`() = runTest {
        // Arrange
        every { store.searchMessages("nonexistent") } returns flowOf(emptyList())

        // Act & Assert
        store.searchMessages("nonexistent").test {
            val items = awaitItem()
            assertTrue("Should emit empty list when no matches", items.isEmpty())
            awaitComplete()
        }
    }

    // ========================================================================
    // Sync
    // ========================================================================

    @Test
    fun `syncWithSystemProvider completes without error`() = runTest {
        // Arrange
        coEvery { store.syncWithSystemProvider() } returns Unit

        // Act
        store.syncWithSystemProvider()

        // Assert
        coVerify { store.syncWithSystemProvider() }
    }

    // ========================================================================
    // Data Class Contracts
    // ========================================================================

    @Test
    fun `Conversation data class has expected properties`() {
        // Act
        val conversation = Conversation(
            id = 1L,
            participants = emptyList(),
            snippet = "Test",
            snippetTimestamp = 123456789L,
            unreadCount = 5,
            isGroup = true,
            isArchived = false
        )

        // Assert
        assertEquals(1L, conversation.id)
        assertEquals("Test", conversation.snippet)
        assertEquals(5, conversation.unreadCount)
        assertTrue(conversation.isGroup)
        assertFalse(conversation.isArchived)
    }

    @Test
    fun `Message data class has expected properties`() {
        // Act
        val message = Message(
            id = 1L,
            conversationId = 2L,
            address = "+1234567890",
            body = "Test message",
            timestamp = System.currentTimeMillis(),
            isIncoming = true,
            status = MessageStatus.RECEIVED,
            type = MessageType.SMS,
            subscriptionId = -1
        )

        // Assert
        assertEquals(1L, message.id)
        assertEquals(2L, message.conversationId)
        assertEquals("+1234567890", message.address)
        assertEquals("Test message", message.body)
        assertTrue(message.isIncoming)
        assertEquals(MessageStatus.RECEIVED, message.status)
        assertEquals(MessageType.SMS, message.type)
    }

    @Test
    fun `MessageStatus enum contains all expected statuses`() {
        // Assert
        val statuses = MessageStatus.values()
        assertTrue(statuses.contains(MessageStatus.DRAFT))
        assertTrue(statuses.contains(MessageStatus.PENDING))
        assertTrue(statuses.contains(MessageStatus.SENDING))
        assertTrue(statuses.contains(MessageStatus.SENT))
        assertTrue(statuses.contains(MessageStatus.DELIVERED))
        assertTrue(statuses.contains(MessageStatus.FAILED))
        assertTrue(statuses.contains(MessageStatus.RECEIVED))
    }

    @Test
    fun `MessageType enum contains SMS and MMS`() {
        // Assert
        val types = MessageType.values()
        assertTrue(types.contains(MessageType.SMS))
        assertTrue(types.contains(MessageType.MMS))
        assertEquals("Should have exactly 2 types", 2, types.size)
    }
}
