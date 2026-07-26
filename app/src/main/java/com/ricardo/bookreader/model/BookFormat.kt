package com.ricardo.bookreader.model

enum class BookFormat {
    TEXT,
    MARKDOWN,
    PDF,
    UNSUPPORTED
}

fun detectBookFormat(name: String?, mimeType: String?): BookFormat {
    val normalizedName = name?.trim()?.lowercase().orEmpty()
    val normalizedMime = mimeType?.trim()?.lowercase().orEmpty()
    return when {
        normalizedName.endsWith(".pdf") || normalizedMime == "application/pdf" ->
            BookFormat.PDF
        normalizedName.endsWith(".md") ||
            normalizedName.endsWith(".markdown") ||
            normalizedMime == "text/markdown" ->
            BookFormat.MARKDOWN
        normalizedName.endsWith(".txt") || normalizedMime == "text/plain" ->
            BookFormat.TEXT
        else ->
            BookFormat.UNSUPPORTED
    }
}
