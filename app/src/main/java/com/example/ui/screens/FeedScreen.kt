package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.example.ui.components.EmptyStateView
import com.example.ui.components.InstabookWordmark
import com.example.ui.components.PostCard
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.example.ui.theme.InstabookSurfaceTint
import com.example.ui.viewmodel.InstabookViewModel

@Composable
fun FeedScreen(
    viewModel: InstabookViewModel,
    onOpenNotifications: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenComments: (VideoPost) -> Unit,
    onOpenShare: (VideoPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val creatorsState by viewModel.publicCreatorsState.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) } // 0 = For You, 1 = Following

    val creatorsMap: Map<String, UserProfile> = remember(creatorsState) {
        (creatorsState as? UiState.Success)?.data?.associateBy { it.userId } ?: emptyMap()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InstabookSurfaceTint)
    ) {
        // Top Brand Bar
        Surface(
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InstabookWordmark()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier.testTag("feed_notifications_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(containerColor = InstabookBlue) {
                                            Text(
                                                text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unreadCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "Notifications",
                                    tint = InstabookDeepBlue
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // For You / Following Pill Switcher
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FeedFilterChip(
                        label = "For You",
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("feed_tab_for_you")
                    )
                    FeedFilterChip(
                        label = "Following",
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("feed_tab_following")
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = feedState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = InstabookBlue)
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is UiState.Success -> {
                    val displayedPosts = remember(state.data, selectedTab, followingIds) {
                        if (selectedTab == 0) {
                            state.data
                        } else {
                            state.data.filter {
                                followingIds.contains(it.authorId) || it.authorId == viewModel.currentUserId
                            }
                        }
                    }

                    if (displayedPosts.isEmpty()) {
                        EmptyStateView(
                            title = if (selectedTab == 1) "No Videos From Followed Creators" else "No Videos Shared Yet",
                            subtitle = if (selectedTab == 1) {
                                "Explore creators and tap Follow to see their newest videos right here."
                            } else {
                                "Be the first creator to post a video on Instabook!"
                            },
                            actionLabel = "Share a Video",
                            onAction = onOpenCreate,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 600.dp)
                        ) {
                            items(displayedPosts, key = { it.id }) { post ->
                                val authorProfile = creatorsMap[post.authorId] ?: UserProfile(
                                    id = post.authorId,
                                    userId = post.authorId,
                                    username = post.authorUsername,
                                    displayName = post.authorDisplayName,
                                    avatarUrl = post.authorAvatarUrl
                                )
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
                                        { viewModel.deletePost(post) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) InstabookBlue else InstabookIceBlue,
        shape = RoundedCornerShape(50),
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else InstabookDeepBlue,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
        )
    }
}
