package com.technicallyrural.junction.persistence.util

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log

/**
 * Utility for extracting AOSP thread IDs from telephony database.
 *
 * Thread ID identifies a unique conversation:
 * - 1:1 conversations: Single recipient
 * - Group conversations: Multiple recipients (sorted canonically)
 */
object AospThreadIdExtractor {

    private const val TAG = "ThreadIdExtractor"

    /**
     * Get thread ID for a single recipient (1:1 conversation).
     *
     * @param address Phone number of other participant
     * @return Thread ID as string, or newly created thread ID if not exists
     */
    fun getThreadIdForAddress(context: Context, address: String): String {
        // Use AOSP's getOrCreateThreadId
        val threadId = Telephony.Threads.getOrCreateThreadId(context, setOf(address))
        return threadId.toString()
    }

    /**
     * Get thread ID for multiple recipients (group conversation).
     *
     * @param addresses Set of phone numbers (excluding self)
     * @return Thread ID as string
     */
    fun getThreadIdForGroup(context: Context, addresses: Set<String>): String {
        val threadId = Telephony.Threads.getOrCreateThreadId(context, addresses)
        return threadId.toString()
    }

    /**
     * Get thread ID from AOSP message ID (SMS or MMS).
     *
     * @param messageId AOSP message ID from BugleDatabase
     * @param isMms Whether this is an MMS message
     * @return Thread ID as string, or null if not found
     */
    fun getThreadIdFromMessageId(
        context: Context,
        messageId: Long,
        isMms: Boolean = false
    ): String? {
        val uri = if (isMms) {
            Uri.parse("content://mms/$messageId")
        } else {
            Uri.parse("content://sms/$messageId")
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                uri,
                arrayOf("thread_id"),
                null,
                null,
                null
            )

            if (cursor?.moveToFirst() == true) {
                val threadId = cursor.getLong(cursor.getColumnIndexOrThrow("thread_id"))
                return threadId.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get thread_id for message $messageId", e)
        } finally {
            cursor?.close()
        }

        return null
    }

    /**
     * Get participants for a thread ID.
     *
     * @param threadId AOSP thread_id
     * @return List of phone numbers in this conversation
     */
    fun getParticipantsForThread(context: Context, threadId: String): List<String> {
        val participants = mutableListOf<String>()

        // Query recipients table
        val uri = Uri.parse("content://mms-sms/conversations/$threadId/recipients")
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                uri,
                arrayOf("address"),
                null,
                null,
                null
            )

            while (cursor?.moveToNext() == true) {
                val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                if (!address.isNullOrEmpty()) {
                    participants.add(address)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get participants for thread $threadId", e)
        } finally {
            cursor?.close()
        }

        return participants
    }

    /**
     * Get own phone number from telephony.
     *
     * Returns null if SIM not available or permission denied.
     */
    fun getOwnPhoneNumber(context: Context): String? {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE)
                as? android.telephony.TelephonyManager
            return telephonyManager?.line1Number
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get own phone number", e)
            return null
        }
    }
}
