package dev.ataullah.message.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NewMessageScreen(
    initialAddress: String = "",
    initialBody: String = "",
    onSend: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var number by remember { mutableStateOf(initialAddress) }
    var body by remember { mutableStateOf(initialBody) }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = number,
            onValueChange = { number = it },
            label = { Text("Recipient number") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        TextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Message body") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = { onCancel() }, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmedNumber = number.trim()
                    val trimmedBody = body.trim()
                    if (trimmedNumber.isNotEmpty() && trimmedBody.isNotEmpty()) {
                        onSend(trimmedNumber, trimmedBody)
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Send")
            }
        }
    }
}
