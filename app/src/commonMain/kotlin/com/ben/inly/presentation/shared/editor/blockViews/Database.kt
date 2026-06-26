package com.ben.inly.presentation.shared.editor.blockViews

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ben.inly.data.local.room.TagEntity
import com.ben.inly.domain.model.ColumnType
import com.ben.inly.domain.model.DatabaseBlock
import com.ben.inly.domain.util.isDesktopPlatform
import com.ben.inly.presentation.shared.components.InlyBottomSheet
import com.ben.inly.presentation.shared.components.MinimalDatePickerDialog
import com.ben.inly.presentation.shared.editor.EditorActions
import com.ben.inly.presentation.shared.editor.mouseScrollable
import com.ben.inly.ui.theme.PoppinsFont
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toLocalDateTime
import kotlin.collections.get
import kotlin.text.equals
import com.ben.inly.data.local.room.NoteMetadataEntity
import com.ben.inly.presentation.shared.editor.NoteLinkVisualTransformation
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import com.ben.inly.presentation.shared.components.InlyDesktopMenu
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.animation.animateContentSize
import com.ben.inly.presentation.shared.components.InlyButtonPrimary
import com.ben.inly.presentation.shared.components.InlyButtonSecondary
import com.ben.inly.presentation.shared.components.InlyTextField

enum class DbSheetType { NONE, COLUMN_OPTIONS, RENAME, FORMULA, FILTER, SORT, CELL_OPTIONS, TAG_SELECTION, FILE_OPTIONS, PRIORITY_SELECTION, AGGREGATION, CURRENCY_SELECTION }

@Composable
fun DbOptionRow(
    icon: ImageVector,
    text: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontFamily = PoppinsFont, fontSize = 14.sp, color = color)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DatabaseBlockView(
    block: DatabaseBlock,
    inSelectionMode: Boolean,
    globalTags: List<TagEntity>,
    allLinkableNotes: List<NoteMetadataEntity>,
    actions: EditorActions
) {
    val hazeState = remember { HazeState() }
    val scrollState = rememberScrollState()
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
    val borderColor1 = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val coroutineScope = rememberCoroutineScope()

    var currentSheet by remember { mutableStateOf(DbSheetType.NONE) }
    var activeColId by remember { mutableStateOf<String?>(null) }
    var activeRowId by remember { mutableStateOf<String?>(null) }
    var textInput by remember { mutableStateOf("") }
    var textInputMax by remember { mutableStateOf("") }
    var filterOperator by remember { mutableStateOf("contains") }
    var filterPriority by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var aggregationExpandedSection by remember { mutableStateOf<String?>(null) }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var playingFileUri by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0
            while (isRecording) {
                delay(1000)
                recordingDuration++
            }
        }
    }

    fun closeSheet() {
        if (isRecording && activeRowId != null && activeColId != null) {
            isRecording = false
            actions.onStopDbAudioRecording(block.id, activeRowId!!, activeColId!!, true)
        }
        if (playingFileUri != null) {
            playingFileUri = null
            actions.onStopAudio()
        }
        currentSheet = DbSheetType.NONE
        activeRowId = null
        aggregationExpandedSection = null
    }

    val visibleColumns = remember(block.columns) {
        block.columns.filter { !it.isDeleted }
    }

    fun applyAction(action: () -> Unit) {
        closeSheet()
        scope.launch {
            delay(250)
            action()
        }
    }

    val visibleRows = remember(block.rows, block.activeSorts, block.activeFilters) {
        var result = block.rows.filter { !it.isDeleted }

        block.activeFilters.forEach { filter ->
            result = result.filter { row ->
                val cellVal = row.cells[filter.columnId] ?: ""
                when (filter.operator) {
                    // Text/String Constraints
                    "contains"     -> cellVal.contains(filter.value, ignoreCase = true)
                    "not_contains" -> !cellVal.contains(filter.value, ignoreCase = true)
                    "equals"       -> cellVal.equals(filter.value, ignoreCase = true)
                    "not_equals"   -> !cellVal.equals(filter.value, ignoreCase = true)
                    "starts_with"  -> cellVal.startsWith(filter.value, ignoreCase = true)
                    "ends_with"    -> cellVal.endsWith(filter.value, ignoreCase = true)

                    // Element state rules
                    "empty"        -> cellVal.isBlank() || cellVal == "—"
                    "not_empty"    -> cellVal.isNotBlank() && cellVal != "—"
                    "checked"      -> cellVal == "true"
                    "unchecked"    -> cellVal != "true"

                    // Math / Numeric comparison bounds
                    "gt"           -> (cellVal.toDoubleOrNull() ?: 0.0) > (filter.value.toDoubleOrNull() ?: 0.0)
                    "gte"          -> (cellVal.toDoubleOrNull() ?: 0.0) >= (filter.value.toDoubleOrNull() ?: 0.0)
                    "lt"           -> (cellVal.toDoubleOrNull() ?: 0.0) < (filter.value.toDoubleOrNull() ?: 0.0)
                    "lte"          -> (cellVal.toDoubleOrNull() ?: 0.0) <= (filter.value.toDoubleOrNull() ?: 0.0)

                    // Between range — value stored as "lo|hi"
                    "between"      -> {
                        val parts = filter.value.split("|")
                        if (parts.size == 2 && cellVal.isNotBlank()) {
                            val lo = parts[0].toDoubleOrNull()
                            val hi = parts[1].toDoubleOrNull()
                            val v  = cellVal.toDoubleOrNull()
                            if (lo != null && hi != null && v != null) v in lo..hi
                            else cellVal >= parts[0] && cellVal <= parts[1]
                        } else true
                    }

                    // Custom priority maps & timestamps
                    "priority"     -> cellVal.equals(filter.value, ignoreCase = true)
                    "before"       -> cellVal.isNotBlank() && cellVal < filter.value
                    "after"        -> cellVal.isNotBlank() && cellVal > filter.value
                    else           -> true
                }
            }
        }

        if (block.activeSorts.isNotEmpty()) {
            result = result.sortedWith(Comparator { row1, row2 ->
                var comparisonResult = 0

                for (sortRule in block.activeSorts) {
                    val targetCol = block.columns.find { it.id == sortRule.columnId } ?: continue
                    val rawVal1 = row1.cells[sortRule.columnId] ?: ""
                    val rawVal2 = row2.cells[sortRule.columnId] ?: ""

                    comparisonResult = when (targetCol.type) {
                        ColumnType.NUMBER, ColumnType.MONEY -> {
                            val num1 = rawVal1.toDoubleOrNull() ?: Double.MAX_VALUE
                            val num2 = rawVal2.toDoubleOrNull() ?: Double.MAX_VALUE
                            num1.compareTo(num2)
                        }
                        ColumnType.CHECKBOX -> {
                            val bool1 = rawVal1 == "true"
                            val bool2 = rawVal2 == "true"
                            bool1.compareTo(bool2)
                        }
                        ColumnType.PRIORITY -> {
                            val weight = mapOf("Low" to 1, "Medium" to 2, "High" to 3, "Urgent" to 4)
                            val p1 = weight[rawVal1] ?: 0
                            val p2 = weight[rawVal2] ?: 0
                            p1.compareTo(p2)
                        }
                        else -> rawVal1.lowercase().compareTo(rawVal2.lowercase())
                    }

                    if (!sortRule.isAscending) comparisonResult = -comparisonResult
                    if (comparisonResult != 0) break
                }
                comparisonResult
            })
        }
        result
    }

    val sheetTitle = when (currentSheet) {
        DbSheetType.CELL_OPTIONS    -> "Cell Actions"
        DbSheetType.COLUMN_OPTIONS  -> visibleColumns.find { it.id == activeColId }?.name ?: "Column Options"
        DbSheetType.RENAME          -> "Rename Column"
        DbSheetType.FORMULA         -> "Edit Formula"
        DbSheetType.SORT            -> "Sort"
        DbSheetType.FILTER          -> "Add Filter"
        DbSheetType.FILE_OPTIONS    -> "Attached Files"
        DbSheetType.PRIORITY_SELECTION -> "Set Priority"
        DbSheetType.AGGREGATION     -> "Calculate"
        else                        -> ""
    }

    val sheetContent = @Composable { targetSheet: DbSheetType ->
        Column(modifier = Modifier.fillMaxWidth()) {

            // 2. Render the Title INSIDE the animated content so it fades beautifully
            val title = when (targetSheet) {
                DbSheetType.CELL_OPTIONS -> "Cell Actions"
                DbSheetType.COLUMN_OPTIONS -> visibleColumns.find { it.id == activeColId }?.name
                    ?: "Column Options"

                DbSheetType.SORT -> "Sort"
                DbSheetType.FILTER -> "Filter"
                DbSheetType.FILE_OPTIONS -> "Attached Files"
                DbSheetType.PRIORITY_SELECTION -> "Set Priority"
                DbSheetType.AGGREGATION -> "Calculate"
                DbSheetType.TAG_SELECTION -> "Select Tag"
                else -> "" // Rename, Formula, Currency use a Back button instead
            }

            if (title.isNotBlank()) {
                Text(
                    text = title,
                    fontFamily = PoppinsFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        bottom = 12.dp,
                        top = if (isDesktopPlatform) 8.dp else 16.dp
                    ).padding(horizontal = 20.dp)
                )
            }

            // 3. Render a "Back to Options" button for sub-menus
            if (targetSheet in listOf(
                    DbSheetType.RENAME,
                    DbSheetType.FORMULA,
                    DbSheetType.CURRENCY_SELECTION
                )
            ) {
                DbOptionRow(Icons.AutoMirrored.Filled.ArrowBack, "Back to Options") {
                    currentSheet = DbSheetType.COLUMN_OPTIONS
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                )
            }

            when (targetSheet) {
                // CELL OPTIONS
                DbSheetType.CELL_OPTIONS -> {
                    val col = visibleColumns.find { it.id == activeColId }
                    val row = block.rows.find { it.id == activeRowId }
                    if (col != null && row != null) {
                        val colIndex = visibleColumns.indexOf(col)
                        val rowIndex = block.rows.indexOf(row)

                        DbOptionRow(
                            Icons.Default.ArrowUpward,
                            "Insert Row Above"
                        ) { applyAction { actions.onAddDbRowAt(block.id, rowIndex) } }
                        DbOptionRow(
                            Icons.Default.ArrowDownward,
                            "Insert Row Below"
                        ) { applyAction { actions.onAddDbRowAt(block.id, rowIndex + 1) } }
                        DbOptionRow(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Insert Column Left"
                        ) { applyAction { actions.onAddDbColumnAt(block.id, colIndex) } }
                        DbOptionRow(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            "Insert Column Right"
                        ) { applyAction { actions.onAddDbColumnAt(block.id, colIndex + 1) } }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        DbOptionRow(
                            Icons.Default.Delete,
                            "Delete Row",
                            MaterialTheme.colorScheme.error
                        ) { applyAction { actions.onDeleteDbRow(block.id, row.id) } }
                        DbOptionRow(
                            Icons.Default.DeleteSweep,
                            "Delete Column",
                            MaterialTheme.colorScheme.error
                        ) { applyAction { actions.onDeleteDbColumn(block.id, col.id) } }
                    }
                }


                // COLUMN OPTIONS
                DbSheetType.COLUMN_OPTIONS -> {
                    val col = visibleColumns.find { it.id == activeColId }
                    if (col != null) {
                        val colIndex = visibleColumns.indexOf(col)

                        DbOptionRow(Icons.Default.Edit, "Rename Column") {
                            textInput = col.name
                            currentSheet = DbSheetType.RENAME
                        }

                        if (col.type == ColumnType.FORMULA) {
                            DbOptionRow(
                                icon = Icons.Default.Functions,
                                text = "Edit Formula",
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                textInput = col.formulaExpression ?: ""
                                currentSheet = DbSheetType.FORMULA
                            }

                            val isCurrency = col.isFormulaCurrency
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        actions.onUpdateDbFormulaCurrency(
                                            block.id,
                                            col.id,
                                            !isCurrency
                                        )
                                    }
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MonetizationOn,
                                        null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Format as currency",
                                        fontFamily = PoppinsFont,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = isCurrency,
                                    onCheckedChange = null,
                                    modifier = Modifier.scale(0.8f)
                                )
                            }

                            if (isCurrency) {
                                val currentCurrency = col.currencySymbol ?: "$"
                                DbOptionRow(
                                    icon = Icons.Default.MonetizationOn,
                                    text = "Currency: $currentCurrency",
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    currentSheet = DbSheetType.CURRENCY_SELECTION
                                }
                            }
                        }

                        if (col.type == ColumnType.MONEY) {
                            val currentCurrency = col.currencySymbol ?: "$"
                            DbOptionRow(
                                icon = Icons.Default.MonetizationOn,
                                text = "Format: $currentCurrency",
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                currentSheet = DbSheetType.CURRENCY_SELECTION
                            }
                        }

                        if (colIndex > 0) {
                            DbOptionRow(Icons.AutoMirrored.Filled.ArrowBack, "Move Left") {
                                applyAction {
                                    actions.onReorderDbColumns(
                                        block.id,
                                        colIndex,
                                        colIndex - 1
                                    )
                                }
                            }
                        }

                        if (colIndex < visibleColumns.lastIndex) {
                            DbOptionRow(Icons.AutoMirrored.Filled.ArrowForward, "Move Right") {
                                applyAction {
                                    actions.onReorderDbColumns(
                                        block.id,
                                        colIndex,
                                        colIndex + 1
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 6.dp
                            ), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Text(
                            text = "Column Width",
                            fontFamily = PoppinsFont,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = 8.dp
                            )
                        )

                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.clickable {
                                    actions.onUpdateDbColumnWidth(
                                        block.id,
                                        col.id,
                                        col.width - 20
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = "${col.width} px",
                                fontFamily = PoppinsFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.widthIn(min = 50.dp),
                                textAlign = TextAlign.Center
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.clickable {
                                    actions.onUpdateDbColumnWidth(
                                        block.id,
                                        col.id,
                                        col.width + 20
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.padding(8.dp).size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Text(
                            text = "Property Type",
                            fontFamily = PoppinsFont,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 10.dp,
                                top = 12.dp
                            )
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            ColumnType.entries.forEach { type ->
                                val isSelected = col.type == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        applyAction {
                                            actions.onUpdateDbColumn(
                                                block.id,
                                                col.id,
                                                col.name,
                                                type
                                            )
                                        }
                                    },
                                    label = {
                                        Text(
                                            text = type.name.lowercase()
                                                .replaceFirstChar { it.uppercase() },
                                            fontFamily = PoppinsFont,
                                            fontSize = 13.sp
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                        DbOptionRow(
                            icon = Icons.Default.Delete,
                            text = "Delete Column",
                            color = MaterialTheme.colorScheme.error,
                            onClick = {
                                applyAction {
                                    actions.onDeleteDbColumn(
                                        block.id,
                                        col.id
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // RENAME
                DbSheetType.RENAME -> {
                    InlyTextField(value = textInput, onValueChange = { textInput = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InlyButtonSecondary(text = "Cancel", onClick = { closeSheet() }, modifier = Modifier.weight(1f))
                        InlyButtonPrimary(
                            text = "Save",
                            onClick = {
                                val c = visibleColumns.find { it.id == activeColId }
                                if (c != null && textInput.isNotBlank()) {
                                    applyAction { actions.onUpdateDbColumn(block.id, c.id, textInput.trim(), c.type) }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // FORMULA
                DbSheetType.FORMULA -> {
                    Text(
                        text = "Properties",
                        fontFamily = PoppinsFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp, top = 12.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    ) {
                        visibleColumns.filter { it.id != activeColId }.forEach { c ->
                            SuggestionChip(
                                onClick = { textInput += "prop(\"${c.name}\") " },
                                label = {
                                    Text(
                                        c.name,
                                        fontFamily = PoppinsFont,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                            )
                        }
                    }

                    Text(
                        text = "Operators",
                        fontFamily = PoppinsFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
                    ) {
                        listOf("+", "-", "*", "/", "(", ")").forEach { op ->
                            SuggestionChip(
                                onClick = { textInput += "$op " },
                                label = {
                                    Text(
                                        op,
                                        fontFamily = PoppinsFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                            )
                        }
                    }

                    InlyTextField(value = textInput, onValueChange = { textInput = it }, placeholder = "e.g. prop(\"Price\") * 2", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InlyButtonSecondary(text = "Cancel", onClick = { closeSheet() }, modifier = Modifier.weight(1f))
                        InlyButtonPrimary(
                            text = "Save",
                            onClick = {
                                val colId = activeColId
                                if (colId != null) applyAction { actions.onUpdateDbFormula(block.id, colId, textInput.trim()) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // SORT (multi-layer, Notion-style)
                DbSheetType.SORT -> {
                    val sortedColIds = block.activeSorts.map { it.columnId }
                    val unsortedColumns = visibleColumns.filter { it.id !in sortedColIds }

                    // Active sort layers
                    if (block.activeSorts.isNotEmpty()) {
                        Text(
                            text = "Sort order — top layer wins, lower layers break ties",
                            fontFamily = PoppinsFont, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = 8.dp
                            )
                        )

                        block.activeSorts.forEachIndexed { index, sortRule ->
                            val col = visibleColumns.find { it.id == sortRule.columnId }
                                ?: return@forEachIndexed
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Layer number badge
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${index + 1}",
                                            fontFamily = PoppinsFont,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    col.name,
                                    fontFamily = PoppinsFont, fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis
                                )
                                // Asc/Desc toggle — stays in sheet so you can keep editing layers
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                                    modifier = Modifier.clickable {
                                        actions.onUpdateDbSort(
                                            block.id,
                                            col.id,
                                            !sortRule.isAscending
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 6.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            if (sortRule.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                            null, modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            if (sortRule.isAscending) "Asc" else "Desc",
                                            fontFamily = PoppinsFont, fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.Close, "Remove sort layer",
                                    modifier = Modifier.size(18.dp).clickable {
                                        actions.onUpdateDbSort(block.id, col.id, null)
                                    },
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 20.dp,
                                vertical = 8.dp
                            ), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )
                    }

                    if (unsortedColumns.isNotEmpty()) {
                        Text(
                            text = if (block.activeSorts.isEmpty()) "Sort by" else "Then by",
                            fontFamily = PoppinsFont, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = 4.dp,
                                bottom = 8.dp
                            )
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                        ) {
                            unsortedColumns.forEach { col ->
                                SuggestionChip(
                                    onClick = { actions.onUpdateDbSort(block.id, col.id, true) },
                                    label = {
                                        Text(
                                            col.name,
                                            fontFamily = PoppinsFont,
                                            fontSize = 13.sp
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface.copy(
                                            alpha = 0.5f
                                        ),
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                )
                            }
                        }
                    } else if (block.activeSorts.isNotEmpty()) {
                        Text(
                            text = "Every column is already in the sort.",
                            fontFamily = PoppinsFont, fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (block.activeSorts.isNotEmpty()) {
                            InlyButtonSecondary(
                                text = "Clear all",
                                onClick = {
                                    block.activeSorts.map { it.columnId }.forEach { cid -> actions.onUpdateDbSort(block.id, cid, null) }
                                    closeSheet()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            InlyButtonPrimary(text = "Done", onClick = { closeSheet() }, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // FILTER
                DbSheetType.FILTER -> {
                    val activeCol = visibleColumns.find { it.id == activeColId }
                    val isCheckbox = activeCol?.type == ColumnType.CHECKBOX
                    val isNumber =
                        activeCol?.type == ColumnType.NUMBER || activeCol?.type == ColumnType.MONEY
                    val isDate = activeCol?.type == ColumnType.DATE

                    Text(
                        text = "Column",
                        fontFamily = PoppinsFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 4.dp,
                            bottom = 8.dp
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).clickable {
                            val idx = visibleColumns.indexOfFirst { it.id == activeColId }
                            activeColId = visibleColumns[(idx + 1) % visibleColumns.size].id
                            filterOperator = "contains"
                            textInput = ""
                            textInputMax = ""
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeCol?.name ?: "",
                                fontFamily = PoppinsFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Condition",
                        fontFamily = PoppinsFont,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = 14.dp,
                            bottom = 8.dp
                        )
                    )

                    val operatorOptions: List<Pair<String, String>> = when {
                        isCheckbox -> listOf(
                            "unchecked" to "Hide Checked rows",
                            "checked" to "Hide Unchecked rows"
                        )

                        isNumber -> listOf(
                            "equals" to "Equals",
                            "not_equals" to "Does not equal",
                            "gt" to "Greater than (>)",
                            "gte" to "Greater than or equal (≥)",
                            "lt" to "Less than (<)",
                            "lte" to "Less than or equal (≤)",
                            "between" to "Between (range)",
                            "not_empty" to "Is not empty",
                            "empty" to "Is empty"
                        )

                        isDate -> listOf(
                            "equals" to "On exactly date",
                            "before" to "Is before date",
                            "after" to "Is after date",
                            "between" to "Between two dates",
                            "not_empty" to "Is scheduled (Not empty)",
                            "empty" to "Is unscheduled (Empty)"
                        )

                        else -> listOf(
                            "contains" to "Contains text",
                            "not_contains" to "Does not contain",
                            "equals" to "Is exactly",
                            "not_equals" to "Is not",
                            "starts_with" to "Starts with",
                            "ends_with" to "Ends with",
                            "not_empty" to "Is not empty",
                            "empty" to "Is empty",
                            "priority" to "Priority status is"
                        )
                    }

                    if (operatorOptions.none { it.first == filterOperator }) filterOperator =
                        operatorOptions.first().first

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    ) {
                        operatorOptions.forEach { (op, label) ->
                            val isSelected = filterOperator == op
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    filterOperator = op; textInput = ""; textInputMax = ""
                                },
                                label = { Text(label, fontFamily = PoppinsFont, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(
                                        alpha = 0.5f
                                    )
                                ),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    labelColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    val needsTextInput = filterOperator in listOf(
                        "contains",
                        "equals",
                        "not_equals",
                        "gt",
                        "gte",
                        "lt",
                        "lte",
                        "before",
                        "after",
                        "starts_with",
                        "ends_with"
                    )
                    val needsRangeInput = filterOperator == "between"
                    val needsPriorityPicker = filterOperator == "priority"

                    if (needsTextInput) {
                        Spacer(Modifier.height(12.dp))
                        InlyTextField(value = textInput, onValueChange = { textInput = it }, placeholder = if (isNumber) "Enter number…" else "Enter value…", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    }

                    if (needsRangeInput) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            InlyTextField(value = textInput, onValueChange = { textInput = it }, placeholder = if (isDate) "Start" else "Min", modifier = Modifier.weight(1f))
                            InlyTextField(value = textInputMax, onValueChange = { textInputMax = it }, placeholder = if (isDate) "End" else "Max", modifier = Modifier.weight(1f))
                        }
                    }

                    if (needsPriorityPicker) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Priority level",
                            fontFamily = PoppinsFont,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            listOf("Low", "Medium", "High", "Urgent").forEach { p ->
                                val isSelected = filterPriority == p
                                val chipColor = when (p) {
                                    "Urgent" -> MaterialTheme.colorScheme.error
                                    "High" -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { filterPriority = p; textInput = p },
                                    label = { Text(p, fontFamily = PoppinsFont, fontSize = 13.sp) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) chipColor else MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.5f
                                        )
                                    ),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = chipColor,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        InlyButtonSecondary(text = "Cancel", onClick = { closeSheet() }, modifier = Modifier.weight(1f))
                        InlyButtonPrimary(
                            text = "Apply",
                            onClick = {
                                val colId = activeColId
                                if (colId != null) {
                                    val canApply = when {
                                        isCheckbox -> true
                                        filterOperator in listOf("not_empty", "empty") -> true
                                        filterOperator == "priority" -> filterPriority.isNotBlank()
                                        filterOperator == "between" -> textInput.isNotBlank() && textInputMax.isNotBlank()
                                        else -> textInput.isNotBlank()
                                    }
                                    if (canApply) applyAction {
                                        actions.onAddDbFilter(
                                            block.id, colId, filterOperator,
                                            when (filterOperator) {
                                                "priority" -> filterPriority.trim()
                                                "between" -> "${textInput.trim()}|${textInputMax.trim()}"
                                                else -> textInput.trim()
                                            }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // TAG SELECTION
                DbSheetType.TAG_SELECTION -> {
                    var tagSearchQuery by remember { mutableStateOf("") }
                    val row = block.rows.find { it.id == activeRowId }
                    if (row != null) {
                        val currentTagIds =
                            row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }
                                ?.toMutableSet() ?: mutableSetOf()

                        InlyTextField(value = tagSearchQuery, onValueChange = { tagSearchQuery = it }, placeholder = "Search or create a tag...", modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                        Spacer(Modifier.height(12.dp))

                        val filteredTags = globalTags.filter {
                            it.name.contains(
                                tagSearchQuery,
                                ignoreCase = true
                            )
                        }
                        val exactMatchExists = globalTags.any {
                            it.name.equals(
                                tagSearchQuery.trim(),
                                ignoreCase = true
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (tagSearchQuery.isNotBlank() && !exactMatchExists) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val colors = listOf(
                                                "#E03E3E",
                                                "#D9730D",
                                                "#DFAB01",
                                                "#0F7B6C",
                                                "#0B6E99",
                                                "#6940A5",
                                                "#9065B0"
                                            )
                                            val newTagId = actions.onCreateGlobalTag(
                                                tagSearchQuery.trim(),
                                                colors.random()
                                            )
                                            currentTagIds.add(newTagId)
                                            actions.onUpdateDbCell(
                                                block.id,
                                                row.id,
                                                activeColId ?: return@clickable,
                                                currentTagIds.joinToString(",")
                                            )
                                            tagSearchQuery = ""
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Create \"${tagSearchQuery.trim()}\"",
                                        fontFamily = PoppinsFont,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            filteredTags.forEach { tag ->
                                val isSelected = currentTagIds.contains(tag.tagId)
                                val tagColor = try {
                                    Color(tag.colorHex.removePrefix("#").toLong(16) or 0xFF000000)
                                } catch (e: Exception) {
                                    MaterialTheme.colorScheme.primary
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) currentTagIds.remove(tag.tagId) else currentTagIds.add(
                                                tag.tagId
                                            )
                                            actions.onUpdateDbCell(
                                                block.id,
                                                row.id,
                                                activeColId ?: return@clickable,
                                                currentTagIds.joinToString(",")
                                            )
                                        }
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = tagColor.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = tag.name,
                                            fontSize = 14.sp,
                                            fontFamily = PoppinsFont,
                                            color = tagColor,
                                            modifier = Modifier.padding(
                                                horizontal = 8.dp,
                                                vertical = 4.dp
                                            )
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // FILE OPTIONS
                DbSheetType.FILE_OPTIONS -> {
                    val row = block.rows.find { it.id == activeRowId }
                    val col = visibleColumns.find { it.id == activeColId }
                    if (row != null && col != null) {
                        val currentFiles =
                            row.cells[activeColId]?.split(",")?.filter { it.isNotBlank() }
                                ?.toMutableList() ?: mutableListOf()

                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {

                            if (col.type == ColumnType.AUDIO) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isRecording) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp).clickable {
                                            if (isRecording) {
                                                isRecording = false
                                                actions.onStopDbAudioRecording(
                                                    block.id,
                                                    row.id,
                                                    col.id,
                                                    false
                                                )
                                            } else {
                                                isRecording = true
                                                actions.onStartRecording()
                                            }
                                        }
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    if (isRecording) {
                                        val mins = recordingDuration / 60
                                        val secs = recordingDuration % 60
                                        Text(
                                            text = "Recording... ${mins}:${
                                                secs.toString().padStart(2, '0')
                                            }",
                                            fontFamily = PoppinsFont,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 14.sp
                                        )
                                    } else {
                                        Text(
                                            "Tap mic to record audio",
                                            fontFamily = PoppinsFont,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(
                                        horizontal = 20.dp,
                                        vertical = 8.dp
                                    ), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                )
                            }

                            currentFiles.forEach { resourceEntry ->
                                val parts = resourceEntry.split("|")
                                val cleanFileName = parts[0].substringAfterLast("/")
                                val resourceName = if (parts.size > 1) parts[1] else cleanFileName

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (col.type == ColumnType.AUDIO) {
                                                if (playingFileUri == cleanFileName) {
                                                    playingFileUri = null
                                                    actions.onStopAudio()
                                                } else {
                                                    if (playingFileUri != null) actions.onStopAudio()
                                                    playingFileUri = cleanFileName
                                                    actions.onPlayAudio(cleanFileName) {
                                                        if (playingFileUri == cleanFileName) playingFileUri =
                                                            null
                                                    }
                                                }
                                            } else {
                                                actions.onOpenFile(cleanFileName, "*/*")
                                            }
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = if (col.type == ColumnType.AUDIO) {
                                            if (playingFileUri == cleanFileName) Icons.Default.Pause else Icons.Default.PlayArrow
                                        } else Icons.AutoMirrored.Filled.InsertDriveFile

                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = resourceName,
                                            fontFamily = PoppinsFont,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp).clickable {
                                            if (playingFileUri == cleanFileName) {
                                                playingFileUri = null
                                                actions.onStopAudio()
                                            }
                                            currentFiles.remove(resourceEntry)
                                            actions.onUpdateDbCell(
                                                block.id,
                                                row.id,
                                                col.id,
                                                currentFiles.joinToString(",")
                                            )
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val rId = row.id
                                        val cId = col.id
                                        val isAud = col.type == ColumnType.AUDIO
                                        applyAction {
                                            actions.onRequestDbFilePicker(
                                                block.id,
                                                rId,
                                                cId,
                                                isAud
                                            )
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = if (col.type == ColumnType.AUDIO) "Upload audio track" else "Attach a new file",
                                    fontFamily = PoppinsFont,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }

                // PRIORITY SELECTION
                DbSheetType.PRIORITY_SELECTION -> {
                    val options = listOf(
                        "Low" to MaterialTheme.colorScheme.outline,
                        "Medium" to MaterialTheme.colorScheme.primary,
                        "High" to MaterialTheme.colorScheme.tertiary,
                        "Urgent" to MaterialTheme.colorScheme.error
                    )
                    val row = block.rows.find { it.id == activeRowId }
                    if (row != null) {
                        val current = row.cells[activeColId] ?: ""

                        options.forEach { (label, color) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val rowId = row.id
                                        val colId = activeColId ?: return@clickable
                                        applyAction {
                                            actions.onUpdateDbCell(
                                                block.id,
                                                rowId,
                                                colId,
                                                label
                                            )
                                        }
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = color.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 14.sp,
                                        fontFamily = PoppinsFont,
                                        color = color,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        )
                                    )
                                }
                                if (current == label) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (current.isNotBlank()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(
                                    horizontal = 20.dp,
                                    vertical = 4.dp
                                ), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            )
                            DbOptionRow(
                                icon = Icons.Default.Close,
                                text = "Clear",
                                color = MaterialTheme.colorScheme.error
                            ) {
                                val rowId = row.id
                                val colId = activeColId ?: return@DbOptionRow
                                applyAction { actions.onUpdateDbCell(block.id, rowId, colId, "") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // AGGREGATION
                DbSheetType.AGGREGATION -> {
                    val col = visibleColumns.find { it.id == activeColId }
                    if (col != null) {
                        val isNum =
                            col.type == ColumnType.NUMBER || col.type == ColumnType.FORMULA || col.type == ColumnType.MONEY
                        val currentAgg = col.aggregationType ?: "None"

                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .animateContentSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        closeSheet()
                                        actions.onUpdateDbAggregation(block.id, col.id, null)
                                    }
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "None",
                                    fontFamily = PoppinsFont,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (currentAgg == "None") Icon(
                                    Icons.Default.Check,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            val groups = mutableListOf(
                                "Count" to listOf(
                                    "Count all",
                                    "Count values",
                                    "Count unique",
                                    "Count empty",
                                    "Count not empty"
                                ),
                                "Percent" to listOf("Percent empty", "Percent not empty")
                            )
                            if (isNum) groups.add(
                                "More options" to listOf(
                                    "Sum",
                                    "Average",
                                    "Min",
                                    "Max",
                                    "Median",
                                    "Range"
                                )
                            )

                            groups.forEach { (groupName, options) ->
                                val isExpanded = aggregationExpandedSection == groupName

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            aggregationExpandedSection =
                                                if (isExpanded) null else groupName
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        groupName,
                                        fontFamily = PoppinsFont,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val rotation by animateFloatAsState(if (isExpanded) -90f else 90f)
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp).rotate(rotation)
                                    )
                                }

                                if (isExpanded) {
                                    options.forEach { opt ->
                                        val isSelected = currentAgg == opt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    closeSheet()
                                                    actions.onUpdateDbAggregation(
                                                        block.id,
                                                        col.id,
                                                        opt
                                                    )
                                                }
                                                .padding(
                                                    start = 40.dp,
                                                    end = 20.dp,
                                                    top = 8.dp,
                                                    bottom = 8.dp
                                                ),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                opt,
                                                fontFamily = PoppinsFont,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) Icon(
                                                Icons.Default.Check,
                                                null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // CURRENCY SELECTION
                DbSheetType.CURRENCY_SELECTION -> {
                    val col = visibleColumns.find { it.id == activeColId }
                    if (col != null) {
                        val currencies = listOf(
                            "$" to "US Dollar",
                            "€" to "Euro",
                            "£" to "British Pound",
                            "¥" to "Yen",
                            "₹" to "Rupee",
                            "A$" to "Australian Dollar",
                            "C$" to "Canadian Dollar"
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            currencies.forEach { (symbol, name) ->
                                val isSelected = (col.currencySymbol ?: "$") == symbol
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            closeSheet()
                                            actions.onUpdateDbCurrency(block.id, col.id, symbol)
                                        }
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "$name ($symbol)",
                                        fontFamily = PoppinsFont,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) Icon(
                                        Icons.Default.Check,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }

    val DesktopDbDropdown = @Composable { visible: Boolean ->
        if (isDesktopPlatform && visible) {
            InlyDesktopMenu(
                expanded = true,
                onDismissRequest = { closeSheet() }
            ) {
                AnimatedContent(
                    // FIX: Watch ONLY currentSheet
                    targetState = currentSheet,
                    transitionSpec = {
                        val isGoingDeeper = targetState !in listOf(DbSheetType.COLUMN_OPTIONS, DbSheetType.CELL_OPTIONS, DbSheetType.NONE)
                        if (isGoingDeeper) {
                            (slideInHorizontally(tween(200)) { it } + fadeIn(tween(200))) togetherWith
                                    (slideOutHorizontally(tween(200)) { -it / 2 } + fadeOut(tween(200))) using SizeTransform(clip = false)
                        } else {
                            (slideInHorizontally(tween(200)) { -it / 2 } + fadeIn(tween(200))) togetherWith
                                    (slideOutHorizontally(tween(200)) { it } + fadeOut(tween(200))) using SizeTransform(clip = false)
                        }
                    },
                    label = "DesktopDbTransition"
                ) { target ->
                    Box(modifier = Modifier.widthIn(min = 280.dp, max = 340.dp).padding(vertical = 4.dp)) {
                        sheetContent(target)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { if (inSelectionMode) actions.onToggleSelection(block.id) },
                onLongClick = { actions.onToggleSelection(block.id) }
            )
    ) {
        // Title + Sort/Filter toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            var titleTfv by remember(block.id) {
                mutableStateOf(TextFieldValue(block.title, TextRange(block.title.length)))
            }
            var lastSentTitle by remember(block.id) { mutableStateOf(block.title) }

            LaunchedEffect(block.title) {
                if (titleTfv.text != block.title && block.title != lastSentTitle) {
                    titleTfv = titleTfv.copy(
                        text = block.title,
                        selection = TextRange(titleTfv.selection.start.coerceAtMost(block.title.length))
                    )
                }
            }

            LaunchedEffect(titleTfv.text) {
                if (titleTfv.text != block.title) {
                    delay(400L)
                    lastSentTitle = titleTfv.text
                    actions.onUpdateDbTitle(block.id, titleTfv.text)
                }
            }

            BasicTextField(
                value = titleTfv,
                onValueChange = { titleTfv = it },
                textStyle = TextStyle(
                    fontFamily = PoppinsFont,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (titleTfv.text.isEmpty()) {
                        Text(
                            text = "Untitled Database",
                            fontFamily = PoppinsFont,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    inner()
                },
                modifier = Modifier.weight(1f).padding(end = 12.dp),
                enabled = !inSelectionMode,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    val hasSort = block.activeSorts.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasSort) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = block.activeSorts.firstOrNull()?.columnId ?: visibleColumns.first().id
                                currentSheet = DbSheetType.SORT
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (hasSort) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.SORT)
                }

                Box {
                    val hasFilter = block.activeFilters.isNotEmpty()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasFilter) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
                        modifier = Modifier.clickable(enabled = !inSelectionMode) {
                            if (visibleColumns.isNotEmpty()) {
                                activeColId = visibleColumns.first().id
                                textInput = ""
                                textInputMax = ""
                                filterOperator = "contains"
                                currentSheet = DbSheetType.FILTER
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                            Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (hasFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    DesktopDbDropdown(currentSheet == DbSheetType.FILTER)
                }
            }
        }

        // Active filter chips
        if (block.activeFilters.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                block.activeFilters.forEach { filter ->
                    val colName = visibleColumns.find { it.id == filter.columnId }?.name ?: "?"
                    val label = when (filter.operator) {
                        "not_empty"    -> "$colName is not empty"
                        "empty"        -> "$colName is empty"
                        "checked"      -> "$colName is checked"
                        "unchecked"    -> "$colName is unchecked"
                        "priority"     -> "$colName = ${filter.value}"
                        "gt"           -> "$colName > ${filter.value}"
                        "lt"           -> "$colName < ${filter.value}"
                        "gte"          -> "$colName ≥ ${filter.value}"
                        "lte"          -> "$colName ≤ ${filter.value}"
                        "between"      -> { val p = filter.value.split("|"); if (p.size == 2) "$colName: ${p[0]} – ${p[1]}" else "$colName between" }
                        "before"       -> "$colName before ${filter.value}"
                        "after"        -> "$colName after ${filter.value}"
                        "starts_with"  -> "$colName starts with \"${filter.value}\""
                        "ends_with"    -> "$colName ends with \"${filter.value}\""
                        "not_contains" -> "$colName does not contain \"${filter.value}\""
                        "not_equals"   -> "$colName is not \"${filter.value}\""
                        else           -> "$colName ${filter.operator} \"${filter.value}\""
                    }

                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(text = label, fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(13.dp).clickable(enabled = !inSelectionMode) { actions.onRemoveDbFilter(block.id, filter) }, tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Table grid
        val bp = borderColor
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState).haze(state = hazeState)) {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Surface(
                    shape = RoundedCornerShape(0.dp),
                    color = Color.Transparent,
                    border = BorderStroke(0.6.dp, borderColor1)
                ) {
                    Column {
                        // Header row
                        Row(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                                .height(IntrinsicSize.Max)
                                .defaultMinSize(minHeight = 44.dp)
                        ) {
                            visibleColumns.forEachIndexed { colIndex, col ->
                                val activeSort = block.activeSorts.find { it.columnId == col.id }

                                val typeIcon = when (col.type) {
                                    ColumnType.TEXT     -> Icons.AutoMirrored.Filled.Subject
                                    ColumnType.NUMBER   -> Icons.Default.Numbers
                                    ColumnType.CHECKBOX -> Icons.Default.CheckBox
                                    ColumnType.DATE     -> Icons.Default.CalendarToday
                                    ColumnType.FORMULA  -> Icons.Default.Functions
                                    ColumnType.PHONE    -> Icons.Default.Phone
                                    ColumnType.EMAIL    -> Icons.Default.Email
                                    ColumnType.TAGS     -> Icons.Default.LocalOffer
                                    ColumnType.URL      -> Icons.Default.Link
                                    ColumnType.FILES    -> Icons.Default.AttachFile
                                    ColumnType.PRIORITY -> Icons.Default.Flag
                                    ColumnType.MONEY    -> Icons.Default.MonetizationOn
                                    ColumnType.AUDIO    -> Icons.Default.Mic
                                    ColumnType.NOTES    -> Icons.AutoMirrored.Filled.Notes
                                }

                                Box {
                                    Box(
                                        modifier = Modifier
                                            .width(col.width.dp)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 44.dp)
                                            .drawBehind {
                                                val px = 0.5.dp.toPx()
                                                drawLine(bp, Offset(size.width, 0f), Offset(size.width, size.height), px)
                                                drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                            }
                                            .clickable(enabled = !inSelectionMode) {
                                                activeColId = col.id
                                                currentSheet = DbSheetType.COLUMN_OPTIONS
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.TopStart
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = typeIcon,
                                                contentDescription = null,
                                                modifier = Modifier.size(13.dp),
                                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Spacer(Modifier.width(7.dp))
                                            Text(
                                                text = col.name,
                                                fontFamily = PoppinsFont, fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            if (activeSort != null) {
                                                if (block.activeSorts.size > 1) {
                                                    val layerIndex = block.activeSorts.indexOfFirst { it.columnId == col.id } + 1
                                                    Text(
                                                        text = "$layerIndex",
                                                        fontFamily = PoppinsFont, fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(end = 2.dp)
                                                    )
                                                }
                                                Icon(
                                                    imageVector = if (activeSort.isAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    DesktopDbDropdown(activeColId == col.id && currentSheet in listOf(DbSheetType.COLUMN_OPTIONS, DbSheetType.RENAME, DbSheetType.FORMULA))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(44.dp)
                                    .defaultMinSize(minHeight = 47.dp)
                                    .drawBehind {
                                        val px = 0.5.dp.toPx()
                                        drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                    }
                                    .clickable(enabled = !inSelectionMode) {
                                        actions.onAddDbColumn(block.id)
                                        coroutineScope.launch { delay(150); scrollState.animateScrollTo(scrollState.maxValue) }
                                    }
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Data rows
                        visibleRows.forEach { row ->
                            Row(modifier = Modifier.height(IntrinsicSize.Max).defaultMinSize(minHeight = 44.dp)) {
                                visibleColumns.forEach { col ->
                                    val cellValue = row.cells[col.id] ?: ""
                                    val isHighlighted = currentSheet == DbSheetType.CELL_OPTIONS && activeRowId == row.id && activeColId == col.id

                                    Box {
                                        Box(
                                            modifier = Modifier
                                                .width(col.width.dp)
                                                .fillMaxHeight()
                                                .defaultMinSize(minHeight = 44.dp)
                                                .drawBehind {
                                                    val px = 0.5.dp.toPx()
                                                    drawLine(bp, Offset(size.width, 0f), Offset(size.width, size.height), px)
                                                    drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                                }
                                                .then(if (isHighlighted) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary) else Modifier)
                                                .pointerInput(inSelectionMode) {
                                                    awaitEachGesture {
                                                        awaitFirstDown(requireUnconsumed = false)
                                                        var isLongPress = false
                                                        try {
                                                            withTimeout(viewConfiguration.longPressTimeoutMillis) {
                                                                waitForUpOrCancellation()
                                                            }
                                                        } catch (e: PointerEventTimeoutCancellationException) {
                                                            isLongPress = true
                                                            currentEvent.changes.forEach { it.consume() }
                                                        }
                                                        if (isLongPress && !inSelectionMode) {
                                                            focusManager.clearFocus()
                                                            activeRowId = row.id
                                                            activeColId = col.id
                                                            currentSheet = DbSheetType.CELL_OPTIONS
                                                        }
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            TableCell(
                                                value = cellValue,
                                                allLinkableNotes = allLinkableNotes,
                                                columnType = col.type,
                                                cellWidth = col.width.dp,
                                                globalTags = globalTags,
                                                inSelectionMode = inSelectionMode,
                                                currencySymbol = col.currencySymbol ?: "$",
                                                isFormulaCurrency = col.isFormulaCurrency,
                                                onValueChange = { actions.onUpdateDbCell(block.id, row.id, col.id, it) },
                                                onDateClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        currentSheet = DbSheetType.NONE
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        showDatePicker = true
                                                    }
                                                },
                                                onTagClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.TAG_SELECTION
                                                    }
                                                },
                                                onFileClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.FILE_OPTIONS
                                                    }
                                                },
                                                onPriorityClick = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.PRIORITY_SELECTION
                                                    }
                                                },
                                                onNoteClick = {
                                                    if (!inSelectionMode) {
                                                        actions.onOpenDatabaseNote(block.id, row.id, col.id, cellValue.ifBlank { null })
                                                    }
                                                },
                                                onGetNoteTitle = { id -> actions.getNoteTitle(id) },
                                                onCreateLinkedNote = { title -> actions.onCreateLinkedNote(title) },
                                                onLongPress = {
                                                    if (!inSelectionMode) {
                                                        focusManager.clearFocus()
                                                        activeRowId = row.id
                                                        activeColId = col.id
                                                        currentSheet = DbSheetType.CELL_OPTIONS
                                                    }
                                                }
                                            )
                                        }
                                        DesktopDbDropdown(activeRowId == row.id && activeColId == col.id && currentSheet in listOf(DbSheetType.CELL_OPTIONS, DbSheetType.TAG_SELECTION, DbSheetType.FILE_OPTIONS, DbSheetType.PRIORITY_SELECTION))
                                    }
                                }

                                // Trailing spacer — bottom edge only
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .drawBehind {
                                            val px = 0.5.dp.toPx()
                                            drawLine(bp, Offset(0f, size.height), Offset(size.width, size.height), px)
                                        }
                                )
                            }
                        }
                    }
                }

                // Aggregation row
                Row(modifier = Modifier.height(IntrinsicSize.Max).defaultMinSize(minHeight = 36.dp)) {
                    visibleColumns.forEach { col ->
                        val aggType = col.aggregationType
                        val isActivelyEditing = currentSheet == DbSheetType.AGGREGATION && activeColId == col.id
                        val isCurr  = col.type == ColumnType.MONEY || (col.type == ColumnType.FORMULA && col.isFormulaCurrency)
                        val prefix  = if (isCurr) (col.currencySymbol ?: "$") else ""

                        val displayValue = if (aggType == null) {
                            if (isActivelyEditing) "Calculate" else ""
                        } else {
                            val values  = visibleRows.mapNotNull { it.cells[col.id] }
                            val numbers = values.mapNotNull { it.toDoubleOrNull() }
                            fun Double.fmt() = if (this == this.toLong().toDouble()) this.toLong().toString() else ((this * 100.0).toLong() / 100.0).toString()

                            val result = when (aggType) {
                                "Count all"         -> visibleRows.size.toString()
                                "Count values"      -> values.count { it.isNotBlank() }.toString()
                                "Count unique"      -> values.filter { it.isNotBlank() }.distinct().size.toString()
                                "Count empty"       -> visibleRows.count { it.cells[col.id].isNullOrBlank() }.toString()
                                "Count not empty"   -> values.count { it.isNotBlank() }.toString()
                                "Percent empty"     -> if (visibleRows.isEmpty()) "0%" else "${(visibleRows.count { it.cells[col.id].isNullOrBlank() } * 100 / visibleRows.size)}%"
                                "Percent not empty" -> if (visibleRows.isEmpty()) "0%" else "${(values.count { it.isNotBlank() } * 100 / visibleRows.size)}%"
                                "Sum"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.sum().fmt()}"
                                "Average" -> if (numbers.isEmpty()) "" else "$prefix${(numbers.sum() / numbers.size).fmt()}"
                                "Min"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.minOrNull()?.fmt() ?: ""}"
                                "Max"     -> if (numbers.isEmpty()) "" else "$prefix${numbers.maxOrNull()?.fmt() ?: ""}"
                                "Median"  -> {
                                    if (numbers.isEmpty()) ""
                                    else {
                                        val sorted = numbers.sorted()
                                        if (sorted.size % 2 == 0) "$prefix${((sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2).fmt()}"
                                        else "$prefix${sorted[sorted.size / 2].fmt()}"
                                    }
                                }
                                "Range" -> if (numbers.isEmpty()) "" else "$prefix${(numbers.maxOrNull()!! - numbers.minOrNull()!!).fmt()}"
                                else    -> ""
                            }
                            if (result.isEmpty()) aggType else "$aggType $result"
                        }

                        Box(
                            modifier = Modifier
                                .width(col.width.dp)
                                .fillMaxHeight()
                                .defaultMinSize(minHeight = 36.dp)
                                .clickable(enabled = !inSelectionMode) {
                                    activeColId = col.id
                                    currentSheet = DbSheetType.AGGREGATION
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Text(
                                text = displayValue,
                                fontFamily = PoppinsFont, fontSize = 13.sp,
                                color = if (aggType == null) MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        DesktopDbDropdown(activeColId == col.id && currentSheet == DbSheetType.AGGREGATION)
                    }
                    Box(modifier = Modifier.width(44.dp).fillMaxHeight().defaultMinSize(minHeight = 36.dp))
                }

                // Add row button
                Row(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !inSelectionMode) { actions.onAddDbRow(block.id) }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(7.dp))
                    Text(text = "New Row", fontFamily = PoppinsFont, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }

    if (!isDesktopPlatform && currentSheet != DbSheetType.NONE) {
        InlyBottomSheet(expanded = true, onDismiss = { closeSheet() }, title = null) { _ ->
            AnimatedContent(
                // FIX: Watch ONLY currentSheet, not activeColId
                targetState = currentSheet,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220, delayMillis = 90))) togetherWith
                            fadeOut(animationSpec = tween(90)) using
                            SizeTransform(clip = false)
                },
                label = "MobileDbTransition"
            ) { target ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    sheetContent(target)
                }
            }
        }
    }

    if (showDatePicker && activeRowId != null && activeColId != null) {
        val currentValue = block.rows.find { it.id == activeRowId }?.cells?.get(activeColId)

        val initialTimestamp = try {
            if (!currentValue.isNullOrBlank()) {
                kotlinx.datetime.Instant.parse("${currentValue}T00:00:00Z").toEpochMilliseconds()
            } else null
        } catch (e: Exception) { null }

        MinimalDatePickerDialog(
            expanded = showDatePicker,
            initialTimestamp = initialTimestamp,
            onDismiss = { showDatePicker = false },
            onConfirm = { millis ->
                val rowToUpdate = activeRowId
                val colToUpdate = activeColId
                showDatePicker = false

                if (rowToUpdate != null && colToUpdate != null) {
                    scope.launch {
                        val dateStr = kotlinx.datetime.Instant.fromEpochMilliseconds(millis)
                            .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date.toString()

                        delay(150)
                        actions.onUpdateDbCell(block.id, rowToUpdate, colToUpdate, dateStr)
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun TableCell(
    value: String,
    columnType: ColumnType,
    cellWidth: Dp,
    globalTags: List<TagEntity>,
    inSelectionMode: Boolean,
    currencySymbol: String = "$",
    isFormulaCurrency: Boolean = false,
    onValueChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onTagClick: () -> Unit,
    onFileClick: () -> Unit,
    onPriorityClick: () -> Unit,
    onNoteClick: () -> Unit,
    onGetNoteTitle: suspend (String) -> String,
    allLinkableNotes: List<NoteMetadataEntity>,
    onCreateLinkedNote: (String) -> String,
    onLongPress: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    val validNoteIds = remember(allLinkableNotes) { allLinkableNotes.map { it.noteId }.toSet() }

    when (columnType) {
        ColumnType.TEXT, ColumnType.NUMBER, ColumnType.PHONE, ColumnType.EMAIL, ColumnType.URL, ColumnType.MONEY -> {
            var isFocused by remember { mutableStateOf(false) }
            val focusRequester = remember { FocusRequester() }

            Box(modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { if (!inSelectionMode) focusRequester.requestFocus() }) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {

                    if (columnType == ColumnType.MONEY && (value.isNotBlank() || isFocused)) {
                        Text(text = currencySymbol, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 4.dp))
                    }

                    IsolatedTableCellTextField(
                        initialText = value,
                        columnType = columnType,
                        allLinkableNotes = allLinkableNotes,
                        visualTransformation = if (columnType == ColumnType.TEXT) NoteLinkVisualTransformation(
                            linkColor = MaterialTheme.colorScheme.primary,
                            fadedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            validNoteIds = validNoteIds
                        ) else androidx.compose.ui.text.input.VisualTransformation.None,
                        inSelectionMode = inSelectionMode,
                        focusRequester = focusRequester,
                        onValueChange = onValueChange,
                        onFocusChanged = { isFocused = it },
                        onCreateLinkedNote = onCreateLinkedNote,
                        modifier = Modifier.weight(1f).defaultMinSize(minWidth = cellWidth - 24.dp)
                    )

                    val isLinkType = columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL
                    if (isLinkType && value.isNotBlank() && !isFocused) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open Link", modifier = Modifier.size(16.dp).clickable {
                                when (columnType) {
                                    ColumnType.EMAIL -> try { uriHandler.openUri("mailto:$value") } catch (e: Exception) {}
                                    ColumnType.PHONE -> try { uriHandler.openUri("tel:$value") } catch (e: Exception) {}
                                    ColumnType.URL -> try {
                                        val url = if (!value.startsWith("http://") && !value.startsWith("https://")) "https://$value" else value
                                        uriHandler.openUri(url)
                                    } catch (e: Exception) {}
                                    else -> {}
                                }
                            }, tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        ColumnType.CHECKBOX -> {
            val isChecked = value == "true"
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                    Checkbox(
                        checked = isChecked, onCheckedChange = { if (!inSelectionMode) onValueChange(it.toString()) }, modifier = Modifier.scale(0.9f).size(18.dp),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.surface, checkmarkColor = MaterialTheme.colorScheme.primary, uncheckedColor = MaterialTheme.colorScheme.outline)
                    )
                }
            }
        }
        ColumnType.DATE -> {
            Text(
                text = value.ifEmpty { "—" }, fontFamily = PoppinsFont, fontSize = 14.sp,
                color = if (value.isEmpty()) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !inSelectionMode) { onDateClick() }
            )
        }
        ColumnType.FORMULA -> {
            val formulaScrollState = rememberScrollState()
            val isInvalid = value.equals("NaN", ignoreCase = true) || value.startsWith("Error", ignoreCase = true)

            val displayValue = if (isInvalid) "" else {
                if (isFormulaCurrency && value.toDoubleOrNull() != null) {
                    val num = value.toDouble()
                    val fmt = if (num == num.toLong().toDouble()) num.toLong().toString() else ((num * 100.0).toLong() / 100.0).toString()
                    "$currencySymbol$fmt"
                } else value
            }

            Text(
                text = displayValue, fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1,
                modifier = Modifier.horizontalScroll(formulaScrollState).mouseScrollable(formulaScrollState)
            )
        }
        ColumnType.TAGS -> {
            val activeTagIds = value.split(",").filter { it.isNotBlank() }
            val activeTags = activeTagIds.mapNotNull { id -> globalTags.find { it.tagId == id } }

            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onTagClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
                if (activeTags.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        activeTags.forEach { tag ->
                            val tagColor = try { Color(tag.colorHex.removePrefix("#").toLong(16) or 0xFF000000) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                            Surface(shape = RoundedCornerShape(4.dp), color = tagColor.copy(alpha = 0.15f)) {
                                Text(text = tag.name, fontSize = 12.sp, fontFamily = PoppinsFont, color = tagColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }

        ColumnType.FILES, ColumnType.AUDIO -> {
            val resources = value.split(",").filter { it.isNotBlank() }

            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onFileClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
                if (resources.isEmpty()) {
                    Text("Empty", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), fontSize = 14.sp)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        resources.forEach { resourceEntry ->
                            val parts = resourceEntry.split("|")
                            val cleanFileName = parts[0].substringAfterLast("/")
                            val resourceName = if (parts.size > 1) parts[1] else cleanFileName

                            Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surface) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                    Icon(
                                        imageVector = if (columnType == ColumnType.AUDIO) Icons.Default.Mic else Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = resourceName, fontSize = 12.sp, fontFamily = PoppinsFont, color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 100.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ColumnType.PRIORITY -> {
            Box(modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 24.dp).combinedClickable(onClick = { if (!inSelectionMode) onPriorityClick() }, onLongClick = { if (!inSelectionMode) onLongPress() })) {
                if (value.isBlank()) {
                    Text("—", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f), fontSize = 14.sp)
                } else {
                    val chipColor = when (value) {
                        "Low"    -> MaterialTheme.colorScheme.outline
                        "Medium" -> MaterialTheme.colorScheme.primary
                        "High"   -> MaterialTheme.colorScheme.tertiary
                        "Urgent" -> MaterialTheme.colorScheme.error
                        else     -> MaterialTheme.colorScheme.outline
                    }
                    Surface(shape = RoundedCornerShape(4.dp), color = chipColor.copy(alpha = 0.15f)) {
                        Text(text = value, fontSize = 13.sp, fontFamily = PoppinsFont, color = chipColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
            }
        }

        ColumnType.NOTES -> {
            val reactiveNote = allLinkableNotes.find { it.noteId == value }
            var noteTitle by remember(value) { mutableStateOf("Loading...") }

            LaunchedEffect(value, reactiveNote) {
                if (value.isNotBlank()) {
                    if (reactiveNote != null) {
                        noteTitle = reactiveNote.title.ifBlank { "Untitled Note" }
                    } else {
                        val title = onGetNoteTitle(value)
                        noteTitle = title.ifBlank { "Untitled Note" }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 24.dp)
                    .combinedClickable(
                        onClick = { if (!inSelectionMode) onNoteClick() },
                        onLongClick = { if (!inSelectionMode) onLongPress() }
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                        Spacer(Modifier.width(4.dp))
                        Text("New Note", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), fontSize = 13.sp, fontFamily = PoppinsFont)
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Icon(Icons.AutoMirrored.Filled.Notes, null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = noteTitle,
                                fontSize = 12.sp,
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = cellWidth - 45.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IsolatedTableCellTextField(
    initialText: String,
    columnType: ColumnType,
    inSelectionMode: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    allLinkableNotes: List<NoteMetadataEntity>,
    onCreateLinkedNote: (String) -> String,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation = androidx.compose.ui.text.input.VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    var tfv by remember { mutableStateOf(TextFieldValue(initialText, TextRange(initialText.length))) }
    var lastSentText by remember { mutableStateOf(initialText) }

    var mentionQuery by remember { mutableStateOf<String?>(null) }
    var mentionAnchorRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    var mentionStartIndex by remember { mutableIntStateOf(-1) }

    LaunchedEffect(initialText) {
        if (tfv.text != initialText && initialText != lastSentText) {
            val safeStart = tfv.selection.start.coerceAtMost(initialText.length)
            val safeEnd = tfv.selection.end.coerceAtMost(initialText.length)
            tfv = tfv.copy(text = initialText, selection = TextRange(safeStart, safeEnd))
        }
    }

    LaunchedEffect(tfv.text) {
        if (tfv.text != initialText) {
            if (mentionQuery == null) delay(400L)
            lastSentText = tfv.text
            onValueChange(tfv.text)
        }
    }

    val isSingleLine = columnType != ColumnType.TEXT

    Box(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = tfv,
            onValueChange = { newValue ->
                val newText = newValue.text
                val cursor = newValue.selection.start

                if (cursor > 0 && cursor <= newText.length) {
                    val textUpToCursor = newText.substring(0, cursor)
                    val lastAt = textUpToCursor.lastIndexOf('@')
                    val isValidAt = lastAt != -1 && (lastAt == 0 || textUpToCursor[lastAt - 1] == ' ' || textUpToCursor[lastAt - 1] == '\n')

                    if (isValidAt && !textUpToCursor.substring(lastAt).contains(" ")) {
                        mentionQuery = textUpToCursor.substring(lastAt + 1)
                        mentionStartIndex = lastAt
                    } else {
                        mentionQuery = null
                        mentionStartIndex = -1
                    }
                } else {
                    mentionQuery = null
                    mentionStartIndex = -1
                }

                tfv = newValue
            },
            onTextLayout = { result ->
                if (mentionStartIndex != -1) {
                    val transformedText = visualTransformation.filter(androidx.compose.ui.text.AnnotatedString(tfv.text))
                    val mappedIndex = transformedText.offsetMapping.originalToTransformed(mentionStartIndex)
                    val safeMappedIndex = mappedIndex.coerceIn(0, transformedText.text.length)
                    mentionAnchorRect = result.getCursorRect(safeMappedIndex)
                }
            },
            enabled = !inSelectionMode,
            textStyle = TextStyle(
                fontFamily = PoppinsFont, fontSize = 14.sp,
                color = if ((columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL) && tfv.text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                textDecoration = if ((columnType == ColumnType.EMAIL || columnType == ColumnType.PHONE || columnType == ColumnType.URL) && tfv.text.isNotBlank()) TextDecoration.Underline else TextDecoration.None
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = when (columnType) {
                ColumnType.NUMBER, ColumnType.MONEY -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
                ColumnType.PHONE -> KeyboardOptions(keyboardType = KeyboardType.Phone)
                ColumnType.EMAIL -> KeyboardOptions(keyboardType = KeyboardType.Email)
                ColumnType.URL -> KeyboardOptions(keyboardType = KeyboardType.Uri)
                else -> KeyboardOptions.Default
            },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            singleLine = isSingleLine,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { onFocusChanged(it.isFocused) }
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Backspace && event.type == KeyEventType.KeyDown) {
                        val cursor = tfv.selection.start
                        if (cursor > 0 && tfv.selection.collapsed) {
                            val textBeforeCursor = tfv.text.substring(0, cursor)
                            val match = """\[([^\]]+)\]\(inly://note/([^)]+)\)$""".toRegex().find(textBeforeCursor)

                            if (match != null) {
                                val textBeforeLink = textBeforeCursor.substring(0, match.range.first)
                                val textAfterCursor = tfv.text.substring(cursor)
                                val newText = textBeforeLink + textAfterCursor

                                tfv = tfv.copy(text = newText, selection = TextRange(textBeforeLink.length))
                                onValueChange(newText)
                                return@onPreviewKeyEvent true
                            }
                        }
                    }
                    false
                }
        )

        val currentQuery = mentionQuery
        if (currentQuery != null) {
            val filteredNotes = allLinkableNotes.filter {
                it.title.contains(currentQuery, ignoreCase = true)
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = with(androidx.compose.ui.platform.LocalDensity.current) { mentionAnchorRect.left.toDp() },
                        y = with(androidx.compose.ui.platform.LocalDensity.current) { mentionAnchorRect.top.toDp() }
                    )
                    .size(
                        width = 1.dp,
                        height = with(androidx.compose.ui.platform.LocalDensity.current) { mentionAnchorRect.height.toDp() }
                    )
            ) {
                val positionProvider = remember {
                    object : androidx.compose.ui.window.PopupPositionProvider {
                        override fun calculatePosition(
                            anchorBounds: androidx.compose.ui.unit.IntRect,
                            windowSize: androidx.compose.ui.unit.IntSize,
                            layoutDirection: androidx.compose.ui.unit.LayoutDirection,
                            popupContentSize: androidx.compose.ui.unit.IntSize
                        ): androidx.compose.ui.unit.IntOffset {
                            var y = anchorBounds.bottom + 8
                            if (y + popupContentSize.height > windowSize.height - 16) {
                                y = anchorBounds.top - popupContentSize.height - 8
                            }
                            var x = anchorBounds.left
                            if (x + popupContentSize.width > windowSize.width - 16) {
                                x = windowSize.width - popupContentSize.width - 16
                            }
                            return androidx.compose.ui.unit.IntOffset(x, Math.max(0, y))
                        }
                    }
                }

                androidx.compose.ui.window.Popup(
                    popupPositionProvider = positionProvider,
                    properties = androidx.compose.ui.window.PopupProperties(focusable = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .width(260.dp)
                            .heightIn(max = 300.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp).verticalScroll(rememberScrollState())) {
                            Text(
                                text = "LINK TO NOTE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = PoppinsFont,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )

                            filteredNotes.forEach { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
                                            val textUpToCursor = tfv.text.substring(0, cursor)
                                            val lastAt = textUpToCursor.lastIndexOf('@')

                                            if (lastAt != -1) {
                                                val safeTitle = note.title.replace("[", "").replace("]", "").ifEmpty { "Untitled" }
                                                val markdownLink = "[$safeTitle](inly://note/${note.noteId}) "

                                                val textBefore = tfv.text.substring(0, lastAt)
                                                val textAfter = tfv.text.substring(cursor)

                                                val newText = textBefore + markdownLink + textAfter
                                                val newCursor = textBefore.length + markdownLink.length

                                                tfv = tfv.copy(
                                                    text = newText,
                                                    selection = TextRange(newCursor),
                                                    composition = null
                                                )
                                                onValueChange(newText)
                                            }
                                            mentionQuery = null
                                            mentionStartIndex = -1
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = note.title.ifEmpty { "Untitled" },
                                        fontFamily = PoppinsFont,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (currentQuery.isNotBlank()) {
                                if (filteredNotes.isNotEmpty()) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val cursor = tfv.selection.start.coerceIn(0, tfv.text.length)
                                            val textUpToCursor = tfv.text.substring(0, cursor)
                                            val lastAt = textUpToCursor.lastIndexOf('@')

                                            if (lastAt != -1) {
                                                val cleanTitle = currentQuery.replace("[", "").replace("]", "").trim()
                                                val safeTitle = cleanTitle.ifEmpty { "Untitled" }

                                                val newNoteId = onCreateLinkedNote(safeTitle)
                                                val markdownLink = "[$safeTitle](inly://note/$newNoteId) "

                                                val textBefore = tfv.text.substring(0, lastAt)
                                                val textAfter = tfv.text.substring(cursor)

                                                val newText = textBefore + markdownLink + textAfter
                                                val newCursor = textBefore.length + markdownLink.length

                                                tfv = tfv.copy(
                                                    text = newText,
                                                    selection = TextRange(newCursor),
                                                    composition = null
                                                )
                                                onValueChange(newText)
                                            }
                                            mentionQuery = null
                                            mentionStartIndex = -1
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("New \"$currentQuery\" note", fontFamily = PoppinsFont, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            } else if (filteredNotes.isEmpty()) {
                                Text(
                                    text = "Start typing to search...",
                                    fontSize = 13.sp,
                                    fontFamily = PoppinsFont,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}