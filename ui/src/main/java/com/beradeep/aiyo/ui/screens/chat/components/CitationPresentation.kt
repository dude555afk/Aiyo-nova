package com.beradeep.aiyo.ui.screens.chat.components

import java.net.URI

internal data class CitationSource(
    val number: Int,
    val title: String,
    val url: String,
    val domain: String
)

internal data class CitationPresentation(
    val markdown: String,
    val sources: List<CitationSource>
)

/**
 * Extracts the persisted web-search footer and turns inline [n] references into real
 * Markdown links. The source footer is stored in the message body so it survives the
 * existing Room schema without a database migration.
 */
internal fun presentCitations(rawContent: String): CitationPresentation {
    val markedStart = "<!--AIYO_SOURCES_START-->"
    val markedEnd = "<!--AIYO_SOURCES_END-->"
    val start = rawContent.lastIndexOf(markedStart)
    val end = if (start >= 0) rawContent.indexOf(markedEnd, start + markedStart.length) else -1

    val body: String
    val sources: List<CitationSource>

    if (start >= 0 && end >= 0) {
        body = rawContent.substring(0, start).trimEnd()
        sources = parseSourceLines(rawContent.substring(start + markedStart.length, end))
    } else {
        // Backwards compatibility for messages saved before the structured footer existed.
        val headingPattern = Regex(
            "(?im)^\\s*(?:\\*{1,2}\\s*)?(?:sources|references)(?:\\s*\\*{1,2})?\\s*:?\\s*$"
        )
        val headings = headingPattern.findAll(rawContent).toList()
        val sourceHeading = headings.lastOrNull { heading ->
            parseSourceLines(rawContent.substring(heading.range.last + 1)).isNotEmpty()
        }
        if (sourceHeading == null) {
            body = rawContent
            sources = emptyList()
        } else {
            // Use the first heading as the cut point to hide duplicate/model-authored lists,
            // while taking the actual URLs from the final, app-generated list.
            val cutAt = headings.first().range.first
            body = rawContent.substring(0, cutAt).trimEnd()
            sources = parseSourceLines(rawContent.substring(sourceHeading.range.last + 1))
        }
    }

    return CitationPresentation(
        markdown = linkInlineCitations(body, sources),
        sources = sources
    )
}

private fun parseSourceLines(text: String): List<CitationSource> {
    val linePattern = Regex(
        "(?m)^\\s*(?:[-*]\\s*)?\\[(\\d{1,2})]\\s*\\[(.+?)]\\((https?://[^)\\s]+)\\)\\s*$"
    )
    return linePattern.findAll(text).mapNotNull { match ->
        val number = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
        val title = match.groupValues[2].trim()
        val url = match.groupValues[3].trim()
        val domain = runCatching { URI(url).host.orEmpty() }
            .getOrDefault("")
            .removePrefix("www.")
        if (title.isBlank() || domain.isBlank()) return@mapNotNull null
        CitationSource(number, title, url, domain)
    }.distinctBy { it.number }.sortedBy { it.number }.toList()
}

private fun linkInlineCitations(
    markdown: String,
    sources: List<CitationSource>
): String {
    if (sources.isEmpty() || markdown.isBlank()) return markdown

    val sourceUrls = sources.associate { it.number to it.url }
    val protectedMarkdown = Regex(
        "\\x60{3}[\\s\\S]*?\\x60{3}|\\x60[^\\x60\\n]*\\x60|!?\\[[^\\]]*]\\([^\\n)]*\\)"
    )
    val citationPattern = Regex("(?<![A-Za-z0-9_])\\[(\\d{1,2})](?!\\s*\\()")

    fun replaceCitations(segment: String): String = citationPattern.replace(segment) { match ->
        val number = match.groupValues[1]
        val url = sourceUrls[number.toIntOrNull()] ?: return@replace match.value
        "[$number](" + url.replace(" ", "%20").replace(")", "%29") + ")"
    }

    val protectedMatches = protectedMarkdown.findAll(markdown).toList()
    if (protectedMatches.isEmpty()) return replaceCitations(markdown)

    return buildString(markdown.length + sources.size * 24) {
        var cursor = 0
        protectedMatches.forEach { match ->
            append(replaceCitations(markdown.substring(cursor, match.range.first)))
            append(match.value)
            cursor = match.range.last + 1
        }
        append(replaceCitations(markdown.substring(cursor)))
    }
}
