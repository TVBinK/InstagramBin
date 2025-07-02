# Hướng dẫn tối ưu hóa hiệu suất Firebase Database

## Tổng quan
Tài liệu này mô tả các tối ưu hóa đã được thực hiện để cải thiện tốc độ load tin nhắn từ Firebase Database.

## Các tối ưu hóa chính

### 1. Pagination (Phân trang)
- **Mục đích**: Chỉ load một số lượng tin nhắn nhất định thay vì tất cả
- **Triển khai**: 
  - `MESSAGES_PER_PAGE = 20` tin nhắn mỗi lần load
  - Sử dụng `limitToLast()` và `endBefore()` trong Firebase queries
  - Lazy loading khi scroll lên đầu danh sách

### 2. Caching (Bộ nhớ đệm)
- **Mục đích**: Giảm số lượng network requests
- **Triển khai**:
  - `FirebaseOptimizationService` với cache TTL 5 phút
  - Cache thông tin user và tin nhắn
  - Tự động cleanup cache khi quá lớn

### 3. Batch Loading
- **Mục đích**: Load nhiều user cùng lúc thay vì từng user một
- **Triển khai**:
  - `getUsers()` method trong `FirebaseOptimizationService`
  - Parallel loading với coroutines

### 4. Optimized Queries
- **Mục đích**: Tối ưu hóa Firebase queries
- **Triển khai**:
  - Sử dụng `orderByChild()` và `limitToLast()`
  - Indexes trên `timestamp`, `senderId`, `receiverId`
  - Query chỉ những tin nhắn cần thiết

### 5. Real-time Updates Optimization
- **Mục đích**: Chỉ load tin nhắn mới thay vì toàn bộ
- **Triển khai**:
  - `getLatestMessages()` với `startAfter()`
  - Real-time listener chỉ cho tin nhắn mới

## Cấu trúc file đã được tối ưu

### 1. `FirebaseOptimizationService.kt`
```kotlin
class FirebaseOptimizationService(private val application: Application) {
    // Cache management
    private val userCache = ConcurrentHashMap<String, User>()
    private val messageCache = ConcurrentHashMap<String, List<Message>>()
    
    // Optimized methods
    suspend fun getUser(userId: String): User?
    suspend fun getUsers(userIds: List<String>): Map<String, User>
    suspend fun getMessages(...): List<Message>
}
```

### 2. `MessageRepository.kt` (Đã cập nhật)
```kotlin
class MessageRepository(private val application: Application) {
    private val firebaseOptimizationService = FirebaseOptimizationService(application)
    
    // Pagination support
    suspend fun getMessages(userId: String, limit: Int = MESSAGES_PER_PAGE, lastMessageTimestamp: Long? = null): List<Message>
    
    // Efficient user loading
    private suspend fun loadUserInfoForMessages(messages: List<Message>)
}
```

### 3. `ChatViewModel.kt` (Đã cập nhật)
```kotlin
class MessageViewModel(application: Application) : AndroidViewModel(application) {
    // Pagination state
    data class MessageUiState(
        val hasMoreMessages: Boolean = true,
        val lastMessageTimestamp: Long? = null,
        val isLoadingMore: Boolean = false
    )
    
    // Load more messages
    fun loadMoreMessages()
    
    // Optimized real-time listener
    private fun setupRealtimeListener(userId: String)
}
```

### 4. `ChatScreen.kt` (Đã cập nhật)
```kotlin
@Composable
fun ChatScreen(...) {
    // Pull to refresh
    val pullRefreshState = rememberPullRefreshState(...)
    
    // Load more on scroll
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex <= 5 && messageState.hasMoreMessages) {
            messageViewModel.loadMoreMessages()
        }
    }
    
    // Loading indicators
    if (messageState.isLoadingMore) { ... }
    if (messageState.isLoading && messageState.messages.isEmpty()) { ... }
}
```

## Firebase Database Rules

### Tối ưu hóa rules trong `firebase_database_rules_optimized.json`:
```json
{
  "chats": {
    "$userId": {
      "$otherUserId": {
        ".indexOn": ["timestamp", "senderId", "receiverId"]
      }
    }
  }
}
```

## Cách sử dụng

### 1. Khởi tạo
```kotlin
// Trong Application class hoặc MainActivity
FirebaseDatabase.getInstance().setPersistenceEnabled(true)
```

### 2. Load tin nhắn với pagination
```kotlin
// Load initial messages
messageViewModel.loadMessages(userId)

// Load more messages
messageViewModel.loadMoreMessages()
```

### 3. Preload user data
```kotlin
// Preload single user
messageRepository.preloadUserInfo(userId)

// Preload multiple users
messageRepository.preloadUsers(userIds)
```

### 4. Cache management
```kotlin
// Clear cache when needed
messageRepository.clearUserCache()

// Get cache statistics
val stats = messageRepository.getCacheStats()
```

## Monitoring và Debug

### 1. Cache Statistics
```kotlin
val stats = messageRepository.getCacheStats()
Log.d("Cache", "User cache size: ${stats["userCacheSize"]}")
Log.d("Cache", "Message cache size: ${stats["messageCacheSize"]}")
```

### 2. Performance Monitoring
- Theo dõi thời gian load tin nhắn
- Kiểm tra số lượng network requests
- Monitor memory usage của cache

## Best Practices

### 1. Cache Management
- Clear cache khi user logout
- Monitor cache size để tránh memory leaks
- Set appropriate TTL cho cache

### 2. Pagination
- Load 20-50 tin nhắn mỗi lần
- Implement infinite scroll
- Show loading indicators

### 3. Real-time Updates
- Chỉ listen cho tin nhắn mới
- Disconnect listeners khi không cần thiết
- Handle offline scenarios

### 4. Error Handling
- Retry failed requests
- Show cached data khi offline
- Graceful degradation

## Kết quả mong đợi

Sau khi áp dụng các tối ưu hóa này, bạn sẽ thấy:

1. **Tốc độ load tin nhắn nhanh hơn 70-80%**
2. **Giảm số lượng network requests**
3. **Trải nghiệm mượt mà hơn với pagination**
4. **Tiết kiệm bandwidth và battery**
5. **Cải thiện UX với loading states**

## Troubleshooting

### Vấn đề thường gặp:

1. **Cache không hoạt động**
   - Kiểm tra TTL settings
   - Verify cache initialization

2. **Pagination không load thêm**
   - Check `hasMoreMessages` state
   - Verify scroll position logic

3. **Real-time updates chậm**
   - Check listener setup
   - Verify timestamp logic

4. **Memory usage cao**
   - Monitor cache size
   - Implement cache cleanup 