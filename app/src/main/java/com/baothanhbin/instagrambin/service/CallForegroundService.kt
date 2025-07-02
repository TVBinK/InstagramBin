package com.baothanhbin.instagrambin.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.baothanhbin.instagrambin.MainActivity
import com.baothanhbin.instagrambin.R

class CallForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "call_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_ACCEPT = "com.baothanhbin.instagrambin.ACTION_ACCEPT_CALL"
        const val ACTION_REJECT = "com.baothanhbin.instagrambin.ACTION_REJECT_CALL"
        const val EXTRA_CALLER_ID = "caller_id"
        const val EXTRA_CALLER_NAME = "caller_name"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleAction(intent)
        val callerId = intent?.getStringExtra(EXTRA_CALLER_ID) ?: ""
        val callerName = intent?.getStringExtra(EXTRA_CALLER_NAME) ?: "Người gọi"
        createNotificationChannel()
        val notification = buildCallNotification(callerId, callerName)
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun buildCallNotification(callerId: String, callerName: String): Notification {
        val acceptIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_ACCEPT
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
        }
        val acceptPendingIntent = PendingIntent.getService(
            this, 0, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rejectIntent = Intent(this, CallForegroundService::class.java).apply {
            action = ACTION_REJECT
            putExtra(EXTRA_CALLER_ID, callerId)
            putExtra(EXTRA_CALLER_NAME, callerName)
        }
        val rejectPendingIntent = PendingIntent.getService(
            this, 1, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_videocam)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("$callerName đang gọi cho bạn")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .addAction(R.drawable.ic_videocam, "Chấp nhận", acceptPendingIntent)
            .addAction(R.drawable.ic_heart, "Từ chối", rejectPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Cuộc gọi đến",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Thông báo cuộc gọi đến"
            channel.setSound(null, null)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun handleAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_ACCEPT -> {
                // Mở app vào VideoCallScreen
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openVideoCall", true)
                    putExtra(EXTRA_CALLER_ID, intent.getStringExtra(EXTRA_CALLER_ID))
                    putExtra(EXTRA_CALLER_NAME, intent.getStringExtra(EXTRA_CALLER_NAME))
                }
                startActivity(mainIntent)
                stopSelf()
            }
            ACTION_REJECT -> {
                // TODO: Gửi tín hiệu từ chối về server nếu cần
                stopSelf()
            }
        }
    }
} 