package com.baothanhbin.instagrambin.model

import androidx.credentials.PasswordCredential

data class User(
    val uid: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val profileImageUrl: String = "",
    val bio: String = "",
    val followers: Map<String, Boolean> = mapOf(),
    val following: Map<String, Boolean> = mapOf(),
    val gender: String = ""
) {
    // Computed properties to get counts
    val followersCount: Int get() = followers.size
    val followingCount: Int get() = following.size
}
