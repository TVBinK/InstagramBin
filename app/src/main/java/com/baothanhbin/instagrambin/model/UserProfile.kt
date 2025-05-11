package com.baothanhbin.instagrambin.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val followers: Int = 0,
    val following: Int = 0,
    val email: String = "",
    val gender: String = "",
    val followingState: Boolean = false
)