package com.baothanhbin.instagrambin.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baothanhbin.instagrambin.R
import com.baothanhbin.instagrambin.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import android.app.Application
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import android.provider.MediaStore
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore.Images.Media
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush

@Composable
fun EditProfileScreen(
    viewModel: EditProfileViewModel, onCancel: () -> Unit, onDone: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showImagePicker by remember { mutableStateOf(false) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    // Load images when screen is created
    LaunchedEffect(Unit) {
        imageUris = loadImages(context)
    }

    // Handle screen closing
    LaunchedEffect(state.shouldCloseScreen) {
        if (state.shouldCloseScreen) {
            onDone()
        }
    }

    Scaffold(topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back",
                modifier = Modifier.clickable { onCancel() })
            Text("Chỉnh sửa trang cá nhân", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 70.dp))
            Text("Hoàn thành", 
                color = if (state.isLoading) Color.Gray else Color(0xFF3797EF), 
                modifier = Modifier.clickable {
                    if (!state.isLoading) {
                        scope.launch {
                            try {
                                viewModel.saveProfile()
                            } catch (e: Exception) {
                                // Error is already handled in ViewModel
                            }
                        }
                    }
                }
            )
        }
    }) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(contentAlignment = Alignment.Center) {
                    if (state.avatarUri != null) {
                        AsyncImage(
                            model = state.avatarUri,
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { showImagePicker = true },
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.profile_pic),
                            contentDescription = "Profile Photo",
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable { showImagePicker = true },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                TextButton(onClick = { showImagePicker = true }) {
                    Text("Thay đổi ảnh đại diện", color = Color(0xFF3797EF))
                }
                Spacer(modifier = Modifier.height(16.dp))
                // Name
                EditProfileField(
                    label = "Họ và tên", value = state.name, onValueChange = viewModel::onNameChange
                )
                Divider()
                // Username
                EditProfileField(
                    label = "Tài khoản",
                    value = state.username,
                    onValueChange = viewModel::onUsernameChange
                )
                Divider()
                // Bio
                EditProfileField(
                    label = "Bio", value = state.bio, onValueChange = viewModel::onBioChange
                )
                Divider()
                Divider(thickness = 8.dp, color = Color(0xFFF5F5F5))
                // Private Information
                Text(
                    "Thông tin cá nhân",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .fillMaxWidth()
                )
                // Email
                EditProfileField(
                    label = "Email",
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    keyboardType = KeyboardType.Email
                )
                Divider()
                // Gender
                EditProfileField(
                    label = "Giới tính", value = state.gender, onValueChange = viewModel::onGenderChange
                )
                Divider()
                if (state.error != null) {
                    Text(
                        text = state.error!!, color = Color.Red, modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Đang cập nhật...",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    // Image picker dialog
    if (showImagePicker) {
        AlertDialog(
            onDismissRequest = { showImagePicker = false },
            title = { Text("Chọn ảnh đại diện") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    items(imageUris) { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "Image",
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.onAvatarSelected(uri)
                                    showImagePicker = false
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showImagePicker = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Email verification dialog
    if (state.showEmailVerificationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideEmailVerificationDialog() },
            title = { Text("Xác minh thay đổi email") },
            text = {
                Column {
                    if (state.showPasswordDialog) {
                        Text("Vui lòng nhập mật khẩu hiện tại để xác minh thay đổi email.")
                        OutlinedTextField(
                            value = state.password,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            label = { Text("Mật khẩu") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation()
                        )
                    } else if (state.verificationSent) {
                        Text("Email xác minh đã được gửi đến email hiện tại của bạn.")
                        Text("Vui lòng kiểm tra hộp thư và nhấn vào link xác minh.")
                        if (state.showVerificationButton) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.checkEmailVerification()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Text("Đã xác minh")
                            }
                        }
                    }
                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (state.showPasswordDialog) {
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.verifyPassword()
                            }
                        },
                        enabled = !state.isLoading && state.password.isNotBlank()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(color = Color.White)
                        } else {
                            Text("Xác nhận")
                        }
                    }
                } else {
                    TextButton(onClick = { viewModel.hideEmailVerificationDialog() }) {
                        Text("Đóng")
                    }
                }
            },
            dismissButton = {
                if (state.showPasswordDialog) {
                    TextButton(onClick = { viewModel.hideEmailVerificationDialog() }) {
                        Text("Hủy")
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label, color = Color.Gray, fontSize = 15.sp, modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = LocalTextStyle.current.copy(
                color = if (enabled) Color.Black else Color.Gray,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            ),
            modifier = Modifier
                .weight(1f)
                .padding(0.dp),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray,
                disabledBorderColor = Color.LightGray,
                cursorColor = Color.Black
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
        )
    }
}

private fun loadImages(context: android.content.Context): List<Uri> {
    val images = mutableListOf<Uri>()
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(
        Media._ID,
        Media.DISPLAY_NAME,
        Media.DATE_TAKEN
    )

    val selection = "${Media.MIME_TYPE} LIKE ?"
    val selectionArgs = arrayOf("image/%")
    val sortOrder = "${Media.DATE_TAKEN} DESC"

    context.contentResolver.query(
        collection,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(Media._ID)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = ContentUris.withAppendedId(collection, id)
            images.add(contentUri)
        }
    }

    return images
}

// Preview function
@Preview(showBackground = true)
@Composable
fun EditProfileScreenPreview() {
    val viewModel = EditProfileViewModel(LocalContext.current.applicationContext as Application)
    EditProfileScreen(viewModel = viewModel, onCancel = {}, onDone = {})
}