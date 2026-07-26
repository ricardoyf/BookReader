package com.ricardo.bookreader.ui.viewmodel

import android.net.Uri
import com.ricardo.bookreader.model.FolderEntry
import com.ricardo.bookreader.model.ReadingPosition
import com.ricardo.bookreader.model.BookDocument

data class ReaderUiState(
    val treeUri: Uri? = null,
    val currentFolderUri: Uri? = null,
    val currentFolderName: String = "Biblioteca",
    val folderStack: List<Uri> = emptyList(),
    val folderEntries: List<FolderEntry> = emptyList(),
    val allBooks: List<BookDocument> = emptyList(),
    val selectedFile: BookDocument? = null,
    val selectedFileIndex: Int = -1,
    val content: String = "",
    val isMarkdownContent: Boolean = false,
    val isPdfContent: Boolean = false,
    val fontSizeSp: Float = 19f,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val restoreAvailable: Boolean = false,
    val readEntries: Set<String> = emptySet(),
    val exportJsonText: String? = null,
    val restoredReadingPosition: ReadingPosition? = null,
    val currentCharacterOffset: Int = 0,
    val currentScrollY: Int = 0,
    val currentPdfPage: Int = 0,
    val currentChunkIndex: Int = -1,
    val currentChunkStartOffset: Int = 0,
    val currentChunkEndOffset: Int = 0,
    val lastRangeOffset: Int? = null
)
