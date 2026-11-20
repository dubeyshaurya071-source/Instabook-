package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PostComment
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.example.ui.viewmodel.InstabookViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: VideoPost,
    viewModel: InstabookViewModel,
    onAuthorClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val commentsFlow = remember(post.id) { viewModel.observeComments(post.id) }
    val commentsState by commentsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    var commentText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Comments (${post.commentsCount})",
                style = MaterialTheme.typography.titleLarge,
                color = InstabookInk
            )
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = InstabookSoftBlue)

            when (val state = commentsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = InstabookBlue)
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Be the first to comment on this video!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InstabookSlate
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 340.dp)
                        ) {
                            items(state.data, key = { it.id }) { comment ->
                                CommentRow(
                                    comment = comment,
                                    canDelete = comment.authorId == viewModel.currentUserId,
                                    onAuthorClick = {
                                        onDismiss()
                                        onAuthorClick(comment.authorId)
                                    },
                                    onDelete = { viewModel.deleteComment(post, comment) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = InstabookSoftBlue)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 300) commentText = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InstabookBlue,
                        unfocusedBorderColor = InstabookSoftBlue,
                        focusedContainerColor = InstabookIceBlue.copy(alpha = 0.4f),
                        unfocusedContainerColor = InstabookIceBlue.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val trimmed = commentText.trim()
                        if (trimmed.isNotEmpty()) {
                            viewModel.addComment(post, trimmed) {
                                commentText = ""
                            }
                        }
                    },
                    enabled = commentText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InstabookBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment"
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentRow(
    comment: PostComment,
    canDelete: Boolean,
    onAuthorClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        CreatorAvatar(
            avatarUrl = comment.authorAvatarUrl,
            displayName = comment.authorDisplayName,
            size = 36.dp,
            onClick = onAuthorClick
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.authorUsername}",
                    style = MaterialTheme.typography.labelLarge,
                    color = InstabookDeepBlue,
                    modifier = Modifier.clickable(onClick = onAuthorClick)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatRelativeTime(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = InstabookSlate
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = InstabookInk
            )
        }
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete comment",
                    tint = InstabookSlate,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharePostBottomSheet(
    post: VideoPost,
    creators: List<UserProfile>,
    currentUserId: String,
    viewModel: InstabookViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var search by remember { mutableStateOf("") }
    val filteredCreators = remember(creators, search, currentUserId) {
        creators.filter {
            it.userId != currentUserId &&
                (search.isBlank() ||
                    it.displayName.contains(search, ignoreCase = true) ||
                    it.username.contains(search, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Share Video",
                style = MaterialTheme.typography.titleLarge,
                color = InstabookInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Send to another Instabook creator or share outside the app.",
                style = MaterialTheme.typography.bodySmall,
                color = InstabookSlate
            )
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = {
                    viewModel.recordExternalShare(post.id)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Check out @${post.authorUsername}'s video on Instabook: ${post.caption}\n${post.videoUrl}"
                        )
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share via"))
                    onDismiss()
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = InstabookBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share via Other Apps", color = InstabookBlue)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search Instabook creators...") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = InstabookBlue,
                    unfocusedBorderColor = InstabookSoftBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredCreators.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No other creators found yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InstabookSlate
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                ) {
                    items(filteredCreators, key = { it.userId }) { creator ->
                        Surface(
                            color = InstabookIceBlue.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                CreatorAvatar(
                                    avatarUrl = creator.avatarUrl,
                                    displayName = creator.displayName,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = creator.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InstabookInk
                                    )
                                    Text(
                                        text = "@${creator.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InstabookSlate
                                    )
                                }
                                Button(
                                    onClick = {
                                        viewModel.shareToCreator(post, creator) {
                                            onDismiss()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = InstabookBlue,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Send", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
