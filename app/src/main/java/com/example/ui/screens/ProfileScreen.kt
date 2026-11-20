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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.FollowEdge
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
import com.example.ui.auth.signOut
import com.example.ui.components.CreatorAvatar
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FollowPillButton
import com.example.ui.components.formatCount
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookSkyBlue
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.example.ui.theme.InstabookSurfaceTint
import com.example.ui.viewmodel.InstabookViewModel

@Composable
fun ProfileScreen(
    targetUserId: String,
    viewModel: InstabookViewModel,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenVideoDetail: (String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val isOwnProfile = targetUserId == viewModel.currentUserId
    val profileFlow = remember(targetUserId) { viewModel.observeCreatorProfile(targetUserId) }
    val postsFlow = remember(targetUserId) { viewModel.observeCreatorPosts(targetUserId) }

    val profileState by profileFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val postsState by postsFlow.collectAsStateWithLifecycle(initialValue = UiState.Loading)
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var followSheetMode by remember { mutableStateOf<String?>(null) } // "followers" or "following"

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
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showBackButton) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = InstabookDeepBlue
                            )
                        }
                    }
                    val headerTitle = (profileState as? UiState.Success)?.data?.let {
                        "@${it.username}"
                    } ?: "Profile"
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = InstabookDeepBlue,
                        modifier = Modifier.padding(start = if (showBackButton) 4.dp else 8.dp)
                    )
                }

                if (isOwnProfile) {
                    IconButton(
                        onClick = { signOut(credentialManager, scope) },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = stringResource(R.string.sign_out),
                            tint = InstabookDeepBlue
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
            when (val pState = profileState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = InstabookBlue)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(pState.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val profile = pState.data
                    if (profile == null) {
                        EmptyStateView(
                            title = "Creator Not Found",
                            subtitle = "This profile may still be setting up."
                        )
                    } else {
                        val userPosts = (postsState as? UiState.Success)?.data.orEmpty()
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 650.dp)
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                ProfileHeaderCard(
                                    profile = profile,
                                    isOwnProfile = isOwnProfile,
                                    isFollowing = followingIds.contains(profile.userId),
                                    onEditProfile = { showEditDialog = true },
                                    onToggleFollow = { viewModel.toggleFollow(profile) },
                                    onOpenFollowers = { followSheetMode = "followers" },
                                    onOpenFollowing = { followSheetMode = "following" }
                                )
                            }

                            item(span = { GridItemSpan(2) }) {
                                Text(
                                    text = "Videos (${userPosts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = InstabookInk,
                                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                                )
                            }

                            if (userPosts.isEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    EmptyStateView(
                                        title = if (isOwnProfile) "No Videos Yet" else "No Public Videos",
                                        subtitle = if (isOwnProfile) {
                                            "Share your first video to build your Instabook creator profile."
                                        } else {
                                            "@${profile.username} hasn't posted any public videos yet."
                                        },
                                        actionLabel = if (isOwnProfile) "Post Video" else null,
                                        onAction = if (isOwnProfile) onOpenCreate else null
                                    )
                                }
                            } else {
                                items(userPosts, key = { it.id }) { post ->
                                    ProfileVideoGridItem(
                                        post = post,
                                        onClick = { onOpenVideoDetail(post.id) }
                                    )
                                }
                            }
                        }

                        if (showEditDialog) {
                            EditProfileDialog(
                                profile = profile,
                                onDismiss = { showEditDialog = false },
                                onSave = { username, displayName, bio, avatarUrl ->
                                    viewModel.updateProfile(username, displayName, bio, avatarUrl) {
                                        showEditDialog = false
                                    }
                                }
                            )
                        }

                        if (followSheetMode != null) {
                            FollowListBottomSheet(
                                mode = followSheetMode!!,
                                targetUserId = profile.userId,
                                viewModel = viewModel,
                                onCreatorClick = { uid ->
                                    followSheetMode = null
                                    onOpenCreatorProfile(uid)
                                },
                                onDismiss = { followSheetMode = null }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    profile: UserProfile,
    isOwnProfile: Boolean,
    isFollowing: Boolean,
    onEditProfile: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, InstabookSoftBlue),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Blue gradient cover banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(InstabookDeepBlue, InstabookBlue, InstabookSkyBlue)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CreatorAvatar(
                        avatarUrl = profile.avatarUrl,
                        displayName = profile.displayName,
                        size = 68.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.weight(1f)
                    ) {
                        ProfileStatColumn(
                            count = profile.postsCount,
                            label = "Posts",
                            onClick = {}
                        )
                        ProfileStatColumn(
                            count = profile.followersCount,
                            label = "Followers",
                            onClick = onOpenFollowers,
                            modifier = Modifier.testTag("followers_count_button")
                        )
                        ProfileStatColumn(
                            count = profile.followingCount,
                            label = "Following",
                            onClick = onOpenFollowing,
                            modifier = Modifier.testTag("following_count_button")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = InstabookInk
                )
                Text(
                    text = "@${profile.username}",
                    style = MaterialTheme.typography.labelLarge,
                    color = InstabookBlue
                )
                if (profile.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = InstabookSlate
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isOwnProfile) {
                    OutlinedButton(
                        onClick = onEditProfile,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = InstabookIceBlue,
                            contentColor = InstabookDeepBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.edit_profile),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                } else {
                    FollowPillButton(
                        isFollowing = isFollowing,
                        onClick = onToggleFollow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("profile_follow_button")
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(
    count: Long,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            text = formatCount(count),
            style = MaterialTheme.typography.titleLarge,
            color = InstabookInk
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = InstabookSlate
        )
    }
}

@Composable
private fun ProfileVideoGridItem(
    post: VideoPost,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, InstabookSoftBlue),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(InstabookBlue, InstabookDeepBlue)
                    )
                )
        ) {
            if (post.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = post.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatCount(post.likesCount),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var username by remember { mutableStateOf(profile.username) }
    var displayName by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var avatarUrl by remember { mutableStateOf(profile.avatarUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = stringResource(R.string.edit_profile),
                style = MaterialTheme.typography.titleLarge,
                color = InstabookDeepBlue
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 50) displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = InstabookBlue)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { if (it.length <= 30) username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = InstabookBlue)
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = InstabookBlue)
                )
                OutlinedTextField(
                    value = avatarUrl,
                    onValueChange = { if (it.length <= 1000) avatarUrl = it },
                    label = { Text("Avatar Image URL (optional)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = InstabookBlue)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(username, displayName, bio, avatarUrl) },
                enabled = username.trim().length >= 2 && displayName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = InstabookBlue)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = InstabookSlate)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowListBottomSheet(
    mode: String,
    targetUserId: String,
    viewModel: InstabookViewModel,
    onCreatorClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val flow = remember(mode, targetUserId) {
        if (mode == "followers") viewModel.observeFollowers(targetUserId)
        else viewModel.observeFollowing(targetUserId)
    }
    val edges by flow.collectAsStateWithLifecycle(initialValue = emptyList<FollowEdge>())

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
                text = if (mode == "followers") "Followers" else "Following",
                style = MaterialTheme.typography.titleLarge,
                color = InstabookDeepBlue
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (edges.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == "followers") "No followers yet." else "Not following anyone yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = InstabookSlate
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                ) {
                    items(edges, key = { it.id }) { edge ->
                        Surface(
                            color = InstabookIceBlue.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCreatorClick(edge.userId) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                CreatorAvatar(
                                    avatarUrl = edge.avatarUrl,
                                    displayName = edge.displayName,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = edge.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = InstabookInk
                                    )
                                    Text(
                                        text = "@${edge.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = InstabookSlate
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
