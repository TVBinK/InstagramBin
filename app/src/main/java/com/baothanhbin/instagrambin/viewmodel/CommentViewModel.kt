package com.baothanhbin.instagrambin.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Comment
import com.baothanhbin.instagrambin.model.User
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
    val comments: List<Comment> = emptyList()
)

class CommentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    private val database = FirebaseDatabase.getInstance()
    private val commentsRef = database.getReference("posts")

    fun loadComments(postId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                commentsRef.child(postId).child("comments")
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val comments = mutableListOf<Comment>()
                            for (commentSnapshot in snapshot.children) {
                                try {
                                    val comment = commentSnapshot.getValue(Comment::class.java)
                                    if (comment != null) {
                                        // Load user data cho mỗi comment
                                        database.getReference("users")
                                            .child(comment.userId)
                                            .get()
                                            .addOnSuccessListener { userSnapshot ->
                                                val user = userSnapshot.getValue(User::class.java)
                                                if (user != null) {
                                                    val commentWithUser = comment.copy(user = user)
                                                    comments.add(commentWithUser)
                                                    _uiState.value = _uiState.value.copy(
                                                        comments = comments.sortedByDescending { it.timestamp }
                                                    )
                                                }
                                            }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CommentViewModel", "Error parsing comment: ${e.message}", e)
                                }
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

                // Tạo comment object
                val comment = Comment(
                    postId = postId,
                    userId = currentUser.uid,
                    content = content,
                    timestamp = System.currentTimeMillis()
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
                commentRef.setValue(comment)
                    .addOnSuccessListener {
                        // Update comment count
                        commentsRef.child(postId).child("commentsCount")
                            .get()
                            .addOnSuccessListener { snapshot ->
                                val currentCount = snapshot.getValue(Int::class.java) ?: 0
                                commentsRef.child(postId).child("commentsCount")
                                    .setValue(currentCount + 1)
                                    .addOnSuccessListener {
                                        _uiState.value = _uiState.value.copy(isLoading = false)
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
} 