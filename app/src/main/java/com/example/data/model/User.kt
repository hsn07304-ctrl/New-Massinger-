package com.example.data.model

data class User(
    val uid: String = "",
    val displayName: String = "",
    val username: String = "",
    val usernameLower: String = "",
    val bio: String = "",
    val socials: List<String> = emptyList(), // Max 3 items
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val fcmToken: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
