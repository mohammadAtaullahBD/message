package dev.ataullah.message.ui.screens

import android.content.ContentUris
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.SimOption
import dev.ataullah.message.ui.components.SimSelector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewMessageScreen(
    initialAddress: String = "",
    initialBody: String = "",
    simOptions: List<SimOption>,
    onSend: (String, String, Int?) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var number by remember { mutableStateOf(initialAddress) }
    var body by remember { mutableStateOf(initialBody) }
    var selectedSimId by remember(simOptions) {
        mutableStateOf(simOptions.firstOrNull()?.subscriptionId)
    }
    var selectedContactName by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = withContext(Dispatchers.IO) { resolveContact(context.contentResolver, uri) }
                if (info != null) {
                    number = info.number
                    selectedContactName = info.name
                }
            }
        }
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Recipient number") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedButton(
            onClick = { contactPicker.launch(null) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(selectedContactName?.let { "Contact: $it" } ?: "Choose from contacts")
        }
        if (simOptions.isNotEmpty()) {
            SimSelector(
                simOptions = simOptions,
                selectedSimId = selectedSimId,
                onSimSelected = { selectedSimId = it },
                modifier = Modifier.fillMaxWidth()
            )
        }
        TextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message body") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onCancel() }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    val trimmedNumber = number.trim()
                    val trimmedBody = body.trim()
                    if (trimmedNumber.isNotEmpty() && trimmedBody.isNotEmpty()) {
                        onSend(trimmedNumber, trimmedBody, selectedSimId)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Send")
            }
        }
    }
}

private data class ContactInfo(val name: String?, val number: String)

private fun resolveContact(contentResolver: android.content.ContentResolver, uri: Uri): ContactInfo? {
    val contactId = ContentUris.parseId(uri)
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER
    )
    contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        projection,
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val name = cursor.getString(nameIndex)
            val number = cursor.getString(numberIndex) ?: return null
            return ContactInfo(name, number)
        }
    }
    return null
}
