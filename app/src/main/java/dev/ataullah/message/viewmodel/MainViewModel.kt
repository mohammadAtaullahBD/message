package dev.ataullah.message.viewmodel

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
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
        val context = getApplication<Application>()
        val canSend = ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (canSend) {
            try {
                SmsManager.getDefault().sendTextMessage(address, null, body, null, null)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Fallback: open default SMS app with prefilled message
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$address")).apply {
                putExtra("sms_body", body)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        loadConversations()
    }
}
