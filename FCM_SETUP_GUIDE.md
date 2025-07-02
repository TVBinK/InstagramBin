# Hướng dẫn cấu hình Firebase Cloud Messaging (FCM) cho Push Notification

## 1. Cấu hình Firebase Console

### Bước 1: Tạo dự án Firebase
1. Truy cập [Firebase Console](https://console.firebase.google.com/)
2. Tạo dự án mới hoặc chọn dự án hiện có
3. Thêm ứng dụng Android vào dự án

### Bước 2: Tải file google-services.json
1. Tải file `google-services.json` từ Firebase Console
2. Đặt file vào thư mục `app/` của dự án Android

### Bước 3: Cấu hình Cloud Messaging
1. Trong Firebase Console, chọn "Cloud Messaging"
2. Lấy Server Key từ tab "Cloud Messaging"
3. Cập nhật Server Key trong `NotificationService.kt`

## 2. Cấu hình trong code

### Bước 1: Cập nhật Server Key
Mở file `app/src/main/java/com/baothanhbin/instagrambin/service/NotificationService.kt` và thay đổi:

```kotlin
companion object {
    private const val FCM_SERVER_KEY = "YOUR_SERVER_KEY" // Thay bằng Server Key từ Firebase Console
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
}
```

### Bước 2: Cập nhật AndroidManifest.xml
Đảm bảo đã khai báo FCMService trong AndroidManifest.xml:

```xml
<service
    android:name=".service.FCMService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

## 3. Cách thức hoạt động

### Khi có người like bài viết:
1. User A like bài viết của User B
2. `PostRepository.toggleLike()` được gọi
3. `NotificationService.sendLikeNotification()` gửi notification
4. User B nhận được notification: "User A đã thích bài viết của bạn"

### Khi có người comment bài viết:
1. User A comment bài viết của User B
2. `CommentViewModel.addComment()` được gọi
3. `NotificationService.sendCommentNotification()` gửi notification
4. User B nhận được notification: "User A đã bình luận bài viết của bạn"

### Khi có người reply comment:
1. User A reply comment của User B
2. `CommentViewModel.addComment()` được gọi với `replyToCommentId`
3. `NotificationService.sendReplyNotification()` gửi notification
4. User B nhận được notification: "User A đã trả lời bình luận của bạn"

## 4. Cấu trúc Database

### Users Collection:
```json
{
  "users": {
    "userId": {
      "fcmToken": "device_fcm_token_here",
      "username": "user_name",
      "email": "user@email.com"
    }
  }
}
```

### Posts Collection:
```json
{
  "posts": {
    "postId": {
      "userId": "post_owner_id",
      "likes": {
        "likerId": true
      },
      "likesCount": 5,
      "comments": {
        "commentId": {
          "userId": "commenter_id",
          "content": "comment_content",
          "timestamp": 1234567890,
          "replyToCommentId": "original_comment_id"
        }
      },
      "commentsCount": 3
    }
  }
}
```

## 5. Testing Push Notification

### Test từ Firebase Console:
1. Vào Firebase Console > Cloud Messaging
2. Chọn "Send your first message"
3. Nhập thông tin notification
4. Chọn target (specific device hoặc topic)
5. Gửi test message

### Test từ code:
1. Đảm bảo user đã đăng nhập và có FCM token
2. Like hoặc comment một bài viết
3. Kiểm tra notification trên thiết bị

## 6. Troubleshooting

### Notification không hiển thị:
1. Kiểm tra FCM token có được lưu trong database không
2. Kiểm tra Server Key có đúng không
3. Kiểm tra quyền notification trong Android settings
4. Kiểm tra log để xem lỗi

### Notification hiển thị nhưng không mở đúng màn hình:
1. Kiểm tra intent extras trong MainActivity
2. Kiểm tra navigation logic
3. Đảm bảo postId được truyền đúng

## 7. Tối ưu hóa

### Batch Notification:
- Có thể gộp nhiều notification cùng loại
- Sử dụng topic để gửi notification cho nhóm user

### Notification Settings:
- Cho phép user tắt/bật notification theo loại
- Lưu preference trong SharedPreferences

### Performance:
- Sử dụng WorkManager cho notification background
- Cache FCM token để tránh gọi API nhiều lần

## 8. Security

### Server Key:
- Không commit Server Key vào git
- Sử dụng BuildConfig hoặc environment variables
- Rotate Server Key định kỳ

### User Permission:
- Chỉ gửi notification cho user đã cho phép
- Kiểm tra user authentication trước khi gửi

## 9. Monitoring

### Analytics:
- Track notification delivery rate
- Monitor user engagement với notification
- Log notification events

### Error Handling:
- Log lỗi khi gửi notification thất bại
- Implement retry mechanism
- Monitor FCM token validity 