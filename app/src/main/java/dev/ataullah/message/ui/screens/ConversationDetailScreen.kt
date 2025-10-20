package dev.ataullah.message.ui.screens

import android.provider.Telephony
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message
import dev.ataullah.message.model.SimOption
import dev.ataullah.message.ui.components.SimSelector
import java.text.DateFormat
import java.util.Date

@Composable
fun ConversationDetailScreen(
    address: String,
    conversation: Conversation?,
    simOptions: List<SimOption>,
    onSend: (String, Int?) -> Unit
) {
    val messages = remember(address, conversation?.messages) {
        conversation?.messages ?: emptyList()
    }
    var input by remember { mutableStateOf("") }
    var selectedSimId by remember(simOptions) {
        mutableStateOf(simOptions.firstOrNull()?.subscriptionId)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        if (simOptions.isNotEmpty()) {
            SimSelector(
                simOptions = simOptions,
                selectedSimId = selectedSimId,
                onSimSelected = { selectedSimId = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 72.dp),
                placeholder = { Text("Type a message") }
            )
            SmallFloatingActionButton(
                onClick = {
                    val text = input.trim()
                    if (text.isNotEmpty()) {
                        onSend(text, selectedSimId)
                        input = ""
                    }
                },
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message"
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val isOutgoing = message.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
        message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
    val arrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape: Shape = if (isOutgoing) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomEnd = 4.dp, bottomStart = 20.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    }
    val timestamp = remember(message.date) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(message.date))
    }
    val statusText = remember(message.status) { statusLabel(message.status) }
    val statusColor = when (message.status) {
        Telephony.Sms.STATUS_FAILED -> MaterialTheme.colorScheme.error
        Telephony.Sms.STATUS_PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        Telephony.Sms.STATUS_COMPLETE -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = arrangement
        ) {
            Surface(
                color = bubbleColor,
                contentColor = contentColor,
                shape = shape,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = arrangement
        ) {
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isOutgoing && statusText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = arrangement
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
        }
    }
}

private fun statusLabel(status: Int?): String? = when (status) {
    Telephony.Sms.STATUS_PENDING -> "Sending…"
    Telephony.Sms.STATUS_COMPLETE -> "Sent"
    Telephony.Sms.STATUS_FAILED -> "Failed"
    else -> null
}
