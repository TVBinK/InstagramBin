package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.GenericTypeIndicator

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val followers: List<User> = emptyList(),
    val following: List<User> = emptyList()
)

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var userListener: ValueEventListener? = null

    init {
        listenUserProfile()
    }

    private fun listenUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    _uiState.value = _uiState.value.copy(user = user, isLoading = false, error = null)
                    // Load followers and following when user data changes
                    loadFollowersAndFollowing(currentUser.uid)
                }
                override fun onCancelled(error: DatabaseError) {
                    _uiState.value = _uiState.value.copy(error = "Không thể tải thông tin: ${error.message}", isLoading = false)
                }
            }
            database.child("users").child(currentUser.uid)
                .addValueEventListener(userListener!!)
            _uiState.value = _uiState.value.copy(isLoading = true)
        } else {
            _uiState.value = _uiState.value.copy(error = "Không tìm thấy người dùng", isLoading = false)
        }
    }

    private fun loadFollowersAndFollowing(userId: String) {
        viewModelScope.launch {
            try {
                // Load followers
                val followersSnapshot = database.child("users").child(userId).child("followers").get().await()
                val followersMap = followersSnapshot.getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {}) ?: mapOf()
                
                val followers = mutableListOf<User>()
                followersMap.keys.forEach { followerId ->
                    val followerSnapshot = database.child("users").child(followerId).get().await()
                    followerSnapshot.getValue(User::class.java)?.let { followers.add(it) }
                }

                // Load following
                val followingSnapshot = database.child("users").child(userId).child("following").get().await()
                val followingMap = followingSnapshot.getValue(object : GenericTypeIndicator<Map<String, Boolean>>() {}) ?: mapOf()
                
                val following = mutableListOf<User>()
                followingMap.keys.forEach { followingId ->
                    val followingUserSnapshot = database.child("users").child(followingId).get().await()
                    followingUserSnapshot.getValue(User::class.java)?.let { following.add(it) }
                }

                _uiState.value = _uiState.value.copy(
                    followers = followers,
                    following = following
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Không thể tải danh sách theo dõi: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        val currentUser = auth.currentUser
        if (currentUser != null && userListener != null) {
            database.child("users").child(currentUser.uid).removeEventListener(userListener!!)
        }
    }

    fun refreshProfile() {
        // No-op: Realtime listener will auto-update
    }
} 