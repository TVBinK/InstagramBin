package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.UserProfile
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SearchUiState(
    val searchResults: List<UserProfile> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SearchViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState

    fun searchUserByUsername(username: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val database = FirebaseDatabase.getInstance().reference
                val snapshot = database.child("users")
                    .orderByChild("username")
                    .startAt(username)
                    .endAt(username + "\uf8ff")
                    .get()
                    .await()
                val users = snapshot.children.mapNotNull { it.getValue(UserProfile::class.java) }
                val filtered = users.filter { 
                    it.username.contains(username, ignoreCase = true) &&
                    it.uid != FirebaseAuth.getInstance().currentUser?.uid
                }
                _uiState.value = _uiState.value.copy(
                    searchResults = filtered,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }

    fun loadAllUsers() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val database = FirebaseDatabase.getInstance().reference
                val snapshot = database.child("users").get().await()
                val users = snapshot.children.mapNotNull { it.getValue(UserProfile::class.java) }
                val filtered = users.filter { 
                    it.uid != FirebaseAuth.getInstance().currentUser?.uid
                }
                _uiState.value = _uiState.value.copy(
                    searchResults = filtered,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
} 