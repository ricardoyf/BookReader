package com.ricardo.bookreader.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ricardo.bookreader.model.ReaderPageSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfBookView(
    uri: Uri,
    initialPage: Int,
    markedPages: List<Int>,
    markedOnly: Boolean,
    onPageChanged: (ReaderPageSnapshot, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var handleResult by remember(uri) { mutableStateOf<Result<PdfHandle>?>(null) }

    LaunchedEffect(uri) {
        handleResult = withContext(Dispatchers.IO) {
            runCatching { openPdfHandle(context.applicationContext, uri) }
        }
    }
    val handle = handleResult?.getOrNull()
    DisposableEffect(handle) {
        onDispose { handle?.close() }
    }

    if (handleResult == null) {
        Box(modifier = modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        return
    }
    if (handle == null) {
        EmptyStateCard(
            title = "No se pudo abrir el PDF",
            description = "El archivo puede estar dañado, cifrado o ya no estar disponible.",
            modifier = modifier.padding(20.dp)
        )
        return
    }

    val pageCount = handle.renderer.pageCount
    if (pageCount <= 0) {
        EmptyStateCard(
            title = "PDF vacío",
            description = "El documento no contiene páginas renderizables.",
            modifier = modifier.padding(20.dp)
        )
        return
    }

    val markedIndices = remember(markedPages, pageCount) {
        markedPages.map { it - 1 }.filter { it in 0 until pageCount }.distinct().sorted()
    }
    val visibleIndices = if (markedOnly) markedIndices else (0 until pageCount).toList()
    var pageIndex by remember(uri) {
        mutableIntStateOf(initialPage.coerceIn(0, pageCount - 1))
    }

    LaunchedEffect(initialPage, pageCount, markedOnly, markedIndices) {
        val target = initialPage.coerceIn(0, pageCount - 1)
        pageIndex = if (visibleIndices.isEmpty()) {
            target
        } else if (target in visibleIndices) {
            target
        } else {
            visibleIndices.minByOrNull { kotlin.math.abs(it - target) } ?: visibleIndices.first()
        }
    }
    LaunchedEffect(pageIndex) {
        onPageChanged(
            ReaderPageSnapshot(
                pageNumber = pageIndex + 1,
                totalPages = pageCount,
                isPdf = true
            ),
            ""
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            if (visibleIndices.isEmpty()) {
                Text(
                    text = "Este PDF todavía no tiene páginas marcadas.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp)
                )
            } else {
                val density = LocalDensity.current
                val targetWidth = with(density) { maxWidth.toPx().roundToInt() }.coerceAtLeast(1)
                val targetHeight = with(density) { maxHeight.toPx().roundToInt() }.coerceAtLeast(1)
                var bitmap by remember(uri, pageIndex, targetWidth, targetHeight) {
                    mutableStateOf<Bitmap?>(null)
                }
                var renderError by remember(uri, pageIndex) { mutableStateOf(false) }

                LaunchedEffect(handle, pageIndex, targetWidth, targetHeight) {
                    bitmap = null
                    renderError = false
                    val rendered = withContext(Dispatchers.IO) {
                        runCatching {
                            synchronized(handle.renderer) {
                                handle.renderer.openPage(pageIndex).use { page ->
                                    val scale = minOf(
                                        targetWidth.toFloat() / page.width,
                                        targetHeight.toFloat() / page.height
                                    )
                                    val width = (page.width * scale).roundToInt().coerceAtLeast(1)
                                    val height = (page.height * scale).roundToInt().coerceAtLeast(1)
                                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { output ->
                                        Canvas(output).drawColor(Color.WHITE)
                                        page.render(output, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    }
                                }
                            }
                        }
                    }
                    bitmap = rendered.getOrNull()
                    renderError = rendered.isFailure
                }

                when {
                    renderError -> Text(
                        "No se pudo renderizar esta página.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    bitmap == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    else -> Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Página ${pageIndex + 1}",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .pointerInput(visibleIndices, pageIndex) {
                                var drag = 0f
                                detectHorizontalDragGestures(
                                    onHorizontalDrag = { _, amount -> drag += amount },
                                    onDragEnd = {
                                        val position = visibleIndices.indexOf(pageIndex)
                                        if (drag < -70f && position < visibleIndices.lastIndex) {
                                            pageIndex = visibleIndices[position + 1]
                                        } else if (drag > 70f && position > 0) {
                                            pageIndex = visibleIndices[position - 1]
                                        }
                                        drag = 0f
                                    }
                                )
                            },
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        val navigationIndex = visibleIndices.indexOf(pageIndex).coerceAtLeast(0)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (navigationIndex > 0) pageIndex = visibleIndices[navigationIndex - 1]
                },
                enabled = visibleIndices.isNotEmpty() && navigationIndex > 0
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Página anterior")
            }
            Text(
                if (markedOnly && visibleIndices.isNotEmpty()) {
                    "Marcada ${navigationIndex + 1} de ${visibleIndices.size} · pág. ${pageIndex + 1}/$pageCount"
                } else {
                    "Página ${pageIndex + 1} de $pageCount"
                },
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                onClick = {
                    if (navigationIndex < visibleIndices.lastIndex) {
                        pageIndex = visibleIndices[navigationIndex + 1]
                    }
                },
                enabled = visibleIndices.isNotEmpty() && navigationIndex < visibleIndices.lastIndex
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Página siguiente")
            }
        }
    }
}

private fun openPdfHandle(context: Context, uri: Uri): PdfHandle {
    val resolver = context.contentResolver
    val directDescriptor = resolver.openFileDescriptor(uri, "r")
        ?: error("No se pudo abrir el PDF")
    try {
        return PdfHandle(directDescriptor, PdfRenderer(directDescriptor))
    } catch (_: Exception) {
        directDescriptor.close()
    }

    val cachedPdf = File(
        context.cacheDir,
        "bookreader_pdf_${uri.toString().hashCode().toUInt().toString(16)}.pdf"
    )
    resolver.openInputStream(uri)?.use { input ->
        cachedPdf.outputStream().buffered().use { output -> input.copyTo(output) }
    } ?: error("No se pudo copiar el PDF")

    val descriptor = ParcelFileDescriptor.open(cachedPdf, ParcelFileDescriptor.MODE_READ_ONLY)
    return PdfHandle(descriptor, PdfRenderer(descriptor))
}

private class PdfHandle(
    private val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    fun close() {
        runCatching { synchronized(renderer) { renderer.close() } }
        runCatching { descriptor.close() }
    }
}
