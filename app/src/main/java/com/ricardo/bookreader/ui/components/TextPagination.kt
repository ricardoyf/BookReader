package com.ricardo.bookreader.ui.components

data class TextPageRange(
    val start: Int,
    val end: Int
)

fun pageRangesFromLines(
    lineStarts: IntArray,
    lineEnds: IntArray,
    lineBottoms: IntArray,
    pageHeight: Int,
    textLength: Int
): List<TextPageRange> {
    if (textLength == 0 || lineStarts.isEmpty()) return listOf(TextPageRange(0, textLength))
    require(lineStarts.size == lineEnds.size && lineEnds.size == lineBottoms.size)
    val usableHeight = pageHeight.coerceAtLeast(1)
    val pages = mutableListOf<TextPageRange>()
    var firstLine = 0

    while (firstLine < lineStarts.size) {
        val pageTop = if (firstLine == 0) 0 else lineBottoms[firstLine - 1]
        val pageBottom = pageTop + usableHeight
        var lastLine = firstLine
        while (
            lastLine + 1 < lineStarts.size &&
            lineBottoms[lastLine + 1] <= pageBottom
        ) {
            lastLine += 1
        }
        pages += TextPageRange(
            start = lineStarts[firstLine].coerceIn(0, textLength),
            end = lineEnds[lastLine].coerceIn(lineStarts[firstLine], textLength)
        )
        firstLine = lastLine + 1
    }
    return pages
}
