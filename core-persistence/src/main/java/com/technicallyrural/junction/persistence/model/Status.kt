package com.technicallyrural.junction.persistence.model

enum class Status {
    /**
     * Message queued for sending, not yet attempted.
     */
    PENDING,

    /**
     * Message sent to destination, waiting for confirmation.
     */
    SENT,

    /**
     * Send confirmed (Matrix event ID received or SMS sent successfully).
     */
    CONFIRMED,

    /**
     * Permanent failure after max retries.
     */
    FAILED
}
