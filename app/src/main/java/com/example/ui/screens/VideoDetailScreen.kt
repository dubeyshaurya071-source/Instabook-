package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PostCard
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookSurfaceTint
import com.example.ui.viewmodel.InstabookViewModel

@Composable
fun VideoDetailScreen(
    postId: String,
    viewModel: InstabookViewModel,
    onBack: () -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenComments: (VideoPost) -> Unit,
    onOpenShare: (VideoPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val postFlow = remember(postId) { viewModel.observePostDetail(postId) }
    val postState by postFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val creatorsState by viewModel.publicCreatorsState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    val creatorsMap: Map<String, UserProfile> = remember(creatorsState) {
        (creatorsState as? UiState.Success)?.data?.associateBy { it.userId } ?: emptyMap()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InstabookSurfaceTint)
    ) {
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = InstabookDeepBlue
                    )
                }
                Text(
                    text = "Video Reel",
                    style = MaterialTheme.typography.titleLarge,
                    color = InstabookDeepBlue
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = postState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = InstabookBlue)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val post = state.data
                    if (post == null) {
                        EmptyStateView(
                            title = "Video Unavailable",
                            subtitle = "This video may have been deleted by its creator.",
                            actionLabel = "Go Back",
                            onAction = onBack
                        )
                    } else {
                        val authorProfile = creatorsMap[post.authorId] ?: UserProfile(
                            id = post.authorId,
                            userId = post.authorId,
                            username = post.authorUsername,
                            displayName = post.authorDisplayName,
                            avatarUrl = post.authorAvatarUrl
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(14.dp)
                                .widthIn(max = 600.dp)
                        ) {
                            PostCard(
                                post = post,
                                currentUserId = viewModel.currentUserId,
                                isLiked = likedIds.contains(post.id),
                                isFollowingAuthor = followingIds.contains(post.authorId),
                                onAuthorClick = onOpenCreatorProfile,
                                onToggleFollow = { viewModel.toggleFollow(authorProfile) },
                                onToggleLike = { viewModel.toggleLike(post) },
                                onOpenComments = { onOpenComments(post) },
                                onShareClick = { onOpenShare(post) },
                                onDeletePost = if (post.authorId == viewModel.currentUserId) {
                                    { viewModel.deletePost(post) { onBack() } }
                                } else null,
                                autoPlay = true
                            )
                        }
                    }
                }
            }
        }
    }
}
