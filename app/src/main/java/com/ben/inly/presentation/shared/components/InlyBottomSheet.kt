package com.ben.inly.presentation.shared.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.ui.theme.BricolageFont
import kotlinx.coroutines.launch

private val BottomSheetShape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InlyBottomSheet(
    expanded: Boolean,
    onDismiss: () -> Unit,
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.(closeAnd: (() -> Unit) -> Unit) -> Unit
) {
    if (expanded) {
        val coroutineScope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        fun closeAnd(action: () -> Unit) {
            coroutineScope.launch {
                sheetState.hide()
                action()
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            contentWindowInsets = { WindowInsets(0) },
            containerColor = MaterialTheme.colorScheme.background,
            shape = BottomSheetShape,
            dragHandle = null,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true
            )
        ) {
            BackHandler {
                closeAnd(onDismiss)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        bottom = WindowInsets.ime
                            .asPaddingValues()
                            .calculateBottomPadding() + 12.dp
                    )
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BottomSheetDefaults.DragHandle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                }

                Text(
                    text = title,
                    fontFamily = BricolageFont,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontFamily = BricolageFont,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp)
                    )
                }

                content { action -> closeAnd(action) }
            }
        }
    }
}