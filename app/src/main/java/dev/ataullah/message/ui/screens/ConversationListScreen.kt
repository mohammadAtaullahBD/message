package dev.ataullah.message.ui.screens

import android.provider.Telephony
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
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
    onDeleteConversation: (String) -> Unit,
    onVisibleAddressesChange: (Set<String>) -> Unit
) {
    val context = LocalContext.current

    val sortedConversations = conversations.sortedByDescending { convo ->
        convo.messages.lastOrNull()?.date ?: Long.MIN_VALUE
    }

    val contactCache = remember { mutableStateMapOf<String, ContactUtils.ContactInfo?>() }

    LaunchedEffect(sortedConversations) {
        val addresses = sortedConversations.map { it.address }.toSet()
        val missingAddresses = addresses.filter { it !in contactCache }

        if (missingAddresses.isNotEmpty()) {
            val resolved = withContext(Dispatchers.IO) {
                missingAddresses.associateWith { address ->
                    ContactUtils.getContactInfo(context, address)
                }
            }
            contactCache.putAll(resolved)
        }

        val staleKeys = contactCache.keys.toList().filter { it !in addresses }
        if (staleKeys.isNotEmpty()) {
            staleKeys.forEach { contactCache.remove(it) }
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

        val query = searchQuery.trim().lowercase()
        val filtered = if (!showSearch || query.isBlank()) {
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

        val visibleAddresses = filtered.map { it.address }

        LaunchedEffect(visibleAddresses) {
            onVisibleAddressesChange(visibleAddresses.toSet())
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
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
                    backgroundContent = {}
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
