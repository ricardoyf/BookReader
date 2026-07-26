package com.ricardo.bookreader.ui.viewmodel

import android.net.Uri
import com.ricardo.bookreader.model.FolderEntry
import com.ricardo.bookreader.model.ReadingPosition
import com.ricardo.bookreader.model.BookDocument
import com.ricardo.bookreader.model.PageMark
import com.ricardo.bookreader.model.ReaderPageSnapshot

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
    val lastRangeOffset: Int? = null,
    val pageMarks: List<PageMark> = emptyList(),
    val currentPage: ReaderPageSnapshot? = null,
    val showMarkedOnly: Boolean = false
)

fun ReaderUiState.currentBookMarks(): List<PageMark> {
    val uri = selectedFile?.uri?.toString() ?: return emptyList()
    return pageMarks.filter { it.fileUri == uri }.sortedBy { it.pageNumber }
}

fun ReaderUiState.isCurrentPageMarked(): Boolean {
    val page = currentPage ?: return false
    return currentBookMarks().any { mark ->
        if (page.isPdf) {
            mark.pageNumber == page.pageNumber
        } else {
            mark.characterOffset in page.startOffset until
                page.endOffset.coerceAtLeast(page.startOffset + 1)
        }
    }
}
