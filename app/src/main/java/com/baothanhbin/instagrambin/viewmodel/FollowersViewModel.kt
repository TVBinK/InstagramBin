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

data class FollowersState(
    val followers: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class FollowersViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FollowersState())
    val uiState: StateFlow<FollowersState> = _uiState.asStateFlow()

    fun loadFollowers(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
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
} 