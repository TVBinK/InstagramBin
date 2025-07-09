// ViewModel quản lý logic và trạng thái UI cho màn hình chat
// Sử dụng cho việc gửi, nhận, phân trang, lắng nghe realtime tin nhắn giữa hai user

package com.baothanhbin.instagrambin.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baothanhbin.instagrambin.model.Message
import com.baothanhbin.instagrambin.repository.MessageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Trạng thái UI cho màn hình chat
// Lưu danh sách tin nhắn, trạng thái loading, lỗi, phân trang...
data class MessageUiState(
    val messages: List<Message> = emptyList(), // Danh sách tin nhắn hiện tại
    val isLoading: Boolean = false,           // Đang tải tin nhắn lần đầu
    val isLoadingMore: Boolean = false,       // Đang tải thêm tin nhắn (phân trang)
    val error: String? = null,                // Thông báo lỗi nếu có
    val hasMoreMessages: Boolean = true,      // Còn tin nhắn cũ để tải tiếp không
    val lastMessageTimestamp: Long? = null    // Timestamp của tin nhắn cũ nhất đã tải
)

// ViewModel chính cho chat, quản lý mọi logic gửi/nhận tin nhắn
class MessageViewModel(application: Application) : AndroidViewModel(application) {
    private val messageRepository = MessageRepository(application) // Repository xử lý dữ liệu
    private val _uiState = MutableStateFlow(MessageUiState())     // State nội bộ
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow() // State public cho UI

    private var messagesListener: ValueEventListener? = null // Listener realtime Firebase
    private var currentChatId: String? = null                // Id user đang chat
    private var isInitialLoad = true                         // Đánh dấu lần load đầu

    companion object {
        private const val MAX_MESSAGES_IN_MEMORY = 200 // Giới hạn số tin nhắn giữ trong bộ nhớ
    }

    // Hàm tải tin nhắn lần đầu khi mở chat với userId
    fun loadMessages(userId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Xóa listener cũ nếu có
                removeMessagesListener()

                // Preload thông tin user để tối ưu
                messageRepository.preloadUserInfo(userId)

                // Tải các tin nhắn mới nhất (có phân trang)
                val initialMessages = messageRepository.getMessages(userId, MessageRepository.MESSAGES_PER_PAGE)
                android.util.Log.d("MessageViewModel", "Initial load: loaded ${initialMessages.size} messages for user $userId")

                val lastTimestamp = if (initialMessages.isNotEmpty()) {
                    initialMessages.first().timestamp
                } else null

                _uiState.value = _uiState.value.copy(
                    messages = initialMessages,
                    isLoading = false,
                    hasMoreMessages = initialMessages.size >= MessageRepository.MESSAGES_PER_PAGE,
                    lastMessageTimestamp = lastTimestamp
                )

                // Đăng ký listener realtime để lắng nghe tin nhắn mới
                setupRealtimeListener(userId)

                // Đánh dấu các tin nhắn là đã đọc
                messageRepository.markMessagesAsRead(userId)

                isInitialLoad = false

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load messages: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // Hàm tải thêm tin nhắn cũ (phân trang)
    fun loadMoreMessages() {
        val currentState = _uiState.value
        if (currentState.isLoadingMore || !currentState.hasMoreMessages || currentState.lastMessageTimestamp == null) {
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = currentState.copy(isLoadingMore = true)

                // Lấy id user đang chat
                val chatUserId = currentChatId ?: return@launch

                val moreMessages = messageRepository.getMessages(
                    chatUserId,
                    MessageRepository.MESSAGES_PER_PAGE,
                    currentState.lastMessageTimestamp
                )

                if (moreMessages.isNotEmpty()) {
                    // Lọc trùng id tin nhắn
                    val existingMessageIds = currentState.messages.map { it.messageId }.toSet()
                    val uniqueMoreMessages = moreMessages.filter { message ->
                        !existingMessageIds.contains(message.messageId)
                    }

                    if (uniqueMoreMessages.isNotEmpty()) {
                        var allMessages = uniqueMoreMessages + currentState.messages
                        // Nếu quá nhiều tin nhắn thì chỉ giữ lại mới nhất
                        if (allMessages.size > MAX_MESSAGES_IN_MEMORY) {
                            allMessages = allMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
                            android.util.Log.d("MessageViewModel", "Trimmed pagination messages to $MAX_MESSAGES_IN_MEMORY for performance")
                        }
                        val newLastTimestamp = uniqueMoreMessages.minOf { it.timestamp }
                        _uiState.value = currentState.copy(
                            messages = allMessages,
                            isLoadingMore = false,
                            hasMoreMessages = moreMessages.size >= MessageRepository.MESSAGES_PER_PAGE,
                            lastMessageTimestamp = newLastTimestamp
                        )
                    } else {
                        _uiState.value = currentState.copy(
                            isLoadingMore = false,
                            hasMoreMessages = false
                        )
                    }
                } else {
                    _uiState.value = currentState.copy(
                        isLoadingMore = false,
                        hasMoreMessages = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = "Failed to load more messages: ${e.message}",
                    isLoadingMore = false
                )
            }
        }
    }

    // Đăng ký listener realtime để lắng nghe tin nhắn mới từ Firebase
    private fun setupRealtimeListener(userId: String) {
        currentChatId = userId
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val chatRef = FirebaseDatabase.getInstance().getReference("chats")
            .child(currentUser.uid)
            .child(userId)
        messagesListener = chatRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isInitialLoad) return // Bỏ qua lần load đầu
                viewModelScope.launch {
                    try {
                        val currentState = _uiState.value
                        val latestTimestamp = currentState.lastMessageTimestamp ?: 0L
                        // Chỉ lấy tin nhắn mới hơn timestamp cuối cùng
                        val newMessages = messageRepository.getLatestMessages(userId, latestTimestamp)
                        if (newMessages.isNotEmpty()) {
                            android.util.Log.d("MessageViewModel", "Realtime: received ${newMessages.size} new messages")
                            // Lọc trùng id tin nhắn
                            val existingMessageIds = currentState.messages.map { it.messageId }.toSet()
                            val uniqueNewMessages = newMessages.filter { newMessage ->
                                !existingMessageIds.contains(newMessage.messageId)
                            }
                            android.util.Log.d("MessageViewModel", "Realtime: ${uniqueNewMessages.size} unique messages after filtering duplicates")
                            if (uniqueNewMessages.isNotEmpty()) {
                                var updatedMessages = currentState.messages + uniqueNewMessages
                                // Nếu quá nhiều tin nhắn thì chỉ giữ lại mới nhất
                                if (updatedMessages.size > MAX_MESSAGES_IN_MEMORY) {
                                    updatedMessages = updatedMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
                                    android.util.Log.d("MessageViewModel", "Trimmed messages to $MAX_MESSAGES_IN_MEMORY for performance")
                                }
                                val newLastTimestamp = uniqueNewMessages.maxOf { it.timestamp }
                                android.util.Log.d("MessageViewModel", "Realtime: updating UI with ${updatedMessages.size} total messages")
                                _uiState.value = currentState.copy(
                                    messages = updatedMessages,
                                    lastMessageTimestamp = newLastTimestamp
                                )
                                // Đánh dấu các tin nhắn mới là đã đọc
                                messageRepository.markMessagesAsRead(userId)
                            }
                        } else {
                            android.util.Log.d("MessageViewModel", "Realtime: no new messages received")
                        }
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update messages: ${e.message}"
                        )
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to listen for messages: ${error.message}"
                )
            }
        })
    }

    // Gửi tin nhắn text
    fun sendMessage(receiverId: String, content: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                messageRepository.sendMessage(receiverId, content)
                // UI sẽ tự động cập nhật nhờ listener realtime
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send message: ${e.message}"
                )
            }
        }
    }

    // Gửi tin nhắn dạng ảnh
    fun sendImage(receiverId: String, imageUri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                messageRepository.sendImage(receiverId, imageUri)
                // UI sẽ tự động cập nhật nhờ listener realtime
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to send image: ${e.message}"
                )
            }
        }
    }

    // Xóa listener realtime khi không cần thiết
    private fun removeMessagesListener() {
        currentChatId?.let { chatId ->
            val currentUser = FirebaseAuth.getInstance().currentUser ?: return
            val chatRef = FirebaseDatabase.getInstance().getReference("chats")
                .child(currentUser.uid)
                .child(chatId)
            messagesListener?.let { listener ->
                chatRef.removeEventListener(listener)
                messagesListener = null
            }
        }
    }

    // Xóa trạng thái lỗi
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // Hàm loại bỏ tin nhắn trùng id trong danh sách
    private fun removeDuplicateMessages(messages: List<Message>): List<Message> {
        return messages.distinctBy { it.messageId }
    }

    // Hàm dọn dẹp tin nhắn trùng lặp trong state hiện tại
    fun cleanupDuplicates() {
        val currentState = _uiState.value
        val cleanedMessages = removeDuplicateMessages(currentState.messages)
        if (cleanedMessages.size != currentState.messages.size) {
            android.util.Log.d("MessageViewModel", "Cleaned up ${currentState.messages.size - cleanedMessages.size} duplicate messages")
            _uiState.value = currentState.copy(messages = cleanedMessages)
        }
    }

    // Làm mới toàn bộ tin nhắn (reload lại từ đầu)
    fun refreshMessages() {
        currentChatId?.let { userId ->
            android.util.Log.d("MessageViewModel", "Refreshing messages for user: $userId")
            // Reset lại state trước khi load lại
            _uiState.value = MessageUiState()
            isInitialLoad = true
            // Xóa listener cũ để tránh xung đột
            removeMessagesListener()
            // Xóa cache repository để đảm bảo dữ liệu mới
            messageRepository.clearUserCache()
            loadMessages(userId)
        }
    }

    // Hàm dọn dẹp trùng lặp và cắt bớt tin nhắn nếu quá nhiều (có thể gọi từ UI)
    fun forceCleanup() {
        val currentState = _uiState.value
        var cleanedMessages = removeDuplicateMessages(currentState.messages)
        // Nếu quá nhiều tin nhắn thì chỉ giữ lại mới nhất
        if (cleanedMessages.size > MAX_MESSAGES_IN_MEMORY) {
            cleanedMessages = cleanedMessages.takeLast(MAX_MESSAGES_IN_MEMORY)
        }
        if (cleanedMessages.size != currentState.messages.size) {
            android.util.Log.d("MessageViewModel", "Force cleanup: ${currentState.messages.size} -> ${cleanedMessages.size} messages")
            _uiState.value = currentState.copy(messages = cleanedMessages)
        }
    }

    // Hàm dọn dẹp khi ViewModel bị hủy (thoát màn chat)
    override fun onCleared() {
        super.onCleared()
        removeMessagesListener()
        messageRepository.clearUserCache()
    }
}
// Kết thúc file, mọi logic chat đều được quản lý tập trung tại đây