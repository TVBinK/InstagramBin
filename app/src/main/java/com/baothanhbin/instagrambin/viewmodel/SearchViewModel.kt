package com.baothanhbin.instagrambin.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.UserProfile
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SearchViewModel : ViewModel() {
    private val _searchResults = MutableStateFlow<List<UserProfile>>(emptyList())
    val searchResults: StateFlow<List<UserProfile>> = _searchResults

    fun searchUserByUsername(username: String) {
        viewModelScope.launch {
            val database = FirebaseDatabase.getInstance().reference
            val snapshot = database.child("users")
                .orderByChild("username")
                .startAt(username)
                .endAt(username + "\uf8ff")
                .get()
                .await()
            val users = snapshot.children.mapNotNull { it.getValue(UserProfile::class.java) }
            val filtered = users.filter { it.username.contains(username, ignoreCase = true) }
            _searchResults.value = filtered
        }
    }
} 