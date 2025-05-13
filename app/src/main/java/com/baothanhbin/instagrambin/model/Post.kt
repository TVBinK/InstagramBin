package com.baothanhbin.instagrambin.model

data class Post(
    val postId: String = "",
    val userId: String = "",
    val imageUrl: String = "",
    val caption: String = "",
    val timestamp: Long = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val user: User = User(),
    val likes: Map<String, Boolean> = mapOf(),
    val timeAgo: String = ""
)
