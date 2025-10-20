package dev.ataullah.message.ui.screens

import android.content.ContentUris
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.ataullah.message.model.SimOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()

    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val info = withContext(Dispatchers.IO) {
                    resolveContact(context.contentResolver, uri)
                }
                info?.let {
                    number = it.number
                    selectedContactName = it.name
                }
            }
        }
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
                Toast.makeText(
                    context,
                    "Selected SIM ${if (it == simOptions[0].subscriptionId) "1" else "2"}",
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
                val newSim =
                    if (selectedSimId == simOptions[0].subscriptionId)
                        simOptions[1].subscriptionId
                    else
                        simOptions[0].subscriptionId
                onSimChange(newSim)
            }) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.DarkGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (selectedSimId == simOptions[0].subscriptionId) "1" else "2",
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
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

private fun resolveContact(resolver: android.content.ContentResolver, uri: Uri): ContactInfo? {
    val id = ContentUris.parseId(uri)
    resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        ),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(id.toString()), null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return ContactInfo(cursor.getString(0), cursor.getString(1))
        }
    }
    return null
}
