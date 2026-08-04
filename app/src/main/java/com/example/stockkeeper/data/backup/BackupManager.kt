package com.example.stockkeeper.data.backup

import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.example.stockkeeper.StockKeeperApplication
import com.example.stockkeeper.data.local.StockKeeperDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val application: StockKeeperApplication) {
    suspend fun export(destination: Uri) = withContext(Dispatchers.IO) {
        application.database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
        val databaseFile = application.getDatabasePath(StockKeeperDatabase.DATABASE_NAME)
        require(databaseFile.isFile) { "Database is not available" }

        application.contentResolver.openOutputStream(destination)?.use { output ->
            ZipOutputStream(output.buffered()).use { zip ->
                val manifest = JSONObject()
                    .put("formatVersion", FORMAT_VERSION)
                    .put("databaseVersion", StockKeeperDatabase.DATABASE_VERSION)
                    .put("createdAt", System.currentTimeMillis())
                    .toString()
                zip.putNextEntry(ZipEntry(MANIFEST_FILE))
                zip.write(manifest.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
                addFile(zip, databaseFile, DATABASE_ENTRY)
                addDirectory(zip, File(application.filesDir, PHOTO_DIRECTORY), "$PHOTO_DIRECTORY/")
            }
        } ?: error("Selected file cannot be opened")
    }

    suspend fun import(source: Uri) = withContext(Dispatchers.IO) {
        val workDir = File(application.cacheDir, "backup_import_${UUID.randomUUID()}").apply { mkdirs() }
        try {
            application.contentResolver.openInputStream(source)?.use { input ->
                extractSafely(ZipInputStream(input.buffered()), workDir)
            } ?: error("Selected file cannot be opened")

            validateManifest(File(workDir, MANIFEST_FILE))
            val importedDatabase = File(workDir, DATABASE_ENTRY)
            validateDatabase(importedDatabase)
            replaceCurrentData(importedDatabase, File(workDir, PHOTO_DIRECTORY))
        } finally {
            workDir.deleteRecursively()
        }
    }

    private fun replaceCurrentData(importedDatabase: File, importedPhotos: File) {
        val databaseFile = application.getDatabasePath(StockKeeperDatabase.DATABASE_NAME)
        val photoDirectory = File(application.filesDir, PHOTO_DIRECTORY)
        val safetyDirectory = File(application.filesDir, SAFETY_DIRECTORY)
        val safetyDatabase = File(safetyDirectory, DATABASE_ENTRY)
        val safetyPhotos = File(safetyDirectory, PHOTO_DIRECTORY)

        application.closeDatabase()
        safetyDirectory.deleteRecursively()
        safetyDirectory.mkdirs()
        if (databaseFile.exists()) databaseFile.copyTo(safetyDatabase, overwrite = true)
        if (photoDirectory.exists()) photoDirectory.copyRecursively(safetyPhotos, overwrite = true)

        runCatching {
            deleteDatabaseSidecars(databaseFile)
            importedDatabase.copyTo(databaseFile, overwrite = true)
            photoDirectory.deleteRecursively()
            if (importedPhotos.exists()) importedPhotos.copyRecursively(photoDirectory, overwrite = true)
        }.getOrElse { failure ->
            deleteDatabaseSidecars(databaseFile)
            if (safetyDatabase.exists()) safetyDatabase.copyTo(databaseFile, overwrite = true)
            photoDirectory.deleteRecursively()
            if (safetyPhotos.exists()) safetyPhotos.copyRecursively(photoDirectory, overwrite = true)
            throw failure
        }
    }

    private fun validateManifest(file: File) {
        require(file.isFile) { "Backup manifest is missing" }
        val manifest = JSONObject(file.readText())
        require(manifest.optInt("formatVersion") == FORMAT_VERSION) { "Unsupported backup format" }
        require(manifest.optInt("databaseVersion") == StockKeeperDatabase.DATABASE_VERSION) {
            "Unsupported database version"
        }
    }

    private fun validateDatabase(file: File) {
        require(file.isFile) { "Database is missing from backup" }
        val database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            database.rawQuery("PRAGMA user_version", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getInt(0) == StockKeeperDatabase.DATABASE_VERSION) {
                    "Unsupported database version"
                }
            }
            database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0).equals("ok", ignoreCase = true)) {
                    "Database integrity check failed"
                }
            }
            database.rawQuery(
                "SELECT identity_hash FROM room_master_table WHERE id = 42",
                null,
            ).use { cursor ->
                require(cursor.moveToFirst() && cursor.getString(0) == ROOM_IDENTITY_HASH) {
                    "Backup database schema does not match this app"
                }
            }
            database.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                require(!cursor.moveToFirst()) { "Backup contains broken data relationships" }
            }
            database.rawQuery("SELECT photo_path FROM products WHERE photo_path IS NOT NULL", null).use { cursor ->
                while (cursor.moveToNext()) requireSafePhotoPath(cursor.getString(0))
            }
        } finally {
            database.close()
        }
    }

    private fun extractSafely(zip: ZipInputStream, destination: File) {
        val root = destination.canonicalFile
        var extractedBytes = 0L
        var entryCount = 0
        val entryNames = mutableSetOf<String>()
        var entry = zip.nextEntry
        while (entry != null) {
            entryCount++
            require(entryCount <= MAX_ZIP_ENTRIES) { "Backup contains too many files" }
            require(entryNames.add(entry.name)) { "Backup contains duplicate entries" }
            val target = File(root, entry.name).canonicalFile
            require(target.path.startsWith(root.path + File.separator)) { "Unsafe backup entry" }
            if (entry.isDirectory) {
                target.mkdirs()
            } else {
                target.parentFile?.mkdirs()
                target.outputStream().buffered().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = zip.read(buffer)
                    while (read >= 0) {
                        extractedBytes += read
                        require(extractedBytes <= MAX_EXTRACTED_BYTES) { "Backup is too large" }
                        output.write(buffer, 0, read)
                        read = zip.read(buffer)
                    }
                }
            }
            zip.closeEntry()
            entry = zip.nextEntry
        }
    }

    private fun requireSafePhotoPath(relativePath: String) {
        val normalized = File(relativePath).normalize().path.replace('\\', '/')
        require(normalized.startsWith("$PHOTO_DIRECTORY/") && ".." !in normalized.split('/')) {
            "Backup contains an unsafe photo path"
        }
    }

    private fun addDirectory(zip: ZipOutputStream, directory: File, prefix: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile) addFile(zip, file, prefix + file.name)
        }
    }

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().buffered().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun deleteDatabaseSidecars(databaseFile: File) {
        databaseFile.delete()
        File(databaseFile.path + "-wal").delete()
        File(databaseFile.path + "-shm").delete()
        File(databaseFile.path + "-journal").delete()
    }

    companion object {
        private const val FORMAT_VERSION = 1
        private const val MANIFEST_FILE = "manifest.json"
        private const val DATABASE_ENTRY = "stockkeeper.db"
        private const val PHOTO_DIRECTORY = "product_photos"
        private const val SAFETY_DIRECTORY = "last_import_backup"
        private const val MAX_ZIP_ENTRIES = 10_000
        private const val MAX_EXTRACTED_BYTES = 1_073_741_824L
        private const val ROOM_IDENTITY_HASH = "d7f8902c01d826a4536a78f1875d99a5"
    }
}
