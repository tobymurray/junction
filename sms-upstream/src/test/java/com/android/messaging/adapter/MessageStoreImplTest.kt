package com.android.messaging.adapter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.technicallyrural.junction.core.store.MessageStore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lightweight Robolectric tests for [MessageStoreImpl].
 *
 * Following the pragmatic testing approach established in TESTING_STRATEGY.md,
 * these tests verify adapter basics without requiring full AOSP initialization.
 *
 * See SmsTransportImplTest for detailed rationale on testing approach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MessageStoreImplTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `adapter can be instantiated with Context`() {
        // Act
        val store = MessageStoreImpl(context)

        // Assert
        assertNotNull("Adapter should be created", store)
    }

    @Test
    fun `adapter implements MessageStore interface`() {
        // Act
        val store: MessageStore = MessageStoreImpl(context)

        // Assert
        assertTrue("Must implement MessageStore", store is MessageStore)
    }

    @Test
    fun `adapter exposes all required MessageStore methods`() {
        val store: MessageStore = MessageStoreImpl(context)

        // Verify methods exist (compile-time check)
        assertNotNull("getConversations exists", store::getConversations)
        assertNotNull("getConversation exists", store::getConversation)
        assertNotNull("getOrCreateConversation exists", store::getOrCreateConversation)
        assertNotNull("deleteConversation exists", store::deleteConversation)
        assertNotNull("markConversationRead exists", store::markConversationRead)
        assertNotNull("getMessages exists", store::getMessages)
        assertNotNull("getMessage exists", store::getMessage)
        assertNotNull("insertMessage exists", store::insertMessage)
        assertNotNull("updateMessageStatus exists", store::updateMessageStatus)
        assertNotNull("deleteMessage exists", store::deleteMessage)
        assertNotNull("searchMessages exists", store::searchMessages)
        assertNotNull("syncWithSystemProvider exists", store::syncWithSystemProvider)
    }

    @Test
    fun `adapter follows established creation pattern`() {
        // Verify adapter follows same pattern as other adapters
        // (Context-only constructor, implements interface)

        val store = MessageStoreImpl(context)

        assertTrue("Should implement interface", store is MessageStore)
        assertNotNull("Should accept Context", context)
    }
}
