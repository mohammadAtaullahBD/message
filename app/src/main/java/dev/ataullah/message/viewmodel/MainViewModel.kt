package dev.ataullah.message.viewmodel

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ataullah.message.data.SmsRepository
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message
import dev.ataullah.message.model.SimOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _simOptions = MutableStateFlow<List<SimOption>>(emptyList())
    val simOptions: StateFlow<List<SimOption>> = _simOptions.asStateFlow()

    private val appContext: Context = application.applicationContext
    private val tempMessageId = AtomicLong(-1L)

    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val tempId = intent?.getLongExtra(EXTRA_TEMP_ID, Long.MIN_VALUE) ?: return
            val address = intent.getStringExtra(EXTRA_ADDRESS) ?: return
            val totalParts = intent.getIntExtra(EXTRA_TOTAL_PARTS, 1)
            val partIndex = intent.getIntExtra(EXTRA_PART_INDEX, 0)

            // Wait for the final part before marking the message.
            if (partIndex != totalParts - 1) return

            val status = when (resultCode) {
                Activity.RESULT_OK -> Telephony.Sms.STATUS_COMPLETE
                SmsManager.RESULT_ERROR_NO_SERVICE -> Telephony.Sms.STATUS_FAILED
                SmsManager.RESULT_ERROR_RADIO_OFF -> Telephony.Sms.STATUS_FAILED
                SmsManager.RESULT_ERROR_NULL_PDU -> Telephony.Sms.STATUS_FAILED
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Telephony.Sms.STATUS_FAILED
                else -> Telephony.Sms.STATUS_FAILED
            }

            updateTemporaryMessageStatus(address, tempId, status)
            if (status == Telephony.Sms.STATUS_COMPLETE) {
                loadConversations()
            }
        }
    }

    private var sentReceiverRegistered = false

    init {
        loadConversations()
        refreshSimOptions()
        registerSentReceiver()
    }

    fun loadConversations() {
        if (!hasConversationPermissions()) {
            _conversations.value = emptyList()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _conversations.emit(repository.getConversations())
        }
    }

    fun refreshSimOptions() {
        viewModelScope.launch(Dispatchers.IO) {
            val manager = appContext.getSystemService(SubscriptionManager::class.java)
            val active = try {
                manager?.activeSubscriptionInfoList?.map { info ->
                    SimOption(
                        subscriptionId = info.subscriptionId,
                        displayName = info.displayName?.toString().orEmpty(),
                        carrierName = info.carrierName?.toString()
                    )
                } ?: emptyList()
            } catch (_: SecurityException) {
                emptyList()
            }
            _simOptions.emit(active)
        }
    }

    fun sendMessage(address: String, body: String, subscriptionId: Int?) {
        val trimmedAddress = address.trim()
        val trimmedBody = body.trim()
        if (trimmedAddress.isEmpty() || trimmedBody.isEmpty()) return

        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val tempId = tempMessageId.getAndDecrement()
            addTemporaryMessage(
                Message(
                    id = tempId,
                    address = trimmedAddress,
                    body = trimmedBody,
                    date = timestamp,
                    type = Telephony.Sms.MESSAGE_TYPE_SENT,
                    status = Telephony.Sms.STATUS_PENDING
                )
            )

            val smsManager = try {
                subscriptionId?.let { SmsManager.getSmsManagerForSubscriptionId(it) }
                    ?: SmsManager.getDefault()
            } catch (_: SecurityException) {
                updateTemporaryMessageStatus(trimmedAddress, tempId, Telephony.Sms.STATUS_FAILED)
                return@launch
            }

            val parts: ArrayList<String> = ArrayList(
                smsManager.divideMessage(trimmedBody).takeIf { it.isNotEmpty() }
                    ?: listOf(trimmedBody)
            )

            val totalParts = parts.size

            val sentIntents: ArrayList<PendingIntent> = ArrayList(
                parts.mapIndexed { index, _ ->
                    val intent = Intent(ACTION_SMS_SENT).apply {
                        putExtra(EXTRA_TEMP_ID, tempId)
                        putExtra(EXTRA_ADDRESS, trimmedAddress)
                        putExtra(EXTRA_TOTAL_PARTS, totalParts)
                        putExtra(EXTRA_PART_INDEX, index)
                    }
                    PendingIntent.getBroadcast(
                        appContext,
                        tempRequestCode(tempId, index),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            )

            try {
                smsManager.sendMultipartTextMessage(
                    trimmedAddress,
                    null,
                    parts,
                    sentIntents,
                    null
                )
            } catch (_: SecurityException) {
                updateTemporaryMessageStatus(
                    trimmedAddress,
                    tempId,
                    Telephony.Sms.STATUS_FAILED
                )
            }
        }
    }

    private fun registerSentReceiver() {
        if (sentReceiverRegistered) return
        ContextCompat.registerReceiver(
            appContext,
            smsSentReceiver,
            IntentFilter(ACTION_SMS_SENT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sentReceiverRegistered = true
    }

    private fun addTemporaryMessage(message: Message) {
        _conversations.update { current ->
            val updated = current.toMutableList()
            val index = updated.indexOfFirst { it.address == message.address }
            if (index >= 0) {
                val conversation = updated[index]
                val newMessages = (conversation.messages + message).sortedBy { it.date }
                updated[index] = conversation.copy(messages = newMessages)
            } else {
                updated.add(Conversation(message.address, listOf(message)))
            }
            updated.sortByDescending { it.messages.lastOrNull()?.date }
            updated
        }
    }

    private fun updateTemporaryMessageStatus(address: String, messageId: Long, status: Int) {
        _conversations.update { current ->
            current.map { conversation ->
                if (conversation.address != address) {
                    conversation
                } else {
                    val updatedMessages = conversation.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(status = status) else msg
                    }.sortedBy { it.date }
                    conversation.copy(messages = updatedMessages)
                }
            }.sortedByDescending { it.messages.lastOrNull()?.date }
        }
    }

    private fun tempRequestCode(messageId: Long, partIndex: Int): Int {
        val base = messageId.hashCode()
        return base * 31 + partIndex
    }

    override fun onCleared() {
        if (sentReceiverRegistered) {
            runCatching { appContext.unregisterReceiver(smsSentReceiver) }
        }
        sentReceiverRegistered = false
        super.onCleared()
    }

    companion object {
        private const val ACTION_SMS_SENT = "dev.ataullah.message.SMS_SENT"
        private const val EXTRA_TEMP_ID = "extra_temp_id"
        private const val EXTRA_ADDRESS = "extra_address"
        private const val EXTRA_TOTAL_PARTS = "extra_total_parts"
        private const val EXTRA_PART_INDEX = "extra_part_index"
    }

    private fun hasConversationPermissions(): Boolean {
        val readSmsGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED

        val readContactsGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        return readSmsGranted && readContactsGranted
    }
}
