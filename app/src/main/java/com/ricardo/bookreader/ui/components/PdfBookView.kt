package com.ricardo.bookreader.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

@Composable
fun PdfBookView(
    uri: Uri,
    initialPage: Int,
    onPageChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var handleResult by remember(uri) {
        mutableStateOf<Result<PdfHandle>?>(null)
    }

    LaunchedEffect(uri) {
        handleResult = withContext(Dispatchers.IO) {
            runCatching {
                openPdfHandle(context.applicationContext, uri)
            }
        }
    }
    val handle = handleResult?.getOrNull()

    DisposableEffect(handle) {
        onDispose {
            handle?.close()
        }
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

    var pageIndex by remember(uri) {
        mutableIntStateOf(initialPage.coerceIn(0, pageCount - 1))
    }

    LaunchedEffect(initialPage, pageCount) {
        pageIndex = initialPage.coerceIn(0, pageCount - 1)
    }

    LaunchedEffect(pageIndex) {
        onPageChanged(pageIndex)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { pageIndex = (pageIndex - 1).coerceAtLeast(0) },
                enabled = pageIndex > 0
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Página anterior"
                )
            }
            Text(
                text = "Página ${pageIndex + 1} de $pageCount",
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(
                onClick = { pageIndex = (pageIndex + 1).coerceAtMost(pageCount - 1) },
                enabled = pageIndex < pageCount - 1
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Página siguiente"
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
        ) {
            val density = LocalDensity.current
            val targetWidth = with(density) {
                maxWidth.toPx().roundToInt().coerceAtLeast(1)
            }
            var bitmap by remember(uri, pageIndex, targetWidth) {
                mutableStateOf<Bitmap?>(null)
            }
            var renderError by remember(uri, pageIndex) {
                mutableStateOf(false)
            }
            val pageScroll = rememberScrollState()

            LaunchedEffect(handle, pageIndex, targetWidth) {
                bitmap = null
                renderError = false
                pageScroll.scrollTo(0)
                val rendered = withContext(Dispatchers.IO) {
                    runCatching {
                        synchronized(handle.renderer) {
                            handle.renderer.openPage(pageIndex).use { page ->
                                val scale = targetWidth.toFloat() / page.width.toFloat()
                                val targetHeight = (page.height * scale)
                                    .roundToInt()
                                    .coerceAtLeast(1)
                                val output = Bitmap.createBitmap(
                                    targetWidth,
                                    targetHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                Canvas(output).drawColor(Color.WHITE)
                                page.render(
                                    output,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                                output
                            }
                        }
                    }
                }
                bitmap = rendered.getOrNull()
                renderError = rendered.isFailure
            }

            when {
                renderError -> {
                    Text(
                        text = "No se pudo renderizar esta página.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(20.dp)
                    )
                }
                bitmap == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(pageScroll),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            bitmap = bitmap!!.asImageBitmap(),
                            contentDescription = "Página ${pageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
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
        cachedPdf.outputStream().buffered().use { output ->
            input.copyTo(output)
        }
    } ?: error("No se pudo copiar el PDF")

    val cachedDescriptor = ParcelFileDescriptor.open(
        cachedPdf,
        ParcelFileDescriptor.MODE_READ_ONLY
    )
    try {
        return PdfHandle(cachedDescriptor, PdfRenderer(cachedDescriptor))
    } catch (cachedError: Exception) {
        cachedDescriptor.close()
        throw cachedError
    }
}

private class PdfHandle(
    private val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    fun close() {
        runCatching {
            synchronized(renderer) {
                renderer.close()
            }
        }
        runCatching { descriptor.close() }
    }
}
