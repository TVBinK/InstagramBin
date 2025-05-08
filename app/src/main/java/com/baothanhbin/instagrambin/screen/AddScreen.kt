package com.baothanhbin.instagrambin.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import android.Manifest
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import com.baothanhbin.instagrambin.R

@Composable
fun AddScreen(
    paddingValues: PaddingValues = PaddingValues(0.dp),
    navController: NavController
) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Thư Viện", "", "Video")
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            // Handle the captured image
            selectedImageUri?.let { uri ->
                // Save to gallery
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

                val galleryUri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                galleryUri?.let { galleryImageUri ->
                    context.contentResolver.openOutputStream(galleryImageUri)?.use { outputStream ->
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        context.contentResolver.update(galleryImageUri, contentValues, null, null)
                    }

                    // Add the new image to the beginning of the list
                    imageUris = listOf(galleryImageUri) + imageUris
                    selectedIndex = 0
                    selectedImageUri = galleryImageUri
                    selectedTab = 0  // Switch to gallery tab after taking photo
                }
            }
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                // Create a temporary file for the camera image
                val photoFile = java.io.File.createTempFile(
                    "IMG_${System.currentTimeMillis()}_",
                    ".jpg",
                    context.cacheDir
                )
                selectedImageUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                cameraLauncher.launch(selectedImageUri)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Permission launcher for gallery
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadImages(context) { uris ->
                imageUris = uris
                if (uris.isNotEmpty()) {
                    selectedImageUri = uris[0]
                }
            }
        }
    }

    // Check and request permission when screen is created
    LaunchedEffect(Unit) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        permissionLauncher.launch(permission)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(paddingValues)
    ) {
        // TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_back),
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { navController.navigateUp() }
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Bài viết mới", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Text(
                "Tiếp",
                color = Color(0xFF3797EF),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    val urisToSend = if (isMultiSelectMode && selectedUris.isNotEmpty()) selectedUris.toList() else selectedImageUri?.let { listOf(it) } ?: emptyList()
                    if (urisToSend.isNotEmpty()) {
                        // Encode list Uri thành 1 chuỗi, phân tách bằng dấu phẩy
                        val uriString = urisToSend.joinToString(",") { Uri.encode(it.toString()) }
                        navController.navigate("post_screen/$uriString")
                    }
                }
            )
        }
        // Ảnh lớn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color.Black),
            contentAlignment = Alignment.BottomCenter
        ) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Thanh công cụ overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x66000000))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_select_multiple),
                    contentDescription = "Chọn nhiều",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Chọn nhiều",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { 
                            isMultiSelectMode = !isMultiSelectMode
                            if (!isMultiSelectMode) {
                                selectedUris = emptySet()
                            }
                        }
                )
            }
        }
        // Grid ảnh nhỏ
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp),
            contentPadding = PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            itemsIndexed(imageUris) { idx, uri ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .border(
                            BorderStroke(
                                width = if (selectedIndex == idx || (isMultiSelectMode && uri in selectedUris)) 3.dp else 0.dp,
                                color = if (selectedIndex == idx || (isMultiSelectMode && uri in selectedUris)) Color(0xFF3797EF) else Color.Transparent
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { 
                            if (isMultiSelectMode) {
                                selectedUris = if (uri in selectedUris) {
                                    selectedUris - uri
                                } else {
                                    selectedUris + uri
                                }
                            } else {
                                selectedIndex = idx
                                selectedImageUri = uri
                            }
                        }
                ) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (isMultiSelectMode && uri in selectedUris) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x66000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
        // TabBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            tabs.forEachIndexed { idx, tab ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .clickable { selectedTab = idx }
                ) {
                    if (idx == 1) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = "Camera",
                            tint = if (selectedTab == idx) Color.Black else Color.Gray,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { 
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                        )
                    } else {
                        Text(
                            text = tab,
                            fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == idx) Color.Black else Color.Gray,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

private fun loadImages(context: android.content.Context, onImagesLoaded: (List<Uri>) -> Unit) {
    val uris = mutableListOf<Uri>()
    val projection = arrayOf(MediaStore.Images.Media._ID)
    val selection = "${MediaStore.Images.Media.DATA} LIKE ?"
    val selectionArgs = arrayOf("%DCIM%")
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
    
    context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        sortOrder
    )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val contentUri = Uri.withAppendedPath(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                id.toString()
            )
            uris.add(contentUri)
        }
    }
    onImagesLoaded(uris)
}

@Preview(showBackground = true)
@Composable
fun AddScreenPreview() {
    // Preview version without NavController
    AddScreen(navController = NavController(LocalContext.current))
}