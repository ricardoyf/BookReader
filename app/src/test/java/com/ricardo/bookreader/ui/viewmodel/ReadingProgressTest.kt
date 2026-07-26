package com.ricardo.bookreader.ui.viewmodel

import com.ricardo.bookreader.model.ReaderPageSnapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingProgressTest {
    @Test
    fun incompleteBookIsNotMarkedAsReadWhenSwitching() {
        assertFalse(isBookComplete(ReaderPageSnapshot(pageNumber = 4, totalPages = 5)))
    }

    @Test
    fun lastPageCompletesBook() {
        assertTrue(isBookComplete(ReaderPageSnapshot(pageNumber = 5, totalPages = 5)))
    }

    @Test
    fun missingPageStateNeverCompletesBook() {
        assertFalse(isBookComplete(null))
    }
}
