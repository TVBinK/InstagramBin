package com.baothanhbin.instagrambin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.instagrambin.model.CallHistory
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallHistoryItem(
    callHistory: CallHistory,
    isCurrentUser: Boolean,
    onCallClick: (() -> Unit)? = null
) {
    // Layout như message item, không căn giữa
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier
                .widthIn(min = 200.dp, max = 280.dp)
                .clickable { onCallClick?.invoke() },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            color = if (isCurrentUser) 
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Call direction icon với background circle
                val (icon, iconColor, backgroundColor) = getCallIconAndColors(callHistory, isCurrentUser)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                
                // Call info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Call status
                        Text(
                            text = getCallStatusText(callHistory, isCurrentUser),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isCurrentUser) 
                                MaterialTheme.colorScheme.primary
                            else 
                                MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Call type icon nhỏ
                        Icon(
                            imageVector = if (callHistory.callType == "video") Icons.Default.Videocam else Icons.Default.Phone,
                            contentDescription = if (callHistory.callType == "video") "Video call" else "Voice call",
                            tint = if (isCurrentUser) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    
                    // Time và duration trên cùng một row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatCallTime(callHistory.startTime),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp
                            ),
                            color = if (isCurrentUser) 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        // Duration (if call was answered)
                        if (callHistory.isAnswered && callHistory.durationString.isNotEmpty()) {
                            Text(
                                text = callHistory.durationString,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Color(0xFF4CAF50) // Green color for duration
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getCallIconAndColors(callHistory: CallHistory, isCurrentUser: Boolean): Triple<ImageVector, Color, Color> {
    return when {
        callHistory.isMissed -> {
            if (isCurrentUser) {
                // User gọi nhưng không được trả lời - outgoing missed
                Triple(
                    Icons.Default.CallMade,
                    Color.White,
                    Color(0xFFFF5252) // Red background
                )
            } else {
                // User nhận cuộc gọi nhưng bỏ lỡ - incoming missed
                Triple(
                    Icons.Default.CallReceived,
                    Color.White,
                    Color(0xFFFF5252) // Red background
                )
            }
        }
        callHistory.isRejected -> {
            if (isCurrentUser) {
                // User gọi nhưng bị từ chối - outgoing rejected
                Triple(
                    Icons.Default.CallMade,
                    Color.White,
                    Color(0xFFFF7043) // Orange-red background
                )
            } else {
                // User từ chối cuộc gọi - incoming rejected
                Triple(
                    Icons.Default.CallReceived,
                    Color.White,
                    Color(0xFFFF7043) // Orange-red background
                )
            }
        }
        callHistory.isAnswered -> {
            if (isCurrentUser) {
                // User gọi và được trả lời - outgoing answered
                Triple(
                    Icons.Default.CallMade,
                    Color.White,
                    Color(0xFF4CAF50) // Green background
                )
            } else {
                // User nhận và trả lời cuộc gọi - incoming answered
                Triple(
                    Icons.Default.CallReceived,
                    Color.White,
                    Color(0xFF4CAF50) // Green background
                )
            }
        }
        else -> {
            // Default - calling state
            if (isCurrentUser) {
                Triple(
                    Icons.Default.CallMade,
                    Color.White,
                    Color(0xFF757575) // Gray background
                )
            } else {
                Triple(
                    Icons.Default.CallReceived,
                    Color.White,
                    Color(0xFF757575) // Gray background
                )
            }
        }
    }
}

private fun getCallStatusText(callHistory: CallHistory, isCurrentUser: Boolean): String {
    return when {
        callHistory.isMissed -> {
            if (isCurrentUser) "Cuộc gọi nhỡ" else "Cuộc gọi bỏ lỡ"
        }
        callHistory.isRejected -> {
            if (isCurrentUser) "Cuộc gọi bị từ chối" else "Đã từ chối"
        }
        callHistory.isAnswered -> {
            if (isCurrentUser) "Cuộc gọi đi" else "Cuộc gọi đến"
        }
        else -> "Cuộc gọi"
    }
}

private fun formatCallTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    val today = calendar.apply { 
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val yesterday = today - 24 * 60 * 60 * 1000
    
    return when {
        timestamp >= today -> {
            // Hôm nay - chỉ hiển thị giờ
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
        timestamp >= yesterday -> {
            // Hôm qua
            "Hôm qua ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        }
        else -> {
            // Lâu hơn - ngắn gọn hơn
            SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }
} 