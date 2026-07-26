package com.ricardo.bookreader.ui.viewmodel

import com.ricardo.bookreader.model.ReaderPageSnapshot

internal fun isBookComplete(page: ReaderPageSnapshot?): Boolean =
    page != null && page.totalPages > 0 && page.pageNumber >= page.totalPages
