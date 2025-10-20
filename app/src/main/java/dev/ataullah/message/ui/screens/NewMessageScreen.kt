package dev.ataullah.message.ui.screens

import android.content.ContentResolver
import android.provider.ContactsContract
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.outlined.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.surfaceColorAtElevation
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.SimOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

@Composable
fun NewMessageScreen(
    initialAddress: String = "",
    initialBody: String = "",
    simOptions: List<SimOption>,
    onSend: (String, String, Int?) -> Unit
) {
    val context = LocalContext.current
    var recipientInput by remember { mutableStateOf(initialAddress) }
    var body by remember { mutableStateOf(initialBody) }
    var selectedSimId by remember(simOptions) {
        mutableStateOf(simOptions.firstOrNull()?.subscriptionId)
    }
    var selectedContact by remember { mutableStateOf<ContactInfo?>(null) }
    var contactSuggestions by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var allContacts by remember { mutableStateOf<List<ContactInfo>>(emptyList()) }
    var suggestionsVisible by remember { mutableStateOf(initialAddress.isNotBlank()) }

    LaunchedEffect(Unit) {
        val contacts = withContext(Dispatchers.IO) {
            loadContacts(context.contentResolver)
        }
        allContacts = contacts
    }

    LaunchedEffect(allContacts, recipientInput, suggestionsVisible) {
        contactSuggestions = if (suggestionsVisible) {
            buildSuggestions(allContacts, recipientInput)
        } else {
            emptyList()
        }
        selectedContact = resolveSelectedContact(allContacts, recipientInput, selectedContact)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextField(
                value = recipientInput,
                onValueChange = {
                    recipientInput = it
                    suggestionsVisible = true
                    contactSuggestions = buildSuggestions(allContacts, it)
                    selectedContact = resolveSelectedContact(allContacts, it, selectedContact)
                },
                label = { Text("Recipient") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (selectedContact?.name != null && recipientInput.isNotBlank()) {
                Text(
                    text = "Contact: ${selectedContact?.name}",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            contactSuggestions.take(5).forEach { contact ->
                SuggestionRow(
                    contact = contact,
                    onSelected = {
                        recipientInput = it.name ?: it.number
                        selectedContact = it
                        contactSuggestions = emptyList()
                        suggestionsVisible = false
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }

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
                val trimmedInput = recipientInput.trim()
                val trimmedBody = body.trim()
                if (trimmedBody.isNotEmpty()) {
                    val resolvedNumber = determineRecipientNumber(
                        input = trimmedInput,
                        selectedContact = selectedContact,
                        contacts = allContacts
                    )
                    if (!resolvedNumber.isNullOrBlank()) {
                        onSend(resolvedNumber, trimmedBody, selectedSimId)
                    }
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
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val navBottom = WindowInsets.navigationBars.getBottom(density)
    val additionalBottomPadding = with(density) { max(imeBottom, navBottom).toDp() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = additionalBottomPadding),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {

            TextField(
                value = body,
                onValueChange = onBodyChange,
                placeholder = { Text("Send SMS") },
                modifier = Modifier.weight(1f),
                singleLine = false,
                minLines = 1,
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

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
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val sendTint = if (body.isNotBlank()) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            IconButton(
                onClick = onSend,
                enabled = body.isNotBlank(),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = sendTint
                )
            }
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
        val matchesNumber = sanitizedQuery.isNotEmpty() &&
            sanitizeNumber(contact.number).contains(sanitizedQuery)
        matchesName || matchesNumber
    }
}

private fun resolveSelectedContact(
    contacts: List<ContactInfo>,
    input: String,
    currentSelection: ContactInfo?
): ContactInfo? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null

    val sanitizedInput = sanitizeNumber(trimmed)
    val numberMatch = contacts.firstOrNull { contact ->
        sanitizedInput.isNotEmpty() && sanitizeNumber(contact.number) == sanitizedInput
    }
    if (numberMatch != null) return numberMatch

    val exactNameMatch = contacts.firstOrNull { contact ->
        contact.name?.equals(trimmed, ignoreCase = true) == true
    }
    if (exactNameMatch != null) return exactNameMatch

    if (sanitizedInput.isEmpty()) {
        val partialMatches = contacts.filter { contact ->
            contact.name?.contains(trimmed, ignoreCase = true) == true
        }
        if (partialMatches.size == 1) {
            return partialMatches.first()
        }
    }

    return currentSelection?.takeIf { selection ->
        when {
            sanitizedInput.isNotEmpty() -> sanitizeNumber(selection.number) == sanitizedInput
            else -> selection.name?.contains(trimmed, ignoreCase = true) == true
        }
    }
}

private fun determineRecipientNumber(
    input: String,
    selectedContact: ContactInfo?,
    contacts: List<ContactInfo>
): String? {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return null

    selectedContact?.number?.takeIf { it.isNotBlank() }?.let { return it }

    val sanitizedInput = sanitizeNumber(trimmed)
    if (sanitizedInput.isNotEmpty()) {
        val directMatch = contacts.firstOrNull {
            sanitizeNumber(it.number) == sanitizedInput
        }
        return directMatch?.number ?: trimmed
    }

    val exactNameMatch = contacts.firstOrNull {
        it.name?.equals(trimmed, ignoreCase = true) == true
    }
    if (exactNameMatch != null) return exactNameMatch.number

    val partialMatches = contacts.filter {
        it.name?.contains(trimmed, ignoreCase = true) == true
    }
    return partialMatches.singleOrNull()?.number
}

private fun sanitizeNumber(value: String?): String =
    value?.filter { it.isDigit() } ?: ""
