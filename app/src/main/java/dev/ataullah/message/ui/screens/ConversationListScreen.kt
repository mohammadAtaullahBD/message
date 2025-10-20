package dev.ataullah.message.ui.screens

import android.provider.Telephony
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message
import dev.ataullah.message.util.ContactUtils

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit
) {
    val context = LocalContext.current

    val sortedConversations by remember(conversations) {
        derivedStateOf {
            conversations.sortedByDescending { convo ->
                convo.messages.maxOfOrNull { it.date } ?: Long.MIN_VALUE
            }
        }
    }

    Column {
        if (showSearch) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                placeholder = { Text("Search…") }
            )
        }

        val filtered = remember(sortedConversations, showSearch, searchQuery) {
            val query = searchQuery.trim().lowercase()
            if (!showSearch || query.isBlank()) {
                sortedConversations
            } else {
                sortedConversations.filter { convo ->
                    val number = convo.address
                    val lastBody = convo.messages.lastOrNull()?.body ?: ""
                    number.contains(query, ignoreCase = true) ||
                        lastBody.contains(query, ignoreCase = true)
                }
            }
        }
        LazyColumn {
            items(filtered) { convo ->
                // Resolve contact name and last message for each row
                val contactInfo = remember(convo.address) {
                    ContactUtils.getContactInfo(context, convo.address)
                }
                val displayName = contactInfo?.name ?: convo.address
                val lastMessage = convo.messages.lastOrNull()
                val preview = lastMessage?.body ?: ""
                val statusPrefix = lastMessage?.let { messageStatusLabel(it) }?.let { "$it • " } ?: ""

                ListItem(
                    headlineContent = { Text(displayName) },
                    supportingContent = {
                        Text(
                            text = statusPrefix + preview,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null
                        )
                    },
                    modifier = Modifier
                        .clickable { onConversationClick(convo.address) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

private fun messageStatusLabel(message: Message): String? {
    val isOutgoing = message.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
        message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
    if (!isOutgoing) return null
    return when (message.status) {
        Telephony.Sms.STATUS_PENDING -> "Sending…"
        Telephony.Sms.STATUS_FAILED -> "Failed"
        Telephony.Sms.STATUS_COMPLETE -> "Sent"
        else -> null
    }
}
