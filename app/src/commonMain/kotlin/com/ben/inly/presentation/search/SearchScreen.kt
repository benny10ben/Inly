package com.ben.inly.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.domain.model.NoteSearchResult
import com.ben.inly.presentation.shared.components.TopBarIconButton
import com.ben.inly.presentation.shared.components.customInlyShadow
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import inly.app.generated.resources.Res
import inly.app.generated.resources.search
import inly.app.generated.resources.x
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Full-screen cross-note search. The input field floats at the bottom of the Box and picks
 * up `.imePadding()`, so it's always pinned directly above the software keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit,
    onDailyNoteClick: (String) -> Unit,
    viewModel: SearchViewModel = koinViewModel()
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val hazeState = remember { HazeState() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            focusRequester.requestFocus()
        } catch (e: Exception) {
            // Focus fallback
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // Main content background with Haze blur applied
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when {
                    query.isBlank() -> SearchHint()
                    results.isEmpty() -> SearchEmptyState()
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                            bottom = 120.dp
                        )
                    ) {
                        items(results, key = { it.note.noteId }) { result ->
                            SearchResultRow(
                                result = result,
                                query = query,
                                onClick = {
                                    if (result.note.isDaily) {
                                        result.note.dateString?.let(onDailyNoteClick)
                                    } else {
                                        onNoteClick(result.note.noteId)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Floating Search Input and Close Button
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Glassy BasicTextField wrapper
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .size(52.dp)
                        .customInlyShadow(RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .hazeChild(hazeState)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.45f))
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource(Res.drawable.search),
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = query,
                        onValueChange = viewModel::onQueryChange,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontFamily = PoppinsFont
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester),
                        decorationBox = { innerTextField ->
                            if (query.isBlank()) {
                                Text(
                                    text = "Search all notes...",
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    fontSize = 15.sp,
                                    fontFamily = PoppinsFont
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.45f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(52.dp)
                        .customInlyShadow(CircleShape)
                        .clip(CircleShape)
                        .hazeChild(hazeState)
                        .clickable(onClick = onBack)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            painterResource(Res.drawable.x),
                            contentDescription = "Close Search",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHint() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Start typing to search titles, snippets, and note content.",
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun SearchEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No matching notes found.",
            fontFamily = PoppinsFont,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun SearchResultRow(
    result: NoteSearchResult,
    query: String,
    onClick: () -> Unit
) {
    val note: NoteMetadataEntity = result.note
    val highlightStyle = defaultHighlightStyle(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                text = highlightMatches(note.title.ifBlank { "Untitled" }, query, highlightStyle),
                fontFamily = PoppinsFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (result.matchedText.isNotBlank() && result.matchedText != note.title) {
                Text(
                    text = highlightMatches(result.matchedText, query, highlightStyle),
                    fontFamily = PoppinsFont,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            if (note.isDaily && note.dateString != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Daily · ${note.dateString}",
                        fontFamily = PoppinsFont,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}