package com.ricardo.bookreader.ui.components

import android.graphics.Typeface
import android.graphics.Paint
import android.text.SpannableString
import android.text.StaticLayout
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.widget.TextView
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ricardo.bookreader.model.ReaderPageSnapshot
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlin.math.roundToInt

@Composable
fun PagedTextReader(
    text: String,
    isMarkdown: Boolean,
    fontSizeSp: Float,
    initialOffset: Int,
    markedOffsets: List<Int>,
    markedOnly: Boolean,
    highlightStart: Int?,
    highlightEnd: Int?,
    onPageChanged: (ReaderPageSnapshot, String) -> Unit,
    onReadFromSelection: (String, Int) -> Unit,
    onRenderedText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val markwon = remember {
        Markwon.builder(context)
            .usePlugin(TablePlugin.create(context))
            .build()
    }
    val styledText = remember(text, isMarkdown) {
        if (isMarkdown) markwon.toMarkdown(text) else SpannableString(text)
    }
    val plainText = remember(styledText) { styledText.toString() }
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f).toArgb()

    LaunchedEffect(plainText) {
        onRenderedText(plainText)
    }

    var pages by remember(styledText) { mutableStateOf(emptyList<TextPageRange>()) }
    var pageIndex by remember(styledText) { mutableIntStateOf(0) }
    var positioningPage by remember(styledText) { mutableStateOf(true) }
    val markedPageIndices = remember(pages, markedOffsets) {
        pages.indices.filter { index ->
            markedOffsets.any { offset ->
                offset in pages[index].start until
                    pages[index].end.coerceAtLeast(pages[index].start + 1)
            }
        }
    }
    val visibleIndices = if (markedOnly) markedPageIndices else pages.indices.toList()

    LaunchedEffect(pages, initialOffset, markedOnly, markedPageIndices) {
        if (visibleIndices.isEmpty()) {
            positioningPage = false
            return@LaunchedEffect
        }
        positioningPage = true
        val target = pages.indexOfFirst {
            initialOffset in it.start until it.end.coerceAtLeast(it.start + 1)
        }.takeIf { it >= 0 } ?: 0
        pageIndex = if (target in visibleIndices) {
            target
        } else {
            visibleIndices.minByOrNull { kotlin.math.abs(it - target) } ?: visibleIndices.first()
        }
        positioningPage = false
    }

    LaunchedEffect(pageIndex, pages, markedOnly, markedPageIndices, positioningPage) {
        if (positioningPage) return@LaunchedEffect
        val range = pages.getOrNull(pageIndex) ?: return@LaunchedEffect
        onPageChanged(
            ReaderPageSnapshot(
                pageNumber = pageIndex + 1,
                totalPages = pages.size,
                startOffset = range.start,
                endOffset = range.end,
                pageText = plainText.substring(range.start, range.end),
                isPdf = false
            ),
            plainText
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val horizontalPaddingPx = with(density) { 38.dp.toPx().roundToInt() }
            val verticalPaddingPx = with(density) { 24.dp.toPx().roundToInt() }
            val widthPx = with(density) { maxWidth.toPx().roundToInt() }
                .minus(horizontalPaddingPx)
                .coerceAtLeast(1)
            val heightPx = with(density) { maxHeight.toPx().roundToInt() }
                .minus(verticalPaddingPx)
                .coerceAtLeast(1)
            val textSizePx = with(density) { fontSizeSp.sp.toPx() }

            val calculatedPages = remember(styledText, widthPx, heightPx, textSizePx, textColor) {
                paginateText(
                    text = styledText,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    textSizePx = textSizePx,
                    textColor = textColor
                )
            }
            LaunchedEffect(calculatedPages) {
                positioningPage = true
                pages = calculatedPages
            }

            if (pages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (visibleIndices.isEmpty()) {
                Text(
                    text = "Este libro todavía no tiene páginas marcadas.",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(28.dp)
                )
            } else {
                val safeIndex = pageIndex.takeIf { it in visibleIndices } ?: visibleIndices.first()
                val page = pages[safeIndex]
                val localHighlightStart = highlightStart
                    ?.takeIf { it < page.end && (highlightEnd ?: it) > page.start }
                    ?.minus(page.start)
                    ?.coerceAtLeast(0)
                val localHighlightEnd = highlightEnd
                    ?.takeIf { localHighlightStart != null }
                    ?.minus(page.start)
                    ?.coerceIn(localHighlightStart ?: 0, page.end - page.start)
                val displayText = styledText.subSequence(page.start, page.end)
                    .withOptionalHighlight(localHighlightStart, localHighlightEnd, highlightColor)

                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 19.dp, vertical = 12.dp)
                        .pointerInput(visibleIndices, safeIndex) {
                            var drag = 0f
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, amount -> drag += amount },
                                onDragEnd = {
                                    val position = visibleIndices.indexOf(safeIndex)
                                    if (drag < -70f && position < visibleIndices.lastIndex) {
                                        pageIndex = visibleIndices[position + 1]
                                    } else if (drag > 70f && position > 0) {
                                        pageIndex = visibleIndices[position - 1]
                                    }
                                    drag = 0f
                                }
                            )
                        },
                    factory = { ctx ->
                        TextView(ctx).apply {
                            setTextIsSelectable(true)
                            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                            setLineSpacing(0f, 1.28f)
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { view ->
                        view.textSize = fontSizeSp
                        view.setTextColor(textColor)
                        view.text = displayText
                        view.customSelectionActionModeCallback = readFromSelectionCallback(view) { _, localOffset ->
                            onReadFromSelection(plainText, page.start + localOffset)
                        }
                    }
                )
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
            if (pages.isEmpty()) {
                CircularProgressIndicator()
            } else {
                Text(
                    if (markedOnly && visibleIndices.isNotEmpty()) {
                        "Marcada ${navigationIndex + 1} de ${visibleIndices.size} · pág. ${pageIndex + 1}/${pages.size}"
                    } else {
                        "Página ${pageIndex + 1} de ${pages.size}"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }
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

private fun paginateText(
    text: CharSequence,
    widthPx: Int,
    heightPx: Int,
    textSizePx: Float,
    textColor: Int
): List<TextPageRange> {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        color = textColor
        typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
    }
    val layout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, widthPx)
        .setIncludePad(true)
        .setLineSpacing(0f, 1.28f)
        .build()
    val starts = IntArray(layout.lineCount) { layout.getLineStart(it) }
    val ends = IntArray(layout.lineCount) { layout.getLineEnd(it) }
    val bottoms = IntArray(layout.lineCount) { layout.getLineBottom(it) }
    return pageRangesFromLines(starts, ends, bottoms, heightPx, text.length)
}
