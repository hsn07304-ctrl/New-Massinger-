package com.example.data.model

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: MessageStatus = MessageStatus.SENT
)
