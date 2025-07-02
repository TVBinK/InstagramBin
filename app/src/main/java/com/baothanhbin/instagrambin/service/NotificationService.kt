package com.baothanhbin.instagrambin.service

import android.app.Application
import android.content.Context
import com.baothanhbin.instagrambin.model.Comment
import com.baothanhbin.instagrambin.model.Post
import com.baothanhbin.instagrambin.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class NotificationService(private val application: Application) {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    companion object {
        private const val FCM_SERVER_KEY = "YOUR_SERVER_KEY" // Thay bằng Server Key từ Firebase Console
        private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

        fun sendRawFCMNotification(context: Context, notification: Map<String, Any>) {
            Thread {
                try {
                    val url = java.net.URL(FCM_URL)
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Authorization", "key=$FCM_SERVER_KEY")
                    connection.doOutput = true
                    val json = org.json.JSONObject(notification)
                    connection.outputStream.use { os ->
                        os.write(json.toString().toByteArray())
                        os.flush()
                    }
                    val responseCode = connection.responseCode
                    if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                        println("Raw FCM notification sent successfully")
                    } else {
                        println("Failed to send raw FCM notification. Response code: $responseCode")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }.start()
        }
    }

    /**
     * Gửi notification khi có người like bài viết
     */
    suspend fun sendLikeNotification(post: Post, likerId: String) {
        try {
            // Không gửi notification nếu người like là chủ bài viết
            if (post.userId == likerId) return

            // Lấy thông tin người like
            val likerSnapshot = database.getReference("users").child(likerId).get().await()
            val liker = likerSnapshot.getValue(User::class.java) ?: return

            // Lấy FCM token của chủ bài viết
            val postOwnerSnapshot = database.getReference("users").child(post.userId).get().await()
            val postOwnerToken = postOwnerSnapshot.child("fcmToken").getValue(String::class.java) ?: return

            // Tạo nội dung notification
            val title = "${liker.username ?: "Người dùng"} đã thích bài viết của bạn"
            val body = "Nhấn để xem chi tiết"

            // Gửi notification
            sendFCMNotification(
                token = postOwnerToken,
                title = title,
                body = body,
                data = mapOf(
                    "type" to "like",
                    "postId" to post.postId,
                    "likerId" to likerId,
                    "likerUsername" to (liker.username ?: "")
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gửi notification khi có người unlike bài viết
     */
    suspend fun sendUnlikeNotification(post: Post, unlikerId: String) {
        // Thường không cần gửi notification khi unlike
        // Nhưng có thể implement nếu cần
    }

    /**
     * Gửi notification khi có người comment bài viết
     */
    suspend fun sendCommentNotification(post: Post, comment: Comment) {
        try {
            // Không gửi notification nếu người comment là chủ bài viết
            if (post.userId == comment.userId) return

            // Lấy thông tin người comment
            val commenterSnapshot = database.getReference("users").child(comment.userId).get().await()
            val commenter = commenterSnapshot.getValue(User::class.java) ?: return

            // Lấy FCM token của chủ bài viết
            val postOwnerSnapshot = database.getReference("users").child(post.userId).get().await()
            val postOwnerToken = postOwnerSnapshot.child("fcmToken").getValue(String::class.java) ?: return

            // Tạo nội dung notification
            val title = "${commenter.username ?: "Người dùng"} đã bình luận bài viết của bạn"
            val body = comment.content.take(50) + if (comment.content.length > 50) "..." else ""

            // Gửi notification
            sendFCMNotification(
                token = postOwnerToken,
                title = title,
                body = body,
                data = mapOf(
                    "type" to "comment",
                    "postId" to post.postId,
                    "commentId" to comment.commentId,
                    "commenterId" to comment.userId,
                    "commenterUsername" to (commenter.username ?: ""),
                    "commentContent" to comment.content
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gửi notification khi có người reply comment
     */
    suspend fun sendReplyNotification(originalComment: Comment, replyComment: Comment) {
        try {
            // Không gửi notification nếu người reply là chủ comment gốc
            if (originalComment.userId == replyComment.userId) return

            // Lấy thông tin người reply
            val replierSnapshot = database.getReference("users").child(replyComment.userId).get().await()
            val replier = replierSnapshot.getValue(User::class.java) ?: return

            // Lấy FCM token của chủ comment gốc
            val originalCommenterSnapshot = database.getReference("users").child(originalComment.userId).get().await()
            val originalCommenterToken = originalCommenterSnapshot.child("fcmToken").getValue(String::class.java) ?: return

            // Tạo nội dung notification
            val title = "${replier.username ?: "Người dùng"} đã trả lời bình luận của bạn"
            val body = replyComment.content.take(50) + if (replyComment.content.length > 50) "..." else ""

            // Gửi notification
            sendFCMNotification(
                token = originalCommenterToken,
                title = title,
                body = body,
                data = mapOf(
                    "type" to "reply",
                    "postId" to originalComment.postId,
                    "originalCommentId" to originalComment.commentId,
                    "replyCommentId" to replyComment.commentId,
                    "replierId" to replyComment.userId,
                    "replierUsername" to (replier.username ?: ""),
                    "replyContent" to replyComment.content
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Gửi FCM notification
     */
    private fun sendFCMNotification(token: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        Thread {
            try {
                val url = URL(FCM_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "key=$FCM_SERVER_KEY")

                val notification = JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("sound", "default")
                    put("badge", "1")
                }

                val json = JSONObject().apply {
                    put("to", token)
                    put("notification", notification)
                    if (data.isNotEmpty()) {
                        put("data", JSONObject().apply {
                            data.forEach { (key, value) ->
                                put(key, value)
                            }
                        })
                    }
                }

                connection.doOutput = true
                connection.outputStream.use { os ->
                    os.write(json.toString().toByteArray())
                    os.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    println("Notification sent successfully to $token")
                } else {
                    println("Failed to send notification. Response code: $responseCode")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Cập nhật FCM token cho user
     */
    suspend fun updateUserFCMToken(userId: String, token: String) {
        try {
            database.getReference("users")
                .child(userId)
                .child("fcmToken")
                .setValue(token)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Xóa FCM token khi user logout
     */
    suspend fun removeUserFCMToken(userId: String) {
        try {
            database.getReference("users")
                .child(userId)
                .child("fcmToken")
                .removeValue()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 