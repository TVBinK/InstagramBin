package com.baothanhbin.instagrambin.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.baothanhbin.instagrambin.service.DataClearService
import android.app.Application

class AuthViewModel(application: Application) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val dataClearService = DataClearService(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Kiểm tra trạng thái đăng nhập khi khởi tạo
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    _authState.value = AuthState.Loading
                    // Lấy thông tin người dùng từ Realtime Database
                    val snapshot = database.child("users").child(currentUser.uid).get().await()
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        // Nếu không tìm thấy thông tin người dùng trong database, vẫn giữ trạng thái đăng nhập
                        _authState.value = AuthState.Success(User(
                            uid = currentUser.uid,
                            email = currentUser.email ?: "",
                            username = currentUser.displayName ?: "",
                            fullName = currentUser.displayName ?: ""
                        ))
                    }
                } catch (e: Exception) {
                    // Nếu có lỗi khi lấy thông tin, vẫn giữ trạng thái đăng nhập
                    _authState.value = AuthState.Success(User(
                        uid = currentUser.uid,
                        email = currentUser.email ?: "",
                        username = currentUser.displayName ?: "",
                        fullName = currentUser.displayName ?: ""
                    ))
                }
            }
        } else {
            _authState.value = AuthState.Initial
        }
    }

    fun signUp(email: String, password: String, username: String, fullName: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                result.user?.let { firebaseUser ->
                    val user = User(
                        uid = firebaseUser.uid,
                        email = email,
                        username = username,
                        fullName = fullName
                    )
                    // Save user data to Realtime Database
                    database.child("users").child(firebaseUser.uid).setValue(user).await()
                    _authState.value = AuthState.Success(user)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                
                // Clear cache trước khi đăng nhập để đảm bảo data mới
                dataClearService.forceRefreshAllData()
                
                val result = auth.signInWithEmailAndPassword(email, password).await()
                result.user?.let { firebaseUser ->
                    // Get user data from Realtime Database
                    val snapshot = database.child("users").child(firebaseUser.uid).get().await()
                    val user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        _authState.value = AuthState.Success(user)
                    } else {
                        // Nếu không tìm thấy thông tin người dùng trong database, vẫn giữ trạng thái đăng nhập
                        _authState.value = AuthState.Success(User(
                            uid = firebaseUser.uid,
                            email = firebaseUser.email ?: "",
                            username = firebaseUser.displayName ?: "",
                            fullName = firebaseUser.displayName ?: ""
                        ))
                    }

                    // LẤY VÀ LƯU FCM TOKEN NGAY SAU KHI ĐĂNG NHẬP
                    FirebaseMessaging.getInstance().token
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val token = task.result
                                FirebaseDatabase.getInstance().getReference("users")
                                    .child(firebaseUser.uid)
                                    .child("fcmToken")
                                    .setValue(token)
                            }
                        }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                currentUser?.let { user ->
                    // Remove FCM token
                    database.child("users").child(user.uid).child("fcmToken").removeValue()
                    
                    // Clear all cache and data
                    dataClearService.clearAllData()
                }
                
                // Sign out from Firebase Auth
                auth.signOut()
                _authState.value = AuthState.Initial
                
            } catch (e: Exception) {
                // Even if clearing fails, still sign out
                auth.signOut()
                _authState.value = AuthState.Initial
            }
        }
    }

    // Thêm hàm để refresh trạng thái đăng nhập
    fun refreshAuthState() {
        checkAuthState()
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    data class Success(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
} 