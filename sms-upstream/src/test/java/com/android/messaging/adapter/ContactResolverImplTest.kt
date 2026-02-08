package com.android.messaging.adapter

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.technicallyrural.junction.core.contacts.ContactResolver
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Lightweight Robolectric tests for [ContactResolverImpl].
 *
 * Following the pragmatic testing approach established in TESTING_STRATEGY.md,
 * these tests verify adapter basics without requiring full AOSP initialization.
 *
 * See SmsTransportImplTest for detailed rationale on testing approach.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ContactResolverImplTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `adapter can be instantiated with Context`() {
        // Act
        val resolver = ContactResolverImpl(context)

        // Assert
        assertNotNull("Adapter should be created", resolver)
    }

    @Test
    fun `adapter implements ContactResolver interface`() {
        // Act
        val resolver: ContactResolver = ContactResolverImpl(context)

        // Assert
        assertTrue("Must implement ContactResolver", resolver is ContactResolver)
    }

    @Test
    fun `adapter exposes all required ContactResolver methods`() {
        val resolver: ContactResolver = ContactResolverImpl(context)

        // Verify methods exist (compile-time check)
        assertNotNull("resolveContact exists", resolver::resolveContact)
        assertNotNull("resolveContacts exists", resolver::resolveContacts)
        assertNotNull("isKnownContact exists", resolver::isKnownContact)
        assertNotNull("normalizePhoneNumber exists", resolver::normalizePhoneNumber)
        assertNotNull("phoneNumbersMatch exists", resolver::phoneNumbersMatch)
    }

    @Test
    fun `adapter follows established creation pattern`() {
        // Verify adapter follows same pattern as other adapters
        // (Context-only constructor, implements interface)

        val resolver = ContactResolverImpl(context)

        assertTrue("Should implement interface", resolver is ContactResolver)
        assertNotNull("Should accept Context", context)
    }
}
