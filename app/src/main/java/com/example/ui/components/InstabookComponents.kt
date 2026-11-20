package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.google.firebase.Timestamp

@Composable
fun InstabookWordmark(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(InstabookBlue, InstabookDeepBlue)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Instabook icon",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "instabook",
            style = MaterialTheme.typography.headlineSmall,
            color = InstabookDeepBlue
        )
    }
}

@Composable
fun CreatorAvatar(
    avatarUrl: String,
    displayName: String,
    size: Dp = 42.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "I"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(InstabookBlue, InstabookDeepBlue)
                )
            )
            .border(1.5.dp, InstabookSoftBlue, CircleShape)
            .let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            },
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl.isNotBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "$displayName avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun FollowPillButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = InstabookIceBlue,
                contentColor = InstabookDeepBlue
            ),
            modifier = modifier.height(38.dp)
        ) {
            Text(
                text = "Following",
                style = MaterialTheme.typography.labelLarge
            )
        }
    } else {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = InstabookBlue,
                contentColor = Color.White
            ),
            modifier = modifier.height(38.dp)
        ) {
            Text(
                text = "Follow",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_state),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, InstabookSoftBlue, RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = InstabookInk,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = InstabookSlate,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = InstabookBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    if (timestamp == null) return "Just now"
    val diffSeconds = ((System.currentTimeMillis() - timestamp.toDate().time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSeconds < 60L -> "Just now"
        diffSeconds < 3600L -> "${diffSeconds / 60L}m ago"
        diffSeconds < 86400L -> "${diffSeconds / 3600L}h ago"
        diffSeconds < 604800L -> "${diffSeconds / 86400L}d ago"
        else -> "${diffSeconds / 604800L}w ago"
    }
}

fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000L -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000L -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}
