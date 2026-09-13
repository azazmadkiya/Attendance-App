package com.example.util

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.example.data.HaazriDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseBackupManager {

    private const val BACKUP_FILE_NAME = "haazri_backup.enc"
    
    /**
     * Exports the current state of the database to an encrypted local file.
     * Returns the encrypted File upon success.
     */
    suspend fun backupDatabase(context: Context, database: HaazriDatabase): Result<File> = withContext(Dispatchers.IO) {
        try {
            // 1. Force a checkpoint to merge WAL data into the main database file
            database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()

            // 2. Get the database file path
            val dbFile = context.getDatabasePath("haazri_pro_database")
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("Database file not found"))
            }

            // 3. Prepare the encrypted file
            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            if (backupFile.exists()) {
                backupFile.delete()
            }

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                backupFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            // 4. Copy data to encrypted file
            val inputStream = FileInputStream(dbFile)
            val outputStream = encryptedFile.openFileOutput()
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            Result.success(backupFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Restores the database from the local encrypted backup file.
     * Important: The application should be restarted after a successful restore 
     * to re-initialize the Room database singleton safely.
     */
    suspend fun restoreDatabase(context: Context, database: HaazriDatabase): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            if (!backupFile.exists()) {
                return@withContext Result.failure(Exception("Backup file not found"))
            }
            
            // 1. Close the current database connection to safely overwrite the file
            database.close()

            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedFile = EncryptedFile.Builder(
                context,
                backupFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            // 2. Get the database file path
            val dbFile = context.getDatabasePath("haazri_pro_database")
            
            // 3. Clear existing WAL and SHM files to prevent corruption with the restored DB
            val walFile = context.getDatabasePath("haazri_pro_database-wal")
            val shmFile = context.getDatabasePath("haazri_pro_database-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 4. Copy decrypted data back to the database file
            val inputStream = encryptedFile.openFileInput()
            val outputStream = FileOutputStream(dbFile)
            
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
