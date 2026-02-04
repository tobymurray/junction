package com.technicallyrural.junction.core.contacts

/**
 * Interface for resolving phone numbers to contact information.
 *
 * This abstracts contact lookup from the upstream implementation,
 * using the standard ContactsContract provider.
 */
interface ContactResolver {

    /**
     * Look up contact info for a phone number.
     *
     * @param phoneNumber The phone number to look up
     * @return Contact info if found, null otherwise
     */
    suspend fun resolveContact(phoneNumber: String): ContactInfo?

    /**
     * Look up multiple contacts at once (more efficient).
     *
     * @param phoneNumbers List of phone numbers
     * @return Map of phone number to contact info
     */
    suspend fun resolveContacts(phoneNumbers: List<String>): Map<String, ContactInfo>

    /**
     * Check if a phone number is a known contact.
     */
    suspend fun isKnownContact(phoneNumber: String): Boolean

    /**
     * Normalize a phone number to standard format.
     */
    fun normalizePhoneNumber(phoneNumber: String): String

    /**
     * Compare two phone numbers for equality (handles formatting differences).
     */
    fun phoneNumbersMatch(number1: String, number2: String): Boolean
}

/**
 * Contact information.
 */
data class ContactInfo(
    val contactId: Long,
    val displayName: String,
    val photoUri: String?,
    val phoneNumbers: List<PhoneNumber>,
    val lookupKey: String
)

/**
 * A phone number associated with a contact.
 */
data class PhoneNumber(
    val number: String,
    val normalizedNumber: String?,
    val type: PhoneNumberType,
    val label: String?
)

/**
 * Type of phone number.
 */
enum class PhoneNumberType {
    MOBILE,
    HOME,
    WORK,
    OTHER
}
