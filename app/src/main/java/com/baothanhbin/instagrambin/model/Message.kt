package com.baothanhbin.instagrambin.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    var sender: User = User(),
    var receiver: User = User()
) 