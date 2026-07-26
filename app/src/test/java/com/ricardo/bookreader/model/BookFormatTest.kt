package com.ricardo.bookreader.model

import org.junit.Assert.assertEquals
import org.junit.Test

class BookFormatTest {
    @Test
    fun detectsSupportedExtensionsCaseInsensitively() {
        assertEquals(BookFormat.TEXT, detectBookFormat("Libro.TXT", null))
        assertEquals(BookFormat.MARKDOWN, detectBookFormat("Notas.MD", null))
        assertEquals(BookFormat.MARKDOWN, detectBookFormat("Manual.Markdown", null))
        assertEquals(BookFormat.PDF, detectBookFormat("Revista.PDF", null))
    }

    @Test
    fun detectsFormatsFromMimeWhenExtensionIsMissing() {
        assertEquals(BookFormat.TEXT, detectBookFormat("documento", "text/plain"))
        assertEquals(BookFormat.MARKDOWN, detectBookFormat("documento", "text/markdown"))
        assertEquals(BookFormat.PDF, detectBookFormat("documento", "application/pdf"))
    }

    @Test
    fun rejectsUnsupportedBooks() {
        assertEquals(BookFormat.UNSUPPORTED, detectBookFormat("portada.jpg", "image/jpeg"))
        assertEquals(BookFormat.UNSUPPORTED, detectBookFormat(null, null))
    }
}
