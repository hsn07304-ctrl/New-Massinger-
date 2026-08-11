package com.example.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.firebase.FirebaseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val settings by FirebaseService.notificationSettings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "إعدادات الإشعارات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("notification_settings_back_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            NotificationToggleCard(
                title = "إشعارات الرسائل",
                subtitle = "تلقي إشعارات فورية عند وصول رسائل جديدة",
                checked = settings.messagesEnabled,
                onCheckedChange = {
                    FirebaseService.updateNotificationSettings(
                        messagesEnabled = it,
                        soundEnabled = settings.soundEnabled,
                        showContent = settings.showContent
                    )
                },
                testTag = "toggle_message_notifications"
            )

            Spacer(modifier = Modifier.height(12.dp))

            NotificationToggleCard(
                title = "صوت الإشعار",
                subtitle = "تشغيل النغمة الصوتية للتنبيه عند وصول الرسائل",
                checked = settings.soundEnabled,
                onCheckedChange = {
                    FirebaseService.updateNotificationSettings(
                        messagesEnabled = settings.messagesEnabled,
                        soundEnabled = it,
                        showContent = settings.showContent
                    )
                },
                testTag = "toggle_notification_sound"
            )

            Spacer(modifier = Modifier.height(12.dp))

            NotificationToggleCard(
                title = "إظهار محتوى الرسالة",
                subtitle = "عرض نص الرسالة داخل معاينة الإشعار",
                checked = settings.showContent,
                onCheckedChange = {
                    FirebaseService.updateNotificationSettings(
                        messagesEnabled = settings.messagesEnabled,
                        soundEnabled = settings.soundEnabled,
                        showContent = it
                    )
                },
                testTag = "toggle_show_content"
            )
        }
    }
}

@Composable
fun NotificationToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
