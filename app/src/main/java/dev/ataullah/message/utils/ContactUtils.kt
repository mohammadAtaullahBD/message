package dev.ataullah.message.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactUtils {
    data class ContactInfo(val name: String?, val photoUri: String?)

    fun getContactInfo(context: Context, phoneNumber: String): ContactInfo? {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(
            ContactsContract.PhoneLookup.DISPLAY_NAME,
            ContactsContract.PhoneLookup.PHOTO_URI
        )
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.DISPLAY_NAME)
                val photoIdx = cursor.getColumnIndexOrThrow(ContactsContract.PhoneLookup.PHOTO_URI)
                val name = cursor.getString(nameIdx)
                val photoUri = cursor.getString(photoIdx)
                return ContactInfo(name, photoUri)
            }
        }
        return null
    }
}
