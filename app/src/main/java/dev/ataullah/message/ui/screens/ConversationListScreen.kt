package dev.ataullah.message.ui.screens

import android.provider.Telephony
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.Conversation
import dev.ataullah.message.model.Message
import dev.ataullah.message.util.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ConversationListScreen(
    conversations: List<Conversation>,
    onConversationClick: (String) -> Unit,
    showSearch: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedAddresses: Set<String>,
    onToggleSelection: (String) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    val context = LocalContext.current

    val sortedConversations by remember(conversations) {
        derivedStateOf {
            conversations.sortedByDescending { convo ->
                convo.messages.maxOfOrNull { it.date } ?: Long.MIN_VALUE
            }
        }
    }

    val contactCache = remember { mutableStateMapOf<String, ContactUtils.ContactInfo?>() }

    LaunchedEffect(sortedConversations) {
        val missingAddresses = sortedConversations
            .map { it.address }
            .filter { it !in contactCache }

        if (missingAddresses.isNotEmpty()) {
            val resolved = withContext(Dispatchers.IO) {
                missingAddresses.associateWith { address ->
                    ContactUtils.getContactInfo(context, address)
                }
            }
            contactCache.putAll(resolved)
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

        val filtered by remember(sortedConversations, showSearch, searchQuery) {
            derivedStateOf {
                val query = searchQuery.trim().lowercase()
                if (!showSearch || query.isBlank()) {
                    sortedConversations
                } else {
                    sortedConversations.filter { convo ->
                        val number = convo.address
                        val contactName = contactCache[number]?.name.orEmpty()
                        val lastBody = convo.messages.lastOrNull()?.body ?: ""
                        number.contains(query, ignoreCase = true) ||
                            lastBody.contains(query, ignoreCase = true) ||
                            contactName.contains(query, ignoreCase = true)
                    }
                }
            }
        }

        LazyColumn {
            items(filtered, key = { it.address }) { convo ->
                val contactInfo = contactCache[convo.address]
                val displayName = contactInfo?.name ?: convo.address
                val lastMessage = convo.messages.lastOrNull()
                val preview = lastMessage?.body ?: ""
                val statusPrefix = lastMessage?.let { messageStatusLabel(it) }?.let { "$it • " } ?: ""
                val isSelected = selectedAddresses.contains(convo.address)
                val selectionMode = selectedAddresses.isNotEmpty()

                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteConversation(convo.address)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.small)
                                    .padding(4.dp)
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                ) {
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
                        trailingContent = {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (selectionMode) {
                                        onToggleSelection(convo.address)
                                    } else {
                                        onConversationClick(convo.address)
                                    }
                                },
                                onLongClick = {
                                    onToggleSelection(convo.address)
                                }
                            )
                            .padding(vertical = 4.dp),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                Color.Transparent
                            }
                        )
                    )
                }
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
