package com.technicallyrural.junction.persistence.util

import java.security.MessageDigest

/**
 * Utility for generating deduplication keys.
 *
 * Key = SHA-256(conversationId|timestamp|bodyHash)
 */
object DedupKeyGenerator {

    /**
     * Generate deduplication key from message components.
     */
    fun generate(
        conversationId: String,
        timestamp: Long,
        body: String
    ): String {
        // Normalize body (trim, collapse whitespace)
        val normalizedBody = normalizeBody(body)

        // Hash body to fixed length
        val bodyHash = sha256(normalizedBody).substring(0, 16)

        // Composite key with conversation context
        val composite = "$conversationId|$timestamp|$bodyHash"
        return sha256(composite)
    }

    /**
     * Normalize message body for consistent hashing.
     */
    fun normalizeBody(body: String): String {
        return body.trim().replace("\\s+".toRegex(), " ")
    }

    /**
     * SHA-256 hash helper.
     */
    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get body hash (first 16 chars of SHA-256).
     */
    fun getBodyHash(body: String): String {
        return sha256(normalizeBody(body)).substring(0, 16)
    }
}
