package com.baothanhbin.instagrambin.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.baothanhbin.instagrambin.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CallActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra("callId") ?: ""
        val fromUserId = intent.getStringExtra("fromUserId") ?: ""
        val callerName = intent.getStringExtra("callerName") ?: "Người gọi"
        
        Log.d("CallActionReceiver", "Received action: ${intent.action}")
        Log.d("CallActionReceiver", "CallId: $callId, FromUserId: $fromUserId")
        
        when (intent.action) {
            "ACTION_ACCEPT_CALL" -> {
                Log.d("CallActionReceiver", "Accepting call from $fromUserId")
                
                // Gửi tín hiệu accept lên server
                sendCallResponse(callId, fromUserId, "accept")
                
                // Mở MainActivity với video call
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("openVideoCall", true)
                    putExtra("caller_id", fromUserId)
                    putExtra("call_id", callId)
                    putExtra("caller_name", callerName)
                }
                context.startActivity(openIntent)
                
                // Đóng notification
                dismissNotification(context, callId)
            }
            "ACTION_REJECT_CALL" -> {
                Log.d("CallActionReceiver", "Rejecting call from $fromUserId")
                
                // Gửi tín hiệu reject lên server
                sendCallResponse(callId, fromUserId, "reject")
                
                // Đóng notification
                dismissNotification(context, callId)
            }
        }
    }
    
    private fun sendCallResponse(callId: String, fromUserId: String, response: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("CallActionReceiver", "User not authenticated")
            return
        }
        
        val database = FirebaseDatabase.getInstance()
        val responseRef = database.getReference("calls")
            .child(callId)
            .child("responses")
            .child(currentUser.uid)
        
        val responseData = mapOf(
            "response" to response,
            "timestamp" to System.currentTimeMillis(),
            "userId" to currentUser.uid
        )
        
        responseRef.setValue(responseData)
            .addOnSuccessListener {
                Log.d("CallActionReceiver", "Call response sent successfully: $response")
            }
            .addOnFailureListener { e ->
                Log.e("CallActionReceiver", "Failed to send call response: ${e.message}")
            }
    }
    
    private fun dismissNotification(context: Context, callId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = "video_call_$callId".hashCode()
        notificationManager.cancel(notificationId)
        Log.d("CallActionReceiver", "Dismissed notification with ID: $notificationId")
    }
} 