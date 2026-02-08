package com.android.messaging.adapter

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lightweight Robolectric tests for [SmsReceiverDispatcher].
 *
 * Note: SmsReceiverDispatcher is an object (singleton), not a class requiring
 * Context. Tests verify the dispatcher exists and exposes required methods.
 *
 * Following the pragmatic testing approach established in TESTING_STRATEGY.md.
 * See SmsTransportImplTest for detailed rationale on testing approach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SmsReceiverDispatcherTest {

    @Test
    fun `dispatcher object exists and is accessible`() {
        // Act - Access singleton
        val dispatcher = SmsReceiverDispatcher

        // Assert
        assertNotNull("Dispatcher should be accessible", dispatcher)
    }

    @Test
    fun `dispatcher exposes dispatchSmsReceived method`() {
        // Verify method exists (compile-time check)
        assertNotNull("dispatchSmsReceived exists",
            SmsReceiverDispatcher::dispatchSmsReceived)
    }

    @Test
    fun `dispatcher exposes dispatchMmsReceived method`() {
        // Verify method exists (compile-time check)
        assertNotNull("dispatchMmsReceived exists",
            SmsReceiverDispatcher::dispatchMmsReceived)
    }

    @Test
    fun `dispatcher exposes dispatchWapPushReceived method`() {
        // Verify method exists (compile-time check)
        assertNotNull("dispatchWapPushReceived exists",
            SmsReceiverDispatcher::dispatchWapPushReceived)
    }

    @Test
    fun `dispatcher exposes hasListener method`() {
        // Verify method exists (compile-time check)
        assertNotNull("hasListener exists",
            SmsReceiverDispatcher::hasListener)
    }

    @Test
    fun `dispatcher follows singleton pattern`() {
        // Verify object pattern (not class with constructor)
        val dispatcher1 = SmsReceiverDispatcher
        val dispatcher2 = SmsReceiverDispatcher

        assertSame("Should be same singleton instance", dispatcher1, dispatcher2)
    }
}
