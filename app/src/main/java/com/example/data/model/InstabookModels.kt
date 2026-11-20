package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

val InstabookCategories = listOf(
    "Trending",
    "Music",
    "Comedy",
    "Tech",
    "Travel",
    "Art",
    "Sports",
    "Lifestyle"
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val profileVisibility: String = "public",
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "username" to username,
        "displayName" to displayName,
        "bio" to bio,
        "avatarUrl" to avatarUrl,
        "profileVisibility" to profileVisibility,
        "followersCount" to 0L,
        "followingCount" to 0L,
        "postsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class FollowEdge(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetUserId: String = "",
    val username: String = "",
    val displayName: String = "",
    val avatarUrl: String = "",
    val profileVisibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "targetUserId" to targetUserId,
        "username" to username,
        "displayName" to displayName,
        "avatarUrl" to avatarUrl,
        "profileVisibility" to "public",
        "createdAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class VideoPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val caption: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String = "",
    val category: String = "Trending",
    val visibility: String = "public",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val viewsCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "caption" to caption,
        "videoUrl" to videoUrl,
        "thumbnailUrl" to thumbnailUrl,
        "category" to category,
        "visibility" to visibility,
        "likesCount" to 0L,
        "commentsCount" to 0L,
        "sharesCount" to 0L,
        "viewsCount" to 0L,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorUsername: String = "",
    val authorDisplayName: String = "",
    val authorAvatarUrl: String = "",
    val text: String = "",
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "postId" to postId,
        "authorId" to authorId,
        "authorUsername" to authorUsername,
        "authorDisplayName" to authorDisplayName,
        "authorAvatarUrl" to authorAvatarUrl,
        "text" to text,
        "visibility" to "public",
        "createdAt" to FieldValue.serverTimestamp()
    )
}

enum class NotificationType(val wireValue: String) {
    LIKE("like"),
    COMMENT("comment"),
    FOLLOW("follow"),
    SHARE("share");

    companion object {
        fun fromWire(raw: String?): NotificationType =
            entries.firstOrNull { it.wireValue == raw } ?: LIKE
    }
}

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorUsername: String = "",
    val actorDisplayName: String = "",
    val actorAvatarUrl: String = "",
    val type: String = NotificationType.LIKE.wireValue,
    val postId: String = "",
    val message: String = "",
    val seen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    val notificationType: NotificationType
        get() = NotificationType.fromWire(type)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "recipientId" to recipientId,
        "actorId" to actorId,
        "actorUsername" to actorUsername,
        "actorDisplayName" to actorDisplayName,
        "actorAvatarUrl" to actorAvatarUrl,
        "type" to type,
        "postId" to postId,
        "message" to message,
        "seen" to false,
        "createdAt" to FieldValue.serverTimestamp()
    )
}
