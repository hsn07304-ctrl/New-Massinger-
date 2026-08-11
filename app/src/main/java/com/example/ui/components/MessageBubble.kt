package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Message
import com.example.data.model.MessageStatus
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.example.ui.theme.BorderIndigo
import com.example.ui.theme.MessageBubbleIncomingDark
import com.example.ui.theme.MessageBubbleIncomingLight
import com.example.ui.theme.MessageBubbleOutgoing
import com.example.util.TimeFormatter

@Composable
fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    modifier: Modifier = Modifier
) {
    val bubbleShape = if (isOutgoing) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
    }

    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val backgroundColor = if (isOutgoing) {
        MessageBubbleOutgoing
    } else {
        if (isDark) MessageBubbleIncomingDark else MessageBubbleIncomingLight
    }

    val textColor = if (isOutgoing) Color.White else MaterialTheme.colorScheme.onSurface
    val timeColor = if (isOutgoing) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .shadow(
                    elevation = if (isOutgoing) 2.dp else 1.dp,
                    shape = bubbleShape,
                    clip = false
                )
                .clip(bubbleShape)
                .background(backgroundColor)
                .then(
                    if (!isOutgoing && !isDark) {
                        Modifier.border(1.dp, BorderIndigo, bubbleShape)
                    } else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .testTag("message_bubble_${message.id}")
        ) {
            Column {
                Text(
                    text = message.text,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = TimeFormatter.formatMessageTime(message.timestamp),
                        color = timeColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp
                    )

                    if (isOutgoing) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val statusIcon = if (message.status == MessageStatus.READ) {
                            Icons.Default.DoneAll
                        } else {
                            Icons.Default.Done
                        }
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = "حالة الرسالة",
                            tint = if (message.status == MessageStatus.READ) Color(0xFF38BDF8) else timeColor,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
