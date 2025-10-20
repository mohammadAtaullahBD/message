package dev.ataullah.message.data

import android.content.Context
import android.provider.Telephony
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message

class SmsRepository(private val context: Context) {
    fun getAllMessages(): List<Message> {
        val messages = mutableListOf<Message>()
        val cursor = context.contentResolver.query(
            Telephony.Sms.CONTENT_URI,
            arrayOf(
                Telephony.Sms._ID,
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.TYPE,
                Telephony.Sms.STATUS
            ),
            null,
            null,
            Telephony.Sms.DEFAULT_SORT_ORDER
        )
        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIndex = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            while (c.moveToNext()) {
                messages.add(
                    Message(
                        id = c.getLong(idIndex),
                        address = c.getString(addressIndex) ?: "",
                        body = c.getString(bodyIndex) ?: "",
                        date = c.getLong(dateIndex),
                        type = c.getInt(typeIndex),
                        status = c.getColumnIndex(Telephony.Sms.STATUS).takeIf { it >= 0 }
                            ?.let { idx ->
                                if (!c.isNull(idx)) c.getInt(idx) else null
                            }
                    )
                )
            }
        }
        return messages
    }

    fun getConversations(): List<Conversation> =
        getAllMessages()
            .groupBy { it.address }
            .map { (addr, msgs) ->
                Conversation(addr, msgs.sortedBy { it.date })
            }
            .sortedByDescending { it.messages.lastOrNull()?.date }

    fun deleteMessages(ids: Collection<Long>): Int {
        if (ids.isEmpty()) return 0
        val placeholders = ids.joinToString(separator = ",") { "?" }
        val selection = "${Telephony.Sms._ID} IN ($placeholders)"
        val selectionArgs = ids.map { it.toString() }.toTypedArray()
        return context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            selection,
            selectionArgs
        )
    }

    fun deleteConversations(addresses: Collection<String>): Int {
        if (addresses.isEmpty()) return 0
        val placeholders = addresses.joinToString(separator = ",") { "?" }
        val selection = "${Telephony.Sms.ADDRESS} IN ($placeholders)"
        val selectionArgs = addresses.toTypedArray()
        return context.contentResolver.delete(
            Telephony.Sms.CONTENT_URI,
            selection,
            selectionArgs
        )
    }
}
