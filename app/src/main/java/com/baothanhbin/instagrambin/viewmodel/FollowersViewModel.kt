package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FollowersState(
    val followers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FollowersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FollowersState())
    val uiState: StateFlow<FollowersState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val userRef = database.getReference("users").child(userId)
            userRef.child("followers").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followerIds = snapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Boolean>>() {})?.keys?.toList() ?: emptyList()
                    
                    if (followerIds.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            followers = emptyList(),
                            isLoading = false
                        )
                        return
                    }

                    val followers = mutableListOf<User>()
                    var loadedCount = 0

                    followerIds.forEach { followerId ->
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(followerId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    userSnapshot.getValue(User::class.java)?.let { user ->
                                        followers.add(user)
                                    }
                                    loadedCount++
                                    
                                    if (loadedCount == followerIds.size) {
                                        _uiState.value = _uiState.value.copy(
                                            followers = followers,
                                            isLoading = false
                                        )
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    loadedCount++
                                    if (loadedCount == followerIds.size) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            error = error.message
                                        )
                                    }
                                }
                            })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            })
        }
    }

    private suspend fun setValue(reference: com.google.firebase.database.DatabaseReference, value: Any?) {
        return suspendCancellableCoroutine { continuation ->
            reference.setValue(value)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    private suspend fun removeValue(reference: com.google.firebase.database.DatabaseReference) {
        return suspendCancellableCoroutine { continuation ->
            reference.removeValue()
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    fun followUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Add to current user's following
                setValue(
                    database.getReference("users")
                        .child(currentUser.uid)
                        .child("following")
                        .child(userId),
                    true
                )
                
                // Add to target user's followers
                setValue(
                    database.getReference("users")
                        .child(userId)
                        .child("followers")
                        .child(currentUser.uid),
                    true
                )
                
                // Reload followers to update UI
                loadFollowers(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser ?: return@launch
                
                // Remove from current user's following
                removeValue(
                    database.getReference("users")
                        .child(currentUser.uid)
                        .child("following")
                        .child(userId)
                )
                
                // Remove from target user's followers
                removeValue(
                    database.getReference("users")
                        .child(userId)
                        .child("followers")
                        .child(currentUser.uid)
                )
                
                // Reload followers to update UI
                loadFollowers(userId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message)
            }
        }
    }
} 