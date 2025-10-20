package dev.ataullah.message.ui.screens

import android.provider.Telephony
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message
import dev.ataullah.message.model.SimOption
import java.text.DateFormat
import java.util.Date

@Composable
fun ConversationDetailScreen(
    address: String,
    conversation: Conversation?,
    simOptions: List<SimOption>,
    onSend: (String, Int?) -> Unit,
    selectedMessageIds: Set<Long>,
    onToggleMessageSelection: (Long) -> Unit
) {
    val messages = remember(address, conversation?.messages) {
        conversation?.messages ?: emptyList()
    }
    var input by remember { mutableStateOf("") }
    var selectedSimId by remember(simOptions) {
        mutableStateOf(simOptions.firstOrNull()?.subscriptionId)
    }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { msg ->
                val isSelected = selectedMessageIds.contains(msg.id)
                MessageBubble(
                    message = msg,
                    isSelected = isSelected,
                    onLongPress = { onToggleMessageSelection(msg.id) },
                    onClick = {
                        if (selectedMessageIds.isNotEmpty()) {
                            onToggleMessageSelection(msg.id)
                        }
                    }
                )
            }
        }

        BottomMessageBar(
            body = input,
            onBodyChange = { input = it },
            selectedSimId = selectedSimId,
            simOptions = simOptions,
            onSimChange = { newSimId ->
                selectedSimId = newSimId
                val info = simOptions.firstOrNull { it.subscriptionId == newSimId }
                Toast.makeText(
                    context,
                    info?.let { option -> "Selected ${option.label}" } ?: "SIM selected",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSend = {
                val text = input.trim()
                if (text.isNotEmpty()) {
                    onSend(text, selectedSimId)
                    input = ""
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isSelected: Boolean,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val isOutgoing = message.type == Telephony.Sms.MESSAGE_TYPE_SENT ||
        message.type == Telephony.Sms.MESSAGE_TYPE_OUTBOX
    val arrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    val bubbleColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        isOutgoing -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
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
                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongPress,
                        enabled = true
                    )
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
