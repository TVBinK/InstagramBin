package com.baothanhbin.instagrambin.repository

import com.baothanhbin.instagrambin.model.Message
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.UUID
import android.net.Uri
import com.baothanhbin.instagrambin.service.CloudinaryService
import android.app.Application
import android.content.Context
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.util.concurrent.ConcurrentHashMap
import com.baothanhbin.instagrambin.service.FirebaseOptimizationService

class MessageRepository(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val cloudinaryService = CloudinaryService(application)
    private val firebaseOptimizationService = FirebaseOptimizationService(application)
    
    // Cache để lưu thông tin user, tránh load lại nhiều lần
    private val userCache = ConcurrentHashMap<String, User>()
    
    companion object {
        const val MESSAGES_PER_PAGE = 20
    }

    suspend fun sendMessage(receiverId: String, content: String): Message {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val messageId = UUID.randomUUID().toString()
        
        val message = Message(
            messageId = messageId,
            senderId = currentUser.uid,
            receiverId = receiverId,
            content = content,
            timestamp = System.currentTimeMillis()
        )

        // Save message to both sender and receiver's chat
        val senderChatRef = database.getReference("chats")
            .child(currentUser.uid)
            .child(receiverId)
            .child(messageId)
        
        val receiverChatRef = database.getReference("chats")
            .child(receiverId)
            .child(currentUser.uid)
            .child(messageId)

        senderChatRef.setValue(message).await()
        receiverChatRef.setValue(message).await()

        // Get receiver's FCM token and send notification
        val receiverSnapshot = database.getReference("users").child(receiverId).get().await()
        val receiver = receiverSnapshot.getValue(User::class.java)
        val receiverToken = receiverSnapshot.child("fcmToken").getValue(String::class.java)

        // Get sender's info for notification
        val senderSnapshot = database.getReference("users").child(currentUser.uid).get().await()
        val sender = senderSnapshot.getValue(User::class.java)

        // Send FCM notification
        receiverToken?.let { token ->
            sendFCMNotification(
                token = token,
                title = sender?.username ?: "Người dùng",
                body = content
            )
        }

        return message
    }

    private fun sendFCMNotification(token: String, title: String, body: String) {
        Thread {
            try {
                val url = URL("https://fcm.googleapis.com/fcm/send")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "key=YOUR_SERVER_KEY") // Thay YOUR_SERVER_KEY bằng Server Key từ Firebase Console

                val json = JSONObject().apply {
                    put("to", token)
                    put("notification", JSONObject().apply {
                        put("title", title)
                        put("body", body)
                        put("sound", "default")
                    })
                }

                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(json.toString().toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Notification sent successfully
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    suspend fun sendImage(receiverId: String, imageUri: Uri): Message {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        val messageId = UUID.randomUUID().toString()
        
        // Upload image to Cloudinary
        val imageUrl = cloudinaryService.uploadImage(imageUri)
        
        val message = Message(
            messageId = messageId,
            senderId = currentUser.uid,
            receiverId = receiverId,
            content = imageUrl, // Store image URL as message content
            timestamp = System.currentTimeMillis()
        )

        // Save message to both sender and receiver's chat
        val senderChatRef = database.getReference("chats")
            .child(currentUser.uid)
            .child(receiverId)
            .child(messageId)
        
        val receiverChatRef = database.getReference("chats")
            .child(receiverId)
            .child(currentUser.uid)
            .child(messageId)

        senderChatRef.setValue(message).await()
        receiverChatRef.setValue(message).await()

        return message
    }

    // Tối ưu hóa: Load tin nhắn với pagination sử dụng FirebaseOptimizationService
    suspend fun getMessages(userId: String, limit: Int = MESSAGES_PER_PAGE, lastMessageTimestamp: Long? = null): List<Message> {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        // Sử dụng FirebaseOptimizationService để load tin nhắn
        val messages = firebaseOptimizationService.getMessages(
            currentUserId = currentUser.uid,
            otherUserId = userId,
            limit = limit,
            lastMessageTimestamp = lastMessageTimestamp
        )

        // Load user information efficiently
        loadUserInfoForMessages(messages)

        return messages
    }

    // Tối ưu hóa: Load thông tin user một cách hiệu quả
    private suspend fun loadUserInfoForMessages(messages: List<Message>) {
        val userIds = mutableSetOf<String>()
        
        // Collect all unique user IDs
        messages.forEach { message ->
            userIds.add(message.senderId)
            userIds.add(message.receiverId)
        }
        
        // Sử dụng FirebaseOptimizationService để batch load users
        val usersMap = firebaseOptimizationService.getUsers(userIds.toList())
        
        // Assign user info to messages
        messages.forEach { message ->
            message.sender = usersMap[message.senderId] ?: User()
            message.receiver = usersMap[message.receiverId] ?: User()
        }
    }

    // Tối ưu hóa: Load tin nhắn mới nhất (cho real-time updates)
    suspend fun getLatestMessages(userId: String, sinceTimestamp: Long): List<Message> {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        val messagesSnapshot = database.getReference("chats")
            .child(currentUser.uid)
            .child(userId)
            .orderByChild("timestamp")
            .startAfter(sinceTimestamp.toDouble())
            .get()
            .await()

        val messages = messagesSnapshot.children.mapNotNull { childSnapshot -> 
            childSnapshot.getValue(Message::class.java) 
        }.sortedBy { message -> message.timestamp }

        // Load user information efficiently
        loadUserInfoForMessages(messages)

        return messages
    }

    suspend fun markMessagesAsRead(userId: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        val messagesSnapshot = database.getReference("chats")
            .child(currentUser.uid)
            .child(userId)
            .orderByChild("isRead")
            .equalTo(false)
            .get()
            .await()

        for (messageSnapshot in messagesSnapshot.children) {
            val message = messageSnapshot.getValue(Message::class.java)
            if (message != null && message.senderId == userId) {
                messageSnapshot.ref.child("isRead").setValue(true).await()
            }
        }
    }

    // Clear cache khi cần thiết
    fun clearUserCache() {
        userCache.clear()
        firebaseOptimizationService.clearCache()
    }

    // Preload user info cho một user cụ thể
    suspend fun preloadUserInfo(userId: String) {
        firebaseOptimizationService.getUser(userId)
    }

    // Preload multiple users
    suspend fun preloadUsers(userIds: List<String>) {
        firebaseOptimizationService.preloadUsers(userIds)
    }

    // Get cache statistics for debugging
    fun getCacheStats(): Map<String, Any> {
        return firebaseOptimizationService.getCacheStats()
    }
} 