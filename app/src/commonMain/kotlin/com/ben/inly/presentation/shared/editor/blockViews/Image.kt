package com.ben.inly.presentation.shared.editor.blockViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ben.inly.domain.model.ImageBlock
import com.ben.inly.domain.util.MediaStorageHelper
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.editor.DefaultBlockShape
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import org.koin.compose.koinInject
import java.io.File


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageBlockView(
    block: ImageBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRequestPicker: () -> Unit,
    onDelete: () -> Unit = {},
    onRequestCamera: () -> Unit,
    onDownload: () -> Unit = {}
) {
    val mediaStorageHelper = koinInject<MediaStorageHelper>()
    var showFullScreen by remember { mutableStateOf(false) }

    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (inSelectionMode) onToggleSelection()
                                else onRequestPicker()
                            },
                            onLongClick = onToggleSelection
                        )
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Add image", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
                }

                if (!isDesktopPlatform) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    )
                    Box(
                        modifier = Modifier
                            .clickable {
                                if (inSelectionMode) onToggleSelection()
                                else onRequestCamera()
                            }
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = "Take Photo",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } else {
        val absolutePath = remember(block.localFilePath) {
            mediaStorageHelper.getAbsoluteMediaPath(block.localFilePath)
        }
        val imageFile = remember(absolutePath) { File(absolutePath) }

        val context = LocalPlatformContext.current
        val request = remember(absolutePath, context) {
            ImageRequest.Builder(context)
                .data(imageFile)
                .memoryCacheKey("$absolutePath-${imageFile.lastModified()}")
                .diskCacheKey("$absolutePath-${imageFile.lastModified()}")
                .build()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 100.dp, max = 260.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else showFullScreen = true
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            AsyncImage(
                model = request,
                contentDescription = "Note Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (showFullScreen) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }

            val pillColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)
            val tint = MaterialTheme.colorScheme.primary

            Dialog(
                onDismissRequest = { showFullScreen = false },
                properties = DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                val dialogHazeState = remember { HazeState() }

                Box(modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .haze(state = dialogHazeState)
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = request,
                            contentDescription = "Full Screen Image",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f
                                                offset = Offset.Zero
                                            } else scale = 2.5f
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        if (scale > 1f) {
                                            val maxX = (size.width * (scale - 1)) / 2
                                            val maxY = (size.height * (scale - 1)) / 2
                                            offset = Offset(
                                                x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                            )
                                        } else offset = Offset.Zero
                                    }
                                }
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                ),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 18.dp, start = 18.dp, end = 18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.25f),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .hazeChild(state = dialogHazeState)
                                .clickable { showFullScreen = false }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    // Bottom Bar
                    Surface(
                        shape = DefaultBlockShape,
                        color = pillColor,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                            .clip(DefaultBlockShape)
                            .hazeChild(state = dialogHazeState)
                    ) {
                        val divider = @Composable {
                            Box(Modifier.width(1.dp).height(18.dp).background(tint.copy(alpha = 0.2f)))
                        }

                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            val iconSize = 18.dp
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download",
                                modifier = Modifier.size(iconSize).clickable {
                                    block.localFilePath?.let { onDownload() }
                                },
                                tint = tint
                            )
                            divider()
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                modifier = Modifier.size(iconSize).clickable { onDelete(); showFullScreen = false },
                                tint = tint
                            )
                        }
                    }
                }
            }
        }
    }
}