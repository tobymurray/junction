package com.technicallyrural.junction.core

import com.technicallyrural.junction.core.contacts.ContactResolver
import com.technicallyrural.junction.core.notification.NotificationFacade
import com.technicallyrural.junction.core.store.MessageStore
import com.technicallyrural.junction.core.transport.SmsReceiveListener
import com.technicallyrural.junction.core.transport.SmsTransport
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CoreSmsRegistry].
 *
 * These tests verify the dependency injection registry behaves correctly:
 * - Initialization with adapter implementations
 * - Listener registration/unregistration
 *
 * Note: CoreSmsRegistry is a singleton. In production, it's initialized once by
 * BugleApplication. In tests, we initialize it fresh for each test.
 */
class CoreSmsRegistryTest {

    private lateinit var mockTransport: SmsTransport
    private lateinit var mockStore: MessageStore
    private lateinit var mockNotification: NotificationFacade
    private lateinit var mockContacts: ContactResolver

    @Before
    fun setUp() {
        // Create fresh mocks for each test
        mockTransport = mockk(relaxed = true)
        mockStore = mockk(relaxed = true)
        mockNotification = mockk(relaxed = true)
        mockContacts = mockk(relaxed = true)

        // Initialize registry for each test
        // This simulates what BugleApplication.initializeSync() does
        CoreSmsRegistry.initialize(
            smsTransport = mockTransport,
            messageStore = mockStore,
            notificationFacade = mockNotification,
            contactResolver = mockContacts,
            smsReceiveListener = null
        )
    }

    @Test
    fun `isInitialized returns true after initialization`() {
        assertTrue("Registry should be initialized after initialize() is called",
            CoreSmsRegistry.isInitialized)
    }

    @Test
    fun `smsTransport returns initialized instance`() {
        assertSame("SmsTransport should return the initialized instance",
            mockTransport, CoreSmsRegistry.smsTransport)
    }

    @Test
    fun `messageStore returns initialized instance`() {
        assertSame("MessageStore should return the initialized instance",
            mockStore, CoreSmsRegistry.messageStore)
    }

    @Test
    fun `notificationFacade returns initialized instance`() {
        assertSame("NotificationFacade should return the initialized instance",
            mockNotification, CoreSmsRegistry.notificationFacade)
    }

    @Test
    fun `contactResolver returns initialized instance`() {
        assertSame("ContactResolver should return the initialized instance",
            mockContacts, CoreSmsRegistry.contactResolver)
    }

    @Test
    fun `smsReceiveListener returns null when not registered`() {
        // Registry initialized with null listener in setUp()
        assertNull("SmsReceiveListener should be null when not registered",
            CoreSmsRegistry.smsReceiveListener)
    }

    @Test
    fun `registerSmsReceiveListener sets listener`() {
        // Arrange
        val mockListener = mockk<SmsReceiveListener>()

        // Act
        CoreSmsRegistry.registerSmsReceiveListener(mockListener)

        // Assert
        assertSame("SmsReceiveListener should be registered",
            mockListener, CoreSmsRegistry.smsReceiveListener)
    }

    @Test
    fun `registerSmsReceiveListener can replace existing listener`() {
        // Arrange
        val firstListener = mockk<SmsReceiveListener>()
        val secondListener = mockk<SmsReceiveListener>()

        CoreSmsRegistry.registerSmsReceiveListener(firstListener)

        // Act
        CoreSmsRegistry.registerSmsReceiveListener(secondListener)

        // Assert
        assertSame("SmsReceiveListener should be replaced with new listener",
            secondListener, CoreSmsRegistry.smsReceiveListener)
    }

    @Test
    fun `unregisterSmsReceiveListener clears listener`() {
        // Arrange
        val mockListener = mockk<SmsReceiveListener>()
        CoreSmsRegistry.registerSmsReceiveListener(mockListener)

        // Act
        CoreSmsRegistry.unregisterSmsReceiveListener()

        // Assert
        assertNull("SmsReceiveListener should be null after unregister",
            CoreSmsRegistry.smsReceiveListener)
    }

    @Test
    fun `initialize with non-null listener sets listener`() {
        // Arrange
        val mockListener = mockk<SmsReceiveListener>()

        // Act
        CoreSmsRegistry.initialize(
            smsTransport = mockTransport,
            messageStore = mockStore,
            notificationFacade = mockNotification,
            contactResolver = mockContacts,
            smsReceiveListener = mockListener
        )

        // Assert
        assertSame("SmsReceiveListener should be set during initialization",
            mockListener, CoreSmsRegistry.smsReceiveListener)
    }
}

