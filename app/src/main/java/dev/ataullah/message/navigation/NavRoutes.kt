package dev.ataullah.message.navigation

sealed class NavRoutes(val route: String) {
    object Permission : NavRoutes("permission")
    object Conversations : NavRoutes("conversations")
    object ConversationDetail : NavRoutes("conversationDetail/{address}") {
        fun createRoute(address: String): String = "conversationDetail/$address"
    }
    // NavRoutes.kt
    object NewMessage : NavRoutes("newMessage")
}
