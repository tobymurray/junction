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

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneNumberUtils
import com.android.messaging.util.PhoneUtils
import com.technicallyrural.junction.core.contacts.ContactInfo
import com.technicallyrural.junction.core.contacts.ContactResolver
import com.technicallyrural.junction.core.contacts.PhoneNumber
import com.technicallyrural.junction.core.contacts.PhoneNumberType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [ContactResolver] that uses Android's ContactsContract
 * and AOSP Messaging's phone number utilities.
 *
 * @param context Application context for content resolver access
 */
class ContactResolverImpl(private val context: Context) : ContactResolver {

    override suspend fun resolveContact(phoneNumber: String): ContactInfo? =
        withContext(Dispatchers.IO) {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )

            context.contentResolver.query(
                uri,
                CONTACT_PROJECTION,
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val contactId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup._ID)
                    )
                    val displayName = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    ) ?: ""
                    val photoUri = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)
                    )
                    val lookupKey = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.LOOKUP_KEY)
                    ) ?: ""

                    // Load all phone numbers for this contact
                    val phoneNumbers = loadPhoneNumbers(contactId)

                    ContactInfo(
                        contactId = contactId,
                        displayName = displayName,
                        photoUri = photoUri,
                        phoneNumbers = phoneNumbers,
                        lookupKey = lookupKey
                    )
                } else null
            }
        }

    override suspend fun resolveContacts(phoneNumbers: List<String>): Map<String, ContactInfo> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<String, ContactInfo>()
            for (number in phoneNumbers) {
                resolveContact(number)?.let { contact ->
                    result[number] = contact
                }
            }
            result
        }

    override suspend fun isKnownContact(phoneNumber: String): Boolean =
        withContext(Dispatchers.IO) {
            val normalizedNumber = normalizePhoneNumber(phoneNumber)
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(normalizedNumber)
            )

            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        }

    override fun normalizePhoneNumber(phoneNumber: String): String {
        // Use AOSP's phone number utilities
        return PhoneUtils.getDefault().getCanonicalBySimLocale(phoneNumber)
    }

    override fun phoneNumbersMatch(number1: String, number2: String): Boolean {
        return PhoneNumberUtils.compare(number1, number2)
    }

    private fun loadPhoneNumbers(contactId: Long): List<PhoneNumber> {
        val phoneNumbers = mutableListOf<PhoneNumber>()

        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            PHONE_PROJECTION,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val number = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                ) ?: continue

                val normalizedNumber = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
                )

                val type = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE)
                )

                val label = cursor.getString(
                    cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
                )

                phoneNumbers.add(
                    PhoneNumber(
                        number = number,
                        normalizedNumber = normalizedNumber,
                        type = mapPhoneType(type),
                        label = label
                    )
                )
            }
        }

        return phoneNumbers
    }

    private fun mapPhoneType(type: Int): PhoneNumberType {
        return when (type) {
            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> PhoneNumberType.MOBILE
            ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> PhoneNumberType.HOME
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE,
            ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER,
            ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN -> PhoneNumberType.WORK
            else -> PhoneNumberType.OTHER
        }
    }

    companion object {
        private val CONTACT_PROJECTION = arrayOf(
            ContactsContract.PhoneLookup._ID,
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI,
            ContactsContract.PhoneLookup.LOOKUP_KEY
        )

        private val PHONE_PROJECTION = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL
        )
    }
}
