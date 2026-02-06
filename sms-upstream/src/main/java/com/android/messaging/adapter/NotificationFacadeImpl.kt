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

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.android.messaging.datamodel.BugleNotifications
import com.technicallyrural.junction.core.notification.NotificationFacade
import com.technicallyrural.junction.core.store.Conversation
import com.technicallyrural.junction.core.store.Message

/**
 * Implementation of [NotificationFacade] that bridges to AOSP Messaging's
 * notification system.
 *
 * This adapter wraps [BugleNotifications] to provide the interface expected
 * by the app module.
 *
 * @param context Application context for notification management
 */
class NotificationFacadeImpl(private val context: Context) : NotificationFacade {

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override fun showNewMessageNotification(message: Message, conversation: Conversation) {
        // AOSP's BugleNotifications handles this automatically when messages arrive
        // through the broadcast receivers. This method is for manual triggering.
        BugleNotifications.update(false, BugleNotifications.UPDATE_ALL)
    }

    override fun showMultipleMessagesNotification(messages: List<Message>) {
        // AOSP handles multiple message notifications via the same update mechanism
        BugleNotifications.update(false, BugleNotifications.UPDATE_ALL)
    }

    override fun showSendFailedNotification(message: Message, conversation: Conversation) {
        // AOSP's ProcessSentMessageAction handles failure notifications
        // This triggers a general notification update
        BugleNotifications.update(false, BugleNotifications.UPDATE_ERRORS)
    }

    override fun cancelConversationNotifications(conversationId: Long) {
        // Mark messages as read, which cancels notifications for that conversation
        BugleNotifications.markMessagesAsRead(conversationId.toString())
    }

    override fun cancelAllNotifications() {
        // Cancel all notifications via the NotificationManager directly
        // since BugleNotifications.cancel() is package-private
        notificationManager.cancelAll()
    }

    override fun createNotificationChannels() {
        BugleNotifications.createNotificationChannels()
    }

    override fun areNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationManager.areNotificationsEnabled()
        } else {
            true // Assume enabled on older versions
        }
    }

}
