package com.example.stockkeeper.data.photo

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ProductPhotoStore {
    private const val DIRECTORY = "product_photos"

    suspend fun copyIntoApp(context: Context, source: Uri): String = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(source).orEmpty()
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val destination = File(directory, "${UUID.randomUUID()}.$extension")
        context.contentResolver.openInputStream(source).use { input ->
            requireNotNull(input) { "Selected photo cannot be opened" }
            destination.outputStream().use(input::copyTo)
        }
        "$DIRECTORY/${destination.name}"
    }

    fun file(context: Context, relativePath: String?): File? = relativePath
        ?.takeIf(String::isNotBlank)
        ?.let { File(context.filesDir, it) }
        ?.takeIf(File::exists)

    fun delete(context: Context, relativePath: String?) {
        relativePath?.let { File(context.filesDir, it).delete() }
    }

    fun createCameraDestination(context: Context): CameraDestination {
        val directory = File(context.filesDir, DIRECTORY).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        val relativePath = "$DIRECTORY/${file.name}"
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return CameraDestination(uri, relativePath)
    }

    data class CameraDestination(val uri: Uri, val relativePath: String)
}
