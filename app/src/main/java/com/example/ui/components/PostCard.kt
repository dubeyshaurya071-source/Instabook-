package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.data.model.VideoPost
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookLikeRed
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue

@Composable
fun PostCard(
    post: VideoPost,
    currentUserId: String,
    isLiked: Boolean,
    isFollowingAuthor: Boolean,
    onAuthorClick: (String) -> Unit,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onShareClick: () -> Unit,
    onDeletePost: (() -> Unit)? = null,
    autoPlay: Boolean = false,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "like_scale"
    )

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, InstabookSoftBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                CreatorAvatar(
                    avatarUrl = post.authorAvatarUrl,
                    displayName = post.authorDisplayName,
                    size = 42.dp,
                    onClick = { onAuthorClick(post.authorId) }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick(post.authorId) }
                ) {
                    Text(
                        text = post.authorDisplayName.ifBlank { "Creator" },
                        style = MaterialTheme.typography.titleSmall,
                        color = InstabookInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "@${post.authorUsername} • ${formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = InstabookSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (post.authorId != currentUserId) {
                    FollowPillButton(
                        isFollowing = isFollowingAuthor,
                        onClick = onToggleFollow,
                        modifier = Modifier.testTag("follow_button_${post.authorId}")
                    )
                } else if (onDeletePost != null) {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Post options",
                                tint = InstabookSlate
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete Video", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeletePost()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Video Player
            Box(modifier = Modifier.fillMaxWidth()) {
                VideoPlayerBox(
                    videoUrl = post.videoUrl,
                    thumbnailUrl = post.thumbnailUrl,
                    autoPlay = autoPlay,
                    aspectRatio = 4f / 5f,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    color = InstabookBlue.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = post.category,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Actions Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = if (isLiked) InstabookLikeRed.copy(alpha = 0.1f) else InstabookIceBlue,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onToggleLike)
                        .testTag("like_button_${post.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isLiked) "Unlike video" else "Like video",
                            tint = if (isLiked) InstabookLikeRed else InstabookDeepBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .scale(likeScale)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCount(post.likesCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isLiked) InstabookLikeRed else InstabookDeepBlue
                        )
                    }
                }

                Surface(
                    color = InstabookIceBlue,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onOpenComments)
                        .testTag("comment_button_${post.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = InstabookDeepBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCount(post.commentsCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = InstabookDeepBlue
                        )
                    }
                }

                Surface(
                    color = InstabookIceBlue,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onShareClick)
                        .testTag("share_button_${post.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = "Share video",
                            tint = InstabookDeepBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatCount(post.sharesCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = InstabookDeepBlue
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = InstabookInk)) {
                        append("@${post.authorUsername} ")
                    }
                    withStyle(SpanStyle(color = InstabookInk)) {
                        append(post.caption)
                    }
                },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
