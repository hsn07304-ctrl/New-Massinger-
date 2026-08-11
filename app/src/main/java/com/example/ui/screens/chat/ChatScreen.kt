package com.example.ui.screens.chat

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.model.Conversation
import com.example.data.model.Message
import com.example.data.model.User
import com.example.ui.components.AvatarInitials
import com.example.ui.components.EmptyState
import com.example.ui.components.MessageBubble
import com.example.ui.components.TypingIndicator
import com.example.ui.theme.StatusOnline
import com.example.util.Debouncer
import com.example.util.TimeFormatter
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.launch

import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.BorderIndigo
import com.example.ui.theme.StatusOnlineText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit
) {
    val currentUser by FirebaseService.currentUserFlow.collectAsState()
    val currentUid = currentUser?.uid ?: ""

    var conversation by remember { mutableStateOf<Conversation?>(null) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var otherUser by remember { mutableStateOf<User?>(null) }
    var textInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val typingDebouncer = remember { Debouncer(scope, delayMs = 2000L) }

    // Other user details
    val otherUid = conversation?.participantIds?.firstOrNull { it != currentUid } ?: ""
    val isOtherTyping = (conversation?.typingMap?.get(otherUid) == true) || 
            (conversation?.isTyping == true && conversation?.lastMessageSenderId == otherUid)
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f

    // Real-time listeners setup
    DisposableEffect(conversationId) {
        var convReg: ListenerRegistration? = null
        var msgReg: ListenerRegistration? = null

        convReg = FirebaseService.listenToConversation(conversationId) { updatedConv ->
            conversation = updatedConv
            val targetUid = updatedConv.participantIds.firstOrNull { it != currentUid } ?: ""
            if (targetUid.isNotBlank() && otherUser == null) {
                scope.launch {
                    otherUser = FirebaseService.getUserProfile(targetUid)
                }
            }
        }

        msgReg = FirebaseService.listenToMessages(conversationId) { updatedMessages ->
            messages = updatedMessages
            FirebaseService.markMessagesAsRead(conversationId)
        }

        onDispose {
            convReg?.remove()
            msgReg?.remove()
            typingDebouncer.cancel()
            FirebaseService.setTypingStatus(conversationId, false)
        }
    }

    LaunchedEffect(conversationId) {
        FirebaseService.markMessagesAsRead(conversationId)
    }

    // Scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

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
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (otherUid.isNotBlank()) onNavigateToProfile(otherUid)
                                }
                                .testTag("chat_header_profile")
                        ) {
                            AvatarInitials(
                                name = otherUser?.displayName ?: "مستخدم",
                                size = 40.dp,
                                showOnlineBadge = true,
                                isOnline = otherUser?.isOnline ?: false
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = otherUser?.displayName ?: "مستخدم",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                val statusText = if (isOtherTyping) {
                                    "يكتب الآن..."
                                } else if (otherUser?.isOnline == true) {
                                    "متصل الآن"
                                } else {
                                    TimeFormatter.formatLastSeen(
                                        otherUser?.isOnline ?: false,
                                        otherUser?.lastSeen ?: System.currentTimeMillis()
                                    )
                                }

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOtherTyping) MaterialTheme.colorScheme.primary
                                    else if (otherUser?.isOnline == true) StatusOnlineText
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("chat_back_button")
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp,
                modifier = if (!isDark) Modifier.border(width = 0.5.dp, color = BorderIndigo) else Modifier
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Quick Reaction & Action Chips Bar
                    androidx.compose.foundation.lazy.LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val quickReactions = listOf("👍", "❤️", "👋", "🔥", "✨", "🎙️ تسجيل صوتي")
                        items(quickReactions) { reaction ->
                            Surface(
                                onClick = {
                                    scope.launch {
                                        val content = if (reaction == "🎙️ تسجيل صوتي") "🎙️ [رسالة صوتية 0:08]" else reaction
                                        FirebaseService.sendMessage(
                                            conversationId = conversationId,
                                            receiverId = otherUid,
                                            textText = content
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderIndigo)
                            ) {
                                Text(
                                    text = reaction,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { newText ->
                                textInput = newText
                                if (newText.isNotBlank()) {
                                    FirebaseService.setTypingStatus(conversationId, true)
                                    typingDebouncer.submit {
                                        FirebaseService.setTypingStatus(conversationId, false)
                                    }
                                } else {
                                    FirebaseService.setTypingStatus(conversationId, false)
                                }
                            },
                            placeholder = { 
                                Text(
                                    text = "اكتب رسالة نصية...",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                ) 
                            },
                            maxLines = 4,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderIndigo
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                val textToSend = textInput.trim()
                                if (textToSend.isNotBlank()) {
                                    textInput = ""
                                    typingDebouncer.cancel()
                                    FirebaseService.setTypingStatus(conversationId, false)

                                    scope.launch {
                                        FirebaseService.sendMessage(
                                            conversationId = conversationId,
                                            receiverId = otherUid,
                                            textText = textToSend
                                        )
                                    }
                                } else {
                                    // Send quick voice note if input is empty
                                    scope.launch {
                                        FirebaseService.sendMessage(
                                            conversationId = conversationId,
                                            receiverId = otherUid,
                                            textText = "🎙️ [رسالة صوتية 0:05]"
                                        )
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .testTag("chat_send_button"),
                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = androidx.compose.ui.graphics.Color.White
                            )
                        ) {
                            Icon(
                                imageVector = if (textInput.isBlank()) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                                contentDescription = if (textInput.isBlank()) "رسالة صوتية" else "إرسال"
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (messages.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.Forum,
                    title = "بدء محادثة جديدة",
                    subtitle = "أرسل أول رسالة نصية إلى ${otherUser?.displayName ?: "المستخدم"}"
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Sleek Date Pill Separator
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = "اليوم",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        val isOutgoing = msg.senderId == currentUid
                        MessageBubble(
                            message = msg,
                            isOutgoing = isOutgoing
                        )
                    }

                    if (isOtherTyping) {
                        item {
                            TypingIndicator(
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
