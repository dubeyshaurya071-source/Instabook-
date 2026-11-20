package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.InstabookCategories
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VideoPost
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
fun ExploreScreen(
    viewModel: InstabookViewModel,
    onOpenVideoDetail: (String) -> Unit,
    onOpenCreatorProfile: (String) -> Unit,
    onOpenCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val feedState by viewModel.feedPostsState.collectAsStateWithLifecycle()
    val creatorsState by viewModel.publicCreatorsState.collectAsStateWithLifecycle()
    val followingIds by viewModel.followingIds.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val categories = remember { listOf("All") + InstabookCategories }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(InstabookSurfaceTint)
    ) {
        // Search & Categories Header
        Surface(color = Color.White, shadowElevation = 2.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Explore",
                    style = MaterialTheme.typography.headlineSmall,
                    color = InstabookDeepBlue
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search videos, tags, or creators...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = InstabookBlue
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear search",
                                    tint = InstabookSlate
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = InstabookBlue,
                        unfocusedBorderColor = InstabookSoftBlue,
                        focusedContainerColor = InstabookIceBlue.copy(alpha = 0.4f),
                        unfocusedContainerColor = InstabookIceBlue.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = InstabookBlue,
                                selectedLabelColor = Color.White,
                                containerColor = InstabookIceBlue,
                                labelColor = InstabookDeepBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = InstabookSoftBlue
                            )
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columnCount = if (maxWidth >= 700.dp) 3 else 2

            when (val postsResult = feedState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = InstabookBlue)
                    }
                }
                is UiState.Error -> {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(postsResult.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is UiState.Success -> {
                    val creators = (creatorsState as? UiState.Success)?.data.orEmpty()
                        .filter { creator ->
                            creator.userId != viewModel.currentUserId &&
                                (searchQuery.isBlank() ||
                                    creator.displayName.contains(searchQuery, ignoreCase = true) ||
                                    creator.username.contains(searchQuery, ignoreCase = true))
                        }

                    val filteredPosts = remember(postsResult.data, selectedCategory, searchQuery) {
                        postsResult.data
                            .filter { post ->
                                val matchesCategory = selectedCategory == "All" ||
                                    post.category.equals(selectedCategory, ignoreCase = true)
                                val matchesQuery = searchQuery.isBlank() ||
                                    post.caption.contains(searchQuery, ignoreCase = true) ||
                                    post.authorUsername.contains(searchQuery, ignoreCase = true) ||
                                    post.authorDisplayName.contains(searchQuery, ignoreCase = true) ||
                                    post.category.contains(searchQuery, ignoreCase = true)
                                matchesCategory && matchesQuery
                            }
                            .sortedByDescending { it.likesCount * 2 + it.commentsCount + it.sharesCount }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columnCount),
                        contentPadding = PaddingValues(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (creators.isNotEmpty()) {
                            item(span = { GridItemSpan(columnCount) }) {
                                Column {
                                    Text(
                                        text = "Discover Creators",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InstabookInk
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(creators, key = { it.userId }) { creator ->
                                            CreatorSpotlightCard(
                                                creator = creator,
                                                isFollowing = followingIds.contains(creator.userId),
                                                onProfileClick = { onOpenCreatorProfile(creator.userId) },
                                                onFollowClick = { viewModel.toggleFollow(creator) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (selectedCategory == "All") "Trending Videos" else "$selectedCategory Videos",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = InstabookInk
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }

                        if (filteredPosts.isEmpty()) {
                            item(span = { GridItemSpan(columnCount) }) {
                                EmptyStateView(
                                    title = "No Videos Match",
                                    subtitle = "Try another category or share a new video with the Instabook community.",
                                    actionLabel = "Create Video",
                                    onAction = onOpenCreate
                                )
                            }
                        } else {
                            items(filteredPosts, key = { it.id }) { post ->
                                ExploreVideoTile(
                                    post = post,
                                    onClick = { onOpenVideoDetail(post.id) }
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
private fun CreatorSpotlightCard(
    creator: UserProfile,
    isFollowing: Boolean,
    onProfileClick: () -> Unit,
    onFollowClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, InstabookSoftBlue),
        modifier = Modifier
            .width(156.dp)
            .clickable(onClick = onProfileClick)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            CreatorAvatar(
                avatarUrl = creator.avatarUrl,
                displayName = creator.displayName,
                size = 54.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = creator.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = InstabookInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "@${creator.username}",
                style = MaterialTheme.typography.bodySmall,
                color = InstabookSlate,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatCount(creator.followersCount)} followers",
                style = MaterialTheme.typography.labelSmall,
                color = InstabookBlue
            )
            Spacer(modifier = Modifier.height(8.dp))
            FollowPillButton(
                isFollowing = isFollowing,
                onClick = onFollowClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ExploreVideoTile(
    post: VideoPost,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, InstabookSoftBlue),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable(onClick = onClick)
            .testTag("explore_tile_${post.id}")
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
            } else {
                // Decorative crisp blue geometric backdrop when no static cover photo is set
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(InstabookSkyBlue.copy(alpha = 0.85f), InstabookDeepBlue)
                            )
                        )
                )
            }

            // Bottom gradient scrim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            )
                        )
                    )
            )

            // Play badge top-right
            Surface(
                color = Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play video",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(5.dp)
                        .size(18.dp)
                )
            }

            // Category pill top-left
            Surface(
                color = InstabookBlue.copy(alpha = 0.9f),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = post.category,
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "@${post.authorUsername}",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
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
}
