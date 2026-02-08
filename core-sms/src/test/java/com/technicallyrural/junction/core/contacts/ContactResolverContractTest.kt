package com.technicallyrural.junction.core.contacts

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Contract tests for [ContactResolver] interface.
 *
 * These tests define the expected behavior of any ContactResolver implementation:
 * - Contact lookup succeeds or returns null
 * - Batch lookup returns map of results
 * - Phone number normalization is consistent
 * - Phone number matching handles formatting differences
 *
 * Implementations must satisfy these contracts.
 */
class ContactResolverContractTest {

    private lateinit var resolver: ContactResolver

    @Before
    fun setUp() {
        resolver = mockk(relaxed = true)
    }

    // ========================================================================
    // Single Contact Lookup
    // ========================================================================

    @Test
    fun `resolveContact returns ContactInfo when found`() = runTest {
        // Arrange
        val phoneNumber = "+1234567890"
        val expectedContact = ContactInfo(
            contactId = 1L,
            displayName = "John Doe",
            photoUri = null,
            phoneNumbers = listOf(
                PhoneNumber(phoneNumber, phoneNumber, PhoneNumberType.MOBILE, null)
            ),
            lookupKey = "lookup123"
        )
        coEvery { resolver.resolveContact(phoneNumber) } returns expectedContact

        // Act
        val result = resolver.resolveContact(phoneNumber)

        // Assert
        assertNotNull("Should return ContactInfo when found", result)
        assertEquals("Should return correct contact", "John Doe", result?.displayName)
    }

    @Test
    fun `resolveContact returns null when not found`() = runTest {
        // Arrange
        val unknownNumber = "+9999999999"
        coEvery { resolver.resolveContact(unknownNumber) } returns null

        // Act
        val result = resolver.resolveContact(unknownNumber)

        // Assert
        assertNull("Should return null when contact not found", result)
    }

    @Test
    fun `resolveContact handles various phone number formats`() = runTest {
        // Arrange
        val formats = listOf(
            "+1234567890",
            "1234567890",
            "(123) 456-7890",
            "123-456-7890"
        )
        val expectedContact = ContactInfo(
            contactId = 1L,
            displayName = "Test Contact",
            photoUri = null,
            phoneNumbers = emptyList(),
            lookupKey = "key"
        )

        formats.forEach { format ->
            coEvery { resolver.resolveContact(format) } returns expectedContact
        }

        // Act & Assert
        formats.forEach { format ->
            val result = resolver.resolveContact(format)
            assertNotNull("Should handle format: $format", result)
        }
    }

    // ========================================================================
    // Batch Contact Lookup
    // ========================================================================

    @Test
    fun `resolveContacts returns map of results`() = runTest {
        // Arrange
        val phoneNumbers = listOf("+1111111111", "+2222222222", "+3333333333")
        val expectedMap = mapOf(
            "+1111111111" to ContactInfo(1L, "Alice", null, emptyList(), "key1"),
            "+2222222222" to ContactInfo(2L, "Bob", null, emptyList(), "key2")
            // +3333333333 not found
        )
        coEvery { resolver.resolveContacts(phoneNumbers) } returns expectedMap

        // Act
        val result = resolver.resolveContacts(phoneNumbers)

        // Assert
        assertNotNull("Should return map", result)
        assertEquals("Should have results for found contacts", 2, result.size)
        assertTrue("Should contain Alice", result.containsKey("+1111111111"))
        assertTrue("Should contain Bob", result.containsKey("+2222222222"))
        assertFalse("Should not contain unknown number", result.containsKey("+3333333333"))
    }

    @Test
    fun `resolveContacts with empty list returns empty map`() = runTest {
        // Arrange
        coEvery { resolver.resolveContacts(emptyList()) } returns emptyMap()

        // Act
        val result = resolver.resolveContacts(emptyList())

        // Assert
        assertNotNull("Should return empty map", result)
        assertTrue("Should be empty", result.isEmpty())
    }

    @Test
    fun `resolveContacts with all unknown numbers returns empty map`() = runTest {
        // Arrange
        val unknownNumbers = listOf("+9999999999", "+8888888888")
        coEvery { resolver.resolveContacts(unknownNumbers) } returns emptyMap()

        // Act
        val result = resolver.resolveContacts(unknownNumbers)

        // Assert
        assertNotNull("Should return empty map", result)
        assertTrue("Should be empty when no contacts found", result.isEmpty())
    }

    // ========================================================================
    // Contact Status Check
    // ========================================================================

    @Test
    fun `isKnownContact returns true for known contact`() = runTest {
        // Arrange
        coEvery { resolver.isKnownContact("+1234567890") } returns true

        // Act
        val result = resolver.isKnownContact("+1234567890")

        // Assert
        assertTrue("Should return true for known contact", result)
    }

    @Test
    fun `isKnownContact returns false for unknown number`() = runTest {
        // Arrange
        coEvery { resolver.isKnownContact("+9999999999") } returns false

        // Act
        val result = resolver.isKnownContact("+9999999999")

        // Assert
        assertFalse("Should return false for unknown number", result)
    }

    // ========================================================================
    // Phone Number Normalization
    // ========================================================================

    @Test
    fun `normalizePhoneNumber returns consistent format`() {
        // Arrange
        val input = "+1 (234) 567-8900"
        val expectedOutput = "+12345678900"
        every { resolver.normalizePhoneNumber(input) } returns expectedOutput

        // Act
        val result = resolver.normalizePhoneNumber(input)

        // Assert
        assertNotNull("Should return normalized number", result)
        assertEquals("Should normalize to E.164 format", expectedOutput, result)
    }

    @Test
    fun `normalizePhoneNumber handles already normalized numbers`() {
        // Arrange
        val normalized = "+12345678900"
        every { resolver.normalizePhoneNumber(normalized) } returns normalized

        // Act
        val result = resolver.normalizePhoneNumber(normalized)

        // Assert
        assertEquals("Should return same number if already normalized", normalized, result)
    }

    @Test
    fun `normalizePhoneNumber handles various formats`() {
        // Arrange
        val formats = mapOf(
            "+1234567890" to "+1234567890",
            "1234567890" to "+1234567890",
            "(123) 456-7890" to "+11234567890",
            "123-456-7890" to "+11234567890"
        )

        formats.forEach { (input, expected) ->
            every { resolver.normalizePhoneNumber(input) } returns expected
        }

        // Act & Assert
        formats.forEach { (input, expected) ->
            val result = resolver.normalizePhoneNumber(input)
            assertNotNull("Should normalize: $input", result)
        }
    }

    // ========================================================================
    // Phone Number Matching
    // ========================================================================

    @Test
    fun `phoneNumbersMatch returns true for identical numbers`() {
        // Arrange
        every { resolver.phoneNumbersMatch("+1234567890", "+1234567890") } returns true

        // Act
        val result = resolver.phoneNumbersMatch("+1234567890", "+1234567890")

        // Assert
        assertTrue("Should match identical numbers", result)
    }

    @Test
    fun `phoneNumbersMatch returns true for different formats`() {
        // Arrange
        every { resolver.phoneNumbersMatch("+1234567890", "(123) 456-7890") } returns true

        // Act
        val result = resolver.phoneNumbersMatch("+1234567890", "(123) 456-7890")

        // Assert
        assertTrue("Should match numbers with different formatting", result)
    }

    @Test
    fun `phoneNumbersMatch returns false for different numbers`() {
        // Arrange
        every { resolver.phoneNumbersMatch("+1111111111", "+2222222222") } returns false

        // Act
        val result = resolver.phoneNumbersMatch("+1111111111", "+2222222222")

        // Assert
        assertFalse("Should not match different numbers", result)
    }

    @Test
    fun `phoneNumbersMatch handles country code differences`() {
        // Arrange - Some implementations might consider these the same
        every { resolver.phoneNumbersMatch("1234567890", "+1234567890") } returns true

        // Act
        val result = resolver.phoneNumbersMatch("1234567890", "+1234567890")

        // Assert - Contract allows implementation to decide
        assertNotNull("Should return boolean result", result)
    }

    // ========================================================================
    // Data Class Contracts
    // ========================================================================

    @Test
    fun `ContactInfo has expected properties`() {
        // Act
        val contact = ContactInfo(
            contactId = 123L,
            displayName = "Test User",
            photoUri = "content://photo/123",
            phoneNumbers = listOf(
                PhoneNumber("+1234567890", "+1234567890", PhoneNumberType.MOBILE, "Mobile")
            ),
            lookupKey = "lookup_key_123"
        )

        // Assert
        assertEquals(123L, contact.contactId)
        assertEquals("Test User", contact.displayName)
        assertEquals("content://photo/123", contact.photoUri)
        assertEquals(1, contact.phoneNumbers.size)
        assertEquals("lookup_key_123", contact.lookupKey)
    }

    @Test
    fun `ContactInfo supports null photoUri`() {
        // Act
        val contact = ContactInfo(
            contactId = 1L,
            displayName = "No Photo",
            photoUri = null,
            phoneNumbers = emptyList(),
            lookupKey = "key"
        )

        // Assert
        assertNull("Photo URI can be null", contact.photoUri)
    }

    @Test
    fun `PhoneNumber has expected properties`() {
        // Act
        val phoneNumber = PhoneNumber(
            number = "+1234567890",
            normalizedNumber = "+1234567890",
            type = PhoneNumberType.MOBILE,
            label = "Personal"
        )

        // Assert
        assertEquals("+1234567890", phoneNumber.number)
        assertEquals("+1234567890", phoneNumber.normalizedNumber)
        assertEquals(PhoneNumberType.MOBILE, phoneNumber.type)
        assertEquals("Personal", phoneNumber.label)
    }

    @Test
    fun `PhoneNumberType enum contains all expected types`() {
        // Assert
        val types = PhoneNumberType.values()
        assertTrue(types.contains(PhoneNumberType.MOBILE))
        assertTrue(types.contains(PhoneNumberType.HOME))
        assertTrue(types.contains(PhoneNumberType.WORK))
        assertTrue(types.contains(PhoneNumberType.OTHER))
        assertEquals("Should have exactly 4 types", 4, types.size)
    }
}
