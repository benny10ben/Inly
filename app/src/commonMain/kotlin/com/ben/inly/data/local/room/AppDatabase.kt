package com.ben.inly.data.local.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [
        NoteMetadataEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteBlockEntity::class,
        CalendarTaskEntity::class,
        ImageBlockEntity::class,
        DocumentBlockEntity::class,
        BookmarkBlockEntity::class
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
    abstract fun calendarTaskDao(): CalendarTaskDao
    abstract fun imageBlockDao(): ImageBlockDao
    abstract fun documentBlockDao(): DocumentBlockDao
    abstract fun bookmarkBlockDao(): BookmarkBlockDao
}