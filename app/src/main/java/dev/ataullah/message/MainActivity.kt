package dev.ataullah.message

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
                val context = LocalContext.current

                var showSearch by rememberSaveable { mutableStateOf(false) }
                var searchQuery by rememberSaveable { mutableStateOf("") }

                LaunchedEffect(currentRoute) {
                    if (currentRoute != NavRoutes.Conversations.route) {
                        showSearch = false
                        searchQuery = ""
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
                    topBar = {
                        when (currentRoute) {
                            NavRoutes.Conversations.route -> {
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
                            }
                            NavRoutes.ConversationDetail.route -> {
                                val address =
                                    currentBackStackEntry?.arguments?.getString("address") ?: ""
                                var contactName by remember(address) { mutableStateOf<String?>(null) }
                                LaunchedEffect(address) {
                                    contactName = withContext(Dispatchers.IO) {
                                        ContactUtils.getContactInfo(context, address)?.name
                                    }
                                }
                                val displayName = contactName?.takeIf { it.isNotBlank() } ?: address
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
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.Permission.route) {
                            PermissionScreen {
                                navController.navigate(NavRoutes.Conversations.route) {
                                    popUpTo(NavRoutes.Permission.route) { inclusive = true }
                                }
                            }
                        }
                        composable(NavRoutes.Conversations.route) {
                            val conversationsState = viewModel.conversations.collectAsStateWithLifecycle()
                            val conversations = conversationsState.value
                            ConversationListScreen(
                                conversations = conversations,
                                onConversationClick = { address ->
                                    navController.navigate(
                                        NavRoutes.ConversationDetail.createRoute(address)
                                    )
                                },
                                showSearch = showSearch,
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it }
                            )
                        }
                        composable(
                            NavRoutes.ConversationDetail.route,
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { entry ->
                            val address = entry.arguments?.getString("address") ?: ""
                            val conversationsState =
                                viewModel.conversations.collectAsStateWithLifecycle()
                            val conversation =
                                conversationsState.value.find { it.address == address }
                            ConversationDetailScreen(
                                address = address,
                                conversation = conversation,
                                simOptions = simOptions,
                                onSend = { text, subscriptionId ->
                                    viewModel.sendMessage(address, text, subscriptionId)
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
