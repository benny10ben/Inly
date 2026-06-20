package com.ben.inly.presentation.shared.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.ui.theme.LocalAppIsDark
import com.ben.inly.ui.theme.PoppinsFont
import kotlinx.coroutines.launch

private val BottomSheetShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlyBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    applyNavPadding: Boolean = true,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    if (expanded) {
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        fun closeAnd(action: () -> Unit) {
            coroutineScope.launch {
                try {
                    kotlinx.coroutines.withTimeoutOrNull(250) {
                        sheetState.hide()
                    }
                } finally {
                    action()
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0) },
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(0.dp),
            scrimColor = Color.Black.copy(alpha = 0.32f),
            dragHandle = null,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false
            )
        ) {
            KmpBackHandler(enabled = true) {
                coroutineScope.launch {
                    try {
                        kotlinx.coroutines.withTimeoutOrNull(250) {
                            sheetState.hide()
                        }
                    } finally {
                        onDismiss()
                    }
                }
            }

            // Floating card
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(bottom = 8.dp)
                    .then(if (applyNavPadding) Modifier.navigationBarsPadding() else Modifier)
                    .clip(BottomSheetShape)
                    .background(
                        if (LocalAppIsDark.current) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.background
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .then(if (applyNavPadding) Modifier.padding(bottom = 12.dp) else Modifier)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        BottomSheetDefaults.DragHandle(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                    }

                    if (title != null) {
                        Text(
                            text = title,
                            fontFamily = PoppinsFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }

                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontFamily = PoppinsFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        content { action -> closeAnd(action) }
                    }
                }
            }
        }
    }
}