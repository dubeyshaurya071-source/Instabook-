package com.example.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.InstabookCategories
import com.example.ui.components.VideoPlayerBox
import com.example.ui.theme.InstabookBlue
import com.example.ui.theme.InstabookDeepBlue
import com.example.ui.theme.InstabookIceBlue
import com.example.ui.theme.InstabookInk
import com.example.ui.theme.InstabookSlate
import com.example.ui.theme.InstabookSoftBlue
import com.example.ui.theme.InstabookSurfaceTint
import com.example.ui.viewmodel.InstabookViewModel
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreatePostScreen(
    viewModel: InstabookViewModel,
    onPostPublished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var videoUrl by remember { mutableStateOf("") }
    var thumbnailUrl by remember { mutableStateOf("") }
    var caption by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Trending") }
    var visibility by remember { mutableStateOf("public") }
    var isCopyingMedia by remember { mutableStateOf(false) }
    var localNotice by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isCopyingMedia = true
                localNotice = null
                val copiedPath = copyPickedFileToAppStorage(context, uri, "videos", "mp4")
                isCopyingMedia = false
                if (copiedPath != null) {
                    videoUrl = copiedPath
                    localNotice = "Video selected from your device and ready to preview."
                } else {
                    localNotice = "Could not import the selected video."
                }
            }
        }
    }

    // Zero-permission Android Photo Picker for Cover Thumbnail
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copiedPath = copyPickedFileToAppStorage(context, uri, "covers", "jpg")
                if (copiedPath != null) {
                    thumbnailUrl = copiedPath
                }
            }
        }
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
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_video_title),
                    style = MaterialTheme.typography.headlineSmall,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Media Picker & Stream URL Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, InstabookSoftBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "1. Select or Link a Video",
                            style = MaterialTheme.typography.titleMedium,
                            color = InstabookInk
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InstabookBlue,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pick_video_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCopyingMedia) "Importing..." else "Pick Video",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    coverPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = InstabookBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (thumbnailUrl.isNotBlank()) "Cover Set" else "Cover Photo",
                                    color = InstabookBlue,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = { videoUrl = it },
                            label = { Text("Or paste video URL (https://... .mp4 / .m3u8)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Link,
                                    contentDescription = null,
                                    tint = InstabookBlue
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = InstabookBlue,
                                unfocusedBorderColor = InstabookSoftBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_url_input")
                        )

                        if (localNotice != null) {
                            Text(
                                text = localNotice ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = InstabookDeepBlue
                            )
                        }

                        if (videoUrl.isNotBlank()) {
                            VideoPlayerBox(
                                videoUrl = videoUrl,
                                thumbnailUrl = thumbnailUrl,
                                autoPlay = false,
                                aspectRatio = 16f / 9f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Caption & Category Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, InstabookSoftBlue)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "2. Caption & Category",
                            style = MaterialTheme.typography.titleMedium,
                            color = InstabookInk
                        )

                        OutlinedTextField(
                            value = caption,
                            onValueChange = { if (it.length <= 500) caption = it },
                            label = { Text("Write a caption or #hashtags...") },
                            minLines = 3,
                            maxLines = 5,
                            supportingText = {
                                Text("${caption.length}/500", color = InstabookSlate)
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = InstabookBlue,
                                unfocusedBorderColor = InstabookSoftBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("post_caption_input")
                        )

                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge,
                            color = InstabookInk
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            InstabookCategories.forEach { item ->
                                val selected = category == item
                                FilterChip(
                                    selected = selected,
                                    onClick = { category = item },
                                    label = { Text(item) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = InstabookBlue,
                                        selectedLabelColor = Color.White,
                                        containerColor = InstabookIceBlue,
                                        labelColor = InstabookDeepBlue
                                    )
                                )
                            }
                        }

                        Text(
                            text = "Visibility",
                            style = MaterialTheme.typography.labelLarge,
                            color = InstabookInk
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            FilterChip(
                                selected = visibility == "public",
                                onClick = { visibility = "public" },
                                label = { Text("Public (Feed & Explore)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Public,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InstabookBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = InstabookIceBlue,
                                    labelColor = InstabookDeepBlue
                                )
                            )
                            FilterChip(
                                selected = visibility == "private",
                                onClick = { visibility = "private" },
                                label = { Text("Only Me") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = InstabookBlue,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = InstabookIceBlue,
                                    labelColor = InstabookDeepBlue
                                )
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        viewModel.createVideoPost(
                            caption = caption,
                            videoUrl = videoUrl,
                            thumbnailUrl = thumbnailUrl,
                            category = category,
                            visibility = visibility
                        ) {
                            caption = ""
                            videoUrl = ""
                            thumbnailUrl = ""
                            localNotice = null
                            onPostPublished()
                        }
                    },
                    enabled = !isBusy && !isCopyingMedia && caption.isNotBlank() && videoUrl.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InstabookBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("publish_post_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Publishing...")
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.publish_video_button),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private suspend fun copyPickedFileToAppStorage(
    context: Context,
    uri: Uri,
    folderName: String,
    extension: String
): String? = withContext(Dispatchers.IO) {
    try {
        val dir = File(context.filesDir, folderName).apply { mkdirs() }
        val targetFile = File(dir, "${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null
        Uri.fromFile(targetFile).toString()
    } catch (_: Exception) {
        null
    }
}
