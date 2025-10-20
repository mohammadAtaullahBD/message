package dev.ataullah.message.viewmodel

import android.app.Application
import android.telephony.SmsManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ataullah.message.data.SmsRepository
import dev.ataullah.message.model.Conversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)
    private val _conversations = MutableStateFlow<List<Conversation>>(emptyList())
    val conversations: StateFlow<List<Conversation>> = _conversations

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch(Dispatchers.IO) {
            _conversations.emit(repository.getConversations())
        }
    }

    fun sendMessage(address: String, body: String) {
        val smsManager = SmsManager.getDefault()

        // Divide long messages into multiple parts
        val parts = smsManager.divideMessage(body)

        try {
            smsManager.sendMultipartTextMessage(address, null, parts, null, null)
        } catch (_: SecurityException) {
            // Handle the rare case where the OS denies sending; you could log or show a toast
        }

        // Refresh conversations to show the sent message
        loadConversations()
    }
}
