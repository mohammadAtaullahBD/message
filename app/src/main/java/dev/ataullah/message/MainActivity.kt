package dev.ataullah.message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ataullah.message.navigation.NavRoutes
import dev.ataullah.message.ui.screens.ConversationDetailScreen
import dev.ataullah.message.ui.screens.ConversationListScreen
import dev.ataullah.message.ui.screens.NewMessageScreen
import dev.ataullah.message.ui.screens.PermissionScreen
import dev.ataullah.message.ui.theme.MessageTheme
import dev.ataullah.message.viewmodel.MainViewModel
import dev.ataullah.message.util.ContactUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get data from intent *before* setContent
        val initialAddress = intent.getStringExtra("initial_address")
        val initialBody = intent.getStringExtra("initial_body")

        setContent {
            MessageTheme {
                val navController = rememberNavController()
                val currentBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = currentBackStackEntry?.destination?.route
                val simOptions by viewModel.simOptions.collectAsStateWithLifecycle()
                val conversations by viewModel.conversations.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val clipboardManager = LocalClipboardManager.current

                var showSearch by rememberSaveable { mutableStateOf(false) }
                var searchQuery by rememberSaveable { mutableStateOf("") }
                var selectedConversations by remember { mutableStateOf(setOf<String>()) }
                var visibleConversationAddresses by remember { mutableStateOf(setOf<String>()) }
                var selectedMessageIds by remember { mutableStateOf(setOf<Long>()) }

                fun <T> Set<T>.toggle(item: T): Set<T> =
                    if (contains(item)) this - item else this + item

                LaunchedEffect(currentRoute) {
                    if (currentRoute != NavRoutes.Conversations.route) {
                        showSearch = false
                        searchQuery = ""
                        selectedConversations = emptySet()
                    }
                    if (currentRoute != NavRoutes.ConversationDetail.route) {
                        selectedMessageIds = emptySet()
                    }
                }

                // Automatically navigate if the app was launched with an SMS intent
                LaunchedEffect(initialAddress) {
                    if (initialAddress != null) {
                        navController.navigate(NavRoutes.NewMessage.route)
                    }
                }

                LaunchedEffect(currentRoute) {
                    if (currentRoute != null && currentRoute != NavRoutes.Permission.route) {
                        viewModel.refreshSimOptions()
                        if (currentRoute == NavRoutes.Conversations.route) {
                            viewModel.loadConversations()
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Horizontal
                    ),
                    topBar = {
                        when (currentRoute) {
                            NavRoutes.Conversations.route -> {
                                val allVisibleSelected =
                                    visibleConversationAddresses.isNotEmpty() &&
                                        selectedConversations.containsAll(visibleConversationAddresses)
                                if (selectedConversations.isEmpty()) {
                                    TopAppBar(
                                        title = { Text("Messages") },
                                        actions = {
                                            IconButton(onClick = {
                                                if (showSearch) {
                                                    showSearch = false
                                                    searchQuery = ""
                                                } else {
                                                    showSearch = true
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Search,
                                                    contentDescription = if (showSearch) {
                                                        "Hide search"
                                                    } else {
                                                        "Show search"
                                                    }
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    TopAppBar(
                                        title = { Text("${selectedConversations.size} selected") },
                                        navigationIcon = {
                                            IconButton(onClick = { selectedConversations = emptySet() }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Cancel selection"
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = {
                                                selectedConversations = if (allVisibleSelected) {
                                                    selectedConversations - visibleConversationAddresses
                                                } else {
                                                    selectedConversations + visibleConversationAddresses
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = if (allVisibleSelected) {
                                                        Icons.Filled.CheckBox
                                                    } else {
                                                        Icons.Outlined.CheckBoxOutlineBlank
                                                    },
                                                    contentDescription = if (allVisibleSelected) {
                                                        "Clear selection"
                                                    } else {
                                                        "Select all"
                                                    }
                                                )
                                            }
                                            IconButton(onClick = {
                                                viewModel.deleteConversations(selectedConversations)
                                                selectedConversations = emptySet()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete conversations"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                            NavRoutes.ConversationDetail.route -> {
                                val address =
                                    currentBackStackEntry?.arguments?.getString("address") ?: ""
                                val conversation = conversations.find { it.address == address }
                                var contactName by remember(address) { mutableStateOf<String?>(null) }
                                LaunchedEffect(address) {
                                    contactName = withContext(Dispatchers.IO) {
                                        ContactUtils.getContactInfo(context, address)?.name
                                    }
                                }
                                val displayName = contactName?.takeIf { it.isNotBlank() } ?: address
                                if (selectedMessageIds.isEmpty()) {
                                    TopAppBar(
                                        title = {
                                            Text(
                                                text = displayName,
                                                modifier = Modifier.clickable { launchContact(context, address, displayName) }
                                            )
                                        },
                                        navigationIcon = {
                                            IconButton(onClick = { navController.navigateUp() }) {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                    )
                                } else {
                                    val messageIds = conversation?.messages?.map { it.id }?.toSet().orEmpty()
                                    val allSelected = messageIds.isNotEmpty() && selectedMessageIds.containsAll(messageIds)
                                    TopAppBar(
                                        title = { Text("${selectedMessageIds.size} selected") },
                                        navigationIcon = {
                                            IconButton(onClick = { selectedMessageIds = emptySet() }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Cancel selection"
                                                )
                                            }
                                        },
                                        actions = {
                                            IconButton(onClick = {
                                                selectedMessageIds = if (allSelected) {
                                                    emptySet()
                                                } else {
                                                    messageIds
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = if (allSelected) {
                                                        Icons.Filled.CheckBox
                                                    } else {
                                                        Icons.Outlined.CheckBoxOutlineBlank
                                                    },
                                                    contentDescription = if (allSelected) {
                                                        "Clear selection"
                                                    } else {
                                                        "Select all"
                                                    }
                                                )
                                            }
                                            if (selectedMessageIds.size == 1) {
                                                IconButton(onClick = {
                                                    val messageId = selectedMessageIds.firstOrNull()
                                                    val body = messageId?.let { id ->
                                                        conversation?.messages?.firstOrNull { it.id == id }?.body
                                                    }
                                                    if (!body.isNullOrEmpty()) {
                                                        clipboardManager.setText(AnnotatedString(body))
                                                        Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
                                                    }
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.ContentCopy,
                                                        contentDescription = "Copy message"
                                                    )
                                                }
                                            }
                                            IconButton(onClick = {
                                                viewModel.deleteMessages(selectedMessageIds)
                                                selectedMessageIds = emptySet()
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete messages"
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        if (currentRoute == NavRoutes.Conversations.route) {
                            FloatingActionButton(onClick = {
                                navController.navigate(NavRoutes.NewMessage.route)
                            }) {
                                Icon(Icons.Filled.Add, contentDescription = "New Conversation")
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Permission.route,
                        modifier = Modifier
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        composable(NavRoutes.Permission.route) {
                            PermissionScreen {
                                navController.navigate(NavRoutes.Conversations.route) {
                                    popUpTo(NavRoutes.Permission.route) { inclusive = true }
                                }
                            }
                        }
                        composable(NavRoutes.Conversations.route) {
                            BackHandler(enabled = selectedConversations.isNotEmpty()) {
                                selectedConversations = emptySet()
                            }
                            ConversationListScreen(
                                conversations = conversations,
                                onConversationClick = { address ->
                                    navController.navigate(
                                        NavRoutes.ConversationDetail.createRoute(address)
                                    )
                                },
                                showSearch = showSearch,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedAddresses = selectedConversations,
                                onToggleSelection = { address ->
                                    selectedConversations = selectedConversations.toggle(address)
                                },
                                onDeleteConversation = { address ->
                                    viewModel.deleteConversations(setOf(address))
                                    selectedConversations = selectedConversations - address
                                },
                                onVisibleAddressesChange = { visibleConversationAddresses = it }
                            )
                        }
                        composable(
                            NavRoutes.ConversationDetail.route,
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { entry ->
                            val address = entry.arguments?.getString("address") ?: ""
                            LaunchedEffect(address) {
                                selectedMessageIds = emptySet()
                            }
                            val conversation = conversations.find { it.address == address }
                            ConversationDetailScreen(
                                address = address,
                                conversation = conversation,
                                simOptions = simOptions,
                                onSend = { text, subscriptionId ->
                                    viewModel.sendMessage(address, text, subscriptionId)
                                },
                                selectedMessageIds = selectedMessageIds,
                                onToggleMessageSelection = { messageId ->
                                    selectedMessageIds = selectedMessageIds.toggle(messageId)
                                }
                            )
                        }
                        composable(NavRoutes.NewMessage.route) {
                            NewMessageScreen(
                                initialAddress = initialAddress ?: "",
                                initialBody = initialBody ?: "",
                                simOptions = simOptions,
                                onSend = { number, body, subscriptionId ->
                                    viewModel.sendMessage(number, body, subscriptionId)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun launchContact(context: Context, phoneNumber: String, displayName: String) {
    val intent = Intent(ContactsContract.Intents.SHOW_OR_CREATE_CONTACT).apply {
        data = Uri.parse("tel:$phoneNumber")
        putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true)
        if (displayName.isNotBlank() && displayName != phoneNumber) {
            putExtra(ContactsContract.Intents.Insert.NAME, displayName)
        }
        if (context !is ComponentActivity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching { context.startActivity(intent) }
}
