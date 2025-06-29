package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Comment
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.service.NotificationService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// UI State cho comment
data class CommentUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val comments: List<Comment> = emptyList(),
    val replyingToCommentId: String? = null,
    val replyingToUsername: String? = null
)

class CommentViewModel(private val application: Application) : ViewModel() {
    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance()
    private val commentsRef = database.getReference("posts")
    private val notificationService = NotificationService(application)

    fun loadComments(postId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                commentsRef.child(postId).child("comments")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val comments = mutableListOf<Comment>()
                            val userLoadTasks = mutableListOf<com.google.android.gms.tasks.Task<Comment>>()
                            val commentList = mutableListOf<Comment>()
                            for (commentSnapshot in snapshot.children) {
                                try {
                                    val comment = commentSnapshot.getValue(Comment::class.java)
                                    if (comment != null) {
                                        // Load user data cho mỗi comment (dùng task để chờ tất cả user load xong)
                                        val userTask = database.getReference("users")
                                            .child(comment.userId)
                                            .get()
                                            .continueWith { userSnapshotTask ->
                                                val user = userSnapshotTask.result?.getValue(User::class.java)
                                                comment.copy(user = user ?: User())
                                            }
                                        userLoadTasks.add(userTask)
                                    }
                                } catch (e: Exception) {
                                    Log.e("CommentViewModel", "Error parsing comment: ", e)
                                }
                            }
                            // Khi tất cả user load xong, cập nhật UI state 1 lần duy nhất
                            com.google.android.gms.tasks.Tasks.whenAllSuccess<Comment>(userLoadTasks)
                                .addOnSuccessListener { loadedComments ->
                                    _uiState.value = _uiState.value.copy(
                                        comments = (loadedComments as List<Comment>).sortedByDescending { it.timestamp }
                                    )
                                }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Failed to load comments: ${error.message}"
                            )
                        }
                    })
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load comments: ${e.message}"
                )
            }
        }
    }

    fun addComment(postId: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                // Lấy thông tin bài viết để gửi notification
                val postSnapshot = database.getReference("posts").child(postId).get().await()
                val post = postSnapshot.getValue(Post::class.java)

                // Tạo comment object
                val comment = Comment(
                    postId = postId,
                    userId = currentUser.uid,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    replyToCommentId = _uiState.value.replyingToCommentId
                )

                // Lấy user data
                val userSnapshot = database.getReference("users")
                    .child(currentUser.uid)
                    .get()
                    .await()

                val user = userSnapshot.getValue(User::class.java)
                if (user == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User data not found"
                    )
                    return@launch
                }

                comment.user = user

                // Thêm comment vào database
                val commentRef = commentsRef.child(postId).child("comments").push()
                val commentId = commentRef.key ?: ""
                val commentWithId = comment.copy(commentId = commentId)
                commentRef.setValue(commentWithId)
                    .addOnSuccessListener {
                        // Update comment count
                        commentsRef.child(postId).child("commentsCount")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                                commentsRef.child(postId).child("commentsCount")
                                    .setValue(currentCount + 1)
                                    .addOnSuccessListener {
                                        // Gửi notification sau khi comment thành công
                                        post?.let { postData ->
                                            viewModelScope.launch {
                                                if (comment.replyToCommentId != null) {
                                                    // Nếu là reply, gửi notification cho chủ comment gốc
                                                    val originalCommentSnapshot = database.getReference("posts")
                                                        .child(postId)
                                                        .child("comments")
                                                        .child(comment.replyToCommentId)
                                                        .get()
                                                        .await()
                                                    val originalComment = originalCommentSnapshot.getValue(Comment::class.java)
                                                    originalComment?.let { original ->
                                                        notificationService.sendReplyNotification(original, commentWithId)
                                                    }
                                                } else {
                                                    // Nếu là comment mới, gửi notification cho chủ bài viết
                                                    notificationService.sendCommentNotification(postData, commentWithId)
                                                }
                                            }
                                        }
                                        
                                        _uiState.value = _uiState.value.copy(isLoading = false)
                                        clearReply()
                                    }
                            }
                    }
                    .addOnFailureListener { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to add comment: ${e.message}"
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to add comment: ${e.message}"
                )
            }
        }
    }

    fun setReplyTo(commentId: String, username: String) {
        _uiState.value = _uiState.value.copy(replyingToCommentId = commentId, replyingToUsername = username)
    }

    fun clearReply() {
        _uiState.value = _uiState.value.copy(replyingToCommentId = null, replyingToUsername = null)
    }
} 