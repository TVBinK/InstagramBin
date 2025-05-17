package com.baothanhbin.instagrambin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.baothanhbin.instagrambin.model.Message
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale

@Composable
fun MessageItem(
    message: Message,
    isCurrentUser: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (isCurrentUser) MaterialTheme.colorScheme.primary
                    else Color.LightGray.copy(alpha = 0.2f)
                )
                .padding(12.dp),
            horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
        ) {
            if (message.content.startsWith("http") && (message.content.endsWith(".jpg") || 
                message.content.endsWith(".jpeg") || message.content.endsWith(".png"))) {
                // Display image
                AsyncImage(
                    model = message.content,
                    contentDescription = "Message image",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { /* TODO: Show full image */ },
                    contentScale = ContentScale.Crop
                )
            } else {
                // Display text
                Text(
                    text = message.content,
                    color = if (isCurrentUser) Color.White else Color.Black,
                    textAlign = if (isCurrentUser) TextAlign.End else TextAlign.Start
                )
            }
            Text(
                text = formatMessageTime(message.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrentUser) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
} 