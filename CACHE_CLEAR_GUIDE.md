# Hướng dẫn Clear Cache khi Đăng xuất

## Tổng quan
Tài liệu này mô tả cách app tự động clear cache và data khi đăng xuất để tránh hiển thị data của user trước đó.

## Vấn đề đã được giải quyết

### 🔍 **Vấn đề ban đầu:**
- Stories không tự động load lại khi đăng xuất và đăng nhập tài khoản khác
- Messages vẫn hiển thị tin nhắn của người dùng phiên đăng nhập trước
- Cache và data cũ không được clear khi chuyển user

### ✅ **Giải pháp đã triển khai:**

## 1. DataClearService
```kotlin
class DataClearService(private val application: Application) {
    suspend fun clearAllData() {
        // Clear Firebase Optimization cache
        firebaseOptimizationService.forceClearAllData()
        
        // Clear Firebase Auth
        auth.signOut()
        
        // Clear local storage cache
        clearLocalCache()
    }
}
```

## 2. AuthViewModel Updates
```kotlin
class AuthViewModel(application: Application) : ViewModel() {
    private val dataClearService = DataClearService(application)
    
    fun signOut() {
        viewModelScope.launch {
            // Clear all cache and data
            dataClearService.clearAllData()
            
            // Sign out from Firebase Auth
            auth.signOut()
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            // Clear cache trước khi đăng nhập
            dataClearService.forceRefreshAllData()
            // ... rest of sign in logic
        }
    }
}
```

## 3. ViewModel Cache Clearing

### ChatListViewModel
```kotlin
init {
    // Clear cache khi khởi tạo
    clearCache()
    setupChatListener()
}

private fun clearCache() {
    cachedChats = emptyList()
    lastUpdateTime = 0
    messageRepository.clearUserCache()
}
```

### HomeViewModel
```kotlin
fun clearCache() {
    isLoaded = false
    currentUserCache = null
    _uiState.value = HomeUiState()
    _isRefreshing.value = false
}

fun forceRefresh() {
    clearCache()
    loadDataIfNeeded()
}
```

## 4. FirebaseOptimizationService
```kotlin
fun forceClearAllData() {
    clearCache()
    
    // Clear Firebase Database cache
    FirebaseDatabase.getInstance().purgeOutstandingWrites()
}
```

## 5. MainActivity Integration
```kotlin
onLogout = {
    authViewModel.signOut()
    
    // Clear cache của các ViewModel
    homeViewModel.clearCache()
    
    navController.navigate("login") {
        popUpTo("home") { inclusive = true }
    }
}
```

## Các loại Cache được Clear

### 1. **Firebase Optimization Cache**
- User cache (TTL 5 phút)
- Message cache (TTL 5 phút)
- Cache timestamps

### 2. **Firebase Database Cache**
- Outstanding writes
- Local persistence data

### 3. **ViewModel Cache**
- ChatListViewModel static cache
- HomeViewModel user cache
- MessageRepository cache

### 4. **Local Storage Cache**
- Application cache directory
- External cache directory

## Quy trình Clear Cache

### Khi Đăng xuất:
1. **Remove FCM token** từ Firebase
2. **Clear Firebase Optimization cache**
3. **Clear Firebase Database cache**
4. **Clear ViewModel cache**
5. **Clear local storage cache**
6. **Sign out từ Firebase Auth**

### Khi Đăng nhập:
1. **Force refresh all data** trước khi đăng nhập
2. **Clear cache** để đảm bảo data mới
3. **Load fresh data** cho user mới

### Khi Khởi tạo ViewModel:
1. **Clear cache** khi tạo ViewModel mới
2. **Reset state** về ban đầu
3. **Load fresh data** từ Firebase

## Kết quả

Sau khi áp dụng các thay đổi này:

✅ **Stories sẽ load lại** khi đăng nhập tài khoản khác
✅ **Messages sẽ clear** và hiển thị tin nhắn của user mới
✅ **Tất cả cache được clear** khi đăng xuất
✅ **Data mới được load** khi đăng nhập
✅ **Không còn data cũ** của user trước đó

## Best Practices

### 1. **Always Clear Cache on Sign Out**
```kotlin
fun signOut() {
    viewModelScope.launch {
        dataClearService.clearAllData()
        auth.signOut()
    }
}
```

### 2. **Clear Cache Before Sign In**
```kotlin
fun signIn() {
    viewModelScope.launch {
        dataClearService.forceRefreshAllData()
        // ... sign in logic
    }
}
```

### 3. **Clear ViewModel Cache on Init**
```kotlin
init {
    clearCache()
    // ... setup logic
}
```

### 4. **Force Refresh When Needed**
```kotlin
fun forceRefresh() {
    clearCache()
    loadDataIfNeeded()
}
```

## Monitoring

### Debug Cache Status:
```kotlin
val stats = firebaseOptimizationService.getCacheStats()
Log.d("Cache", "User cache: ${stats["userCacheSize"]}")
Log.d("Cache", "Message cache: ${stats["messageCacheSize"]}")
```

### Check Cache Clearing:
```kotlin
// After sign out, cache should be empty
val stats = firebaseOptimizationService.getCacheStats()
assert(stats["userCacheSize"] == 0)
assert(stats["messageCacheSize"] == 0)
``` 