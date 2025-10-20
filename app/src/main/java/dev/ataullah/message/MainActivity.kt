package dev.ataullah.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

                // Automatically navigate if the app was launched with an SMS intent
                LaunchedEffect(initialAddress) {
                    if (initialAddress != null) {
                        navController.navigate(NavRoutes.NewMessage.route)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        when (currentRoute) {
                            NavRoutes.Conversations.route -> {
                                TopAppBar(title = { Text("Messages") })
                            }
                            NavRoutes.ConversationDetail.route -> {
                                val address =
                                    currentBackStackEntry?.arguments?.getString("address") ?: ""
                                TopAppBar(
                                    title = { Text(address) },
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
                            ConversationListScreen(conversations) { address ->
                                navController.navigate(
                                    NavRoutes.ConversationDetail.createRoute(address)
                                )
                            }
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
                                onSend = { text -> viewModel.sendMessage(address, text) }
                            )
                        }
                        composable(NavRoutes.NewMessage.route) {
                            NewMessageScreen(
                                initialAddress = initialAddress ?: "",
                                initialBody = initialBody ?: "",
                                onSend = { number, body ->
                                    viewModel.sendMessage(number, body)
                                    navController.popBackStack()
                                },
                                onCancel = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
