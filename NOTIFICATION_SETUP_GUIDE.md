# Hướng dẫn Setup Notification với 2 nút cho Video Call

## Tổng quan
Ứng dụng Android đã được cập nhật để hỗ trợ notification với 2 nút "Chấp nhận" và "Từ chối" cho video call, tương thích với Firebase Cloud Functions.

## Các thay đổi đã thực hiện

### 1. FCMService.kt
- Tạo method `showVideoCallNotification()` riêng biệt cho video call
- Sử dụng channel ID "video_calls" khớp với Firebase Cloud Functions
- Thêm 2 action buttons: "Chấp nhận" và "Từ chối"
- Notification không tự động đóng và không thể swipe away
- Có âm thanh và rung khi có cuộc gọi đến

### 2. CallActionReceiver.kt
- Xử lý action "ACTION_ACCEPT_CALL" và "ACTION_REJECT_CALL"
- Gửi tín hiệu accept/reject lên Firebase Database
- Tự động đóng notification sau khi xử lý
- Mở MainActivity với video call khi chấp nhận

### 3. MainActivity.kt
- Cập nhật xử lý intent từ notification
- Hỗ trợ navigation cho chat và video call
- Lưu trữ thông tin notification trong SharedPreferences

### 4. Icons
- Tạo `ic_call_answer.xml` cho nút chấp nhận
- Tạo `ic_call_decline.xml` cho nút từ chối

## Cấu trúc Database cho Call Response

Khi user chấp nhận hoặc từ chối cuộc gọi, response sẽ được lưu vào:
```
/calls/{callId}/responses/{userId}
{
  "response": "accept" | "reject",
  "timestamp": 1234567890,
  "userId": "user_id"
}
```

## Firebase Cloud Functions

Đảm bảo Firebase Cloud Functions sử dụng:
- Channel ID: "video_calls"
- Action names: "ACCEPT_CALL", "DECLINE_CALL"
- Icon names: "ic_call_answer", "ic_call_decline"

## Cách hoạt động

1. **Khi có cuộc gọi đến:**
   - Firebase Cloud Functions gửi notification với 2 nút
   - Android app hiển thị notification với channel "video_calls"
   - Notification có âm thanh và rung

2. **Khi user chấp nhận:**
   - CallActionReceiver nhận action "ACTION_ACCEPT_CALL"
   - Gửi response "accept" lên Firebase Database
   - Mở MainActivity với video call screen
   - Đóng notification

3. **Khi user từ chối:**
   - CallActionReceiver nhận action "ACTION_REJECT_CALL"
   - Gửi response "reject" lên Firebase Database
   - Đóng notification

## Testing

1. Deploy Firebase Cloud Functions
2. Build và cài đặt Android app
3. Gửi test notification từ Firebase Console
4. Kiểm tra notification hiển thị với 2 nút
5. Test chức năng chấp nhận/từ chối

## Lưu ý

- Notification video call có priority HIGH và category CALL
- Sử dụng PendingIntent.FLAG_UPDATE_CURRENT để tránh lỗi
- Notification ID được tạo từ callId để tránh conflict
- Cần đăng ký CallActionReceiver trong AndroidManifest.xml 