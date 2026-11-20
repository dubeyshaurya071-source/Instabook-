package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.NotificationType
import com.example.data.model.UiState
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookLikeRed
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.example.ui.theme.InstabookSuccess
import com.example.ui.theme.InstabookSurfaceTint
import com.example.ui.viewmodel.InstabookViewModel

@Composable
fun NotificationsScreen(
    viewModel: InstabookViewModel,
    onOpenPost: (String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InstabookSurfaceTint)
    ) {
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.headlineSmall,
                        color = InstabookDeepBlue
                    )
                    if (unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = InstabookBlue,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "$unreadCount new",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (unreadCount > 0) {
                    TextButton(
                        onClick = { viewModel.markAllNotificationsSeen() },
                        modifier = Modifier.testTag("mark_all_read_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DoneAll,
                            contentDescription = null,
                            tint = InstabookBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.mark_all_read),
                            color = InstabookBlue
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = notificationsState) {
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
                    if (state.data.isEmpty()) {
                        EmptyStateView(
                            title = "You're All Caught Up",
                            subtitle = "Real-time notifications for likes, comments, shares, and new followers appear here.",
                            actionLabel = "Explore Videos",
                            onAction = onOpenExplore,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 600.dp)
                        ) {
                            items(state.data, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    onClick = {
                                        if (!notification.seen) {
                                            viewModel.markNotificationSeen(notification.id)
                                        }
                                        if (notification.postId.isNotBlank()) {
                                            onOpenPost(notification.postId)
                                        } else {
                                            onOpenCreatorProfile(notification.actorId)
                                        }
                                    },
                                    onAvatarClick = {
                                        if (!notification.seen) {
                                            viewModel.markNotificationSeen(notification.id)
                                        }
                                        onOpenCreatorProfile(notification.actorId)
                                    }
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
private fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val (badgeIcon, badgeColor) = when (notification.notificationType) {
        NotificationType.LIKE -> Icons.Filled.Favorite to InstabookLikeRed
        NotificationType.COMMENT -> Icons.Filled.ChatBubble to InstabookBlue
        NotificationType.FOLLOW -> Icons.Filled.PersonAdd to InstabookSuccess
        NotificationType.SHARE -> Icons.AutoMirrored.Filled.Send to InstabookDeepBlue
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.seen) Color.White else InstabookIceBlue
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (notification.seen) InstabookSoftBlue else InstabookBlue.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_card_${notification.id}")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(modifier = Modifier.size(48.dp)) {
                CreatorAvatar(
                    avatarUrl = notification.actorAvatarUrl,
                    displayName = notification.actorDisplayName,
                    size = 44.dp,
                    onClick = onAvatarClick
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InstabookInk
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "@${notification.actorUsername} • ${formatRelativeTime(notification.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = InstabookSlate
                )
            }

            if (!notification.seen) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(InstabookBlue)
                )
            }
        }
    }
}
