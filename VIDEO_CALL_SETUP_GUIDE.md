# Video Call Setup Guide

Hướng dẫn tích hợp WebRTC video call vào ứng dụng Instagram clone.

## Tính năng đã được tích hợp

### 1. WebRTC Service (`WebRTCService.kt`)
- Quản lý kết nối WebRTC peer-to-peer
- Xử lý camera và microphone
- Gửi/nhận tín hiệu call qua Firebase Firestore
- Quản lý trạng thái cuộc gọi

### 2. Video Call Screen (`VideoCallScreen.kt`)
- Hiển thị video local và remote
- Picture-in-picture layout
- Controls cho mute, end call, switch camera
- Overlay cho trạng thái cuộc gọi

### 3. Video Call ViewModel (`VideoCallViewModel.kt`)
- Quản lý state của video call
- Xử lý permissions
- Listen cho incoming calls
- Tích hợp với WebRTC service

### 4. Permission Service (`PermissionService.kt`)
- Kiểm tra và request permissions
- Camera và microphone permissions
- Validation trước khi bắt đầu call

### 5. Chat Screen Integration
- Nút video call trong top bar
- Tự động request permissions
- Chuyển sang video call screen khi bắt đầu call

## Dependencies đã thêm

```kotlin
// WebRTC
implementation("org.webrtc:google-webrtc:1.0.32006")
implementation("com.google.firebase:firebase-firestore-ktx:24.10.2")
```

## Permissions đã thêm

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.camera.autofocus" android:required="false" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
```

## Cách sử dụng

### 1. Bắt đầu cuộc gọi
```kotlin
// Trong ChatScreen, nhấn nút video call
videoCallViewModel.startCall(user)
```

### 2. Nhận cuộc gọi
```kotlin
// Tự động xử lý incoming call
LaunchedEffect(incomingCall) {
    incomingCall?.let { call ->
        videoCallViewModel.acceptCall(call.callId, call.fromUserId)
    }
}
```

### 3. Kết thúc cuộc gọi
```kotlin
videoCallViewModel.endCall()
```

## Firebase Firestore Structure

### Collection: `calls`
```json
{
  "callId": "uuid",
  "fromUserId": "user_id",
  "toUserId": "user_id", 
  "type": "offer|answer|reject|end",
  "timestamp": 1234567890
}
```

### Subcollection: `calls/{callId}/candidates`
```json
{
  "callId": "uuid",
  "fromUserId": "user_id",
  "toUserId": "user_id",
  "type": "ice_candidate",
  "candidate": "candidate_string",
  "sdpMLineIndex": 0,
  "sdpMid": "0",
  "timestamp": 1234567890
}
```

## Tính năng chính

### ✅ Đã hoàn thành
- [x] WebRTC peer-to-peer connection
- [x] Camera và microphone access
- [x] Video display (local + remote)
- [x] Call controls (end call, mute, switch camera)
- [x] Permission handling
- [x] Firebase signaling
- [x] Call state management
- [x] UI integration với ChatScreen

### 🔄 Cần cải thiện
- [ ] STUN/TURN server configuration
- [ ] Better error handling
- [ ] Call quality optimization
- [ ] Background call handling
- [ ] Push notifications cho incoming calls
- [ ] Call history
- [ ] Screen sharing
- [ ] Group video calls

## Troubleshooting

### 1. Camera không hoạt động
- Kiểm tra camera permissions
- Đảm bảo device có camera
- Restart app sau khi grant permissions

### 2. Microphone không hoạt động
- Kiểm tra microphone permissions
- Đảm bảo device có microphone
- Kiểm tra audio settings

### 3. Kết nối không thành công
- Kiểm tra internet connection
- Đảm bảo Firebase Firestore được setup đúng
- Kiểm tra STUN server configuration

### 4. Video lag/quality issues
- Giảm video resolution
- Kiểm tra network bandwidth
- Optimize WebRTC settings

## Security Considerations

1. **Permissions**: Chỉ request permissions khi cần thiết
2. **Firebase Rules**: Setup proper Firestore security rules
3. **Data Privacy**: Không lưu trữ video/audio data
4. **Network Security**: Sử dụng HTTPS cho signaling

## Performance Optimization

1. **Video Quality**: Tự động adjust dựa trên network
2. **Battery Usage**: Optimize camera usage
3. **Memory Management**: Proper cleanup của WebRTC resources
4. **Network Usage**: Efficient signaling protocol

## Testing

### Test Cases
1. ✅ Start video call
2. ✅ Accept incoming call  
3. ✅ End call
4. ✅ Permission handling
5. ✅ Camera switch
6. ✅ Mute/unmute
7. ✅ Network disconnection
8. ✅ App background/foreground

### Test Devices
- Android 8.0+ (API 27+)
- Devices with camera và microphone
- Different network conditions (WiFi, 4G, 3G)

## Next Steps

1. **Production Ready**:
   - Add proper STUN/TURN servers
   - Implement call quality monitoring
   - Add call analytics

2. **Advanced Features**:
   - Group video calls
   - Screen sharing
   - Call recording
   - Virtual backgrounds

3. **UI/UX Improvements**:
   - Better call animations
   - Custom video filters
   - Picture-in-picture mode
   - Floating call window 