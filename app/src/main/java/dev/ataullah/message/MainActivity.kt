package dev.ataullah.message

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.ataullah.message.navigation.NavRoutes
import dev.ataullah.message.ui.screens.ConversationDetailScreen
import dev.ataullah.message.ui.screens.ConversationListScreen
import dev.ataullah.message.ui.screens.PermissionScreen
import dev.ataullah.message.ui.theme.MessageTheme
import dev.ataullah.message.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MessageTheme {
                val navController = rememberNavController()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = NavRoutes.Permission.route ,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(NavRoutes.Permission.route) {
                            PermissionScreen(onPermissionsGranted = {
                                navController.navigate(NavRoutes.Conversations.route) {
                                    popUpTo(NavRoutes.Permission.route) { inclusive = true }
                                }
                            })
                        }
                        composable(NavRoutes.Conversations.route) {
                            // Collect the StateFlow into a State<List<Conversation>>
                            val conversationsState = viewModel.conversations.collectAsStateWithLifecycle()
                            // Use .value instead of `by` to get the underlying list
                            val conversations = conversationsState.value

                            ConversationListScreen(conversations) { selectedAddress ->
                                navController.navigate(NavRoutes.ConversationDetail.createRoute(selectedAddress))
                            }
                        }

                        composable(
                            route = NavRoutes.ConversationDetail.route,
                            arguments = listOf(navArgument("address") { type = NavType.StringType })
                        ) { entry ->
                            val address = entry.arguments?.getString("address") ?: ""
                            // Collect the conversations again and use .value
                            val conversationsState = viewModel.conversations.collectAsStateWithLifecycle()
                            val conversation = conversationsState.value.find { it.address == address }

                            ConversationDetailScreen(
                                address = address,
                                conversation = conversation,
                                onSend = { text -> viewModel.sendMessage(address, text) }
                            )
                        }

                    }
                }
            }
        }
    }
}
