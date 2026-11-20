package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.AppNotification
import com.example.data.model.FollowEdge
import com.example.data.model.NotificationType
import com.example.data.model.PostComment
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class InstabookRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun <T> Flow<T>.logFirestoreErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<UserProfile> {
        val path = "users/${user.uid}"
        return try {
            val uid = requireUserId()
            check(uid == user.uid) { "Authenticated user mismatch." }
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                if (existing != null) {
                    return Result.success(existing)
                }
            }

            val rawDisplayName = (user.displayName?.trim().takeIf { !it.isNullOrBlank() } ?: "Creator")
                .take(50)
            val rawHandleBase = (user.email?.substringBefore("@") ?: rawDisplayName)
                .lowercase()
                .replace(Regex("[^a-z0-9_.]"), "")
                .take(24)
            val safeUsername = if (rawHandleBase.length >= 2) rawHandleBase else "user_${uid.take(6)}"
            val avatar = (user.photoUrl?.toString() ?: "").take(1000)

            val profile = UserProfile(
                id = uid,
                userId = uid,
                username = safeUsername,
                displayName = rawDisplayName,
                bio = "Sharing videos on Instabook",
                avatarUrl = avatar,
                profileVisibility = "public"
            )
            docRef.set(profile.toCreateMap()).await()
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(targetUserId: String, currentUserId: String): Flow<UserProfile?> {
        val path = "users/$targetUserId"
        return db.collection("users").document(targetUserId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.GET, path, currentUserId)
    }

    fun observePublicCreators(currentUserId: String): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .whereEqualTo("profileVisibility", "public")
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.followersCount }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeFeedPosts(currentUserId: String): Flow<List<VideoPost>> {
        val path = "posts"
        return db.collection("posts")
            .whereEqualTo("visibility", "public")
            .limit(100)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VideoPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeUserPosts(targetUserId: String, currentUserId: String): Flow<List<VideoPost>> {
        val path = "posts"
        val query = if (targetUserId == currentUserId) {
            db.collection("posts")
                .whereEqualTo("authorId", targetUserId)
                .limit(100)
        } else {
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .limit(100)
        }
        return query.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    VideoPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                    .filter { it.authorId == targetUserId }
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observePost(postId: String, currentUserId: String): Flow<VideoPost?> {
        val path = "posts/$postId"
        return db.collection("posts").document(postId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    VideoPost::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.GET, path, currentUserId)
    }

    fun observeLikedPostIds(currentUserId: String): Flow<Set<String>> {
        val path = "users/$currentUserId/likedPosts"
        return db.collection("users").document(currentUserId).collection("likedPosts")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeFollowingIds(currentUserId: String): Flow<Set<String>> {
        val path = "users/$currentUserId/following"
        return db.collection("users").document(currentUserId).collection("following")
            .snapshots()
            .map { snapshot -> snapshot.documents.map { it.id }.toSet() }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeFollowers(targetUserId: String, currentUserId: String): Flow<List<FollowEdge>> {
        val path = "users/$targetUserId/followers"
        return db.collection("users").document(targetUserId).collection("followers")
            .whereEqualTo("profileVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FollowEdge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeFollowing(targetUserId: String, currentUserId: String): Flow<List<FollowEdge>> {
        val path = "users/$targetUserId/following"
        return db.collection("users").document(targetUserId).collection("following")
            .whereEqualTo("profileVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    FollowEdge::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeComments(postId: String, currentUserId: String): Flow<List<PostComment>> {
        val path = "posts/$postId/comments"
        return db.collection("posts").document(postId).collection("comments")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PostComment::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    fun observeNotifications(currentUserId: String): Flow<List<AppNotification>> {
        val path = "users/$currentUserId/notifications"
        return db.collection("users").document(currentUserId).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(60)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AppNotification::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .logFirestoreErrors(OperationType.LIST, path, currentUserId)
    }

    suspend fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String
    ): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_.]"), "").take(30)
            val cleanDisplay = displayName.trim().take(50)
            val cleanBio = bio.trim().take(200)
            val cleanAvatar = avatarUrl.trim().take(1000)
            require(cleanUsername.length >= 2) { "Username must be at least 2 characters." }
            require(cleanDisplay.isNotEmpty()) { "Display name cannot be empty." }

            val docRef = db.collection("users").document(uid)
            path = docRef.path
            docRef.update(
                mapOf(
                    "username" to cleanUsername,
                    "displayName" to cleanDisplay,
                    "bio" to cleanBio,
                    "avatarUrl" to cleanAvatar,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun createVideoPost(
        author: UserProfile,
        caption: String,
        videoUrl: String,
        thumbnailUrl: String,
        category: String,
        visibility: String
    ): Result<String> {
        var path = "posts"
        return try {
            val uid = requireUserId()
            check(uid == author.userId) { "Author UID mismatch." }
            val cleanCaption = caption.trim().take(500)
            val cleanVideoUrl = videoUrl.trim().take(2000)
            val cleanThumb = thumbnailUrl.trim().take(2000)
            val cleanCategory = category.trim().ifEmpty { "Trending" }.take(30)
            val safeVisibility = if (visibility == "private") "private" else "public"

            require(cleanCaption.isNotEmpty()) { "Please write a caption for your video." }
            require(cleanVideoUrl.isNotEmpty()) { "Please pick a video or enter a video URL." }

            val postRef = db.collection("posts").document()
            path = postRef.path
            val userRef = db.collection("users").document(uid)

            val post = VideoPost(
                authorId = uid,
                authorUsername = author.username.ifBlank { "creator" }.take(30),
                authorDisplayName = author.displayName.ifBlank { "Creator" }.take(50),
                authorAvatarUrl = author.avatarUrl.take(1000),
                caption = cleanCaption,
                videoUrl = cleanVideoUrl,
                thumbnailUrl = cleanThumb,
                category = cleanCategory,
                visibility = safeVisibility
            )

            db.batch().apply {
                set(postRef, post.toCreateMap())
                update(
                    userRef,
                    mapOf(
                        "postsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.commit().await()

            Result.success(postRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVideoPost(post: VideoPost, currentPostsCount: Long): Result<Unit> {
        val path = "posts/${post.id}"
        return try {
            val uid = requireUserId()
            require(post.id.isNotBlank()) { "Post ID missing." }
            check(post.authorId == uid) { "Only the author can delete this video." }

            val postRef = db.collection("posts").document(post.id)
            val userRef = db.collection("users").document(uid)

            db.batch().apply {
                delete(postRef)
                if (currentPostsCount > 0L) {
                    update(
                        userRef,
                        mapOf(
                            "postsCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.commit().await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(
        post: VideoPost,
        actor: UserProfile,
        currentlyLiked: Boolean
    ): Result<Unit> {
        val path = "posts/${post.id}/likes/${actor.userId}"
        return try {
            val uid = requireUserId()
            check(uid == actor.userId) { "Actor UID mismatch." }
            require(post.id.isNotBlank()) { "Post ID missing." }

            val postRef = db.collection("posts").document(post.id)
            val postLikeRef = postRef.collection("likes").document(uid)
            val userLikedRef = db.collection("users").document(uid)
                .collection("likedPosts").document(post.id)

            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(postLikeRef)
                batch.delete(userLikedRef)
                if (post.likesCount > 0L) {
                    batch.update(
                        postRef,
                        mapOf(
                            "likesCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                val likePayload = mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                batch.set(postLikeRef, likePayload)
                batch.set(userLikedRef, likePayload)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (post.authorId != uid) {
                    val notifRef = db.collection("users").document(post.authorId)
                        .collection("notifications").document()
                    val notif = AppNotification(
                        recipientId = post.authorId,
                        actorId = uid,
                        actorUsername = actor.username.ifBlank { "creator" }.take(30),
                        actorDisplayName = actor.displayName.ifBlank { "Creator" }.take(50),
                        actorAvatarUrl = actor.avatarUrl.take(1000),
                        type = NotificationType.LIKE.wireValue,
                        postId = post.id,
                        message = "${actor.displayName.ifBlank { actor.username }} liked your video"
                            .take(250),
                        seen = false
                    )
                    batch.set(notifRef, notif.toCreateMap())
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addComment(
        post: VideoPost,
        actor: UserProfile,
        text: String
    ): Result<Unit> {
        val path = "posts/${post.id}/comments"
        return try {
            val uid = requireUserId()
            check(uid == actor.userId) { "Actor UID mismatch." }
            require(post.id.isNotBlank()) { "Post ID missing." }
            val cleanText = text.trim().take(300)
            require(cleanText.isNotEmpty()) { "Comment cannot be empty." }

            val postRef = db.collection("posts").document(post.id)
            val commentRef = postRef.collection("comments").document()
            val comment = PostComment(
                postId = post.id,
                authorId = uid,
                authorUsername = actor.username.ifBlank { "creator" }.take(30),
                authorDisplayName = actor.displayName.ifBlank { "Creator" }.take(50),
                authorAvatarUrl = actor.avatarUrl.take(1000),
                text = cleanText,
                visibility = "public"
            )

            val batch = db.batch()
            batch.set(commentRef, comment.toCreateMap())
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (post.authorId != uid) {
                val notifRef = db.collection("users").document(post.authorId)
                    .collection("notifications").document()
                val preview = cleanText.take(70)
                val notif = AppNotification(
                    recipientId = post.authorId,
                    actorId = uid,
                    actorUsername = actor.username.ifBlank { "creator" }.take(30),
                    actorDisplayName = actor.displayName.ifBlank { "Creator" }.take(50),
                    actorAvatarUrl = actor.avatarUrl.take(1000),
                    type = NotificationType.COMMENT.wireValue,
                    postId = post.id,
                    message = "${actor.displayName.ifBlank { actor.username }} commented: \"$preview\""
                        .take(250),
                    seen = false
                )
                batch.set(notifRef, notif.toCreateMap())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteComment(post: VideoPost, comment: PostComment): Result<Unit> {
        val path = "posts/${post.id}/comments/${comment.id}"
        return try {
            val uid = requireUserId()
            check(comment.authorId == uid) { "Only comment author can delete." }
            val postRef = db.collection("posts").document(post.id)
            val commentRef = postRef.collection("comments").document(comment.id)
            db.batch().apply {
                delete(commentRef)
                if (post.commentsCount > 0L) {
                    update(
                        postRef,
                        mapOf(
                            "commentsCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun sharePostToCreator(
        post: VideoPost,
        sender: UserProfile,
        recipient: UserProfile
    ): Result<Unit> {
        val path = "users/${recipient.userId}/notifications"
        return try {
            val uid = requireUserId()
            check(uid == sender.userId) { "Sender UID mismatch." }
            require(post.id.isNotBlank()) { "Post ID missing." }

            val postRef = db.collection("posts").document(post.id)
            val notifRef = db.collection("users").document(recipient.userId)
                .collection("notifications").document()

            val notif = AppNotification(
                recipientId = recipient.userId,
                actorId = uid,
                actorUsername = sender.username.ifBlank { "creator" }.take(30),
                actorDisplayName = sender.displayName.ifBlank { "Creator" }.take(50),
                actorAvatarUrl = sender.avatarUrl.take(1000),
                type = NotificationType.SHARE.wireValue,
                postId = post.id,
                message = "${sender.displayName.ifBlank { sender.username }} shared @${post.authorUsername}'s video with you"
                    .take(250),
                seen = false
            )

            db.batch().apply {
                set(notifRef, notif.toCreateMap())
                update(
                    postRef,
                    mapOf(
                        "sharesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.commit().await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordExternalShare(postId: String): Result<Unit> {
        val path = "posts/$postId"
        return try {
            requireUserId()
            require(postId.isNotBlank()) { "Post ID missing." }
            db.collection("posts").document(postId).update(
                mapOf(
                    "sharesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(
        currentUser: UserProfile,
        targetUser: UserProfile,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val path = "users/${targetUser.userId}/followers/${currentUser.userId}"
        return try {
            val uid = requireUserId()
            check(uid == currentUser.userId) { "Current user mismatch." }
            require(uid != targetUser.userId) { "You cannot follow yourself." }

            val targetRef = db.collection("users").document(targetUser.userId)
            val currentRef = db.collection("users").document(uid)
            val followerEdgeRef = targetRef.collection("followers").document(uid)
            val followingEdgeRef = currentRef.collection("following").document(targetUser.userId)

            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followerEdgeRef)
                batch.delete(followingEdgeRef)
                if (targetUser.followersCount > 0L) {
                    batch.update(
                        targetRef,
                        mapOf(
                            "followersCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                if (currentUser.followingCount > 0L) {
                    batch.update(
                        currentRef,
                        mapOf(
                            "followingCount" to FieldValue.increment(-1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            } else {
                val followerEdge = FollowEdge(
                    userId = uid,
                    targetUserId = targetUser.userId,
                    username = currentUser.username.ifBlank { "creator" }.take(30),
                    displayName = currentUser.displayName.ifBlank { "Creator" }.take(50),
                    avatarUrl = currentUser.avatarUrl.take(1000),
                    profileVisibility = "public"
                )
                val followingEdge = FollowEdge(
                    userId = targetUser.userId,
                    targetUserId = targetUser.userId,
                    username = targetUser.username.ifBlank { "creator" }.take(30),
                    displayName = targetUser.displayName.ifBlank { "Creator" }.take(50),
                    avatarUrl = targetUser.avatarUrl.take(1000),
                    profileVisibility = "public"
                )
                batch.set(followerEdgeRef, followerEdge.toCreateMap())
                batch.set(followingEdgeRef, followingEdge.toCreateMap())
                batch.update(
                    targetRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    currentRef,
                    mapOf(
                        "followingCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val notifRef = targetRef.collection("notifications").document()
                val notif = AppNotification(
                    recipientId = targetUser.userId,
                    actorId = uid,
                    actorUsername = currentUser.username.ifBlank { "creator" }.take(30),
                    actorDisplayName = currentUser.displayName.ifBlank { "Creator" }.take(50),
                    actorAvatarUrl = currentUser.avatarUrl.take(1000),
                    type = NotificationType.FOLLOW.wireValue,
                    postId = "",
                    message = "${currentUser.displayName.ifBlank { currentUser.username }} started following you"
                        .take(250),
                    seen = false
                )
                batch.set(notifRef, notif.toCreateMap())
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationSeen(notificationId: String): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notifications/$notificationId"
        return try {
            val uid = requireUserId()
            require(notificationId.isNotBlank()) { "Notification ID missing." }
            val ref = db.collection("users").document(uid)
                .collection("notifications").document(notificationId)
            path = ref.path
            ref.update("seen", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsSeen(notifications: List<AppNotification>): Result<Unit> {
        var path = "users/${auth.currentUser?.uid ?: "unauthenticated"}/notifications"
        return try {
            val uid = requireUserId()
            val unread = notifications.filter { !it.seen && it.id.isNotBlank() }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.take(50).forEach { item ->
                val ref = db.collection("users").document(uid)
                    .collection("notifications").document(item.id)
                path = ref.path
                batch.update(ref, "seen", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }
}
