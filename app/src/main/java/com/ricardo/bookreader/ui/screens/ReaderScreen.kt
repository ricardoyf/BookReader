package com.ricardo.bookreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ricardo.bookreader.model.ReaderPageSnapshot
import com.ricardo.bookreader.ui.components.EmptyStateCard
import com.ricardo.bookreader.ui.components.PagedTextReader
import com.ricardo.bookreader.ui.components.PdfBookView
import com.ricardo.bookreader.ui.viewmodel.ReaderUiState
import com.ricardo.bookreader.ui.viewmodel.currentBookMarks
import com.ricardo.bookreader.ui.viewmodel.isCurrentPageMarked

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    onBack: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onNextFile: () -> Unit,
    onPreviousFile: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportMarkedPages: () -> Unit,
    onTogglePageMark: () -> Unit,
    onToggleMarkedOnly: () -> Unit,
    onOpenLibrary: () -> Unit,
    ttsReady: Boolean,
    ttsSpeaking: Boolean,
    ttsPaused: Boolean,
    ttsHighlightStart: Int?,
    ttsHighlightEnd: Int?,
    speechRate: Float,
    continuousPlayback: Boolean,
    onStartReading: (String, Int) -> Unit,
    onStartReadingFromText: (String, Int) -> Unit,
    onPauseReading: () -> Unit,
    onResumeReading: () -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onToggleContinuousPlayback: () -> Unit,
    onVisiblePageChanged: (ReaderPageSnapshot, String) -> Unit
) {
    var fontBarVisible by remember { mutableStateOf(false) }
    var renderedText by remember(state.selectedFile?.uri) { mutableStateOf("") }
    val marks = state.currentBookMarks()
    val currentMarked = state.isCurrentPageMarked()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.selectedFile?.name ?: "Lector", maxLines = 1)
                        Text(
                            if (state.showMarkedOnly) {
                                "Solo páginas marcadas"
                            } else if (state.selectedFileIndex >= 0 && state.allBooks.isNotEmpty()) {
                                "Libro ${state.selectedFileIndex + 1} de ${state.allBooks.size}"
                            } else {
                                "Sin archivo"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onTogglePageMark,
                        enabled = state.currentPage != null
                    ) {
                        Icon(
                            if (currentMarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            if (currentMarked) "Quitar post-it" else "Poner post-it",
                            tint = if (currentMarked) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onToggleMarkedOnly,
                        enabled = marks.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.CollectionsBookmark,
                            "Ver solo páginas marcadas",
                            tint = if (state.showMarkedOnly) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onExportMarkedPages,
                        enabled = marks.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PictureAsPdf, "Exportar páginas marcadas")
                    }
                }
            )
        }
    ) { padding ->
        if (state.selectedFile == null) {
            EmptyStateCard(
                title = "No hay archivo abierto",
                description = "Vuelve a la biblioteca y elige un TXT, Markdown o PDF.",
                actionLabel = "Ir a biblioteca",
                onAction = onOpenLibrary,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!state.isPdfContent) {
                    IconButton(
                        onClick = when {
                            ttsSpeaking -> onPauseReading
                            ttsPaused -> onResumeReading
                            else -> {
                                {
                                    onStartReading(
                                        renderedText.ifBlank { state.content },
                                        state.currentCharacterOffset
                                    )
                                }
                            }
                        },
                        enabled = ttsReady
                    ) {
                        Icon(
                            if (ttsSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (ttsSpeaking) "Pausar lectura" else "Leer en voz alta"
                        )
                    }
                    IconButton(onClick = { fontBarVisible = !fontBarVisible }) {
                        Icon(Icons.Default.TextIncrease, "Tamaño de letra")
                    }
                }
                IconButton(onClick = onExportJson) {
                    Icon(Icons.Default.Save, "Exportar copia de seguridad")
                }
                IconButton(onClick = onImportJson) {
                    Icon(Icons.Default.UploadFile, "Importar copia de seguridad")
                }
                IconButton(onClick = onOpenLibrary) {
                    Icon(Icons.Default.FolderOpen, "Biblioteca")
                }
            }

            if (fontBarVisible && !state.isPdfContent) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Slider(
                        value = state.fontSizeSp,
                        onValueChange = onFontSizeChange,
                        valueRange = 14f..30f,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${state.fontSizeSp.toInt()}sp")
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (state.isPdfContent) {
                    PdfBookView(
                        uri = state.selectedFile.uri,
                        initialPage = state.currentPdfPage,
                        markedPages = marks.map { it.pageNumber },
                        markedOnly = state.showMarkedOnly,
                        onPageChanged = onVisiblePageChanged,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    PagedTextReader(
                        text = state.content,
                        isMarkdown = state.isMarkdownContent,
                        fontSizeSp = state.fontSizeSp,
                        initialOffset = state.currentCharacterOffset,
                        markedOffsets = marks.map { it.characterOffset },
                        markedOnly = state.showMarkedOnly,
                        highlightStart = ttsHighlightStart,
                        highlightEnd = ttsHighlightEnd,
                        onPageChanged = onVisiblePageChanged,
                        onReadFromSelection = onStartReadingFromText,
                        onRenderedText = { renderedText = it },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomFileNavButton(onClick = onPreviousFile) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Libro anterior")
                }
                if (!state.isPdfContent) {
                    CompactControlButton(onClick = onToggleContinuousPlayback) {
                        Text(if (continuousPlayback) "Lectura continua ✓" else "Lectura continua")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onSpeechRateChange(speechRate - 0.1f) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Remove, "Reducir velocidad", modifier = Modifier.size(17.dp))
                        }
                        Text("${"%.1f".format(speechRate)}×")
                        IconButton(
                            onClick = { onSpeechRateChange(speechRate + 0.1f) },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(Icons.Default.Add, "Aumentar velocidad", modifier = Modifier.size(17.dp))
                        }
                    }
                } else {
                    Text("${marks.size} post-it", style = MaterialTheme.typography.labelLarge)
                }
                BottomFileNavButton(onClick = onNextFile) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Libro siguiente")
                }
            }
        }
    }
}

@Composable
private fun BottomFileNavButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
            content()
        }
    }
}

@Composable
private fun CompactControlButton(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 0.dp),
        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 0.dp),
        content = content
    )
}
