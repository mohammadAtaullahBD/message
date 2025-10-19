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
                Telephony.Sms.TYPE
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
                        type = c.getInt(typeIndex)
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
                Conversation(addr, msgs.sortedByDescending { it.date })
            }
            .sortedByDescending { it.messages.firstOrNull()?.date }
}
