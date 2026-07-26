package com.ricardo.bookreader.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class TextPaginationTest {
    @Test
    fun dividesLinesWithoutLosingCharacters() {
        val pages = pageRangesFromLines(
            lineStarts = intArrayOf(0, 10, 20, 30, 40),
            lineEnds = intArrayOf(10, 20, 30, 40, 50),
            lineBottoms = intArrayOf(20, 40, 60, 80, 100),
            pageHeight = 45,
            textLength = 50
        )

        assertEquals(
            listOf(
                TextPageRange(0, 20),
                TextPageRange(20, 40),
                TextPageRange(40, 50)
            ),
            pages
        )
    }

    @Test
    fun keepsAtLeastOneLineWhenLineIsTallerThanPage() {
        val pages = pageRangesFromLines(
            lineStarts = intArrayOf(0, 12),
            lineEnds = intArrayOf(12, 24),
            lineBottoms = intArrayOf(80, 160),
            pageHeight = 40,
            textLength = 24
        )

        assertEquals(
            listOf(TextPageRange(0, 12), TextPageRange(12, 24)),
            pages
        )
    }

    @Test
    fun emptyTextStillProducesOnePage() {
        assertEquals(
            listOf(TextPageRange(0, 0)),
            pageRangesFromLines(
                lineStarts = intArrayOf(),
                lineEnds = intArrayOf(),
                lineBottoms = intArrayOf(),
                pageHeight = 500,
                textLength = 0
            )
        )
    }
}
