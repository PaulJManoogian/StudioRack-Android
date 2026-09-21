package com.manoogianmedia.studiorack.ui

private val lrcTimestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")

internal data class TimedLyricLine(
    val atMs: Long,
    val text: String,
)

internal data class TimedSongSection(
    val atMs: Long,
    val endMs: Long?,
    val name: String,
)

internal fun parseLrcTimeline(content: String): List<TimedLyricLine> = content.lineSequence()
    .flatMap { rawLine ->
        val timestamps = lrcTimestamp.findAll(rawLine).toList()
        val text = lrcTimestamp.replace(rawLine, "").trim()
        if (text.isBlank()) emptySequence()
        else timestamps.asSequence().map { match ->
            val minutes = match.groupValues[1].toLongOrNull() ?: 0L
            val seconds = match.groupValues[2].toLongOrNull() ?: 0L
            val fraction = match.groupValues[3].take(3).padEnd(3, '0').toLongOrNull() ?: 0L
            TimedLyricLine(((minutes * 60L) + seconds) * 1_000L + fraction, text)
        }
    }
    .sortedBy(TimedLyricLine::atMs)
    .toList()

internal fun activeLyricIndex(lines: List<TimedLyricLine>, positionMs: Long): Int =
    lines.indexOfLast { it.atMs <= positionMs }

internal fun activeSongSection(sections: List<TimedSongSection>, positionMs: Long): TimedSongSection? {
    val ordered = sections.sortedBy(TimedSongSection::atMs)
    return ordered.indexOfLast { it.atMs <= positionMs }
        .takeIf { it >= 0 }
        ?.let { index -> ordered[index].takeIf { section -> section.endMs == null || positionMs < section.endMs } }
}
