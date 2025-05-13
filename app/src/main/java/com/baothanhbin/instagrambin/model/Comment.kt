package com.baothanhbin.instagrambin.model

data class Comment(
    val commentId: String = "",
    val postId: String = "",
    val userId: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    var user: User = User()
) 