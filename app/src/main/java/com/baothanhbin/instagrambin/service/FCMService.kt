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
                // Khi có cuộc gọi video đến, hiển thị notification
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
        // Xác định channelId và channelName dựa trên loại notification
        val channelId = when (type) {
            "like", "post_like" -> "like_notification"
            "comment", "post_comment" -> "comment_notification"
            "reply" -> "reply_notification"
            "new_message" -> "chat_notification"
            else -> "general_notification"
        }
        // Tạo NotificationManager và NotificationChannel
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Tạo notification channel nếu chưa tồn tại
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelId, // Dùng luôn channelId làm tên channel cho gọn
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        // Tạo intent để mở app khi nhấn vào notification
        val intent = when (type) {
            "like", "post_like", "comment", "post_comment" -> {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)  // Xóa các activity cũ trong stack
                    putExtra("openPostDetail", true)
                    putExtra("postId", data["postId"])
                }
            }
            "reply" -> {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openPostDetail", true)
                    putExtra("postId", data["postId"])
                    putExtra("commentId", data["commentId"])
                }
            }
            "new_message" -> {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openChat", true)
                    putExtra("senderId", data["senderId"])
                }
            }
            else -> {
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            }
        }
        // Tạo PendingIntent để mở activity khi nhấn vào notification
        val pendingIntent = PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Tạo notification với các icon, title,
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        // Hiển thị notification
        notificationManager.notify(type.hashCode(), notification)
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
        // Hàm này sẽ được gọi khi người dùng đăng nhập thành công
        fun updateFCMToken() {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Lấy token FCM mới và lưu vào database
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