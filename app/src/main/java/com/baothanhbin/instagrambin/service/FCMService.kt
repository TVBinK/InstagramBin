package com.baothanhbin.instagrambin.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.baothanhbin.instagrambin.MainActivity
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.ui.screens.PostDetailScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Lưu token mới vào database để có thể gửi thông báo cho người dùng này
        saveTokenToDatabase(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: "Thông báo"
        val body = message.notification?.body ?: "Bạn có thông báo mới"
        val data = message.data
        val notificationType = data["type"] ?: "message"

        when (notificationType) {
            "call" -> {
                // Khi có cuộc gọi đến, start foreground service
                val callerId = data["callerId"] ?: ""
                val callerName = data["callerName"] ?: "Người gọi"
                val serviceIntent = Intent(this, CallForegroundService::class.java).apply {
                    putExtra(CallForegroundService.EXTRA_CALLER_ID, callerId)
                    putExtra(CallForegroundService.EXTRA_CALLER_NAME, callerName)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                return
            }
            "video_call_offer" -> {
                // Hiển thị notification với 2 nút cho video call
                showVideoCallNotification(
                    title,
                    body,
                    data
                )
                return
            }
            else -> {
                showNotification(title, body, notificationType, data)
            }
        }
    }

    private fun showVideoCallNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = "video_calls" // Phải khớp với channelId trong Firebase Cloud Functions
        val channelName = "Video Call Notifications"
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Tạo notification channel cho video calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for video calls"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val callId = data["callId"] ?: ""
        val fromUserId = data["fromUserId"] ?: ""
        val callerName = data["callerName"] ?: "Người gọi"
        
        // Intent chính khi tap vào notification
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("openVideoCall", true)
            putExtra("caller_id", fromUserId)
            putExtra("call_id", callId)
            putExtra("caller_name", callerName)
        }
        
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            "video_call_main".hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_videocam)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
        
        val notification = notificationBuilder.build()
        val notificationId = "video_call_$callId".hashCode()
        notificationManager.notify(notificationId, notification)
    }

    private fun showNotification(title: String, body: String, type: String, data: Map<String, String>) {
        val channelId = when (type) {
            "like" -> "like_notification"
            "comment" -> "comment_notification"
            "reply" -> "reply_notification"
            "new_message" -> "chat_notification"
            "post_like" -> "like_notification"
            "post_comment" -> "comment_notification"
            else -> "general_notification"
        }
        
        val channelName = when (type) {
            "like", "post_like" -> "Like Notifications"
            "comment", "post_comment" -> "Comment Notifications"
            "reply" -> "Reply Notifications"
            "new_message" -> "Chat Notifications"
            else -> "General Notifications"
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for $channelName"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val intent = when (type) {
            "like", "post_like", "comment", "post_comment" -> {
                val postId = data["postId"]
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openPostDetail", true)
                    putExtra("postId", postId)
                }
            }
            "reply" -> {
                val postId = data["postId"]
                val commentId = data["commentId"]
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openPostDetail", true)
                    putExtra("postId", postId)
                    putExtra("commentId", commentId)
                }
            }
            "new_message" -> {
                val senderId = data["senderId"]
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openChat", true)
                    putExtra("senderId", senderId)
                }
            }
            else -> {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val icon = when (type) {
            "like", "post_like" -> R.drawable.ic_heart
            "comment", "post_comment", "reply" -> R.drawable.ic_comment
            "new_message" -> R.drawable.ic_send
            else -> R.drawable.instagram_tag_icon
        }
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        val notification = notificationBuilder.build()
        val notificationId = type.hashCode()
        notificationManager.notify(notificationId, notification)
    }

    private fun saveTokenToDatabase(token: String) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        currentUser?.let { user ->
            val database = FirebaseDatabase.getInstance()
            database.getReference("users")
                .child(user.uid)
                .child("fcmToken")
                .setValue(token)
                .addOnSuccessListener {
                    println("FCM Token saved successfully: $token")
                }
                .addOnFailureListener { e ->
                    println("Failed to save FCM token: ${e.message}")
                }
        }
    }

    companion object {
        // Method để gọi từ bên ngoài khi user đăng nhập
        fun updateFCMToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    token?.let { 
                        val auth = FirebaseAuth.getInstance()
                        val currentUser = auth.currentUser
                        
                        currentUser?.let { user ->
                            val database = FirebaseDatabase.getInstance()
                            database.getReference("users")
                                .child(user.uid)
                                .child("fcmToken")
                                .setValue(token)
                                .addOnSuccessListener {
                                    println("FCM Token updated on login: $token")
                                }
                                .addOnFailureListener { e ->
                                    println("Failed to update FCM token: ${e.message}")
                                }
                        }
                    }
                } else {
                    println("Failed to get FCM token: ${task.exception?.message}")
                }
            }
        }
    }
} 