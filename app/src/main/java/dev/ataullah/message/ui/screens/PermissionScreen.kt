package dev.ataullah.message.ui.screens

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    // Request the required permissions. SEND_SMS is optional on Android 15.
    val permissionsState: MultiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
    )

    // Only these permissions are mandatory; we ignore SEND_SMS because Android 15 won’t grant it.
    val mandatoryPermissionsGranted by remember {
        derivedStateOf {
            permissionsState.permissions
                .filter { it.permission != Manifest.permission.SEND_SMS }
                .all { it.status.isGranted }
        }
    }

    /**
     * Helper function to check if our app is the default SMS handler.
     * On Android Q (29) and above, use RoleManager; below, fallback to Telephony.Sms.
     */
    fun isDefaultSmsApp(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            roleManager?.isRoleHeld(RoleManager.ROLE_SMS) ?: false
        } else {
            Telephony.Sms.getDefaultSmsPackage(context) == context.packageName
        }
    }

    // Launcher for requesting the default SMS role (or ACTION_CHANGE_DEFAULT on older versions).
    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // After returning from the settings, check again if we're now the default.
        if (result.resultCode == Activity.RESULT_OK && isDefaultSmsApp()) {
            onPermissionsGranted()
        }
    }

    // Whenever mandatory permissions change, verify whether we’ve become the default.
    LaunchedEffect(mandatoryPermissionsGranted) {
        if (mandatoryPermissionsGranted && isDefaultSmsApp()) {
            onPermissionsGranted()
        }
    }

    when {
        // Missing mandatory permissions
        !mandatoryPermissionsGranted -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("This app needs SMS and Contacts permissions to display your conversations, send messages, and show contact names.")
                Button(
                    onClick = { permissionsState.launchMultiplePermissionRequest() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant permissions")
                }
            }
        }
        // Have the necessary permissions but not the default SMS role yet
        !isDefaultSmsApp() -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("To receive and send SMS messages, please set this app as your default SMS app.")
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val roleManager = context.getSystemService(RoleManager::class.java)
                            val intent = roleManager?.createRequestRoleIntent(RoleManager.ROLE_SMS)
                            if (intent != null) {
                                defaultSmsLauncher.launch(intent)
                            }
                        } else {
                            val intent = Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT).apply {
                                putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, context.packageName)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            defaultSmsLauncher.launch(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set as default")
                }
            }
        }
        // Permissions satisfied and app already default → navigate away via LaunchedEffect.
        else -> Unit
    }
}
