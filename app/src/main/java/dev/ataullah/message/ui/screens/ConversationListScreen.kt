package dev.ataullah.message.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.util.ContactUtils

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column {
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            placeholder = { Text("Search…") }
        )
        // Filter conversations by number/name or last message body
        val filtered = conversations.filter { convo ->
            val query = searchQuery.trim().lowercase()
            if (query.isBlank()) return@filter true
            val number = convo.address
            val lastBody = convo.messages.firstOrNull()?.body ?: ""
            number.contains(query, ignoreCase = true) || lastBody.contains(query, ignoreCase = true)
        }
        LazyColumn {
            items(filtered) { convo ->
                // Resolve contact name and last message for each row
                val contactInfo = remember(convo.address) {
                    ContactUtils.getContactInfo(context, convo.address)
                }
                val displayName = contactInfo?.name ?: convo.address
                val lastMessage = convo.messages.firstOrNull()

                ListItem(
                    headlineContent = { Text(displayName) },
                    supportingContent = {
                        Text(
                            text = lastMessage?.body ?: "",
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
