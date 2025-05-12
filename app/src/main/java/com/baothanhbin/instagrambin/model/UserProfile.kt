package com.baothanhbin.instagrambin.model

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val fullName: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val followers: Map<String, Boolean> = mapOf(),
    val following: Map<String, Boolean> = mapOf(),
    val email: String = "",
    val gender: String = "",
    val followingState: Boolean = false
) {
    // Computed properties to get counts
    val followersCount: Int get() = followers.size
    val followingCount: Int get() = following.size
}