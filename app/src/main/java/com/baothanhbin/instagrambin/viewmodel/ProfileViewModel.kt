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

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
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