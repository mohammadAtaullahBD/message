package dev.ataullah.message.model

data class Conversation(
    val address: String,
    val messages: List<Message>
)
