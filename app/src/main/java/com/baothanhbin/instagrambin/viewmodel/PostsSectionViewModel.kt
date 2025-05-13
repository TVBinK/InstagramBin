package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import android.util.Log
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
import kotlinx.coroutines.delay

data class PostsSectionUiState(
    val posts: List<Post> = emptyList(),
    val currentPost: Post? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PostsSectionViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(PostsSectionUiState())
    val uiState: StateFlow<PostsSectionUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    init {
        loadPosts()
    }

    fun loadPosts() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                
                // Get posts from current user
                val postsSnapshot = database.getReference("posts")
                    .orderByChild("userId")
                    .equalTo(currentUser.uid)
                    .get()
                    .await()

                val posts = postsSnapshot.children.mapNotNull { 
                    it.getValue(Post::class.java) 
                }.sortedByDescending { it.timestamp }

                // Load user data for each post
                val postsWithUserData = posts.map { post ->
                    val postUserSnapshot = database.getReference("users")
                        .child(post.userId)
                        .get()
                        .await()
                    val postUser = postUserSnapshot.getValue(User::class.java)
                    post.copy(user = postUser ?: User())
                }

                _uiState.update { it.copy(posts = postsWithUserData, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun loadPostById(postId: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Get post directly by ID
                val postSnapshot = database.getReference("posts")
                    .child(postId)
                    .get()
                    .await()
                
                val post = postSnapshot.getValue(Post::class.java)
                    ?: throw Exception("Post not found")

                // Load user data for the post
                val postUserSnapshot = database.getReference("users")
                    .child(post.userId)
                    .get()
                    .await()
                val postUser = postUserSnapshot.getValue(User::class.java)
                val postWithUserData = post.copy(user = postUser ?: User())

                // Update state with current post
                _uiState.update { currentState ->
                    currentState.copy(
                        currentPost = postWithUserData,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("PostsSectionViewModel", "Error loading post: ${e.message}")
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                val postRef = database.getReference("posts").child(post.postId)
                val isLiked = post.likes.containsKey(currentUser.uid)
                val updatedLikes = post.likes.toMutableMap().apply {
                    if (isLiked) remove(currentUser.uid) else put(currentUser.uid, true)
                }
                val newLikesCount = if (isLiked) post.likesCount - 1 else post.likesCount + 1
                
                // Update Firebase
                postRef.child("likes").setValue(updatedLikes).await()
                postRef.child("likesCount").setValue(newLikesCount).await()
                
                // Update UI state immediately
                _uiState.update { currentState ->
                    val updatedPosts = currentState.posts.map { currentPost ->
                        if (currentPost.postId == post.postId) {
                            currentPost.copy(
                                likes = updatedLikes,
                                likesCount = newLikesCount
                            )
                        } else {
                            currentPost
                        }
                    }
                    currentState.copy(posts = updatedPosts)
                }
            } catch (e: Exception) {
                Log.e("PostsSectionViewModel", "Error toggling like: ${e.message}")
            }
        }
    }
} 