/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.messaging.adapter

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import com.android.messaging.datamodel.BugleNotifications
import com.android.messaging.datamodel.DataModel
import com.android.messaging.datamodel.DatabaseHelper
import com.android.messaging.datamodel.DatabaseHelper.ConversationColumns
import com.android.messaging.datamodel.DatabaseHelper.MessageColumns
import com.android.messaging.datamodel.DatabaseHelper.ParticipantColumns
import com.android.messaging.datamodel.MessagingContentProvider
import com.android.messaging.datamodel.data.MessageData
import com.technicallyrural.junction.core.store.Conversation
import com.technicallyrural.junction.core.store.Message
import com.technicallyrural.junction.core.store.MessageStatus
import com.technicallyrural.junction.core.store.MessageStore
import com.technicallyrural.junction.core.store.MessageType
import com.technicallyrural.junction.core.store.MmsPartInfo
import com.technicallyrural.junction.core.store.Participant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Implementation of [MessageStore] that bridges to AOSP Messaging's data layer.
 *
 * This adapter provides access to conversations and messages stored by AOSP Messaging.
 * It uses callbackFlow to convert cursor-based ContentObserver updates to Kotlin Flow.
 *
 * Note: Some operations delegate to AOSP's action system for proper integration.
 *
 * @param context Application context for content resolver access
 */
class MessageStoreImpl(private val context: Context) : MessageStore {

    private val contentResolver: ContentResolver = context.contentResolver

    // ========================================================================
    // Conversations
    // ========================================================================

    override fun getConversations(): Flow<List<Conversation>> = callbackFlow {
        val uri = MessagingContentProvider.CONVERSATIONS_URI

        // Initial load
        trySend(loadConversations())

        // Observe for changes
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(loadConversations())
            }
        }

        contentResolver.registerContentObserver(uri, true, observer)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getConversation(conversationId: Long): Conversation? =
        withContext(Dispatchers.IO) {
            val uri = MessagingContentProvider.buildConversationMetadataUri(conversationId.toString())
            contentResolver.query(uri, CONVERSATION_PROJECTION, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursorToConversation(cursor, conversationId)
                } else null
            }
        }

    override suspend fun getOrCreateConversation(address: String): Conversation =
        withContext(Dispatchers.IO) {
            // For now, just search for existing conversation with this address
            // Full implementation would use BugleDatabaseOperations with ParticipantData
            val conversations = loadConversations()
            val existing = conversations.find { conv ->
                conv.participants.any { it.address.contains(address) || address.contains(it.address) }
            }

            existing ?: throw UnsupportedOperationException(
                "Creating new conversations not yet implemented. Use AOSP Messaging UI."
            )
        }

    override suspend fun deleteConversation(conversationId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val db = DataModel.get().database
            try {
                db.beginTransaction()
                // Delete messages first
                db.delete(
                    DatabaseHelper.MESSAGES_TABLE,
                    "${MessageColumns.CONVERSATION_ID} = ?",
                    arrayOf(conversationId.toString())
                )
                // Delete conversation
                val deleted = db.delete(
                    DatabaseHelper.CONVERSATIONS_TABLE,
                    "${ConversationColumns._ID} = ?",
                    arrayOf(conversationId.toString())
                )
                db.setTransactionSuccessful()
                deleted > 0
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun markConversationRead(conversationId: Long): Unit =
        withContext(Dispatchers.IO) {
            // Use AOSP's notification system which handles marking read properly
            BugleNotifications.markMessagesAsRead(conversationId.toString())
        }

    // ========================================================================
    // Messages
    // ========================================================================

    override fun getMessages(conversationId: Long): Flow<List<Message>> = callbackFlow {
        val uri = MessagingContentProvider.buildConversationMessagesUri(conversationId.toString())

        // Initial load
        trySend(loadMessages(conversationId))

        // Observe for changes
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(loadMessages(conversationId))
            }
        }

        contentResolver.registerContentObserver(uri, true, observer)

        awaitClose {
            contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getMessage(messageId: Long): Message? =
        withContext(Dispatchers.IO) {
            val db = DataModel.get().database
            db.query(
                DatabaseHelper.MESSAGES_TABLE,
                MESSAGE_PROJECTION,
                "${MessageColumns._ID} = ?",
                arrayOf(messageId.toString()),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursorToMessage(cursor)
                } else null
            }
        }

    override suspend fun insertMessage(message: Message): Long =
        withContext(Dispatchers.IO) {
            val db = DataModel.get().database
            val values = messageToContentValues(message)

            db.beginTransaction()
            try {
                val id = db.insert(DatabaseHelper.MESSAGES_TABLE, null, values)
                db.setTransactionSuccessful()
                id
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun updateMessageStatus(messageId: Long, status: MessageStatus): Unit =
        withContext(Dispatchers.IO) {
            val db = DataModel.get().database
            val values = ContentValues().apply {
                put(MessageColumns.STATUS, mapStatusToAosp(status))
            }
            db.update(
                DatabaseHelper.MESSAGES_TABLE,
                values,
                "${MessageColumns._ID} = ?",
                arrayOf(messageId.toString())
            )
        }

    override suspend fun deleteMessage(messageId: Long): Boolean =
        withContext(Dispatchers.IO) {
            val db = DataModel.get().database
            val deleted = db.delete(
                DatabaseHelper.MESSAGES_TABLE,
                "${MessageColumns._ID} = ?",
                arrayOf(messageId.toString())
            )
            deleted > 0
        }

    // ========================================================================
    // Search
    // ========================================================================

    override fun searchMessages(query: String): Flow<List<Message>> = callbackFlow {
        trySend(performSearch(query))
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    // ========================================================================
    // Sync
    // ========================================================================

    override suspend fun syncWithSystemProvider(): Unit =
        withContext(Dispatchers.IO) {
            // Sync is handled automatically by AOSP Messaging's SyncMessagesAction
            // This is a no-op placeholder - sync happens via broadcast receivers
        }

    // ========================================================================
    // Private helpers - Loading
    // ========================================================================

    private fun loadConversations(): List<Conversation> {
        val conversations = mutableListOf<Conversation>()
        contentResolver.query(
            MessagingContentProvider.CONVERSATIONS_URI,
            CONVERSATION_PROJECTION,
            "${ConversationColumns.ARCHIVE_STATUS} = 0",
            null,
            "${ConversationColumns.SORT_TIMESTAMP} DESC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ConversationColumns._ID))
                conversations.add(cursorToConversation(cursor, id))
            }
        }
        return conversations
    }

    private fun loadMessages(conversationId: Long): List<Message> {
        val messages = mutableListOf<Message>()
        val db = DataModel.get().database
        db.query(
            DatabaseHelper.MESSAGES_TABLE,
            MESSAGE_PROJECTION,
            "${MessageColumns.CONVERSATION_ID} = ?",
            arrayOf(conversationId.toString()),
            null, null,
            "${MessageColumns.RECEIVED_TIMESTAMP} ASC"
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                messages.add(cursorToMessage(cursor))
            }
        }
        return messages
    }

    private fun performSearch(query: String): List<Message> {
        // Note: Full-text search requires joining with parts table
        // This is a simplified implementation
        return emptyList()
    }

    // ========================================================================
    // Private helpers - Cursor mapping
    // ========================================================================

    private fun cursorToConversation(cursor: Cursor, id: Long): Conversation {
        // Load participants for this conversation
        val participants = loadParticipants(id)

        return Conversation(
            id = id,
            participants = participants,
            snippet = cursor.getStringOrNull(ConversationColumns.SNIPPET_TEXT),
            snippetTimestamp = cursor.getLongOrDefault(ConversationColumns.SORT_TIMESTAMP, 0L),
            unreadCount = 0, // Would need separate query
            isGroup = participants.size > 1,
            isArchived = cursor.getIntOrDefault(ConversationColumns.ARCHIVE_STATUS, 0) != 0
        )
    }

    private fun loadParticipants(conversationId: Long): List<Participant> {
        val participants = mutableListOf<Participant>()
        val uri = MessagingContentProvider.buildConversationParticipantsUri(conversationId.toString())

        contentResolver.query(uri, PARTICIPANT_PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                participants.add(cursorToParticipant(cursor))
            }
        }
        return participants
    }

    private fun cursorToParticipant(cursor: Cursor): Participant {
        return Participant(
            address = cursor.getStringOrNull(ParticipantColumns.NORMALIZED_DESTINATION) ?: "",
            displayName = cursor.getStringOrNull(ParticipantColumns.FULL_NAME),
            photoUri = cursor.getStringOrNull(ParticipantColumns.PROFILE_PHOTO_URI),
            isContact = cursor.getStringOrNull(ParticipantColumns.CONTACT_ID) != null
        )
    }

    private fun cursorToMessage(cursor: Cursor): Message {
        val protocol = cursor.getIntOrDefault(MessageColumns.PROTOCOL, 0)
        val statusCode = cursor.getIntOrDefault(MessageColumns.STATUS, 0)
        val senderParticipantId = cursor.getLongOrDefault(MessageColumns.SENDER_PARTICIPANT_ID, -1)

        return Message(
            id = cursor.getLong(cursor.getColumnIndexOrThrow(MessageColumns._ID)),
            conversationId = cursor.getLong(
                cursor.getColumnIndexOrThrow(MessageColumns.CONVERSATION_ID)
            ),
            address = "", // Would need join with participants table
            body = null, // Body is in parts table
            timestamp = cursor.getLongOrDefault(MessageColumns.RECEIVED_TIMESTAMP, 0L),
            isIncoming = senderParticipantId > 0, // Sender set means incoming
            status = mapAospStatusToStatus(statusCode),
            type = if (protocol == MessageData.PROTOCOL_MMS ||
                protocol == MessageData.PROTOCOL_MMS_PUSH_NOTIFICATION
            ) MessageType.MMS else MessageType.SMS,
            subscriptionId = cursor.getIntOrDefault(MessageColumns.SELF_PARTICIPANT_ID, -1),
            mmsSubject = cursor.getStringOrNull(MessageColumns.MMS_SUBJECT),
            mmsParts = null // Would need separate query to parts table
        )
    }

    private fun messageToContentValues(message: Message): ContentValues {
        return ContentValues().apply {
            put(MessageColumns.CONVERSATION_ID, message.conversationId)
            put(MessageColumns.RECEIVED_TIMESTAMP, message.timestamp)
            put(MessageColumns.SENT_TIMESTAMP, message.timestamp)
            put(MessageColumns.PROTOCOL, if (message.type == MessageType.MMS) 1 else 0)
            put(MessageColumns.STATUS, mapStatusToAosp(message.status))
            put(MessageColumns.SEEN, 1)
            put(MessageColumns.READ, 1)
        }
    }

    // ========================================================================
    // Private helpers - Status mapping
    // ========================================================================

    private fun mapAospStatusToStatus(aospStatus: Int): MessageStatus {
        return when (aospStatus) {
            MessageData.BUGLE_STATUS_OUTGOING_DRAFT -> MessageStatus.DRAFT
            MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND -> MessageStatus.PENDING
            MessageData.BUGLE_STATUS_OUTGOING_SENDING -> MessageStatus.SENDING
            MessageData.BUGLE_STATUS_OUTGOING_RESENDING -> MessageStatus.SENDING
            MessageData.BUGLE_STATUS_OUTGOING_AWAITING_RETRY -> MessageStatus.PENDING
            MessageData.BUGLE_STATUS_OUTGOING_COMPLETE -> MessageStatus.SENT
            MessageData.BUGLE_STATUS_OUTGOING_DELIVERED -> MessageStatus.DELIVERED
            MessageData.BUGLE_STATUS_OUTGOING_FAILED,
            MessageData.BUGLE_STATUS_OUTGOING_FAILED_EMERGENCY_NUMBER -> MessageStatus.FAILED
            MessageData.BUGLE_STATUS_INCOMING_COMPLETE -> MessageStatus.RECEIVED
            else -> MessageStatus.RECEIVED
        }
    }

    private fun mapStatusToAosp(status: MessageStatus): Int {
        return when (status) {
            MessageStatus.DRAFT -> MessageData.BUGLE_STATUS_OUTGOING_DRAFT
            MessageStatus.PENDING -> MessageData.BUGLE_STATUS_OUTGOING_YET_TO_SEND
            MessageStatus.SENDING -> MessageData.BUGLE_STATUS_OUTGOING_SENDING
            MessageStatus.SENT -> MessageData.BUGLE_STATUS_OUTGOING_COMPLETE
            MessageStatus.DELIVERED -> MessageData.BUGLE_STATUS_OUTGOING_DELIVERED
            MessageStatus.FAILED -> MessageData.BUGLE_STATUS_OUTGOING_FAILED
            MessageStatus.RECEIVED -> MessageData.BUGLE_STATUS_INCOMING_COMPLETE
        }
    }

    // ========================================================================
    // Cursor extension helpers
    // ========================================================================

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getString(index) else null
    }

    private fun Cursor.getIntOrDefault(columnName: String, default: Int): Int {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getInt(index) else default
    }

    private fun Cursor.getLongOrDefault(columnName: String, default: Long): Long {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else default
    }

    companion object {
        private val CONVERSATION_PROJECTION = arrayOf(
            ConversationColumns._ID,
            ConversationColumns.SNIPPET_TEXT,
            ConversationColumns.SORT_TIMESTAMP,
            ConversationColumns.ARCHIVE_STATUS
        )

        private val MESSAGE_PROJECTION = arrayOf(
            MessageColumns._ID,
            MessageColumns.CONVERSATION_ID,
            MessageColumns.SENDER_PARTICIPANT_ID,
            MessageColumns.SELF_PARTICIPANT_ID,
            MessageColumns.RECEIVED_TIMESTAMP,
            MessageColumns.SENT_TIMESTAMP,
            MessageColumns.PROTOCOL,
            MessageColumns.STATUS,
            MessageColumns.MMS_SUBJECT
        )

        private val PARTICIPANT_PROJECTION = arrayOf(
            ParticipantColumns._ID,
            ParticipantColumns.NORMALIZED_DESTINATION,
            ParticipantColumns.FULL_NAME,
            ParticipantColumns.PROFILE_PHOTO_URI,
            ParticipantColumns.CONTACT_ID
        )
    }
}
