package com.ben.inly.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Represents the metadata for a note.
 * Crucially, this does NOT hold the actual note content (blocks, text, images).
 * Content is securely encrypted and stored as files via FileStorageManager.
 * This entity just keeps track of titles, dates, and UI state so the app can quickly load lists and search.
 */
@Serializable
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
    val trashedAt: Long? = null,
    val isSubNote: Boolean = false,
    val showWordCount: Boolean = false,
    val sortOrder: Int = 0,
    val isTemplate: Boolean = false
)

/**
 * Basic structure for folders to organize notes.
 */
@Serializable
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val folderId: String,
    val name: String,
    val parentFolderId: String?,
    val createdAt: Long,
    val isDeleted: Boolean = false,
    val sortOrder: Int = 0
)

/**
 * Central registry for tags used across all databases.
 */
@Serializable
@Entity(tableName = "global_tags")
data class TagEntity(
    @PrimaryKey val tagId: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long
)

/**
 * A user-defined calendar category (e.g. "Personal", "Work") used to color-code events.
 * `updatedAt`/`isDeleted` exist purely for sync (see SyncRepositoryImpl) - `updatedAt` drives the
 * "modified since last sync" query and last-write-wins merge, `isDeleted` is a soft-delete
 * tombstone so a deletion on one device actually propagates to others instead of just vanishing
 * locally with nothing left to sync.
 */
@Serializable
@Entity(tableName = "calendar_categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val name: String,
    val colorHex: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isDeleted: Boolean = false
)

/**
 * A saved DatabaseBlock schema (columns + views only, never rows) that can be reused when
 * creating a new database. Columns/views are stored as JSON so the schema shape can evolve
 * without a Room migration for every DatabaseColumn/DatabaseView field addition.
 */
@Serializable
@Entity(tableName = "database_templates")
data class DatabaseTemplateEntity(
    @PrimaryKey val templateId: String,
    val name: String,
    val serializedColumns: String,
    val serializedViews: String
)