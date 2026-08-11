package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.firebase.FirebaseService
import com.google.firebase.FirebaseApp

class WhisperApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Safely initialize Firebase if needed
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Initialize Firebase Service singleton
        FirebaseService.init(this)
        
        // Create Notification Channel for Messages
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "whisper_messages"
            val channelName = "رسائل المحادثات"
            val channelDescription = "إشعارات الرسائل النصية القادمة"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
