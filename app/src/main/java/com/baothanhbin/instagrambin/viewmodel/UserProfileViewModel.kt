package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfileUiState(
    val user: User? = null,
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isFollowing: Boolean = false
)

class UserProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private var userId: String? = null

    fun setUserId(id: String) {
        userId = id
        loadUserProfile()
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                userId?.let { id ->
                    val userRef = database.getReference("users").child(id)
                    val currentUserRef = database.getReference("users").child(auth.currentUser?.uid ?: "")
                    
                    // Get user data
                    val userSnapshot = userRef.get().await()
                    val user = userSnapshot.getValue(User::class.java)
                    
                    // Check if current user is following this user
                    val isFollowing = currentUserRef.child("following")
                        .child(id)
                        .get()
                        .await()
                        .exists()

                    // Load user's posts
                    val postsSnapshot = database.getReference("posts")
                        .orderByChild("userId")
                        .equalTo(id)
                        .get()
                        .await()

                    val posts = postsSnapshot.children.mapNotNull { 
                        it.getValue(Post::class.java) 
                    }.sortedByDescending { it.timestamp }
                    
                    _uiState.update { 
                        it.copy(
                            user = user,
                            posts = posts,
                            isFollowing = isFollowing,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Có lỗi xảy ra",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val currentUserId = auth.currentUser?.uid
                userId?.let { targetUserId ->
                    if (currentUserId != null) {
                        val currentUserRef = database.getReference("users").child(currentUserId)
                        val targetUserRef = database.getReference("users").child(targetUserId)
                        
                        if (_uiState.value.isFollowing) {
                            // Unfollow
                            currentUserRef.child("following").child(targetUserId).removeValue().await()
                            targetUserRef.child("followers").child(currentUserId).removeValue().await()
                        } else {
                            // Follow
                            currentUserRef.child("following").child(targetUserId).setValue(true).await()
                            targetUserRef.child("followers").child(currentUserId).setValue(true).await()
                        }
                        
                        _uiState.update { 
                            it.copy(
                                isFollowing = !it.isFollowing,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message ?: "Có lỗi xảy ra",
                        isLoading = false
                    )
                }
            }
        }
    }
} 