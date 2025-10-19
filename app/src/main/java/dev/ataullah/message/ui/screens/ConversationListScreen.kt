package dev.ataullah.message.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit
) {
    LazyColumn {
        items(conversations) { convo ->
            val lastMessage = convo.messages.firstOrNull()
            ListItem(
                headlineContent = { Text(convo.address) },
                supportingContent = { Text(lastMessage?.body ?: "") },
                modifier = Modifier
                    .clickable { onConversationClick(convo.address) }
                    .padding(vertical = 4.dp)
            )
        }
    }
}
