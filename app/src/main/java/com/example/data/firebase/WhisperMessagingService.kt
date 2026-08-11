package com.example.data.firebase

import android.util.Log
import com.example.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WhisperMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM token generated: $token")
        // Token will be saved to current user document upon sign-in/registration
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val settings = FirebaseService.notificationSettings.value
        if (!settings.messagesEnabled) return

        val data = remoteMessage.data
        val senderName = data["senderName"] ?: remoteMessage.notification?.title ?: "رسالة جديدة"
        val messageText = data["messageText"] ?: remoteMessage.notification?.body ?: ""
        val conversationId = data["conversationId"] ?: ""

        if (messageText.isNotBlank()) {
            NotificationHelper.showMessageNotification(
                context = applicationContext,
                senderName = senderName,
                messageText = messageText,
                conversationId = conversationId,
                soundEnabled = settings.soundEnabled,
                showContent = settings.showContent
            )
        }
    }
}
