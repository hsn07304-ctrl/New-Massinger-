package com.example.data.model

data class ParticipantInfo(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String = "", // Format: minUid_maxUid
    val participantIds: List<String> = emptyList(),
    val participantData: Map<String, ParticipantInfo> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val isTyping: Boolean = false,
    val typingMap: Map<String, Boolean> = emptyMap(),
    val unreadCountMap: Map<String, Int> = emptyMap()
)
