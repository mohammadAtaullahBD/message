package dev.ataullah.message.ui.screens

import android.content.ContentResolver
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.SimOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NewMessageScreen(
    initialAddress: String = "",
    initialBody: String = "",
    simOptions: List<SimOption>,
    onSend: (String, String, Int?) -> Unit
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf(initialAddress) }
    var body by remember { mutableStateOf(initialBody) }
    var selectedSimId by remember(simOptions) {
        mutableStateOf(simOptions.firstOrNull()?.subscriptionId)
    }
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    var contactSuggestions by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var allContacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val contacts = withContext(Dispatchers.IO) {
            loadContacts(context.contentResolver)
        }
        allContacts = contacts
    }

    LaunchedEffect(allContacts, number) {
        contactSuggestions = buildSuggestions(allContacts, number)
        selectedContactName = allContacts.firstOrNull {
            sanitizeNumber(it.number) == sanitizeNumber(number)
        }?.name
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 🧩 Main UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = number,
                onValueChange = {
                    number = it
                    contactSuggestions = buildSuggestions(allContacts, it)
                    selectedContactName = allContacts.firstOrNull { contact ->
                        sanitizeNumber(contact.number) == sanitizeNumber(it)
                    }?.name
                },
                label = { Text("Recipient number") },
                modifier = Modifier.fillMaxWidth()
            )

            if (selectedContactName != null && number.isNotBlank()) {
                Text(
                    text = "Contact: $selectedContactName",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            contactSuggestions.take(5).forEach { contact ->
                SuggestionRow(
                    contact = contact,
                    onSelected = {
                        number = it.number
                        selectedContactName = it.name
                        contactSuggestions = emptyList()
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

        // ✉️ Bottom Message Bar
        BottomMessageBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            body = body,
            onBodyChange = { body = it },
            selectedSimId = selectedSimId,
            simOptions = simOptions,
            onSimChange = {
                selectedSimId = it
                val info = simOptions.firstOrNull { option -> option.subscriptionId == it }
                Toast.makeText(
                    context,
                    info?.let { option -> "Selected ${option.label}" } ?: "SIM selected",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onSend = {
                val trimmedNumber = number.trim()
                val trimmedBody = body.trim()
                if (trimmedBody.isNotEmpty() && trimmedNumber.isNotEmpty()) {
                    onSend(trimmedNumber, trimmedBody, selectedSimId)
                }
            }
        )
    }
}

@Composable
fun BottomMessageBar(
    modifier: Modifier = Modifier,
    body: String,
    onBodyChange: (String) -> Unit,
    selectedSimId: Int?,
    simOptions: List<SimOption>,
    onSimChange: (Int) -> Unit,
    onSend: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✍️ Message Field
        TextField(
            value = body,
            onValueChange = onBodyChange,
            placeholder = { Text("Send SMS") },
            modifier = Modifier.weight(1f),
            singleLine = true
        )

        // 🔄 SIM Switch button (this was missing!)
        if (simOptions.size > 1) {
            IconButton(onClick = {
                val currentIndex = simOptions.indexOfFirst { it.subscriptionId == selectedSimId }
                val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % simOptions.size
                val newSim = simOptions[nextIndex].subscriptionId
                onSimChange(newSim)
            }) {
                val currentSim = simOptions.firstOrNull { it.subscriptionId == selectedSimId }
                    ?: simOptions[0]
                val icon = if (currentSim.subscriptionId == simOptions[0].subscriptionId) {
                    Icons.Filled.SimCard
                } else {
                    Icons.Outlined.SimCard
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Switch SIM",
                    tint = Color(0xFF2196F3)
                )
            }
        }

        // ✅ Send Button
        IconButton(
            onClick = onSend,
            enabled = body.isNotBlank(),
            modifier = Modifier
                .size(55.dp)
                .background(
                    if (body.isNotBlank()) Color(0xFF2196F3) else Color.Gray,
                    CircleShape
                )
        ) {
            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
        }
    }
}


private data class ContactInfo(val name: String?, val number: String)

@Composable
private fun SuggestionRow(contact: ContactInfo, onSelected: (ContactInfo) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected(contact) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            contact.name?.let {
                Text(text = it, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(
                text = contact.number,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadContacts(resolver: ContentResolver): List<ContactInfo> {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    val contacts = mutableListOf<ContactInfo>()
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        null,
        null,
        "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
    )?.use { cursor ->
        val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
            val name = cursor.getString(nameIndex)
            val number = cursor.getString(numberIndex)
            if (!number.isNullOrBlank()) {
                contacts += ContactInfo(name, number)
            }
        }
    }
    return contacts.distinctBy { sanitizeNumber(it.number) }
}

private fun buildSuggestions(contacts: List<ContactInfo>, query: String): List<ContactInfo> {
    if (query.isBlank()) return emptyList()
    val trimmed = query.trim()
    val sanitizedQuery = sanitizeNumber(trimmed)
    return contacts.filter { contact ->
        val matchesName = contact.name?.contains(trimmed, ignoreCase = true) == true
        val matchesNumber = sanitizeNumber(contact.number).contains(sanitizedQuery)
        matchesName || matchesNumber
    }
}

private fun sanitizeNumber(value: String?): String =
    value?.filter { it.isDigit() } ?: ""
