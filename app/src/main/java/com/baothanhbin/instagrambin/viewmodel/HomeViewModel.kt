package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.repository.PostRepository
import com.baothanhbin.instagrambin.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

data class HomeUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val friends: List<User> = emptyList(),
    val currentUser: User? = null
)

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val postRepository = PostRepository()
    private val userRepository = UserRepository()

    private var isLoaded = false

    init {
        // Do not auto-load here, use loadDataIfNeeded from HomeScreen
    }

    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffInSeconds = (currentTime - timestamp) / 1000

        return when {
            diffInSeconds < 60 -> "Vừa xong"
            diffInSeconds < 3600 -> "${diffInSeconds / 60} phút trước"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600} giờ trước"
            diffInSeconds < 2592000 -> "${diffInSeconds / 86400} ngày trước"
            diffInSeconds < 31536000 -> "${diffInSeconds / 2592000} tháng trước"
            else -> "${diffInSeconds / 31536000} năm trước"
        }
    }

    fun loadDataIfNeeded() {
        if (!isLoaded) {
            viewModelScope.launch {
                try {
                    _uiState.update { it.copy(isLoading = true) }
                    val currentUserId = auth.currentUser?.uid ?: return@launch
                    
                    // Load current user
                    val currentUser = userRepository.getUser(currentUserId)
                    
                    // Load posts
                    val posts = postRepository.getPosts()
                    
                    // Load friends
                    val friends = userRepository.getFriends(currentUserId)
                    
                    _uiState.update { 
                        it.copy(
                            posts = posts,
                            currentUser = currentUser,
                            friends = friends,
                            isLoading = false
                        )
                    }
                    isLoaded = true
                } catch (e: Exception) {
                    _uiState.update { 
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                val currentUserId = auth.currentUser?.uid ?: return@launch
                
                // Load posts
                val posts = postRepository.getPosts()
                
                _uiState.update { 
                    it.copy(
                        posts = posts
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e.message
                    )
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    val updatedPost = postRepository.toggleLike(post.postId, currentUserId)
                    _uiState.update { currentState ->
                        currentState.copy(
                            posts = currentState.posts.map { if (it.postId == post.postId) updatedPost else it }
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
} 