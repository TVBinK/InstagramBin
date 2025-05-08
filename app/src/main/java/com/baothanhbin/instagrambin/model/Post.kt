package com.baothanhbin.instagrambin.model

data class Post(
    val user: User,
    val post: String,
    val description: String,
    val likesCount: Int,
    val commentsCount: Int
)
