package com.ben.inly.presentation.settings

import androidx.lifecycle.ViewModel
import com.ben.inly.domain.model.backup.InlyBackupData
import com.ben.inly.domain.repository.BackupRepository
import com.ben.inly.domain.repository.NoteRepository
import com.ben.inly.domain.util.SyncEventBus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsViewModel(
    private val backupRepository: BackupRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    // A safe JSON parser that won't crash if future app versions add new fields
    private val safeJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun getBackupJson(): String {
        val backupData = backupRepository.createBackupData()
        return safeJson.encodeToString(backupData)
    }

    /**
     * Parses the JSON backup and merges it into the local database.
     */
    suspend fun mergeBackupJson(jsonString: String) {
        val backupData = safeJson.decodeFromString<InlyBackupData>(jsonString)

        // FUTURE MIGRATION CHECK:
        // If backupData.version > 1, pass it through a migration mapper here
        // before handing it to the repository to ensure old data structures
        // are correctly converted to the newest schema.

        /** val migratedData = when (backupData.version) {
            1 -> runMigrationV1toV2(backupData) // Manually re-map the old structure to the new one
            2 -> backupData
            else -> backupData
        } */

        backupRepository.restoreBackup(backupData)

        // Wipe the stale memory caches so the next read hits the raw Room DB!
        noteRepository.clearCaches()

        // Tell the UI an import just finished so it can reload immediately
        kotlinx.coroutines.delay(100) // Brief pause to ensure DB transactions settle
        SyncEventBus.emitSyncCompleted("import_complete")
    }
}