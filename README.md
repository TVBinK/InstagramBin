# InstagramBin

InstagramBin là ứng dụng mạng xã hội di động lấy cảm hứng từ Instagram, phát triển bằng Kotlin và Jetpack Compose, tích hợp nhiều tính năng hiện đại như chat, gọi video, đăng bài, thông báo đẩy, tối ưu cache và nhiều tiện ích khác.

## 🚀 Tính năng nổi bật

- **Đăng nhập/Đăng ký**: Hỗ trợ đăng nhập, đăng ký bằng email, Google, Facebook.
- **Trang chủ (Home)**: Xem danh sách bài đăng, tương tác (like, comment), xem stories, làm mới dữ liệu.
- **Tìm kiếm (Search)**: Tìm kiếm người dùng, xem nhanh hồ sơ cá nhân.
- **Đăng bài (Add Post)**: Đăng ảnh, caption, upload ảnh lên Cloudinary.
- **Chi tiết bài viết**: Xem chi tiết bài đăng, bình luận, trả lời bình luận.
- **Chat & Danh sách chat**: Nhắn tin realtime, gửi ảnh, emoji, xem lịch sử chat.
- **Gọi video (Video Call)**: Gọi video 1-1 sử dụng WebRTC, thông báo cuộc gọi đến.
- **Thông báo đẩy (Push Notification)**: Nhận thông báo khi có like, comment, reply, cuộc gọi đến (FCM).
- **Hồ sơ cá nhân**: Xem, chỉnh sửa thông tin cá nhân, avatar, bio, highlights.
- **Theo dõi/Bỏ theo dõi**: Quản lý danh sách followers/following, xem hồ sơ người khác.
- **Tối ưu cache & clear data**: Tự động clear cache khi đăng xuất, chuyển tài khoản, tối ưu tải dữ liệu.
- **Giao diện hiện đại**: Thiết kế theo Material 3, hỗ trợ light/dark mode, hiệu ứng mượt mà.

## 🛠️ Công nghệ & Thư viện sử dụng

- **Ngôn ngữ**: Kotlin
- **UI**: Jetpack Compose, Material 3
- **Navigation**: androidx.navigation-compose
- **Realtime Database**: Firebase Realtime Database
- **Authentication**: Firebase Auth, Google Sign-In, Facebook Login
- **Push Notification**: Firebase Cloud Messaging (FCM)
- **Upload ảnh**: Cloudinary Android SDK
- **Chat & Video Call**: WebRTC (libwebrtc.aar), Firebase
- **Image Loading**: Coil
- **Khác**: Coroutines

## 📦 Cấu trúc dự án

- `app/src/main/java/com/baothanhbin/instagrambin/` - Code chính
  - `model/` - Định nghĩa dữ liệu (User, Post, Message, ...)
  - `repository/` - Xử lý dữ liệu, kết nối Firebase, Cloudinary
  - `service/` - Dịch vụ nền: WebRTC, Notification, DataClear, ...
  - `ui/` - Giao diện: screens, components, theme
  - `viewmodel/` - Quản lý trạng thái, logic UI
  - `navigation/` - Điều hướng màn hình
- `app/src/main/res/` - Tài nguyên: ảnh, icon, theme, string
- `app/libs/` - Thư viện native (libwebrtc.aar)

## 📖 Hướng dẫn cài đặt nhanh

1. Clone repo về máy:
   ```bash
   git clone https://github.com/tenban/InstagramBin.git
   ```
2. Mở bằng Android 
3. Thêm file `google-services.json` vào thư mục `app/` (lấy từ Firebase Console)
4. Sync Gradle, build và chạy app trên thiết bị/emulator



> Dự án được phát triển với mục đích học tập, nghiên cứu và demo kỹ thuật. Mọi đóng góp, phản hồi xin gửi về [github issues](https://github.com/tenban/InstagramBin/issues). 