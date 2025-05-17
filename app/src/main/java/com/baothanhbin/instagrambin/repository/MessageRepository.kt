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

class MessageRepository(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val cloudinaryService = CloudinaryService(application)

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

        return message
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

    suspend fun getMessages(userId: String): List<Message> {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        val messagesSnapshot = database.getReference("chats")
            .child(currentUser.uid)
            .child(userId)
            .get()
            .await()

        val messages = messagesSnapshot.children.mapNotNull { 
            it.getValue(Message::class.java) 
        }.sortedBy { it.timestamp }

        // Load user information for each message
        for (message in messages) {
            val senderSnapshot = database.getReference("users")
                .child(message.senderId)
                .get()
                .await()
            message.sender = senderSnapshot.getValue(User::class.java) ?: User()

            val receiverSnapshot = database.getReference("users")
                .child(message.receiverId)
                .get()
                .await()
            message.receiver = receiverSnapshot.getValue(User::class.java) ?: User()
        }

        return messages
    }

    suspend fun markMessagesAsRead(userId: String) {
        val currentUser = auth.currentUser ?: throw Exception("User not logged in")
        
        val messagesSnapshot = database.getReference("chats")
            .child(currentUser.uid)
            .child(userId)
            .get()
            .await()

        for (messageSnapshot in messagesSnapshot.children) {
            val message = messageSnapshot.getValue(Message::class.java)
            if (message != null && !message.isRead && message.senderId == userId) {
                messageSnapshot.ref.child("isRead").setValue(true).await()
            }
        }
    }
} 