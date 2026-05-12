package com.ben.inly.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The main Room database setup.
 * It registers the entities (tables) and DAOs so the app can query local metadata.
 */
@Database(
    entities = [NoteMetadataEntity::class, FolderEntity::class],
    version = 1,
    exportSchema = false // true to track schema changes in git
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
}