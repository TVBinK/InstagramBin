package com.baothanhbin.instagrambin.viewmodel

import android.net.Uri
import android.util.Log
import android.app.Application
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase

// UI State data class
data class EditProfileUiState(
    val avatarRes: Int = android.R.drawable.sym_def_app_icon,
    val avatarUri: Uri? = null,
    val name: String = "Jacob West",
    val username: String = "jacob_w",
    val website: String = "",
    val bio: String = "Digital goodies designer @pixsellz\nEverything is designed.",
    val email: String = "jacob.west@gmail.com",
    val phone: String = "+1 202 555 0147",
    val gender: String = "Male",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEmailVerificationDialog: Boolean = false,
    val newEmail: String = "",
    val shouldCloseScreen: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val password: String = "",
    val verificationSent: Boolean = false,
    val showVerificationButton: Boolean = false,
    val isEmailVerified: Boolean = false
)

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val context = getApplication<Application>().applicationContext

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val snapshot = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(currentUser.uid)
                        .get().await()
                    val user = snapshot.getValue(com.baothanhbin.instagrambin.model.User::class.java)
                    user?.let {
                        _uiState.update { state ->
                            state.copy(
                                name = it.fullName,
                                username = it.username,
                                website = "", // Nếu có trường này trong model thì lấy luôn
                                bio = it.bio,
                                email = it.email,
                                phone = "", // Nếu có trường này trong model thì lấy luôn
                                gender = it.gender,
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun onAvatarSelected(uri: Uri) {
        _uiState.update { it.copy(avatarUri = uri) }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun onUsernameChange(username: String) {
        _uiState.update { it.copy(username = username) }
    }

    fun onWebsiteChange(website: String) {
        _uiState.update { it.copy(website = website) }
    }

    fun onBioChange(bio: String) {
        _uiState.update { it.copy(bio = bio) }
    }

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone) }
    }

    fun onGenderChange(gender: String) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun onChangeAvatar() {
        // TODO: Xử lý đổi ảnh đại diện ở đây
    }

    fun showEmailVerificationDialog() {
        _uiState.update { it.copy(showEmailVerificationDialog = true, showPasswordDialog = true) }
    }

    fun hideEmailVerificationDialog() {
        _uiState.update { it.copy(
            showEmailVerificationDialog = false,
            showPasswordDialog = false,
            password = "",
            error = null,
            verificationSent = false,
            showVerificationButton = false,
            isEmailVerified = false
        ) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun verifyPassword() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val credential = EmailAuthProvider
                    .getCredential(currentUser.email!!, _uiState.value.password)

                currentUser.reauthenticate(credential).await()

                // Send email verification
                currentUser.updateEmail(_uiState.value.email).await()
                currentUser.sendEmailVerification().await()

                _uiState.update {
                    it.copy(
                        showPasswordDialog = false,
                        verificationSent = true,
                        showVerificationButton = true,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                currentUser.reload().await()

                if (currentUser.isEmailVerified) {
                    _uiState.update {
                        it.copy(
                            showEmailVerificationDialog = false,
                            verificationSent = false,
                            showVerificationButton = false,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            error = "Email chưa được xác minh. Vui lòng kiểm tra hộp thư của bạn.",
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    suspend fun updateEmail() {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e("EditProfile", "Current user is null")
                throw Exception("Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
            }

            // Check if current email is verified
            if (!currentUser.isEmailVerified) {
                currentUser.sendEmailVerification().await()
                throw Exception("Vui lòng xác minh email hiện tại trước khi đổi email mới. Email xác minh đã được gửi đến ${currentUser.email}")
            }

            // Re-authenticate
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, _uiState.value.password)
            currentUser.reauthenticate(credential).await()

            // Update email
            currentUser.updateEmail(_uiState.value.newEmail).await()
            currentUser.sendEmailVerification().await()

            _uiState.update { it.copy(
                email = _uiState.value.newEmail,
                showEmailVerificationDialog = false,
                shouldCloseScreen = true,
                password = "",
                verificationSent = false
            ) }
            Toast.makeText(context, "Email đã được cập nhật thành công. Vui lòng xác minh email mới.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("EditProfile", "Error updating email: ${e.message}", e)
            _uiState.update { it.copy(
                error = "Không thể cập nhật email: ${e.message}",
                password = ""
            ) }
            Toast.makeText(context, "Lỗi cập nhật email: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val currentUser = auth.currentUser ?: throw Exception("User not logged in")
                val user = com.baothanhbin.instagrambin.model.User(
                    uid = currentUser.uid,
                    email = _uiState.value.email,
                    username = _uiState.value.username,
                    fullName = _uiState.value.name,
                    profileImageUrl = "", // Nếu có trường này thì lấy từ state
                    bio = _uiState.value.bio,
                    followers = 0, // Hoặc lấy từ state nếu muốn giữ nguyên
                    following = 0, // Hoặc lấy từ state nếu muốn giữ nguyên
                    gender = _uiState.value.gender
                )
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.uid)
                    .setValue(user).await()
                _uiState.update { it.copy(isLoading = false, shouldCloseScreen = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
} 