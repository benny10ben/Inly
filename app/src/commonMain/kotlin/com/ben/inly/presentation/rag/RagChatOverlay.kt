package com.ben.inly.presentation.rag

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import com.ben.inly.presentation.shared.stableStatusBarsPadding
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.domain.util.AiEventBus
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.KmpBackHandler
import com.ben.inly.ui.theme.LocalInlyFontStyle
import com.ben.inly.ui.theme.fontFamilyFor
import kotlinx.coroutines.delay

private val DesktopMaxContentWidth = 720.dp

@Composable
fun RagChatOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    viewModel: RagViewModel
) {
    KmpBackHandler(enabled = isVisible) { onDismiss() }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(250)),
        exit  = slideOutVertically(animationSpec = tween(250, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(200)),
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                RagChatContent(
                    viewModel = viewModel,
                    onDismiss = onDismiss,
                    isVisible = isVisible,
                    contentModifier = Modifier
                        .fillMaxHeight()
                        .then(
                            if (isDesktopPlatform) Modifier.width(DesktopMaxContentWidth)
                            else Modifier.fillMaxWidth()
                        )
                )
            }
        }
    }
}

// Desktop-only: persistent, resizable side-panel variant.
@Composable
fun RagChatPanel(
    onDismiss: () -> Unit,
    viewModel: RagViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = MaterialTheme.colorScheme.background
    ) {
        RagChatContent(
            viewModel = viewModel,
            onDismiss = onDismiss,
            isVisible = true,
            contentModifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun RagChatContent(
    viewModel: RagViewModel,
    onDismiss: () -> Unit,
    isVisible: Boolean,
    contentModifier: Modifier
) {
    val messages        by viewModel.messages.collectAsState()
    val isLoading       by viewModel.isLoading.collectAsState()
    val isModelAvailable by viewModel.isModelAvailable.collectAsState()
    val listState       = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            AiEventBus.requestImmediateIndex()
        } else {
            inputText = ""
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    LaunchedEffect(messages.lastOrNull()?.text) {
        if (messages.isNotEmpty() && isLoading) listState.animateScrollToItem(messages.lastIndex)
    }

    val submit: () -> Unit = {
        val trimmed = inputText.trim()
        if (trimmed.isNotEmpty() && !isLoading) {
            viewModel.submitQuery(trimmed)
            inputText = ""
        }
    }

    Column(modifier = contentModifier) {
        // Header
                    val headerPadding = if (isDesktopPlatform) 32.dp else 20.dp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .stableStatusBarsPadding()
                            .padding(
                                start = headerPadding,
                                end = if (isDesktopPlatform) 24.dp else 12.dp,
                                top = if (isDesktopPlatform) 18.dp else 12.dp,
                                bottom = 8.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Ask Inly",
                                fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Body
                    val sidePadding = if (isDesktopPlatform) 32.dp else 16.dp
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            // Still checking
                            isModelAvailable == null -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }

                            // Model not found
                            isModelAvailable == false -> {
                                ModelUnavailablePrompt(
                                    sidePadding = sidePadding,
                                    onDownloadClick = { /* TODO: implement download */ },
                                    onApiKeyClick = { /* TODO: implement API key entry */ }
                                )
                            }

                            // Model ready, no messages yet
                            messages.isEmpty() -> {
                                EmptyState(
                                    sidePadding = sidePadding,
                                    onSuggestionTap = { suggestion ->
                                        inputText = suggestion
                                        submit()
                                    }
                                )
                            }

                            // Chat
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        horizontal = sidePadding,
                                        vertical = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(items = messages, key = { it.id }) { message ->
                                        ChatBubble(message)
                                    }
                                    if (isLoading && messages.lastOrNull()?.text?.isEmpty() == true) {
                                        item { ThinkingIndicator() }
                                    }
                                }
                            }
                        }
                    }

                    // Input bar — always visible
                    ChatInputBar(
                        value = inputText,
                        onValueChange = { inputText = it },
                        onSubmit = submit,
                        enabled = !isLoading && isModelAvailable == true,
                        sidePadding = sidePadding
                    )
    }
}

// Model unavailable

@Composable
private fun ModelUnavailablePrompt(
    sidePadding: androidx.compose.ui.unit.Dp,
    onDownloadClick: () -> Unit,
    onApiKeyClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = sidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Local AI model not found",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Download the on-device model to chat privately, or connect an external AI with an API key.",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(32.dp))

        // Download option
        ModelOptionCard(
            icon = { Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
            title = "Download local model",
            subtitle = "Runs fully on-device. Private & offline.",
            onClick = onDownloadClick
        )
        Spacer(Modifier.height(10.dp))

        // API key option
        ModelOptionCard(
            icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) },
            title = "Use an API key",
            subtitle = "Connect an external AI provider.",
            onClick = onApiKeyClick
        )
    }
}

@Composable
private fun ModelOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icon()
            Column {
                Text(
                    text = title,
                    fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Empty state

@Composable
private fun EmptyState(
    sidePadding: androidx.compose.ui.unit.Dp,
    onSuggestionTap: (String) -> Unit
) {
    val suggestions = listOf(
        "What are my deadlines this week?",
        "Summarize my recent notes",
        "What tasks are still pending?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = sidePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "What's on your mind?",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Everything runs privately on your device.",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(36.dp))

        suggestions.forEachIndexed { index, suggestion ->
            var chipVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { delay(80L * (index + 1)); chipVisible = true }

            val chipAlpha  by animateFloatAsState(if (chipVisible) 1f else 0f, tween(250), label = "a$index")
            val chipOffset by animateDpAsState(if (chipVisible) 0.dp else 10.dp, tween(250), label = "o$index")

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .graphicsLayer { alpha = chipAlpha; translationY = chipOffset.toPx() }
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSuggestionTap(suggestion) }
            ) {
                Text(
                    text = suggestion,
                    fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                )
            }
        }
    }
}

//  Chat bubble

@Composable
fun ChatBubble(message: ChatMessage) {
    if (message.text.isEmpty() && !message.isUser) return

    val isUser  = message.isUser
    val bgColor = if (isUser) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    val shape = if (isUser) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    else         RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    val align = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val widthMod = if (isDesktopPlatform) Modifier.fillMaxWidth(0.85f) else Modifier.widthIn(max = 300.dp)

    Box(Modifier.fillMaxWidth(), contentAlignment = align) {
        Box(
            modifier = Modifier
                .then(widthMod)
                .clip(shape)
                .background(bgColor)
                .animateContentSize(animationSpec = tween(120))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text      = message.text,
                color     = textColor,
                fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                fontSize  = 15.sp,
                lineHeight = 23.sp
            )
        }
    }
}

// Input bar

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    enabled: Boolean,
    sidePadding: androidx.compose.ui.unit.Dp
) {
    val canSend = value.isNotBlank() && enabled

    val sendColor by animateColorAsState(
        targetValue = if (canSend) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(200),
        label = "sendColor"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                horizontal = sidePadding,
                vertical   = if (isDesktopPlatform) 24.dp else 12.dp
            ),
        verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .animateContentSize(animationSpec = tween(150))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(26.dp)
                )
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = "Ask about your notes…",
                        fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = enabled,
                    textStyle = TextStyle(
                        fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
                        fontSize   = 14.sp,
                        color      = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 21.sp
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        Surface(
            shape = CircleShape,
            color = sendColor,
            modifier = Modifier.size(52.dp)
        ) {
            IconButton(onClick = onSubmit, enabled = canSend) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Thinking indicator

@Composable
private fun ThinkingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
    ) {
        Text(
            text = "Thinking",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        val transition = rememberInfiniteTransition(label = "dots")
        val alpha by transition.animateFloat(
            initialValue = 0.2f,
            targetValue  = 1f,
            animationSpec = infiniteRepeatable(
                animation  = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dotAlpha"
        )
        Text(
            text = "•••",
            fontFamily = fontFamilyFor(LocalInlyFontStyle.current),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
    }
}