package com.example.data.model

data class NotificationSettings(
    val messagesEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val showContent: Boolean = true
)
