package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import org.webrtc.RendererCommon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import kotlin.math.abs
import com.baothanhbin.instagrambin.model.User
import com.baothanhbin.instagrambin.service.WebRTCService
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import com.baothanhbin.instagrambin.ui.components.SafeSurfaceViewRenderer
import com.baothanhbin.instagrambin.viewmodel.VideoCallViewModel
import androidx.compose.runtime.LaunchedEffect
import android.widget.FrameLayout
import android.graphics.drawable.GradientDrawable
import androidx.core.graphics.toColorInt
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun VideoCallScreen(
    user: User,
    webRTCService: WebRTCService,
    onEndCall: () -> Unit,
    videoCallViewModel: VideoCallViewModel? = null
) {
    val context = LocalContext.current
    val callState by webRTCService.callState.collectAsState()
    val incomingCall by videoCallViewModel?.incomingCall?.collectAsState() ?: remember { mutableStateOf(null) }
    
    // Sử dụng derivedStateOf để tránh re-render không cần thiết
    val shouldShowOverlay = remember(callState, incomingCall) {
        callState == WebRTCService.CallState.CALLING ||
        callState == WebRTCService.CallState.RINGING ||
        callState == WebRTCService.CallState.ERROR
    }
    
    // Lấy video tracks và EGL context - không cache để đảm bảo luôn fresh
    val eglBase by remember {
        derivedStateOf {
            try {
                if (!webRTCService.isDisposed()) {
                    webRTCService.getEglBase()
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
    
    val isEglValid by remember {
        derivedStateOf {
            try {
                if (!webRTCService.isDisposed()) {
                    webRTCService.isEglContextValid()
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
    
    val remoteVideoTrack by webRTCService.remoteVideoTrackFlow.collectAsState()
    val localVideoTrack by webRTCService.localVideoTrackFlow.collectAsState()
    
    // Lưu trữ track và EGL context hiện tại để tránh re-composition không cần thiết
    val currentRemoteTrack = remoteVideoTrack
    val currentLocalTrack = localVideoTrack  
    val currentEglBase = eglBase
    
   // Biến trạng thái để hoán đổi video preview
    var isVideoSwapped by remember { mutableStateOf(false) }

    val density = LocalDensity.current

    var ringtone: Ringtone? by remember { mutableStateOf(null) }

    val isMicMuted = videoCallViewModel?.isMicMuted?.collectAsState() ?: remember { mutableStateOf(false) }
    val isVideoMuted = videoCallViewModel?.isVideoMuted?.collectAsState() ?: remember { mutableStateOf(false) }

    // Phát nhạc chuông khi có cuộc gọi đến bằng RingtoneManager
    LaunchedEffect(callState) {
        if (callState == WebRTCService.CallState.RINGING) {
            if (ringtone == null) {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ringtone = RingtoneManager.getRingtone(context, uri)
                try {
                    ringtone?.isLooping = true // Chỉ có tác dụng trên Android 9+
                } catch (_: Throwable) {}
                ringtone?.play()
            }
        } else if (callState == WebRTCService.CallState.CONNECTED ||
                   callState == WebRTCService.CallState.ENDED ||
                   callState == WebRTCService.CallState.ERROR ||
                   callState != WebRTCService.CallState.RINGING) {
            ringtone?.stop()
            ringtone = null
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenWidth = constraints.maxWidth.toFloat()
        val screenHeight = constraints.maxHeight.toFloat()
        val previewSizePx = with(density) { 120.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }
        val defaultOffset = Offset(
            screenWidth - previewSizePx - paddingPx,
            paddingPx
        )
        var localVideoOffset by remember { mutableStateOf(defaultOffset) }
        // 1. Remote video (full screen) - chỉ render khi có track và EGL context hợp lệ
        if (currentRemoteTrack != null && currentEglBase != null && isEglValid) {
            val remoteSurfaceView = remember(currentEglBase, currentRemoteTrack) {
                SafeSurfaceViewRenderer(context).apply {
                    val rendererEvents = object : RendererCommon.RendererEvents {
                        override fun onFirstFrameRendered() {
                        }
                        override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {
                        }
                    }
                    
                    if (!safeInit(currentEglBase, rendererEvents)) {
                    } else {
                        // Set scaling mode to fill the view
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        setEnableHardwareScaler(true)
                    }
                }
            }
            
            DisposableEffect(currentEglBase, currentRemoteTrack) {
                if (currentRemoteTrack != null && remoteSurfaceView.isInitialized()) {
                    remoteSurfaceView.safeAddSink(currentRemoteTrack)
                }
                onDispose {
                    if (webRTCService.isDisposed()) {
                        remoteSurfaceView.safeRelease()
                        return@onDispose
                    }
                    
                    if (currentRemoteTrack != null && remoteSurfaceView.isInitialized()) {
                        try {
                            val trackId = currentRemoteTrack.id()
                            if (trackId != null && trackId.isNotEmpty()) {
                                remoteSurfaceView.safeRemoveSink(currentRemoteTrack)
                            }
                        } catch (e: Exception) {
                        }
                    }
                    remoteSurfaceView.safeRelease()
                }
            }
            
            AndroidView(
                factory = { remoteSurfaceView },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f) // Remote video ở layer thấp nhất
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.DarkGray)
                    .zIndex(0f), // Placeholder ở layer thấp
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Đang chờ video từ xa...",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Trạng thái: $callState",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Track từ xa: ${if (currentRemoteTrack != null) "Có sẵn" else "Không có sẵn"}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "EGL: ${if (currentEglBase != null && isEglValid) "Hợp lệ" else "Không hợp lệ"}",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        
        // 3. Call state overlay (chỉ render khi CALLING, RINGING, ERROR)
        if (shouldShowOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 140.dp)
                    .zIndex(8f), // Overlay ở layer rất cao
                contentAlignment = Alignment.TopCenter
            ) {
                when (callState) {
                    WebRTCService.CallState.CALLING -> CallStateOverlay(
                        title = "Đang gọi ${user.username}",
                        subtitle = "Vui lòng chờ...",
                        showSpinner = true
                    )
                    WebRTCService.CallState.RINGING -> CallStateOverlay(
                        title = "Cuộc gọi đến",
                        subtitle = "${user.username} đang gọi...",
                        showSpinner = false,
                        showAcceptReject = true,
                        onAccept = {
                            // Dừng nhạc chuông ngay khi bấm nhận cuộc gọi
                            ringtone?.stop()
                            ringtone = null
                            val callId = incomingCall?.callId
                            val fromUserId = incomingCall?.fromUserId
                            if (callId != null && fromUserId != null && videoCallViewModel != null) {
                                videoCallViewModel.acceptCall(callId, fromUserId)
                            }
                        },
                        onReject = {
                            val callId = incomingCall?.callId
                            val fromUserId = incomingCall?.fromUserId
                            if (callId != null && fromUserId != null && videoCallViewModel != null) {
                                videoCallViewModel.rejectCall(callId, fromUserId)
                            } else {
                                webRTCService.endCall()
                                onEndCall()
                            }
                        }
                    )
                    WebRTCService.CallState.ERROR -> CallStateOverlay(
                        title = "Cuộc gọi thất bại",
                        subtitle = "Không thể kết nối",
                        showSpinner = false
                    )
                    else -> {}
                }
            }
        }
        
        // User info (center top)
        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .zIndex(5f), // User info ở layer cao
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.username,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when (callState) {
                        WebRTCService.CallState.CALLING -> "Đang gọi..."
                        WebRTCService.CallState.RINGING -> "Đang đổ chuông..."
                        WebRTCService.CallState.CONNECTED -> "Đã kết nối"
                        WebRTCService.CallState.ENDED -> "Cuộc gọi đã kết thúc"
                        WebRTCService.CallState.ERROR -> "Lỗi"
                        else -> ""
                    },
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Call controls (bottom)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .zIndex(5f), // Call controls ở layer cao
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            IconButton(
                onClick = { videoCallViewModel?.toggleMic() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isMicMuted.value) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMicMuted.value) "Bật mic" else "Tắt tiếng",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            // Video (camera) button
            IconButton(
                onClick = { videoCallViewModel?.toggleVideo() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (isVideoMuted.value) Icons.Default.VideocamOff else Icons.Default.Videocam,
                    contentDescription = if (isVideoMuted.value) "Bật camera" else "Tắt camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // End call button
            IconButton(
                onClick = {
                    onEndCall()
                },
                modifier = Modifier
                    .size(64.dp)
                    .background(Color.Red, CircleShape)
            ) {
                Icon(
                    Icons.Default.CallEnd,
                    contentDescription = "Kết thúc cuộc gọi",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Switch camera button
            IconButton(
                onClick = { 
                    webRTCService.switchCamera()
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Chuyển camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // LOCAL VIDEO PREVIEW - RENDER LAST TO ENSURE IT'S ALWAYS ON TOP
        // 2. Local video preview - ALWAYS ON TOP - chỉ render khi có track và EGL context hợp lệ
        if (currentLocalTrack != null && currentEglBase != null && isEglValid) {
            val localSurfaceView = remember(currentEglBase) {
                SafeSurfaceViewRenderer(context).apply {
                    val localRendererEvents = object : RendererCommon.RendererEvents {
                        override fun onFirstFrameRendered() {}
                        override fun onFrameResolutionChanged(videoWidth: Int, videoHeight: Int, rotation: Int) {}
                    }
                    safeInit(currentEglBase, localRendererEvents)
                    setMirror(true)
                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                    setEnableHardwareScaler(true)
                    setZOrderMediaOverlay(true)
                }
            }

            DisposableEffect(currentLocalTrack, localSurfaceView) {
                if (currentLocalTrack != null && localSurfaceView.isInitialized()) {
                    localSurfaceView.safeAddSink(currentLocalTrack)
                }
                onDispose {
                    if (localSurfaceView.isInitialized()) {
                        if (currentLocalTrack != null) {
                            localSurfaceView.safeRemoveSink(currentLocalTrack)
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset {
                        IntOffset(
                            localVideoOffset.x.roundToInt(),
                            localVideoOffset.y.roundToInt()
                        )
                    }
                    .zIndex(50f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clickable {
                        isVideoSwapped = !isVideoSwapped
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            localVideoOffset = Offset(
                                (localVideoOffset.x + dragAmount.x).coerceIn(
                                    0f,
                                    (screenWidth - previewSizePx).coerceAtLeast(0f)
                                ),
                                (localVideoOffset.y + dragAmount.y).coerceIn(
                                    0f,
                                    (screenHeight - previewSizePx).coerceAtLeast(0f)
                                )
                            )
                        }
                    }
            ) {
                AndroidView(
                    factory = { ctx ->
                        val frameLayout = FrameLayout(ctx).apply {
                            background = GradientDrawable().apply {
                                cornerRadius = ctx.resources.displayMetrics.density * 12 // 12dp
                                setColor(android.graphics.Color.TRANSPARENT)
                            }
                            clipToOutline = true
                        }
                        frameLayout.addView(localSurfaceView)
                        frameLayout
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(50f)
                )
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Chạm để hoán đổi video",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .zIndex(51f)
                )
            }
        } else {
            // Placeholder cho local video - ALSO RENDER LAST
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .offset {
                        IntOffset(
                            localVideoOffset.x.roundToInt(),
                            localVideoOffset.y.roundToInt()
                        )
                    }
                    .zIndex(50f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clickable {
                        isVideoSwapped = !isVideoSwapped
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            localVideoOffset = Offset(
                                (localVideoOffset.x + dragAmount.x).coerceIn(
                                    0f,
                                    (screenWidth - previewSizePx).coerceAtLeast(0f)
                                ),
                                (localVideoOffset.y + dragAmount.y).coerceIn(
                                    0f,
                                    (screenHeight - previewSizePx).coerceAtLeast(0f)
                                )
                            )
                        }
                    }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Video cá nhân",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = if (currentLocalTrack != null) "Track OK" else "Không có track",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                
                // Swap icon indicator for placeholder
                Icon(
                    Icons.Default.Cameraswitch,
                    contentDescription = "Chạm để hoán đổi video",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .zIndex(51f) // Highest priority for interactive elements
                )
            }
        }
    }
}

@Composable
private fun CallStateOverlay(
    title: String,
    subtitle: String,
    showSpinner: Boolean,
    showAcceptReject: Boolean = false,
    onAccept: (() -> Unit)? = null,
    onReject: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (showSpinner) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall
            )
            
            Text(
                text = subtitle,
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (showAcceptReject) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { onReject?.invoke() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Red, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Từ chối",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = { onAccept?.invoke() },
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.Green, CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Chấp nhận",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
} 