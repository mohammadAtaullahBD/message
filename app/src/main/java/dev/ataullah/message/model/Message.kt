package dev.ataullah.message.model

data class Message(
    val id: Long,
    val address: String,
    val body: String,
    val date: Long,
    val type: Int, // 1 = received, 2 = sent
    val status: Int? = null
)
