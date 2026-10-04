package com.manoogianmedia.studiorack.ui

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

internal data class CalendarDay(
    val date: LocalDate,
    val inMonth: Boolean,
    val isToday: Boolean,
    val events: List<JSONObject>,
)

internal fun buildCalendarMonth(
    month: YearMonth,
    events: List<JSONObject>,
    sundayFirst: Boolean,
    today: LocalDate = LocalDate.now(),
): List<CalendarDay> {
    val first = month.atDay(1)
    val firstDay = if (sundayFirst) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
    val leadingDays = (first.dayOfWeek.value - firstDay.value + 7) % 7
    val gridStart = first.minusDays(leadingDays.toLong())
    return (0 until 42).map { offset ->
        val date = gridStart.plusDays(offset.toLong())
        CalendarDay(
            date = date,
            inMonth = YearMonth.from(date) == month,
            isToday = date == today,
            events = events.filter { eventOccursOn(it, date) }
                .sortedWith(compareBy<JSONObject>({ it.optString("start_time") }, { it.optString("title").lowercase() })),
        )
    }
}

internal fun eventOccursOn(event: JSONObject, date: LocalDate): Boolean {
    val start = event.optString("event_date").asIsoDateOrNull() ?: return false
    val end = event.optString("end_date").asIsoDateOrNull() ?: start
    return !date.isBefore(start) && !date.isAfter(if (end.isBefore(start)) start else end)
}

internal fun validateEventWindow(
    startDate: String,
    startTime: String,
    endDate: String,
    endTime: String,
    allDay: Boolean,
): String? {
    val start = startDate.asIsoDateOrNull() ?: return "Enter a valid start date."
    val end = endDate.ifBlank { startDate }.asIsoDateOrNull() ?: return "Enter a valid end date."
    if (end.isBefore(start)) return "The end date cannot be before the start date."
    if (allDay) return null
    val parsedStartTime = startTime.asTimeOrNull()
    val parsedEndTime = endTime.asTimeOrNull()
    if (startTime.isNotBlank() && parsedStartTime == null) return "Enter a valid start time."
    if (endTime.isNotBlank() && parsedEndTime == null) return "Enter a valid end time."
    if (start == end && parsedStartTime != null && parsedEndTime != null && !parsedEndTime.isAfter(parsedStartTime)) {
        return "The end time must be after the start time."
    }
    return null
}

internal fun buildEventIcs(
    event: JSONObject,
    displayLocation: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    val startDate = event.optString("event_date").asIsoDateOrNull() ?: LocalDate.now(zoneId)
    val endDate = event.optString("end_date").asIsoDateOrNull()?.takeUnless { it.isBefore(startDate) } ?: startDate
    val allDay = event.optInt("all_day") == 1 || event.optString("start_time").isBlank()
    val dateLines = if (allDay) {
        listOf(
            "DTSTART;VALUE=DATE:${startDate.format(ICS_DATE)}",
            "DTEND;VALUE=DATE:${endDate.plusDays(1).format(ICS_DATE)}",
        )
    } else {
        val startTime = event.optString("start_time").asTimeOrNull() ?: LocalTime.MIDNIGHT
        val requestedEnd = event.optString("end_time").asTimeOrNull()
        val start = LocalDateTime.of(startDate, startTime).atZone(zoneId)
        val end = if (requestedEnd == null) start.plusHours(1) else LocalDateTime.of(endDate, requestedEnd).atZone(zoneId)
        val safeEnd = if (end.isAfter(start)) end else start.plusHours(1)
        listOf(
            "DTSTART:${start.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_DATE_TIME)}",
            "DTEND:${safeEnd.withZoneSameInstant(ZoneId.of("UTC")).format(ICS_DATE_TIME)}",
        )
    }
    val importance = when (event.optString("importance", "normal")) {
        "critical" -> "1"
        "important" -> "5"
        else -> "9"
    }
    val uid = event.optString("calendar_uid").ifBlank {
        "${event.optString("id", "event-local")}@studioleviathan.com"
    }
    return buildList {
        add("BEGIN:VCALENDAR")
        add("VERSION:2.0")
        add("CALSCALE:GREGORIAN")
        add("METHOD:PUBLISH")
        add("PRODID:-//Manoogian Media//Studio Leviathan Calendar//EN")
        add("BEGIN:VEVENT")
        add("UID:${icsEscape(uid)}")
        add("SEQUENCE:${event.optInt("calendar_revision", 0).coerceAtLeast(0)}")
        add("DTSTAMP:${java.time.ZonedDateTime.now(ZoneId.of("UTC")).format(ICS_DATE_TIME)}")
        addAll(dateLines)
        add("SUMMARY:${icsEscape(event.optString("title", "Studio Leviathan Event"))}")
        add("LOCATION:${icsEscape(displayLocation)}")
        add("DESCRIPTION:${icsEscape(event.optString("notes"))}")
        add("CATEGORIES:${icsEscape(event.optString("event_type", "event").replace('_', ' ').titleCaseWords())}")
        add("CLASS:${if (event.optInt("is_private") == 1) "PRIVATE" else "PUBLIC"}")
        add("PRIORITY:$importance")
        add("END:VEVENT")
        add("END:VCALENDAR")
        add("")
    }.joinToString("\r\n")
}

internal fun eventCalendarFileName(event: JSONObject): String {
    val safeTitle = event.optString("title", "studio-leviathan-event")
        .lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "studio-leviathan-event" }
    return "$safeTitle.ics"
}

internal fun eventDateSummary(event: JSONObject): String {
    val start = event.optString("event_date")
    val end = event.optString("end_date").takeIf { it.isNotBlank() && it != start }
    val dates = if (end == null) start else "$start through $end"
    val time = if (event.optInt("all_day") == 1 || event.optString("start_time").isBlank()) "All day" else event.optString("start_time")
    return listOf(dates, time).filter(String::isNotBlank).joinToString(" | ")
}

private fun String.asIsoDateOrNull(): LocalDate? = try {
    LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
} catch (_: DateTimeParseException) {
    null
}

private fun String.asTimeOrNull(): LocalTime? {
    val clean = trim()
    if (clean.isBlank()) return null
    return listOf("H:mm", "HH:mm", "H:mm:ss", "HH:mm:ss").firstNotNullOfOrNull { pattern ->
        try {
            LocalTime.parse(clean, DateTimeFormatter.ofPattern(pattern))
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

private fun icsEscape(value: String): String = value
    .replace("\\", "\\\\")
    .replace("\r\n", "\\n")
    .replace("\n", "\\n")
    .replace(",", "\\,")
    .replace(";", "\\;")

private fun String.titleCaseWords(): String = split(' ').joinToString(" ") { word ->
    word.lowercase().replaceFirstChar { it.titlecase() }
}

private val ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
private val ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
