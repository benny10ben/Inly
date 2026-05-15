package com.ben.inly.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the metadata for a note.
 * Crucially, this does NOT hold the actual note content (blocks, text, images).
 * Content is securely encrypted and stored as files via FileStorageManager.
 * This entity just keeps track of titles, dates, and UI state so the app can quickly load lists and search.
 */
@Entity(tableName = "notes_metadata")
data class NoteMetadataEntity(
    @PrimaryKey val noteId: String,
    val title: String,
    val icon: String? = null,
    val folderId: String?,
    val isDaily: Boolean,
    val dateString: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val filePath: String,
    val snippet: String = "",
    val isFavorite: Boolean = false,
    val coverImagePath: String? = null,
    val trashedAt: Long? = null
)

/**
 * Basic structure for folders to organize standalone notes.
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val name: String,
    val parentFolderId: String?,
    val createdAt: Long
)

/**
 * Central registry for tags used across all databases.
 */
@Entity(tableName = "global_tags")
data class TagEntity(
    @PrimaryKey val tagId: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long
)