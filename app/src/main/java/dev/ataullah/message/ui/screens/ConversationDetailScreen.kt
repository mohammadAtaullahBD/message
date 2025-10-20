package dev.ataullah.message.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message

@Composable
fun ConversationDetailScreen(
    address: String,
    conversation: Conversation?,
    onSend: (String) -> Unit
) {
    val messages: List<Message> = conversation?.messages ?: emptyList()
    val input = remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            items(messages) { msg ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors()
                ) {
                    Text(
                        text = msg.body,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = input.value,
                onValueChange = { input.value = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message") }
            )
            Button(
                onClick = {
                    val text = input.value.trim()
                    if (text.isNotEmpty()) {
                        onSend(text)
                        input.value = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Send")
            }
        }
    }
}
