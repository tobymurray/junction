package com.technicallyrural.junction.core

import com.technicallyrural.junction.core.contacts.ContactResolver
import com.technicallyrural.junction.core.notification.NotificationFacade
import com.technicallyrural.junction.core.store.MessageStore
import com.technicallyrural.junction.core.transport.OutboundMessageObserver
import com.technicallyrural.junction.core.transport.SmsReceiveListener
import com.technicallyrural.junction.core.transport.SmsTransport

/**
 * Unified registry for all core-sms interface implementations.
 *
 * The sms-upstream module registers adapter implementations during app startup
 * (in BugleApplication.initializeSync). The app module can then access them
 * via property accessors without importing any AOSP classes.
 *
 * SmsReceiveListener and OutboundMessageObserver are registered separately
 * by the app module since the app owns the BroadcastReceivers and Matrix bridging.
 */
object CoreSmsRegistry {

    private var _smsTransport: SmsTransport? = null
    private var _messageStore: MessageStore? = null
    private var _notificationFacade: NotificationFacade? = null
    private var _contactResolver: ContactResolver? = null
    private var _smsReceiveListener: SmsReceiveListener? = null
    private var _outboundMessageObserver: OutboundMessageObserver? = null

    val isInitialized: Boolean
        get() = _smsTransport != null

    val smsTransport: SmsTransport
        get() = _smsTransport
            ?: throw IllegalStateException("CoreSmsRegistry not initialized. Call initialize() first.")

    val messageStore: MessageStore
        get() = _messageStore
            ?: throw IllegalStateException("CoreSmsRegistry not initialized. Call initialize() first.")

    val notificationFacade: NotificationFacade
        get() = _notificationFacade
            ?: throw IllegalStateException("CoreSmsRegistry not initialized. Call initialize() first.")

    val contactResolver: ContactResolver
        get() = _contactResolver
            ?: throw IllegalStateException("CoreSmsRegistry not initialized. Call initialize() first.")

    val smsReceiveListener: SmsReceiveListener?
        get() = _smsReceiveListener

    val outboundMessageObserver: OutboundMessageObserver?
        get() = _outboundMessageObserver

    /**
     * Initialize the registry with adapter implementations.
     * Called once during app startup from BugleApplication.initializeSync().
     */
    fun initialize(
        smsTransport: SmsTransport,
        messageStore: MessageStore,
        notificationFacade: NotificationFacade,
        contactResolver: ContactResolver,
        smsReceiveListener: SmsReceiveListener? = null
    ) {
        _smsTransport = smsTransport
        _messageStore = messageStore
        _notificationFacade = notificationFacade
        _contactResolver = contactResolver
        _smsReceiveListener = smsReceiveListener
    }

    /**
     * Register an SMS receive listener.
     * Called by the app module to receive incoming message notifications.
     */
    fun registerSmsReceiveListener(listener: SmsReceiveListener) {
        _smsReceiveListener = listener
    }

    /**
     * Unregister the SMS receive listener.
     */
    fun unregisterSmsReceiveListener() {
        _smsReceiveListener = null
    }

    /**
     * Register an outbound message observer.
     * Called by the app module to receive outbound message send notifications.
     */
    fun registerOutboundMessageObserver(observer: OutboundMessageObserver) {
        _outboundMessageObserver = observer
    }

    /**
     * Unregister the outbound message observer.
     */
    fun unregisterOutboundMessageObserver() {
        _outboundMessageObserver = null
    }
}
