package com.ben.inly.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        NoteMetadataEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteBlockEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun blockDao(): BlockDao
}