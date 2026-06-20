package com.ben.inly.presentation.shared.editor.blockViews

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.model.DocumentBlock
import com.ben.inly.presentation.shared.editor.DefaultBlockShape
import com.ben.inly.ui.theme.PoppinsFont

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentBlockView(
    block: DocumentBlock,
    inSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onRequestPicker: () -> Unit,
    onOpenFile: (filePath: String, mimeType: String) -> Unit)
{
    if (block.localFilePath == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) onToggleSelection()
                        else onRequestPicker()
                    },
                    onLongClick = onToggleSelection
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("Attach a file", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(DefaultBlockShape)
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (inSelectionMode) {
                            onToggleSelection()
                        } else {
                            block.localFilePath?.let { path ->
                                onOpenFile(path, block.mimeType ?: "*/*")
                            }
                        }
                    },
                    onLongClick = onToggleSelection
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 48.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = block.fileName,
                    fontFamily = PoppinsFont,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = block.fileSizeString,
                    fontFamily = PoppinsFont,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(18.dp)
            )
        }
    }
}