package com.ricardo.bookreader.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ricardo.bookreader.data.PreferencesRepository
import com.ricardo.bookreader.data.BookRepository
import com.ricardo.bookreader.model.ReadingPosition
import com.ricardo.bookreader.model.BookDocument
import com.ricardo.bookreader.model.BookFormat
import com.ricardo.bookreader.model.PageMark
import com.ricardo.bookreader.model.ReaderPageSnapshot
import com.ricardo.bookreader.model.detectBookFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Normalizer

class ReaderViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesRepository(application)
    private val bookRepository = BookRepository(application)

    private val _uiState = MutableStateFlow(ReaderUiState(isLoading = true))
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()
    private var positionSaveJob: Job? = null

    init {
        restoreLastState()
    }

    fun restoreLastState() {
        viewModelScope.launch {
            val saved = prefs.preferences.first()
            val treeUri = saved.treeUri?.let(Uri::parse)
            val currentFolderUri = saved.currentFolderUri?.let(Uri::parse)
            val currentFileUri = saved.currentFileUri?.let(Uri::parse)
            val currentFileName = saved.currentFileName

            if (treeUri == null) {
                _uiState.value = ReaderUiState(
                    isLoading = false,
                    fontSizeSp = saved.fontScaleSp,
                    restoreAvailable = false,
                    readEntries = saved.readEntries,
                    pageMarks = saved.pageMarks
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                fontSizeSp = saved.fontScaleSp,
                readEntries = saved.readEntries,
                pageMarks = saved.pageMarks
            )

            runCatching {
                reloadTree(treeUri, currentFolderUri, currentFileUri, currentFileName)
            }.onFailure {
                _uiState.value = ReaderUiState(
                    isLoading = false,
                    fontSizeSp = saved.fontScaleSp,
                    errorMessage = "No se pudo restaurar la carpeta. Selecciónala de nuevo.",
                    restoreAvailable = true,
                    pageMarks = saved.pageMarks
                )
            }
        }
    }

    fun onFolderSelected(uri: Uri) {
        viewModelScope.launch {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
            }
            prefs.saveTreeUri(uri.toString())
            prefs.saveCurrentFolderUri(uri.toString())
            prefs.saveCurrentFileUri(null)
            prefs.saveCurrentFileName(null)
            reloadTree(uri, uri, null, null)
        }
    }

    fun openFolder(uri: Uri) {
        val current = _uiState.value
        val entry = current.folderEntries.firstOrNull { it.uri == uri } ?: return
        _uiState.value = current.copy(
            currentFolderUri = uri,
            currentFolderName = entry.name,
            folderStack = current.folderStack + uri,
            folderEntries = bookRepository.getFolderEntries(uri),
            allBooks = bookRepository.getBooksInFolder(uri, current.readEntries),
            errorMessage = null
        )
        viewModelScope.launch { prefs.saveCurrentFolderUri(uri.toString()) }
    }

    fun navigateUp() {
        val current = _uiState.value
        val treeUri = current.treeUri ?: return
        if (current.currentFolderUri == treeUri || current.folderStack.isEmpty()) return

        val newStack = current.folderStack.dropLast(1)
        val target = newStack.lastOrNull() ?: treeUri

        _uiState.value = current.copy(
            currentFolderUri = target,
            currentFolderName = documentName(target) ?: "Biblioteca",
            folderStack = newStack,
            folderEntries = bookRepository.getFolderEntries(target),
            allBooks = bookRepository.getBooksInFolder(target, current.readEntries),
            errorMessage = null
        )
        viewModelScope.launch { prefs.saveCurrentFolderUri(target.toString()) }
    }

    fun openFile(uri: Uri) {
        val file = currentFolderBooks().firstOrNull { it.uri == uri }
            ?: run {
                val name = documentName(uri) ?: "Archivo"
                val format = detectBookFormat(name, null)
                BookDocument(
                    uri = uri,
                    name = name,
                    parentUri = _uiState.value.currentFolderUri,
                    isMarkdown = format == BookFormat.MARKDOWN,
                    isPdf = format == BookFormat.PDF
                )
            }
        viewModelScope.launch {
            flushCurrentReadingPosition()
            openBookDocument(file)
        }
    }

    fun openNextFile() {
        viewModelScope.launch {
            val current = _uiState.value
            val nextFile = currentFolderBooks(current).getOrNull(current.selectedFileIndex + 1)
            flushCurrentReadingPosition()
            val marked = if (isBookComplete(current.currentPage)) {
                markCurrentAsReadIfNeeded()
            } else {
                false
            }
            if (marked) refreshFilesKeepingSelection()
            nextFile?.let { openBookDocument(it) }
        }
    }

    fun openPreviousFile() {
        viewModelScope.launch {
            flushCurrentReadingPosition()
            val refreshed = _uiState.value
            val files = currentFolderBooks(refreshed)
            val previousIndex = refreshed.selectedFileIndex - 1
            if (previousIndex in files.indices) openBookDocument(files[previousIndex])
        }
    }

    fun markCurrentAsRead() {
        viewModelScope.launch {
            flushCurrentReadingPosition()
            val marked = markCurrentAsReadIfNeeded()
            if (marked) {
                refreshFilesKeepingSelection()
            }
        }
    }

    fun markCurrentAsUnread() {
        viewModelScope.launch {
            val current = _uiState.value.selectedFile ?: return@launch
            val updatedReadEntries = _uiState.value.readEntries - current.uri.toString() - readNameKey(current.name)
            prefs.saveReadEntries(updatedReadEntries)
            _uiState.value = _uiState.value.copy(
                readEntries = updatedReadEntries,
                selectedFile = current.copy(isRead = false),
                errorMessage = null
            )
            refreshFilesKeepingSelection()
        }
    }

    fun buildExportJson() {
        viewModelScope.launch {
            val json = prefs.exportBackupJson()
            _uiState.value = _uiState.value.copy(exportJsonText = json)
        }
    }

    fun clearExportJson() {
        _uiState.value = _uiState.value.copy(exportJsonText = null)
    }

    fun setFontSize(sp: Float) {
        val normalized = sp.coerceIn(14f, 30f)
        _uiState.value = _uiState.value.copy(fontSizeSp = normalized)
        viewModelScope.launch { prefs.saveFontScale(normalized) }
    }

    fun onVisiblePageChanged(page: ReaderPageSnapshot, renderedText: String = "") {
        _uiState.value = _uiState.value.copy(currentPage = page)
        if (page.isPdf) {
            rememberPdfPage(page.pageNumber - 1)
        } else if (renderedText.isNotEmpty()) {
            rememberVisiblePosition(renderedText, page.startOffset, 0)
        }
    }

    fun toggleCurrentPageMark() {
        val current = _uiState.value
        val file = current.selectedFile ?: return
        val page = current.currentPage ?: return
        val existing = current.pageMarks.firstOrNull { mark ->
            mark.fileUri == file.uri.toString() &&
                if (page.isPdf) {
                    mark.isPdf && mark.pageNumber == page.pageNumber
                } else {
                    !mark.isPdf && mark.characterOffset in page.startOffset until
                        page.endOffset.coerceAtLeast(page.startOffset + 1)
                }
        }
        val updated = if (existing != null) {
            current.pageMarks - existing
        } else {
            current.pageMarks + PageMark(
                fileUri = file.uri.toString(),
                fileName = file.name,
                pageNumber = page.pageNumber,
                characterOffset = page.startOffset,
                pageText = page.pageText.trim(),
                isPdf = page.isPdf,
                createdAt = System.currentTimeMillis()
            )
        }
        _uiState.value = current.copy(pageMarks = updated)
        viewModelScope.launch { prefs.savePageMarks(updated) }
    }

    fun toggleMarkedOnly() {
        _uiState.value = _uiState.value.copy(showMarkedOnly = !_uiState.value.showMarkedOnly)
    }

    fun saveReadingPosition(
        characterOffset: Int,
        sourceText: String,
        scrollY: Int = _uiState.value.currentScrollY,
        chunkIndex: Int = _uiState.value.currentChunkIndex,
        chunkStartOffset: Int = _uiState.value.currentChunkStartOffset,
        chunkEndOffset: Int = _uiState.value.currentChunkEndOffset,
        rangeOffset: Int? = _uiState.value.lastRangeOffset
    ) {
        val fileUri = _uiState.value.selectedFile?.uri?.toString() ?: return
        val safeOffset = characterOffset.coerceIn(0, sourceText.length)
        val position = ReadingPosition(
            fileUri = fileUri,
            characterOffset = safeOffset,
            chunkIndex = chunkIndex,
            chunkStartOffset = chunkStartOffset.coerceIn(0, sourceText.length),
            chunkEndOffset = chunkEndOffset.coerceIn(0, sourceText.length),
            textPreview = previewAround(sourceText, safeOffset),
            scrollY = scrollY.coerceAtLeast(0),
            pdfPage = 0,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(
            currentCharacterOffset = safeOffset,
            currentScrollY = scrollY.coerceAtLeast(0),
            currentChunkIndex = chunkIndex,
            currentChunkStartOffset = position.chunkStartOffset,
            currentChunkEndOffset = position.chunkEndOffset,
            lastRangeOffset = rangeOffset,
            restoredReadingPosition = position
        )
        enqueueReadingPositionSave(position)
    }

    fun rememberPdfPage(page: Int) {
        val file = _uiState.value.selectedFile?.takeIf { it.isPdf } ?: return
        val safePage = page.coerceAtLeast(0)
        val position = ReadingPosition(
            fileUri = file.uri.toString(),
            characterOffset = 0,
            chunkIndex = -1,
            chunkStartOffset = 0,
            chunkEndOffset = 0,
            textPreview = "",
            scrollY = 0,
            pdfPage = safePage,
            updatedAt = System.currentTimeMillis()
        )
        _uiState.value = _uiState.value.copy(
            currentPdfPage = safePage,
            restoredReadingPosition = position
        )
        enqueueReadingPositionSave(position)
    }

    fun rememberVisiblePosition(sourceText: String, characterOffset: Int, scrollY: Int) {
        saveReadingPosition(
            characterOffset = characterOffset,
            sourceText = sourceText,
            scrollY = scrollY,
            chunkIndex = -1,
            chunkStartOffset = characterOffset,
            chunkEndOffset = characterOffset,
            rangeOffset = null
        )
    }

    fun importBackupJson(rawJson: String) {
        viewModelScope.launch {
            runCatching {
                prefs.importBackupJson(rawJson, currentFileUrisByName())
            }.onSuccess { result ->
                val saved = prefs.preferences.first()
                val importMessage = if (result.legacyTxtReaderBackup) {
                    "JSON antiguo importado: ${result.readEntries} leídos restaurados. Ese JSON no contiene posiciones."
                } else {
                    "Backup importado: ${result.readingPositions} posiciones, " +
                        "${result.pageMarks} post-it y ${result.readEntries} leídos restaurados."
                }
                _uiState.value = _uiState.value.copy(
                    readEntries = saved.readEntries,
                    pageMarks = saved.pageMarks,
                    errorMessage = importMessage
                )
                refreshFilesKeepingSelection()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo importar el backup JSON."
                )
            }
        }
    }

    private suspend fun openBookDocument(file: BookDocument) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        val allFiles = currentFolderBooks()
        val index = allFiles.indexOfFirst { it.uri == file.uri }
        val folderUri = file.parentUri ?: _uiState.value.currentFolderUri
        val savedPosition = prefs.getReadingPosition(file.uri.toString())

        if (file.isPdf) {
            _uiState.value = _uiState.value.copy(
                selectedFile = file,
                selectedFileIndex = index,
                content = "",
                isMarkdownContent = false,
                isPdfContent = true,
                isLoading = false,
                currentFolderUri = folderUri,
                currentFolderName = folderUri?.let { documentName(it) }
                    ?: _uiState.value.currentFolderName,
                folderEntries = folderUri?.let { bookRepository.getFolderEntries(it) }
                    ?: _uiState.value.folderEntries,
                folderStack = if (folderUri != null && folderUri != _uiState.value.treeUri) {
                    listOf(folderUri)
                } else {
                    emptyList()
                },
                restoredReadingPosition = savedPosition,
                currentCharacterOffset = 0,
                currentScrollY = 0,
                currentPdfPage = savedPosition?.pdfPage ?: 0,
                currentChunkIndex = -1,
                currentChunkStartOffset = 0,
                currentChunkEndOffset = 0,
                lastRangeOffset = null,
                currentPage = null,
                showMarkedOnly = false
            )
            prefs.saveCurrentFileUri(file.uri.toString())
            prefs.saveCurrentFileName(file.name)
            prefs.saveCurrentFolderUri(folderUri?.toString())
            return
        }

        bookRepository.readText(file.uri)
            .onSuccess { text ->
                _uiState.value = _uiState.value.copy(
                    selectedFile = file,
                    selectedFileIndex = index,
                    content = text,
                    isMarkdownContent = file.isMarkdown,
                    isPdfContent = false,
                    isLoading = false,
                    currentFolderUri = folderUri,
                    currentFolderName = folderUri?.let { documentName(it) } ?: _uiState.value.currentFolderName,
                    folderEntries = folderUri?.let { bookRepository.getFolderEntries(it) } ?: _uiState.value.folderEntries,
                    folderStack = if (folderUri != null && folderUri != _uiState.value.treeUri) listOf(folderUri) else emptyList(),
                    restoredReadingPosition = savedPosition,
                    currentCharacterOffset = savedPosition?.characterOffset ?: 0,
                    currentScrollY = savedPosition?.scrollY ?: 0,
                    currentPdfPage = 0,
                    currentChunkIndex = savedPosition?.chunkIndex ?: -1,
                    currentChunkStartOffset = savedPosition?.chunkStartOffset ?: 0,
                    currentChunkEndOffset = savedPosition?.chunkEndOffset ?: 0,
                    lastRangeOffset = null,
                    currentPage = null,
                    showMarkedOnly = false
                )
                prefs.saveCurrentFileUri(file.uri.toString())
                prefs.saveCurrentFileName(file.name)
                prefs.saveCurrentFolderUri(folderUri?.toString())
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "No se pudo leer el archivo seleccionado."
                )
            }
    }

    private suspend fun reloadTree(treeUri: Uri, currentFolderUri: Uri?, currentFileUri: Uri?, currentFileName: String? = null) {
        val root = bookRepository.getTreeDocument(treeUri) ?: error("Árbol no accesible")
        val rootName = root.name ?: "Biblioteca"
        val visibleFolder = currentFolderUri ?: treeUri
        val folderFiles = bookRepository.getBooksInFolder(visibleFolder, _uiState.value.readEntries)
        _uiState.value = ReaderUiState(
            treeUri = treeUri,
            currentFolderUri = visibleFolder,
            currentFolderName = documentName(visibleFolder) ?: rootName,
            folderStack = if (visibleFolder != treeUri) listOf(visibleFolder) else emptyList(),
            folderEntries = bookRepository.getFolderEntries(visibleFolder),
            allBooks = folderFiles,
            fontSizeSp = _uiState.value.fontSizeSp,
            isLoading = false,
            restoreAvailable = true,
            errorMessage = if (folderFiles.isEmpty()) {
                "No hay libros TXT, Markdown o PDF en esta carpeta."
            } else {
                null
            },
            readEntries = _uiState.value.readEntries,
            pageMarks = _uiState.value.pageMarks
        )

        val savedFile = currentFileUri?.let { savedUri ->
            folderFiles.firstOrNull { it.uri == savedUri }
        } ?: currentFileName?.let { savedName ->
            folderFiles.firstOrNull { readNameKey(it.name) == readNameKey(savedName) }
        }
        savedFile?.let {
            openBookDocument(it)
        }
    }

    private suspend fun markCurrentAsReadIfNeeded(): Boolean {
        val current = _uiState.value.selectedFile ?: return false
        val updatedReadEntries = _uiState.value.readEntries + current.uri.toString() + readNameKey(current.name)
        prefs.saveReadEntries(updatedReadEntries)
        _uiState.value = _uiState.value.copy(readEntries = updatedReadEntries)

        var marked = false
        bookRepository.markAsRead(current.uri, current.parentUri)
            .onSuccess { newUri ->
                val renamedName = documentName(newUri) ?: current.name
                val migratedPosition = _uiState.value.restoredReadingPosition
                    ?.copy(fileUri = newUri.toString())
                val replacedReadEntries = updatedReadEntries - current.uri.toString() + newUri.toString() + readNameKey(renamedName)
                val migratedMarks = _uiState.value.pageMarks.map { mark ->
                    if (mark.fileUri == current.uri.toString()) {
                        mark.copy(fileUri = newUri.toString(), fileName = renamedName)
                    } else {
                        mark
                    }
                }
                _uiState.value = _uiState.value.copy(
                    selectedFile = current.copy(uri = newUri, name = renamedName, isRead = true),
                    readEntries = replacedReadEntries,
                    pageMarks = migratedMarks,
                    restoredReadingPosition = migratedPosition
                )
                prefs.moveReadingPosition(current.uri.toString(), newUri.toString())
                prefs.saveCurrentFileUri(newUri.toString())
                prefs.saveCurrentFileName(renamedName)
                prefs.saveReadEntries(replacedReadEntries)
                prefs.savePageMarks(migratedMarks)
                marked = true
            }
            .onFailure {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "No se pudo renombrar el archivo como leído. Se ha guardado igualmente como leído dentro de la app."
                )
                marked = true
            }
        return marked
    }

    private fun enqueueReadingPositionSave(position: ReadingPosition) {
        val previousSave = positionSaveJob
        positionSaveJob = viewModelScope.launch {
            previousSave?.join()
            prefs.saveReadingPosition(position)
        }
    }

    private suspend fun flushCurrentReadingPosition() {
        positionSaveJob?.join()
        _uiState.value.restoredReadingPosition?.let { prefs.saveReadingPosition(it) }
    }

    private fun refreshFilesKeepingSelection(): ReaderUiState {
        val current = _uiState.value
        val folderUri = current.currentFolderUri ?: return current
        val refreshedFiles = bookRepository.getBooksInFolder(folderUri, current.readEntries)
        val refreshedFolderEntries = bookRepository.getFolderEntries(folderUri)
        val selectedUri = current.selectedFile?.uri
        val newIndex = selectedUri?.let { uri -> refreshedFiles.indexOfFirst { it.uri == uri } } ?: -1
        val refreshedSelected = if (newIndex in refreshedFiles.indices) refreshedFiles[newIndex] else current.selectedFile?.copy(isRead = current.readEntries.contains(current.selectedFile.uri.toString()))
        val refreshedState = current.copy(
            allBooks = refreshedFiles,
            folderEntries = refreshedFolderEntries,
            selectedFileIndex = newIndex,
            selectedFile = refreshedSelected,
            isMarkdownContent = refreshedSelected?.isMarkdown ?: current.isMarkdownContent,
            isPdfContent = refreshedSelected?.isPdf ?: current.isPdfContent
        )
        _uiState.value = refreshedState
        return refreshedState
    }

    private fun currentFolderBooks(state: ReaderUiState = _uiState.value): List<BookDocument> = state.allBooks

    private fun currentFileUrisByName(): Map<String, String> =
        currentFolderBooks().associate { it.name to it.uri.toString() }

    private fun documentName(uri: Uri): String? {
        val app = getApplication<Application>()
        return DocumentFile.fromTreeUri(app, uri)?.name ?: DocumentFile.fromSingleUri(app, uri)?.name
    }

    private fun previewAround(text: String, offset: Int, radius: Int = 80): String {
        if (text.isBlank()) return ""
        val safeOffset = offset.coerceIn(0, text.length)
        val start = (safeOffset - radius).coerceAtLeast(0)
        val end = (safeOffset + radius).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    private fun readNameKey(name: String): String =
        "name:" + Normalizer.normalize(name.trim().lowercase(), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")
}
