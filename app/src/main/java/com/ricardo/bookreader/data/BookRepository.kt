package com.ricardo.bookreader.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ricardo.bookreader.model.FolderEntry
import com.ricardo.bookreader.model.BookFormat
import com.ricardo.bookreader.model.BookDocument
import com.ricardo.bookreader.model.detectBookFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

class BookRepository(
    private val context: Context,
    private val contentResolver: ContentResolver = context.contentResolver
) {
    fun getTreeDocument(uri: Uri): DocumentFile? = DocumentFile.fromTreeUri(context, uri)

    fun getFolderEntries(folderUri: Uri): List<FolderEntry> {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: DocumentFile.fromSingleUri(context, folderUri)
            ?: return emptyList()

        return folder.listFiles()
            .filter { it.isDirectory || isReadableBookFile(it) }
            .mapNotNull { file ->
                val name = file.name ?: return@mapNotNull null
                FolderEntry(uri = file.uri, name = name, isDirectory = file.isDirectory)
            }
            .sortedWith(compareBy<FolderEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    fun getBooksInFolder(folderUri: Uri, readEntries: Set<String> = emptySet()): List<BookDocument> {
        val folder = DocumentFile.fromTreeUri(context, folderUri)
            ?: DocumentFile.fromSingleUri(context, folderUri)
            ?: return emptyList()

        return folder.listFiles()
            .filter { isReadableBookFile(it) }
            .sortedBy { it.name?.lowercase().orEmpty() }
            .map {
                val name = it.name.orEmpty()
                val format = detectBookFormat(name, it.type)
                BookDocument(
                    uri = it.uri,
                    name = name,
                    parentUri = folderUri,
                    isRead = readEntries.contains(it.uri.toString()) || readEntries.contains(readNameKey(name)),
                    isMarkdown = format == BookFormat.MARKDOWN,
                    isPdf = format == BookFormat.PDF
                )
            }
    }

    suspend fun readText(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: error("No se pudo abrir el archivo")
        }
    }

    suspend fun markAsRead(uri: Uri, parentUri: Uri?): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val file = DocumentFile.fromSingleUri(context, uri)
                ?: error("No se pudo acceder al archivo actual")

            val currentName = file.name ?: error("Archivo sin nombre")
            val dotIndex = currentName.lastIndexOf('.')
            val baseName = if (dotIndex > 0) currentName.substring(0, dotIndex) else currentName
            val extension = if (dotIndex > 0) currentName.substring(dotIndex) else ".txt"

            if (baseName.endsWith("_leido", ignoreCase = true) || baseName.endsWith("_leído", ignoreCase = true)) {
                return@runCatching uri
            }

            val targetName = baseName + "_leido" + extension
            val renamed = file.renameTo(targetName)
            if (!renamed) error("No se pudo renombrar el archivo como leído")

            val parent = parentUri?.let { DocumentFile.fromTreeUri(context, it) ?: DocumentFile.fromSingleUri(context, it) }
            val renamedFile = parent?.findFile(targetName)
            renamedFile?.uri ?: file.uri
        }
    }

    private fun isReadableBookFile(file: DocumentFile): Boolean {
        return file.isFile &&
            detectBookFormat(file.name, file.type) != BookFormat.UNSUPPORTED
    }

    private fun readNameKey(name: String): String =
        "name:" + Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
}
