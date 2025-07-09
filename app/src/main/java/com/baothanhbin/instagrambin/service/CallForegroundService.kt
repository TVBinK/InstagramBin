package com.baothanhbin.instagrambin.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.baothanhbin.instagrambin.R

class CallForegroundService : Service() {
    companion object {
        const val CHANNEL_ID = "call_foreground_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_CALLER_ID = "caller_id"
        const val EXTRA_CALLER_NAME = "caller_name"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ForegroundServiceType")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val callerId = intent?.getStringExtra(EXTRA_CALLER_ID) ?: ""
        val callerName = intent?.getStringExtra(EXTRA_CALLER_NAME) ?: "Người gọi"
        createNotificationChannel()
        val notification = buildCallNotification(callerId, callerName)
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    private fun buildCallNotification(callerId: String, callerName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_videocam)
            .setContentTitle("Cuộc gọi đến")
            .setContentText("$callerName đang gọi cho bạn")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
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
} 