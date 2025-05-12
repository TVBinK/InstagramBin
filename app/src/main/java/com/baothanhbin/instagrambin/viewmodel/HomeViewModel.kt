package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val friends: List<User> = emptyList(),
    val currentUser: User? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Get current user's following list
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val userRef = database.getReference("users").child(currentUser.uid)
                val userSnapshot = userRef.get().await()
                val user = userSnapshot.getValue(User::class.java)
                
                // Get posts from current user and users they follow
                val followingIds = user?.following?.keys?.toList() ?: emptyList()
                val userIds = followingIds + currentUser.uid
                
                val posts = mutableListOf<Post>()
                for (userId in userIds) {
                    val userPostsSnapshot = database.getReference("posts")
                        .orderByChild("userId")
                        .equalTo(userId)
                        .get()
                        .await()
                    
                    val userPosts = userPostsSnapshot.children.mapNotNull { 
                        it.getValue(Post::class.java) 
                    }
                    posts.addAll(userPosts)
                }

                // Sort posts by timestamp
                val sortedPosts = posts.sortedByDescending { it.timestamp }

                // Load user data for each post
                val postsWithUserData = sortedPosts.map { post ->
                    val postUserSnapshot = database.getReference("users")
                        .child(post.userId)
                        .get()
                        .await()
                    val postUser = postUserSnapshot.getValue(User::class.java)
                    post.copy(user = postUser ?: User())
                }

                // Load friends (following)
                val friends = mutableListOf<User>()
                for (friendId in followingIds) {
                    val friendSnapshot = database.getReference("users").child(friendId).get().await()
                    friendSnapshot.getValue(User::class.java)?.let { friends.add(it) }
                }

                _uiState.update { it.copy(posts = postsWithUserData, isLoading = false, friends = friends, currentUser = user) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val currentUser = auth.currentUser ?: return@launch
            val postRef = database.getReference("posts").child(post.postId)
            val isLiked = post.likes.containsKey(currentUser.uid)
            val updatedLikes = post.likes.toMutableMap().apply {
                if (isLiked) remove(currentUser.uid) else put(currentUser.uid, true)
            }
            val newLikesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
            postRef.child("likes").setValue(updatedLikes).await()
            postRef.child("likesCount").setValue(newLikesCount).await()
            loadPosts()
        }
    }
} 