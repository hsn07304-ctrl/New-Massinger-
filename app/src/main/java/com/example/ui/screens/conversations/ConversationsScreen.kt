package com.example.ui.screens.conversations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.example.data.firebase.FirebaseService
import com.example.data.model.Conversation
import com.example.ui.components.AvatarInitials
import com.example.ui.components.EmptyState
import com.example.ui.theme.BorderIndigo
import com.example.util.TimeFormatter

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Surface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationsScreen(
    onNavigateToChat: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null
) {
    val conversations by FirebaseService.conversationsFlow.collectAsState()
    val currentUser by FirebaseService.currentUserFlow.collectAsState()
    val currentUid = currentUser?.uid ?: ""
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .shadow(1.dp)
                    .then(
                        if (!isDark) Modifier.border(width = 0.5.dp, color = BorderIndigo) else Modifier
                    ),
                color = MaterialTheme.colorScheme.surface
            ) {
                TopAppBar(
                    title = { Text(text = "المحادثات", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onNavigateBack != null) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.testTag("conversations_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع"
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onNavigateToSearch,
                            modifier = Modifier.testTag("conversations_search_button")
                        ) {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "بحث عن شخص")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (conversations.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ChatBubbleOutline,
                    title = "لا توجد محادثات بعد",
                    subtitle = "ابحث عن أي شخص بواسطة Username وابدأ معه محادثة نصية سريعة"
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(conversations, key = { it.id }) { conv ->
                        ConversationCard(
                            conversation = conv,
                            currentUid = currentUid,
                            onClick = { onNavigateToChat(conv.id) },
                            onAvatarClick = { otherUid -> onNavigateToProfile(otherUid) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationCard(
    conversation: Conversation,
    currentUid: String,
    onClick: () -> Unit,
    onAvatarClick: (String) -> Unit
) {
    val otherUid = conversation.participantIds.firstOrNull { it != currentUid } ?: ""
    val otherInfo = conversation.participantData[otherUid]

    val otherName = otherInfo?.displayName?.ifBlank { "مستخدم" } ?: "مستخدم"
    val otherUsername = otherInfo?.username?.ifBlank { "user" } ?: "user"
    val isOtherOnline = otherInfo?.isOnline ?: false

    val isOtherTyping = conversation.typingMap[otherUid] == true
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .then(
                if (!isDark) Modifier.border(1.dp, BorderIndigo, RoundedCornerShape(16.dp)) else Modifier
            )
            .testTag("conversation_card_${conversation.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarInitials(
                name = otherName,
                size = 50.dp,
                showOnlineBadge = true,
                isOnline = isOtherOnline,
                modifier = Modifier.clickable { onAvatarClick(otherUid) }
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = otherName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (conversation.lastMessageTimestamp > 0) {
                        Text(
                            text = TimeFormatter.formatConversationTime(conversation.lastMessageTimestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "@$otherUsername",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isOtherTyping) {
                    Text(
                        text = "يكتب الآن...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = conversation.lastMessageText.ifBlank { "انقر لبدء المحادثة" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
