package com.ben.inly.domain.model

import androidx.compose.runtime.Immutable
import com.ben.inly.data.local.room.NoteMetadataEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The root container for a saved note.
 * It holds a list of blocks and a version number so I can handle migrations easily if the structure changes later.
 */

@Immutable
@Serializable
@SerialName("row_container")
data class RowContainerBlock(
    override val id: String,
    val columns: List<ColumnBlock>,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
data class ColumnBlock(
    val id: String,
    val blocks: List<NoteBlock>,
    val weight: Float = 1f
)

@Immutable
@Serializable
data class NoteContent(
    val version: Int = 1,
    val blocks: List<NoteBlock>
)

/**
 * A single cross-note search hit. [matchedText] is whichever snippet actually contains the
 * query - the note's own snippet/title for a metadata match, or the flattened text of the
 * first matching block for a content-only match - so the UI has one field to highlight.
 */
@Immutable
data class NoteSearchResult(
    val note: NoteMetadataEntity,
    val matchedText: String
)

/**
 * The base class for everything in the editor.
 * The editor is block-based, meaning every paragraph, image, or list item is its own distinct, serializable block.
 */

@Immutable
@Serializable
sealed class NoteBlock {
    abstract val id: String
    abstract val indentationLevel: Int
    abstract val isBold: Boolean
    abstract val isItalic: Boolean
    abstract val isStrikeThrough: Boolean
    abstract val isUnderlined: Boolean
    abstract val isDeleted: Boolean
    abstract val isPinned: Boolean
    abstract val updatedAt: Long
}

@Immutable
@Serializable
@SerialName("text")
data class TextBlock(
    override val id: String,
    val text: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("heading")
data class HeadingBlock(
    override val id: String,
    val text: String = "",
    val level: Int = 1,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("quote")
data class QuoteBlock(
    override val id: String,
    val text: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("checkbox")
data class CheckboxBlock(
    override val id: String,
    val text: String = "",
    val isChecked: Boolean = false,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    val reminderTimestamp: Long? = null,
    val completedAt: Long? = null,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("bullet")
data class BulletedListBlock(
    override val id: String,
    val text: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("number")
data class NumberedListBlock(
    override val id: String,
    val text: String = "",
    val number: Int = 1,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("toggle")
data class ToggleBlock(
    override val id: String,
    val text: String = "",
    val isExpanded: Boolean = true,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("code")
data class CodeBlock(
    override val id: String,
    val code: String = "",
    val language: String = "plaintext",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("bookmark")
data class BookmarkBlock(
    override val id: String,
    val url: String = "",
    val title: String? = null,
    val description: String? = null,
    val previewImageUrl: String? = null,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("image")
data class ImageBlock(
    override val id: String,
    val localFilePath: String? = null,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("document")
data class DocumentBlock(
    override val id: String,
    val localFilePath: String? = null,
    val fileName: String = "Unknown Document",
    val mimeType: String = "application/octet-stream",
    val fileSizeString: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

enum class ViewType { TABLE, KANBAN, GALLERY }

/** Card density for a GALLERY view - purely a layout knob. */
enum class GalleryCardSize { SMALL, MEDIUM, LARGE }

@Immutable
@Serializable
data class DatabaseView(
    val id: String,
    val name: String,
    val type: ViewType,
    val activeSorts: List<SortConfig> = emptyList(),
    val activeFilters: List<FilterConfig> = emptyList(),
    val groupByColumnId: String? = null,
    val hiddenGroups: List<String> = emptyList(),
    val groupOrder: List<String> = emptyList(),
    val galleryCardSize: GalleryCardSize = GalleryCardSize.MEDIUM
)

@Immutable
@Serializable
@SerialName("database")
data class DatabaseBlock(
    override val id: String,
    val title: String = "",
    val columns: List<DatabaseColumn>,
    val rows: List<DatabaseRow>,
    val views: List<DatabaseView> = emptyList(),
    val activeViewId: String? = null,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
data class DatabaseColumn(
    val id: String,
    val databaseId: String,
    val name: String,
    val type: ColumnType,
    val width: Int = 140,
    val formulaExpression: String? = null,
    val aggregationType: String? = null,
    val currencySymbol: String? = null,
    val isFormulaCurrency: Boolean = false,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0L
)

@Immutable
@Serializable
data class DatabaseRow(
    val id: String,
    val databaseId: String,
    val cells: Map<String, CellData>,
    val isDeleted: Boolean = false,
    val updatedAt: Long = 0L
)

/**
 * Every value a database cell can hold. Each [ColumnType] maps to exactly one subclass, so a cell's Kotlin type always matches what the column expects.
 * Nested (not top-level) so `Number`/`Boolean`/`Date` don't shadow the `kotlin.*` types of the same name.
 */
@Immutable
@Serializable
sealed class CellData {
    @Immutable
    @Serializable
    @SerialName("text")
    data class Text(val value: String) : CellData()

    @Immutable
    @Serializable
    @SerialName("number")
    data class Number(val value: Double?) : CellData()

    @Immutable
    @Serializable
    @SerialName("boolean")
    data class Boolean(val value: kotlin.Boolean) : CellData()

    @Immutable
    @Serializable
    @SerialName("date")
    data class Date(val timestamp: Long?) : CellData()

    @Immutable
    @Serializable
    @SerialName("tag_list")
    data class TagList(val tagIds: List<String>) : CellData()

    @Immutable
    @Serializable
    @SerialName("media_list")
    data class MediaList(val files: List<MediaItem>) : CellData()

    @Immutable
    @Serializable
    @SerialName("note_relation")
    data class NoteRelation(val noteIds: List<String>) : CellData()

    @Immutable
    @Serializable
    @SerialName("formula")
    data class Formula(val result: String) : CellData()
}

@Immutable
@Serializable
data class MediaItem(val fileName: String, val originalName: String)

/**
 * Canonical "cell as plain text" rendering, shared by export/PDF/search so they don't each
 * reimplement an 8-way `when` over [CellData].
 */
fun CellData?.displayText(): String = when (this) {
    null -> ""
    is CellData.Text -> value
    is CellData.Number -> value?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
    is CellData.Boolean -> value.toString()
    is CellData.Date -> timestamp?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date.toString() } ?: ""
    is CellData.TagList -> tagIds.joinToString(",")
    is CellData.MediaList -> files.joinToString(",") { "${it.fileName}|${it.originalName}" }
    is CellData.NoteRelation -> noteIds.firstOrNull() ?: ""
    is CellData.Formula -> result
}

@Immutable
@Serializable
@SerialName("voice")
data class VoiceBlock(
    override val id: String,
    val localFilePath: String? = null,
    val durationSeconds: Int = 0,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
data class Point(val x: Float, val y: Float)

@Immutable
@Serializable
data class Stroke(
    val points: List<Point>,
    val colorHex: String = "#FF000000",
    val strokeWidth: Float = 4f,
    val isEraser: Boolean = false
)

@Immutable
@Serializable
@SerialName("sketch")
data class SketchBlock(
    override val id: String,
    val strokes: List<Stroke> = emptyList(),
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("solid_divider")
data class SolidDividerBlock(
    override val id: String,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()

@Immutable
@Serializable
@SerialName("dot_divider")
data class ThreeDotDividerBlock(
    override val id: String,
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false,
    override val isDeleted: Boolean = false,
    override val isPinned: Boolean = false,
    override val updatedAt: Long = 0L
) : NoteBlock()
enum class ColumnType { TEXT, NUMBER, CHECKBOX, DATE, FORMULA, PHONE, EMAIL, TAGS, URL, FILES, PRIORITY, MONEY, AUDIO, NOTES, STATUS }

/**
 * Canonical Kanban status values. A STATUS cell is stored as [CellData.Text] holding one of these
 * (or blank, meaning "No Status") - kept as a fixed set so Kanban bucketing never has to deal with
 * arbitrary free-form values.
 */
val DEFAULT_STATUS_OPTIONS = listOf("Not Started", "In Progress", "Done")

@Immutable
@Serializable
data class SortConfig(val columnId: String, val isAscending: Boolean)

@Immutable
@Serializable
data class FilterConfig(val columnId: String, val operator: String, val value: String)

fun NoteBlock.markDeleted(): NoteBlock = when (this) {
    is TextBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is HeadingBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is CheckboxBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is BulletedListBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is NumberedListBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is ToggleBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is CodeBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is BookmarkBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is ImageBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is DocumentBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is DatabaseBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is VoiceBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is QuoteBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is SketchBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is RowContainerBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is SolidDividerBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
    is ThreeDotDividerBlock -> copy(isDeleted = true, updatedAt = System.currentTimeMillis())
}

fun NoteBlock.withPin(pinned: Boolean, now: Long): NoteBlock = when (this) {
    is TextBlock -> copy(isPinned = pinned, updatedAt = now)
    is HeadingBlock -> copy(isPinned = pinned, updatedAt = now)
    is CheckboxBlock -> copy(isPinned = pinned, updatedAt = now)
    is BulletedListBlock -> copy(isPinned = pinned, updatedAt = now)
    is NumberedListBlock -> copy(isPinned = pinned, updatedAt = now)
    is ToggleBlock -> copy(isPinned = pinned, updatedAt = now)
    is CodeBlock -> copy(isPinned = pinned, updatedAt = now)
    is BookmarkBlock -> copy(isPinned = pinned, updatedAt = now)
    is ImageBlock -> copy(isPinned = pinned, updatedAt = now)
    is DocumentBlock -> copy(isPinned = pinned, updatedAt = now)
    is DatabaseBlock -> copy(isPinned = pinned, updatedAt = now)
    is VoiceBlock -> copy(isPinned = pinned, updatedAt = now)
    is QuoteBlock -> copy(isPinned = pinned, updatedAt = now)
    is SketchBlock -> copy(isPinned = pinned, updatedAt = now)
    is RowContainerBlock -> copy(isPinned = pinned, updatedAt = now)
    is SolidDividerBlock -> copy(isPinned = pinned, updatedAt = now)
    is ThreeDotDividerBlock -> copy(isPinned = pinned, updatedAt = now)
}