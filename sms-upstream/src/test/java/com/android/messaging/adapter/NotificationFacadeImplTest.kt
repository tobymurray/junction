package com.android.messaging.adapter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.technicallyrural.junction.core.notification.NotificationFacade
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lightweight Robolectric tests for [NotificationFacadeImpl].
 *
 * Following the pragmatic testing approach established in TESTING_STRATEGY.md,
 * these tests verify adapter basics without requiring full AOSP initialization.
 *
 * See SmsTransportImplTest for detailed rationale on testing approach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationFacadeImplTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `adapter can be instantiated with Context`() {
        // Act
        val facade = NotificationFacadeImpl(context)

        // Assert
        assertNotNull("Adapter should be created", facade)
    }

    @Test
    fun `adapter implements NotificationFacade interface`() {
        // Act
        val facade: NotificationFacade = NotificationFacadeImpl(context)

        // Assert
        assertTrue("Must implement NotificationFacade", facade is NotificationFacade)
    }

    @Test
    fun `adapter exposes all required NotificationFacade methods`() {
        val facade: NotificationFacade = NotificationFacadeImpl(context)

        // Verify methods exist (compile-time check)
        assertNotNull("showNewMessageNotification exists", facade::showNewMessageNotification)
        assertNotNull("showMultipleMessagesNotification exists",
            facade::showMultipleMessagesNotification)
        assertNotNull("showSendFailedNotification exists", facade::showSendFailedNotification)
        assertNotNull("cancelConversationNotifications exists",
            facade::cancelConversationNotifications)
        assertNotNull("cancelAllNotifications exists", facade::cancelAllNotifications)
        assertNotNull("createNotificationChannels exists", facade::createNotificationChannels)
        assertNotNull("areNotificationsEnabled exists", facade::areNotificationsEnabled)
    }

    @Test
    fun `adapter follows established creation pattern`() {
        // Verify adapter follows same pattern as other adapters
        // (Context-only constructor, implements interface)

        val facade = NotificationFacadeImpl(context)

        assertTrue("Should implement interface", facade is NotificationFacade)
        assertNotNull("Should accept Context", context)
    }
}
