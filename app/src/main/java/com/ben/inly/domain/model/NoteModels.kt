package com.ben.inly.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The root container for a saved note.
 * It holds a list of blocks and a version number so I can handle migrations easily if the structure changes later.
 */
@Serializable
data class NoteContent(
    val version: Int = 1,
    val blocks: List<NoteBlock>
)

/**
 * The base class for everything in the editor.
 * The editor is block-based, meaning every paragraph, image, or list item is its own distinct, serializable block.
 */
@Serializable
sealed class NoteBlock {
    abstract val id: String
    abstract val indentationLevel: Int
    abstract val isBold: Boolean
    abstract val isItalic: Boolean
    abstract val isStrikeThrough: Boolean
    abstract val isUnderlined: Boolean
}

@Serializable
@SerialName("text")
data class TextBlock(
    override val id: String,
    val text: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    val completedAt: Long? = null
) : NoteBlock()

@Serializable
@SerialName("bullet")
data class BulletedListBlock(
    override val id: String,
    val text: String = "",
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

@Serializable
@SerialName("image")
data class ImageBlock(
    override val id: String,
    val localFilePath: String? = null, // Stored locally so the app doesn't lose access if the gallery image is deleted
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false
) : NoteBlock()

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
    override val isUnderlined: Boolean = false
) : NoteBlock()

// --- Database specific model ---

@Serializable
@SerialName("database")
data class DatabaseBlock(
    override val id: String,
    val title: String = "",
    val columns: List<DatabaseColumn>,
    val rows: List<DatabaseRow>,
    val activeSorts: List<SortConfig> = emptyList(),
    val activeFilters: List<FilterConfig> = emptyList(),
    override val indentationLevel: Int = 0,
    override val isBold: Boolean = false,
    override val isItalic: Boolean = false,
    override val isStrikeThrough: Boolean = false,
    override val isUnderlined: Boolean = false
) : NoteBlock()

@Serializable
data class DatabaseColumn(
    val id: String,
    val name: String,
    val type: ColumnType,
    val width: Int = 140, // Useful for drag-to-resize columns later
    val formulaExpression: String? = null
)

@Serializable
data class DatabaseRow(
    val id: String,
    val cells: Map<String, String> // Maps the Column ID to the actual Cell Value
)

enum class ColumnType {
    TEXT, NUMBER, CHECKBOX, DATE, FORMULA,
    PHONE, EMAIL, TAGS, URL, FILES, PRIORITY
}

@Serializable
data class SortConfig(val columnId: String, val isAscending: Boolean)

@Serializable
data class FilterConfig(val columnId: String, val operator: String, val value: String)

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
    override val isUnderlined: Boolean = false
) : NoteBlock()