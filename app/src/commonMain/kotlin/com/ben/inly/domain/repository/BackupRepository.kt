package com.ben.inly.domain.repository

import com.ben.inly.domain.model.backup.InlyBackupData

/**
 * Handles the extraction and restoration of the entire Inly database.
 */
interface BackupRepository {

    /**
     * Gathers all data from all Room tables and packages it into a single InlyBackupData object.
     */
    suspend fun createBackupData(): InlyBackupData

    suspend fun restoreBackup(backupData: InlyBackupData)
}