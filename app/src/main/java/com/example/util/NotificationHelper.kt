package com.example.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.MainActivity

object NotificationHelper {

    private const val CHANNEL_ID = "whisper_messages"
    private const val SUMMARY_NOTIFICATION_ID = 9999

    fun showMessageNotification(
        context: Context,
        senderName: String,
        messageText: String,
        conversationId: String,
        soundEnabled: Boolean = true,
        showContent: Boolean = true
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to launch MainActivity and open specific conversation
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_CONVERSATION_ID", conversationId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val displayText = if (showContent) messageText else "رسالة جديدة"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(senderName)
            .setContentText(displayText)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setGroup("whisper_messages_group")

        if (!soundEnabled) {
            builder.setSilent(true)
        }

        // Post individual notification
        val notificationId = (conversationId + System.currentTimeMillis()).hashCode()
        notificationManager.notify(notificationId, builder.build())

        // Group summary notification to group multiple messages cleanly
        val summaryBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("محادثات New message"))
            .setGroup("whisper_messages_group")
            .setGroupSummary(true)
            .setAutoCancel(true)

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryBuilder.build())
    }
}
