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

private data class ChordProSectionMarker(
    val type: String,
    val explicitName: String,
    val atMs: Long?,
    val endMs: Long?,
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

internal fun parseChordProTimeline(content: String, durationMs: Long? = null): List<TimedSongSection> {
    val markers = mutableListOf<ChordProSectionMarker>()
    var pendingStart: Long? = null
    var pendingEnd: Long? = null
    content.replace("\r\n", "\n").lineSequence().forEach { rawLine ->
        val directive = parseChordProDirective(rawLine) ?: return@forEach
        val name = directive.first
        val argument = directive.second
        if (name == "x_leviathan_time") {
            val timing = Regex("^\\s*(\\d+:\\d{2})(?:\\s*-\\s*(\\d+:\\d{2}))?\\s*$").matchEntire(argument)
            pendingStart = timing?.groupValues?.get(1)?.let(::chordProTimeMs)
            pendingEnd = timing?.groupValues?.get(2)?.takeIf(String::isNotBlank)?.let(::chordProTimeMs)
            return@forEach
        }
        val type = when (name) {
            "sov" -> "verse"
            "soc" -> "chorus"
            "sob" -> "bridge"
            "sot" -> "tab"
            else -> name.takeIf { it.startsWith("start_of_") }?.removePrefix("start_of_")
        } ?: return@forEach
        if (type in setOf("abc", "grid", "ly", "svg", "textblock")) {
            pendingStart = null
            pendingEnd = null
            return@forEach
        }
        val labelMatch = Regex("(?:^|\\s)label\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s]+))", RegexOption.IGNORE_CASE).find(argument)
        val explicitLabel = labelMatch?.groupValues?.drop(1)?.firstOrNull(String::isNotBlank)
            ?: argument.takeIf { it.isNotBlank() && '=' !in it }?.trim('"', '\'').orEmpty()
        markers += ChordProSectionMarker(type, explicitLabel, pendingStart, pendingEnd)
        pendingStart = null
        pendingEnd = null
    }
    val totals = markers.groupingBy(ChordProSectionMarker::type).eachCount()
    val seen = mutableMapOf<String, Int>()
    val named = markers.map { marker ->
        val position = seen.getOrDefault(marker.type, 0) + 1
        seen[marker.type] = position
        val base = marker.type.replace('_', ' ').replaceFirstChar(Char::uppercase)
        val sectionName = marker.explicitName.ifBlank { if (totals[marker.type] == 1) base else "$base $position" }
        marker to sectionName
    }.filter { it.first.atMs != null }
    return named.mapIndexed { index, (marker, sectionName) ->
        val nextStart = named.getOrNull(index + 1)?.first?.atMs
        TimedSongSection(marker.atMs!!, marker.endMs ?: nextStart ?: durationMs?.takeIf { it > marker.atMs }, sectionName)
    }
}

private fun chordProTimeMs(value: String): Long? {
    val parts = value.trim().split(':')
    if (parts.size != 2) return null
    val minutes = parts[0].toLongOrNull() ?: return null
    val seconds = parts[1].toLongOrNull()?.takeIf { it in 0..59 } ?: return null
    return (minutes * 60L + seconds) * 1_000L
}

internal fun activeSongSection(sections: List<TimedSongSection>, positionMs: Long): TimedSongSection? {
    val ordered = sections.sortedBy(TimedSongSection::atMs)
    return ordered.indexOfLast { it.atMs <= positionMs }
        .takeIf { it >= 0 }
        ?.let { index -> ordered[index].takeIf { section -> section.endMs == null || positionMs < section.endMs } }
}
