package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.repository.PostRepository
import com.baothanhbin.instagrambin.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.delay

// Lớp dữ liệu lưu trạng thái giao diện của màn hình chính
data class HomeUiState(
    val posts: List<Post> = emptyList(), // Danh sách bài đăng
    val isLoading: Boolean = true, // Trạng thái đang tải dữ liệu
    val error: String? = null, // Lỗi nếu có khi tải dữ liệu
    val friends: List<User> = emptyList(), // Danh sách bạn bè (người dùng đang theo dõi)
    val currentUser: User? = null // Thông tin người dùng hiện tại
)

// ViewModel quản lý dữ liệu và logic cho màn hình chính
class HomeViewModel(
    application: Application // Context ứng dụng
) : AndroidViewModel(application) {
    // StateFlow để lưu và cập nhật trạng thái giao diện
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // StateFlow để theo dõi trạng thái làm mới dữ liệu
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // Instance của FirebaseAuth để quản lý xác thực người dùng
    private val auth = FirebaseAuth.getInstance()
    // Repository để lấy dữ liệu bài đăng
    private val postRepository = PostRepository()
    // Repository để lấy dữ liệu người dùng
    private val userRepository = UserRepository()

    // Biến kiểm tra xem dữ liệu đã được tải lần đầu chưa
    private var isLoaded = false
    // Bộ nhớ đệm cho thông tin người dùng hiện tại
    private var currentUserCache: User? = null

    // Hàm chuyển đổi timestamp thành chuỗi thời gian tương đối (VD: "Vừa xong", "1 giờ trước")
    private fun getTimeAgo(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffInSeconds = (currentTime - timestamp) / 1000

        return when {
            diffInSeconds < 60 -> "Vừa xong"
            diffInSeconds < 3600 -> "${diffInSeconds / 60} phút trước"
            diffInSeconds < 86400 -> "${diffInSeconds / 3600} giờ trước"
            diffInSeconds < 2592000 -> "${diffInSeconds / 86400} ngày trước"
            diffInSeconds < 31536000 -> "${diffInSeconds / 2592000} tháng trước"
            else -> "${diffInSeconds / 31536000} năm trước"
        }
    }

    // Hàm tải dữ liệu lần đầu nếu chưa tải
    fun loadDataIfNeeded() {
        if (!isLoaded) { // Kiểm tra xem dữ liệu đã được tải chưa
            viewModelScope.launch { // Chạy bất đồng bộ trong viewModelScope
                try {
                    _uiState.update { it.copy(isLoading = true) } // Cập nhật trạng thái đang tải
                    val currentUserId = auth.currentUser?.uid ?: return@launch // Lấy ID người dùng hiện tại

                    // Tải thông tin người dùng nếu chưa có trong bộ đệm
                    if (currentUserCache == null) {
                        currentUserCache = userRepository.getUser(currentUserId)
                    }

                    // Tải danh sách bài đăng từ người dùng hiện tại và người đang theo dõi
                    val posts = postRepository.getPosts()

                    // Tải danh sách bạn bè (người đang theo dõi)
                    val friends = userRepository.getFriends(currentUserId)

                    // Cập nhật trạng thái giao diện với dữ liệu đã tải
                    _uiState.update {
                        it.copy(
                            posts = posts,
                            currentUser = currentUserCache,
                            friends = friends,
                            isLoading = false
                        )
                    }
                    isLoaded = true // Đánh dấu dữ liệu đã được tải
                } catch (e: Exception) {
                    // Xử lý lỗi khi tải dữ liệu
                    _uiState.update {
                        it.copy(
                            error = e.message,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    // Hàm làm mới danh sách bài đăng
    fun refreshPosts() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true // Cập nhật trạng thái đang làm mới
                val currentUserId = auth.currentUser?.uid ?: return@launch // Lấy ID người dùng

                // Tải lại danh sách bài đăng
                val posts = postRepository.getPosts()

                // Cập nhật trạng thái giao diện với danh sách bài đăng mới
                _uiState.update {
                    it.copy(
                        posts = posts
                    )
                }
            } catch (e: Exception) {
                // Xử lý lỗi khi làm mới
                _uiState.update {
                    it.copy(
                        error = e.message
                    )
                }
            } finally {
                _isRefreshing.value = false // Kết thúc trạng thái làm mới
            }
        }
    }

    // Hàm xử lý hành động thích/không thích bài đăng
    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId != null) {
                    // Gọi repository để cập nhật trạng thái thích
                    val updatedPost = postRepository.toggleLike(post.postId, currentUserId)
                    // Cập nhật danh sách bài đăng với bài đăng đã được cập nhật
                    _uiState.update { currentState ->
                        currentState.copy(
                            posts = currentState.posts.map { if (it.postId == post.postId) updatedPost else it }
                        )
                    }
                }
            } catch (e: Exception) {
                // Xử lý lỗi (hiện chưa hiển thị lỗi ra giao diện)
            }
        }
    }

    // Hàm clear cache và reset state
    fun clearCache() {
        isLoaded = false
        currentUserCache = null
        _uiState.value = HomeUiState()
        _isRefreshing.value = false
    }

    // Hàm force refresh data
    fun forceRefresh() {
        clearCache()
        loadDataIfNeeded()
    }
}