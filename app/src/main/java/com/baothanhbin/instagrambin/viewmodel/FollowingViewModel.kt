package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FollowingState(
    val following: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FollowingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FollowingState())
    val uiState: StateFlow<FollowingState> = _uiState.asStateFlow()

    fun loadFollowing(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
            userRef.child("following").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val followingIds = snapshot.getValue(object : com.google.firebase.database.GenericTypeIndicator<Map<String, Boolean>>() {})?.keys?.toList() ?: emptyList()
                    
                    if (followingIds.isEmpty()) {
                        _uiState.value = _uiState.value.copy(
                            following = emptyList(),
                            isLoading = false
                        )
                        return
                    }

                    val following = mutableListOf<User>()
                    var loadedCount = 0

                    followingIds.forEach { followingId ->
                        FirebaseDatabase.getInstance().getReference("users")
                            .child(followingId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(userSnapshot: DataSnapshot) {
                                    userSnapshot.getValue(User::class.java)?.let { user ->
                                        following.add(user)
                                    }
                                    loadedCount++
                                    
                                    if (loadedCount == followingIds.size) {
                                        _uiState.value = _uiState.value.copy(
                                            following = following,
                                            isLoading = false
                                        )
                                    }
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    loadedCount++
                                    if (loadedCount == followingIds.size) {
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
} 