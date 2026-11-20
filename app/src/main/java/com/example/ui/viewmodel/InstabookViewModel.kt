package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.FollowEdge
import com.example.data.model.PostComment
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.example.data.repository.InstabookRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InstabookViewModel(
    private val repository: InstabookRepository,
    val currentUserId: String
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toastEvents = _toastEvents.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    init {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null && currentUser.uid == currentUserId) {
            viewModelScope.launch {
                repository.ensureUserProfile(currentUser)
                    .onFailure { error ->
                        Log.w(TAG, "Profile initialization warning: ${error.message}")
                    }
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(errorFallback: String): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, errorFallback, rootError)
                emit(UiState.Error(rootError.message ?: errorFallback))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val currentProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId, currentUserId)
            .toUiStateFlow("Failed to load your profile")

    val feedPostsState: StateFlow<UiState<List<VideoPost>>> =
        repository.observeFeedPosts(currentUserId)
            .toUiStateFlow("Failed to load video feed")

    val publicCreatorsState: StateFlow<UiState<List<UserProfile>>> =
        repository.observePublicCreators(currentUserId)
            .toUiStateFlow("Failed to load creators")

    val notificationsState: StateFlow<UiState<List<AppNotification>>> =
        repository.observeNotifications(currentUserId)
            .toUiStateFlow("Failed to load notifications")

    val likedPostIds: StateFlow<Set<String>> =
        repository.observeLikedPostIds(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val followingIds: StateFlow<Set<String>> =
        repository.observeFollowingIds(currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val unreadNotificationsCount: StateFlow<Int> = notificationsState
        .map { state ->
            when (state) {
                is UiState.Success -> state.data.count { !it.seen }
                else -> 0
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )

    fun observeCreatorProfile(targetUserId: String): Flow<UiState<UserProfile?>> =
        repository.observeUserProfile(targetUserId, currentUserId)
            .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load creator"))
            }

    fun observeCreatorPosts(targetUserId: String): Flow<UiState<List<VideoPost>>> =
        repository.observeUserPosts(targetUserId, currentUserId)
            .map<List<VideoPost>, UiState<List<VideoPost>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load posts"))
            }

    fun observePostDetail(postId: String): Flow<UiState<VideoPost?>> =
        repository.observePost(postId, currentUserId)
            .map<VideoPost?, UiState<VideoPost?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load video"))
            }

    fun observeComments(postId: String): Flow<UiState<List<PostComment>>> =
        repository.observeComments(postId, currentUserId)
            .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error((firestoreError ?: error).message ?: "Failed to load comments"))
            }

    fun observeFollowers(targetUserId: String): Flow<List<FollowEdge>> =
        repository.observeFollowers(targetUserId, currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }

    fun observeFollowing(targetUserId: String): Flow<List<FollowEdge>> =
        repository.observeFollowing(targetUserId, currentUserId)
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(emptyList())
            }

    fun toggleLike(post: VideoPost) {
        val actor = (currentProfileState.value as? UiState.Success)?.data ?: return
        val isLiked = likedPostIds.value.contains(post.id)
        viewModelScope.launch {
            repository.toggleLike(post, actor, isLiked)
                .onFailure { _toastEvents.tryEmit(it.message ?: "Could not update like") }
        }
    }

    fun addComment(post: VideoPost, text: String, onSuccess: () -> Unit = {}) {
        val actor = (currentProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.addComment(post, actor, text)
                .onSuccess { onSuccess() }
                .onFailure { _toastEvents.tryEmit(it.message ?: "Could not post comment") }
        }
    }

    fun deleteComment(post: VideoPost, comment: PostComment) {
        viewModelScope.launch {
            repository.deleteComment(post, comment)
                .onFailure { _toastEvents.tryEmit(it.message ?: "Could not delete comment") }
        }
    }

    fun shareToCreator(post: VideoPost, recipient: UserProfile, onSuccess: () -> Unit = {}) {
        val sender = (currentProfileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.sharePostToCreator(post, sender, recipient)
                .onSuccess {
                    _toastEvents.tryEmit("Shared with @${recipient.username}")
                    onSuccess()
                }
                .onFailure { _toastEvents.tryEmit(it.message ?: "Could not share video") }
        }
    }

    fun recordExternalShare(postId: String) {
        viewModelScope.launch {
            repository.recordExternalShare(postId)
        }
    }

    fun toggleFollow(targetUser: UserProfile) {
        val current = (currentProfileState.value as? UiState.Success)?.data ?: return
        if (current.userId == targetUser.userId) return
        val isFollowing = followingIds.value.contains(targetUser.userId)
        viewModelScope.launch {
            repository.toggleFollow(current, targetUser, isFollowing)
                .onSuccess {
                    val label = if (isFollowing) "Unfollowed @${targetUser.username}" else "Following @${targetUser.username}"
                    _toastEvents.tryEmit(label)
                }
                .onFailure { _toastEvents.tryEmit(it.message ?: "Follow action failed") }
        }
    }

    fun createVideoPost(
        caption: String,
        videoUrl: String,
        thumbnailUrl: String,
        category: String,
        visibility: String,
        onSuccess: (String) -> Unit
    ) {
        val author = (currentProfileState.value as? UiState.Success)?.data
        if (author == null) {
            _toastEvents.tryEmit("Profile is still loading, please try again.")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            repository.createVideoPost(author, caption, videoUrl, thumbnailUrl, category, visibility)
                .onSuccess { postId ->
                    _isBusy.value = false
                    _toastEvents.tryEmit("Video published to Instabook!")
                    onSuccess(postId)
                }
                .onFailure {
                    _isBusy.value = false
                    _toastEvents.tryEmit(it.message ?: "Failed to publish video")
                }
        }
    }

    fun deletePost(post: VideoPost, onSuccess: () -> Unit = {}) {
        val current = (currentProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            repository.deleteVideoPost(post, current?.postsCount ?: 0L)
                .onSuccess {
                    _toastEvents.tryEmit("Video removed")
                    onSuccess()
                }
                .onFailure { _toastEvents.tryEmit(it.message ?: "Failed to delete video") }
        }
    }

    fun updateProfile(
        username: String,
        displayName: String,
        bio: String,
        avatarUrl: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            repository.updateProfile(username, displayName, bio, avatarUrl)
                .onSuccess {
                    _isBusy.value = false
                    _toastEvents.tryEmit("Profile updated")
                    onSuccess()
                }
                .onFailure {
                    _isBusy.value = false
                    _toastEvents.tryEmit(it.message ?: "Could not update profile")
                }
        }
    }

    fun markNotificationSeen(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationSeen(notificationId)
        }
    }

    fun markAllNotificationsSeen() {
        val list = (notificationsState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            repository.markAllNotificationsSeen(list)
                .onSuccess { _toastEvents.tryEmit("All notifications marked read") }
        }
    }

    private companion object {
        const val TAG = "InstabookVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
