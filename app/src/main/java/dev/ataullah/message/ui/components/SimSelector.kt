package dev.ataullah.message.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.ataullah.message.model.SimOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimSelector(
    simOptions: List<SimOption>,
    selectedSimId: Int?,
    onSimSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Send with"
) {
    if (simOptions.isEmpty()) {
        return
    }

    var expanded by remember { mutableStateOf(false) }
    val currentSelection = simOptions.firstOrNull { it.subscriptionId == selectedSimId }
        ?: simOptions.first()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        TextField(
            readOnly = true,
            value = currentSelection.label,
            onValueChange = {},
            label = { Text(label) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            simOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        expanded = false
                        onSimSelected(option.subscriptionId)
                    }
                )
            }
        }
    }
}
