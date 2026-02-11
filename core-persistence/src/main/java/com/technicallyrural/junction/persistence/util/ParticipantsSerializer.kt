package com.technicallyrural.junction.persistence.util

/**
 * Utility for serializing/deserializing participant lists to/from JSON.
 */
object ParticipantsSerializer {

    /**
     * Serialize participants to JSON array string.
     *
     * Example: ["+12345678901", "+19876543210"]
     *
     * Array is sorted for consistent comparison.
     */
    fun serialize(participants: List<String>): String {
        val sorted = participants.sorted()
        return sorted.joinToString(
            prefix = "[\"",
            separator = "\",\"",
            postfix = "\"]"
        )
    }

    /**
     * Deserialize JSON array string to participant list.
     */
    fun deserialize(json: String): List<String> {
        if (json.isEmpty() || json == "[]") return emptyList()

        return json
            .removeSurrounding("[", "]")
            .split(",")
            .map { it.trim().removeSurrounding("\"") }
            .filter { it.isNotEmpty() }
    }

    /**
     * Check if participant is in serialized JSON array.
     */
    fun contains(json: String, participant: String): Boolean {
        return deserialize(json).contains(participant)
    }
}
