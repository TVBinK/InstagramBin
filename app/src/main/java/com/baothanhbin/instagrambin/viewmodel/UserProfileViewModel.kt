package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserProfileViewModel : ViewModel() {
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == null) {
                    _error.value = "You must be logged in to view profiles"
                    return@launch
                }

                val database = FirebaseDatabase.getInstance().reference

                // Fetch user profile data
                val userSnapshot = database.child("users").child(userId).get().await()
                val userData = userSnapshot.getValue(UserProfile::class.java)

                // Check if current user is following this user
                val isFollowing = if (currentUserId != null) {
                    val followSnapshot = database.child("follows")
                        .child(currentUserId)
                        .child(userId)
                        .get()
                        .await()
                    followSnapshot.exists()
                } else false

                userData?.let {
                    _userProfile.value = it.copy(followingState = isFollowing)
                }

                // Load user's posts
                val postsSnapshot = database.child("posts")
                    .orderByChild("userId")
                    .equalTo(userId)
                    .get()
                    .await()

                val postsList = postsSnapshot.children.mapNotNull { it.getValue(Post::class.java) }
                    .sortedByDescending { it.timestamp }
                _posts.value = postsList

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load profile"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == null) {
                    _error.value = "You must be logged in to follow users"
                    return@launch
                }

                val database = FirebaseDatabase.getInstance().reference
                val isFollowing = _userProfile.value?.followingState ?: false

                if (isFollowing) {
                    // Unfollow
                    database.child("follows")
                        .child(currentUserId)
                        .child(userId)
                        .removeValue()
                        .await()
                } else {
                    // Follow
                    database.child("follows")
                        .child(currentUserId)
                        .child(userId)
                        .setValue(true)
                        .await()
                }

                // Update local state
                _userProfile.value = _userProfile.value?.copy(
                    followingState = !isFollowing,
                    followers = if (isFollowing) _userProfile.value?.followers?.minus(1) ?: 0
                    else _userProfile.value?.followers?.plus(1) ?: 0
                )

            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to update follow status"
            }
        }
    }
} 