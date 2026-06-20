package com.ben.inly.presentation.shared.editor.blockViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.network.httpHeaders
import coil3.request.crossfade
import com.ben.inly.domain.model.BookmarkBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.editor.DefaultBlockShape
import com.ben.inly.ui.theme.PoppinsFont

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkBlockView(
    block: BookmarkBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onUrlSubmit: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(block.url.isEmpty()) }
    var inputUrl by remember { mutableStateOf(block.url) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (inSelectionMode) onToggleSelection()
                    else if (!isEditing && block.url.isNotEmpty()) {
                        try { uriHandler.openUri(block.url) } catch (_: Exception) {}
                    }
                },
                onLongClick = onToggleSelection
            )
    ) {
        if (isEditing) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(DefaultBlockShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = inputUrl,
                    onValueChange = { inputUrl = it },
                    textStyle = TextStyle(
                        fontFamily = PoppinsFont,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (inputUrl.isNotBlank()) {
                                onUrlSubmit(inputUrl)
                                isEditing = false
                            }
                        }
                    ),
                    decorationBox = { inner ->
                        if (inputUrl.isEmpty()) {
                            Text(
                                "Paste a link and press Enter...",
                                fontFamily = PoppinsFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        inner()
                    }
                )
            }
        } else {
            val commonContainerModifier = Modifier
                .fillMaxWidth()
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), DefaultBlockShape)

            val textContent = @Composable { modifier: Modifier ->
                Column(
                    modifier = modifier.padding(14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = block.title ?: block.url,
                        fontFamily = PoppinsFont,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!block.description.isNullOrEmpty()) {
                        Text(
                            text = block.description,
                            fontFamily = PoppinsFont,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = remember(block.url) {
                                try { java.net.URI(block.url).host ?: block.url }
                                catch (_: Exception) { block.url }
                            },
                            fontFamily = PoppinsFont,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            val imageContent = @Composable { modifier: Modifier ->
                if (block.previewImageUrl != null) {
                    coil3.compose.AsyncImage(
                        model = coil3.request.ImageRequest.Builder(coil3.compose.LocalPlatformContext.current)
                            .data(block.previewImageUrl)
                            .crossfade(true)
                            .httpHeaders(
                                coil3.network.NetworkHeaders.Builder()
                                    .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                                    .set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                                    .build()
                            )
                            .build(),
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)),
                        onState = { state ->
                            if (state is coil3.compose.AsyncImagePainter.State.Error) {
                                println("Coil failed to load bookmark image: ${state.result.throwable.message}")
                            }
                        }
                    )
                }
            }

            if (isDesktopPlatform) {
                Row(
                    modifier = commonContainerModifier.height(120.dp)
                ) {
                    textContent(Modifier.weight(1f).fillMaxHeight())

                    if (block.previewImageUrl != null) {
                        imageContent(Modifier.weight(0.35f).fillMaxHeight())
                    }
                }
            } else {
                Column(
                    modifier = commonContainerModifier
                ) {
                    if (block.previewImageUrl != null) {
                        imageContent(Modifier.fillMaxWidth().height(140.dp))
                    }
                    textContent(Modifier.fillMaxWidth())
                }
            }
        }
    }
}