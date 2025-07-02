package com.baothanhbin.instagrambin.model

data class CallHistory(
    val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val callType: String = "video", // "video" hoặc "audio"
    val status: String = "", // "missed", "answered", "rejected", "ended"
    val startTime: Long = 0L, // timestamp khi bắt đầu gọi
    val endTime: Long = 0L, // timestamp khi kết thúc gọi
    val duration: Long = 0L, // thời lượng cuộc gọi tính bằng milliseconds
    val timestamp: Long = System.currentTimeMillis()
) {
    // Computed property để lấy duration dưới dạng string
    val durationString: String
        get() {
            if (duration <= 0) return ""
            val seconds = duration / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            
            return when {
                hours > 0 -> "${hours}h ${minutes % 60}m"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
            }
        }
    
    // Kiểm tra cuộc gọi có được trả lời không
    val isAnswered: Boolean
        get() = status == "answered" || status == "ended"
    
    // Kiểm tra cuộc gọi bị nhỡ
    val isMissed: Boolean
        get() = status == "missed"
    
    // Kiểm tra cuộc gọi bị từ chối
    val isRejected: Boolean
        get() = status == "rejected"
} 